package dev.dong4j.zeka.stack.idea.plugin.common.codefree;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

import dev.dong4j.zeka.stack.idea.plugin.common.config.CodefreeAgentSettings;
import lombok.SneakyThrows;

/**
 * 管理 Codefree 本地代理的下载与启动
 */
@Service(Service.Level.APP)
public final class CodefreeAgentManager {
    private static final Logger LOG = Logger.getInstance(CodefreeAgentManager.class);
    public static final String DEFAULT_JAR_NAME = "codefree-agent.jar";
    public static final String DEFAULT_JAR_PREFIX = "codefree-agent";
    public static final String DEFAULT_OPENAI_ENDPOINT = System.getProperty("codefree.agent.api", "http://127.0.0.1:10011/v1");
    private static final String VERSION_ENDPOINT = "https://download.dong4j.site/codefree/version";
    private static final String DOWNLOAD_BASE = "https://download.dong4j.site/";

    private final Object processLock = new Object();
    @Nullable
    private OSProcessHandler processHandler;
    @Nullable
    private Path runningJarPath;

    /**
         * 已解析的本地 jar 信息
         */
        public record JarInfo(String fileName, Path path, long size) {
            public JarInfo(@NotNull String fileName, @NotNull Path path, long size) {
                this.fileName = fileName;
                this.path = path;
                this.size = size;
            }
        }

    @FunctionalInterface
    public interface DownloadProgressListener {
        void onProgress(long downloaded, long totalBytes);
    }

    @NotNull
    public static CodefreeAgentManager getInstance() {
        return ApplicationManager.getApplication().getService(CodefreeAgentManager.class);
    }

    /**
     * 获取最新可用的 jar 名称
     */
    @Nullable
    public String fetchLatestJarName() throws IOException {
        String version = HttpRequests.request(VERSION_ENDPOINT).productNameAsUserAgent().readString();
        if (version != null) {
            version = version.trim();
        }
        return version != null && !version.isEmpty() ? version : null;
    }

    /**
     * 获取远端 jar 大小
     */
    public long fetchRemoteJarSize(@NotNull String jarFileName) throws IOException {
        String url = buildDownloadUrl(jarFileName);
        return HttpRequests.request(url).productNameAsUserAgent().connect(request -> {
            URLConnection connection = request.getConnection();
            long length = connection.getContentLengthLong();
            return length > 0 ? length : -1;
        });
    }

    /**
     * 拼接远端下载地址
     */
    @NotNull
    public String buildDownloadUrl(@NotNull String jarFileName) {
        return DOWNLOAD_BASE + jarFileName;
    }

    /**
     * 从下载地址推导 jar 文件名
     */
    @Nullable
    public String deriveJarNameFromUrl(@NotNull String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path != null && path.contains("/")) {
                return path.substring(path.lastIndexOf('/') + 1);
            }
            return path;
        } catch (Exception e) {
            LOG.debug("无法从 url 解析 jar 名称: " + url, e);
            return null;
        }
    }

    /**
     * 下载 jar 文件
     */
    public Path downloadJar(@NotNull CodefreeAgentSettings settings,
                            @NotNull String jarFileName,
                            @NotNull ProgressIndicator indicator,
                            @Nullable DownloadProgressListener progressListener) throws IOException {
        String url = settings.downloadUrl != null ? settings.downloadUrl.trim() : "";
        if (url.isEmpty()) {
            throw new IOException("未配置下载地址");
        }
        if (isLocalPath(url)) {
            Path local = resolveJarPath(settings);
            if (Files.notExists(local)) {
                throw new IOException("本地 Jar 不存在: " + local);
            }
            return local;
        }
        Path jarPath = resolveDownloadTarget(jarFileName);
        Path parent = jarPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        indicator.setText("正在下载 Codefree 代理...");
        HttpRequests.request(url).productNameAsUserAgent().connect(request -> {
            URLConnection connection = request.getConnection();
            long total = connection.getContentLengthLong();
            indicator.setIndeterminate(total <= 0);
            try (InputStream inputStream = request.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(jarPath)) {
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    indicator.checkCanceled();
                    outputStream.write(buffer, 0, read);
                    downloaded += read;
                    if (total > 0) {
                        indicator.setFraction(Math.min(1.0, downloaded / (double) total));
                    }
                    if (progressListener != null) {
                        progressListener.onProgress(downloaded, total);
                    }
                }
            }
            return null;
        });
        return jarPath;
    }

    /**
     * 兼容旧调用，自动根据 URL 推导 jar 名称
     */
    public void downloadJar(@NotNull CodefreeAgentSettings settings, @NotNull ProgressIndicator indicator) throws IOException {
        String url = settings.downloadUrl != null ? settings.downloadUrl.trim() : "";
        String jarName = settings.jarFileName != null && !settings.jarFileName.isBlank()
                         ? settings.jarFileName
                         : Optional.ofNullable(deriveJarNameFromUrl(url)).orElse(DEFAULT_JAR_NAME);
        downloadJar(settings, jarName, indicator, null);
    }

    @NotNull
    private Path resolveDownloadTarget(@NotNull String jarFileName) {
        String targetName = jarFileName.isBlank() ? DEFAULT_JAR_NAME : jarFileName;
        return getWorkDir().resolve(targetName);
    }

    /**
     * 启动 jar
     *
     * @return 进程 PID, 未知则返回 -1
     */
    @SneakyThrows
    public long startAgent(@NotNull CodefreeAgentSettings settings) throws IOException {
        Path jarPath = resolveJarPath(settings);
        if (Files.notExists(jarPath)) {
            throw new IOException("Jar 不存在: " + jarPath);
        }
        Path javaPath = resolveJavaExecutable();
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(javaPath.toString());
        // macOS 隐藏 Dock 图标
        if (SystemInfo.isMac) {
            commandLine.addParameter("-Dapple.awt.UIElement=true");
        }
        commandLine.addParameters("-jar", jarPath.toString());
        if (jarPath.getParent() != null) {
            commandLine.setWorkDirectory(jarPath.getParent().toFile());
        }

        OSProcessHandler handler = new OSProcessHandler(commandLine);
        handler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                synchronized (processLock) {
                    processHandler = null;
                    runningJarPath = null;
                }
            }
        });
        handler.startNotify();
        synchronized (processLock) {
            processHandler = handler;
            runningJarPath = jarPath;
        }
        Process process = handler.getProcess();
        return process.pid();
    }

    /**
     * 停止运行中的代理
     */
    public void stopAgent() {
        synchronized (processLock) {
            if (processHandler != null) {
                try {
                    processHandler.destroyProcess();
                } catch (Exception e) {
                    LOG.warn("停止 Codefree 代理失败", e);
                }
                processHandler = null;
                runningJarPath = null;
            }
        }
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        synchronized (processLock) {
            return processHandler != null && !processHandler.isProcessTerminated();
        }
    }

    /**
     * 当前运行的 jar 路径
     */
    @Nullable
    public Path getRunningJarPath() {
        synchronized (processLock) {
            return runningJarPath;
        }
    }

    /**
     * 默认的本地 OpenAI API 地址
     */
    @NotNull
    public String getLocalOpenAiEndpoint() {
        return DEFAULT_OPENAI_ENDPOINT;
    }

    /**
     * 默认 jar 路径
     */
    @NotNull
    public Path getDefaultJarPath() {
        return getWorkDir().resolve(DEFAULT_JAR_NAME);
    }

    /**
     * 查找本地已经存在的 jar（按修改时间排序）
     */
    @Nullable
    public Path findExistingJar() {
        Path dir = getWorkDir();
        if (Files.notExists(dir)) {
            return null;
        }
        try (var stream = Files.list(dir)) {
            Optional<Path> candidate = stream
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".jar") && name.startsWith(DEFAULT_JAR_PREFIX);
                })
                .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
            return candidate.orElse(null);
        } catch (IOException e) {
            LOG.debug("扫描本地 Codefree jar 失败: " + dir, e);
            return null;
        }
    }

    /**
     * 解析用户配置的 jar 路径, 支持文件名或绝对路径
     */
    @NotNull
    public Path resolveJarPath(@NotNull CodefreeAgentSettings settings) {
        String rawPath = settings.downloadUrl != null ? settings.downloadUrl.trim() : "";
        if (isLocalPath(rawPath)) {
            Path localPath = toLocalPath(rawPath);
            if (localPath.isAbsolute()) {
                return localPath.normalize();
            }
            return getWorkDir().resolve(localPath).normalize();
        }
        String jarName = settings.jarFileName != null ? settings.jarFileName.trim() : "";
        if (!jarName.isEmpty()) {
            return getWorkDir().resolve(jarName).normalize();
        }
        Path existed = findExistingJar();
        if (existed != null) {
            return existed.normalize();
        }
        return getDefaultJarPath();
    }

    /**
     * 读取本地 jar 信息
     */
    @Nullable
    public JarInfo resolveLocalJarInfo(@NotNull CodefreeAgentSettings settings) {
        Path jarPath = resolveJarPath(settings);
        if (Files.exists(jarPath)) {
            return new JarInfo(jarPath.getFileName().toString(), jarPath, safeSize(jarPath));
        }
        Path existed = findExistingJar();
        if (existed != null && Files.exists(existed)) {
            return new JarInfo(existed.getFileName().toString(), existed, safeSize(existed));
        }
        return null;
    }

    private long safeSize(@NotNull Path jarPath) {
        try {
            return Files.size(jarPath);
        } catch (IOException e) {
            LOG.debug("读取 jar 大小失败: " + jarPath, e);
            return -1;
        }
    }

    @NotNull
    private Path getWorkDir() {
        Path dir = Paths.get(System.getProperty("user.home"), ".zeka-stack", "registry", "local", "agent");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.warn("创建 Codefree 目录失败: " + dir, e);
        }
        return dir;
    }

    @NotNull
    private Path resolveJavaExecutable() throws IOException {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) {
            throw new IOException("无法定位 java.home");
        }
        if (SystemInfo.isWindows) {
            Path javaw = Paths.get(javaHome, "bin", "javaw.exe");
            if (Files.exists(javaw)) {
                return javaw;
            }
        }
        Path javaPath = Paths.get(javaHome, "bin", SystemInfo.isWindows ? "java.exe" : "java");
        if (Files.exists(javaPath)) {
            return javaPath;
        }
        throw new IOException("未找到 Java 可执行文件: " + javaPath);
    }

    private boolean isLocalPath(@NotNull String url) {
        if (url.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || "file".equalsIgnoreCase(uri.getScheme())) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            return true;
        }
        return false;
    }

    @NotNull
    private Path toLocalPath(@NotNull String url) {
        try {
            URI uri = URI.create(url);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Paths.get(uri);
            }
        } catch (Exception ignored) {
        }
        if (url.startsWith("~/")) {
            return Paths.get(System.getProperty("user.home"), url.substring(2));
        }
        return Paths.get(url);
    }
}

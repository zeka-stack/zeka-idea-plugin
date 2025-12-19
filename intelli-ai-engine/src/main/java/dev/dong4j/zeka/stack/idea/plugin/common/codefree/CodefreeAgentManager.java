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
    /**
     * 日志记录器
     * <p> 用于记录 CodefreeAgentManager 类的日志信息.
     *
     * @see Logger
     */
    private static final Logger LOG = Logger.getInstance(CodefreeAgentManager.class);
    /** 默认的 Codefree 代理 JAR 文件名称. */
    public static final String DEFAULT_JAR_NAME = "codefree-agent.jar";
    /** 默认 jar 前缀 */
    public static final String DEFAULT_JAR_PREFIX = "codefree-agent";
    /** 默认的本地 OpenAI API 地址 */
    public static final String DEFAULT_OPENAI_ENDPOINT = System.getProperty("codefree.agent.api", "http://127.0.0.1:10011/v1");

    /** 用于同步对 processHandler 的访问, 防止多线程冲突 */
    private final Object processLock = new Object();
    /** 当前运行的 Codefree 代理进程处理器, 如果代理未运行则为 null */
    @Nullable
    private OSProcessHandler processHandler;

    /**
     * JarInfo 记录类
     * <p> 用于封装 JAR 文件的基本信息, 包括文件名, 文件路径和文件大小
     *
     * @author dong4j
     * @version 1.0.0
     * @since 1.0.0
     */
    public record JarInfo(String fileName, Path path, long size) {
        /**
         * 构造一个 {@code JarInfo} 实例
         * <p> 使用指定的文件名, 路径和大小初始化记录
         *
         * @param fileName JAR 文件名, 不能为空
         * @param path     JAR 文件路径, 不能为空
         * @param size     JAR 文件大小 (字节)
         * @since 1.0.0
         */
        public JarInfo(@NotNull String fileName, @NotNull Path path, long size) {
            this.fileName = fileName;
            this.path = path;
            this.size = size;
        }
    }

    /**
     * 下载进度监听器接口
     * <p> 用于在文件下载过程中实时获取已下载字节数和总字节数的回调.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.19
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface DownloadProgressListener {
        /**
         * 进度回调方法
         * <p> 当下载进度更新时调用, 提供已下载字节数和总字节数
         *
         * @param downloaded 已下载字节数
         * @param totalBytes 总字节数
         */
        void onProgress(long downloaded, long totalBytes);
    }

    /**
     * 获取 {@code CodefreeAgentManager} 的单例实例.
     * <p>
     * 通过 IntelliJ 平台的 {@code ApplicationManager} 获取已注册的 {@code CodefreeAgentManager} 服务对象.
     *
     * @return {@code CodefreeAgentManager} 单例实例
     */
    @NotNull
    public static CodefreeAgentManager getInstance() {
        return ApplicationManager.getApplication().getService(CodefreeAgentManager.class);
    }

    /**
     * 获取最新可用的 jar 名称
     * <p> 通过提供的基础 URL 拼接版本端点, 发送 HTTP 请求获取 Codefree 的最新版本信息.
     *
     * @param baseUrl 基础 URL, 用于构建完整的版本检查地址
     * @return 最新可用的 jar 名称, 如果获取失败则返回空字符串
     */
    @NotNull
    public String fetchLatestJarName(@NotNull String baseUrl) {
        String versionEndpoint = normalizeBase(baseUrl) + "/codefree/version";
        try {
            String version = HttpRequests.request(versionEndpoint).productNameAsUserAgent().readString();
            return version.trim();
        } catch (Exception e) {
            LOG.warn("获取 Codefree 最新版本失败", e);
        }
        return "";
    }

    /**
     * 获取远端 jar 文件的大小
     * <p> 根据提供的基础 URL 和 jar 文件名构建下载地址, 并通过 HTTP 请求获取远端 jar 文件的大小.
     *
     * @param baseUrl     基础 URL, 用于构建完整的下载地址
     * @param jarFileName jar 文件名, 用于拼接完整的下载路径
     * @return 远端 jar 文件的大小 (字节), 如果获取失败则返回 0
     */
    public long fetchRemoteJarSize(@NotNull String baseUrl, @NotNull String jarFileName) throws IOException {
        String url = buildDownloadUrl(baseUrl, jarFileName);
        try {
            return HttpRequests.request(url).productNameAsUserAgent().connect(request -> {
                URLConnection connection = request.getConnection();
                long length = connection.getContentLengthLong();
                return length > 0 ? length : 0;
            });
        } catch (Exception e) {
            LOG.warn("获取远端 jar 大小失败: " + url, e);
            return 0;
        }
    }

    /**
     * 构建下载 URL
     * <p> 根据基础 URL 和 JAR 文件名构建完整的下载地址
     *
     * @param baseUrl     基础 URL, 将进行规范化处理 (移除末尾的斜杠)
     * @param jarFileName JAR 文件名, 将直接拼接到基础 URL 后
     * @return 完整的下载 URL
     */
    @NotNull
    public String buildDownloadUrl(@NotNull String baseUrl, @NotNull String jarFileName) {
        return normalizeBase(baseUrl) + "/" + jarFileName;
    }

    /**
     * 从给定的 URL 中推导出 JAR 文件名
     * <p>该方法会解析 URL 的路径部分, 并提取最后一个斜杠 (/) 之后的内容作为文件名.
     * 如果 URL 路径为空或不包含斜杠, 则直接返回路径部分.
     * 如果解析过程中发生异常, 则返回 null.
     *
     * @param url 要解析的 URL 字符串, 不能为 null
     * @return 从 URL 中提取的 JAR 文件名, 如果解析失败则返回 null
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
     * 下载 Codefree 代理的 jar 文件.
     * <p>
     * 根据配置的下载地址下载 jar 文件到本地工作目录. 如果下载地址指向本地文件, 则直接返回本地路径.
     * 下载过程中会显示进度指示, 并支持进度监听.
     *
     * @param settings         Codefree 代理配置, 包含下载地址等信息
     * @param jarFileName      要下载的 jar 文件名
     * @param indicator        进度指示器, 用于显示下载进度和取消检查
     * @param progressListener 下载进度监听器, 可选参数, 用于接收下载进度回调
     * @return 下载的 jar 文件路径
     * @throws IOException 当下载地址未配置, 本地文件不存在或下载过程中发生 I/O 错误时抛出
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
     * 解析下载目标路径
     * <p> 根据指定的 jar 文件名确定下载的目标路径. 如果文件名为空或空白, 则使用默认的 jar 文件名.
     *
     * @param jarFileName jar 文件名, 如果为空或空白则使用默认文件名
     * @return 下载目标路径
     */
    @NotNull
    private Path resolveDownloadTarget(@NotNull String jarFileName) {
        String targetName = jarFileName.isBlank() ? DEFAULT_JAR_NAME : jarFileName;
        return getWorkDir().resolve(targetName);
    }

    /**
     * 启动 Codefree 代理进程
     * <p>
     * 该方法会执行以下操作:
     * 1. 检查指定的 JAR 文件是否存在
     * 2. 解析 Java 可执行文件路径
     * 3. 构建并配置启动命令
     * 4. 启动进程并返回其 PID
     *
     * @param settings Codefree 代理配置, 包含 JAR 文件路径等信息
     * @return 启动的进程 PID, 如果无法获取 PID 则返回 -1
     * @throws IOException 如果 JAR 文件不存在或启动过程中发生 I/O 错误
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
                }
            }
        });
        handler.startNotify();
        synchronized (processLock) {
            processHandler = handler;
        }
        Process process = handler.getProcess();
        return process.pid();
    }

    /**
     * 停止运行中的 Codefree 代理进程
     *
     * <p> 该方法会安全地停止当前正在运行的代理进程. 如果进程处理器存在且进程尚未终止,
     * 则会尝试销毁进程, 并在成功停止后将处理器置为 null. 如果停止过程中发生异常,
     * 会记录警告日志但不会抛出异常.
     *
     * <p> 该方法使用同步锁确保多线程环境下的线程安全.
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
            }
        }
    }

    /**
     * 检查 Codefree 代理是否正在运行
     *
     * @return 如果代理进程存在且未终止则返回 true, 否则返回 false
     */
    public boolean isRunning() {
        synchronized (processLock) {
            return processHandler != null && !processHandler.isProcessTerminated();
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
     * 查找本地已经存在的 Codefree jar 文件
     * <p> 在指定的工作目录中查找符合条件的 jar 文件, 按修改时间排序, 返回最新的 jar 文件路径
     *
     * @return 本地已存在的最新 Codefree jar 文件路径, 如果未找到则返回 null
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
     * 解析本地 jar 文件的信息
     * <p>根据提供的设置尝试获取本地 jar 文件的详细信息. 如果配置的路径存在 jar 文件, 则返回其信息;
     * 否则, 查找工作目录中现有的 jar 文件 (按修改时间排序) 并返回信息; 如果都找不到, 则返回 null.
     *
     * @param settings CodefreeAgentSettings 对象, 包含 jar 文件的配置信息
     * @return JarInfo 对象, 包含 jar 文件的名称, 路径和大小; 如果找不到, 则返回 null
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

    /**
     * 安全地获取文件大小
     * <p> 尝试读取指定路径文件的大小, 若读取失败则记录日志并返回 -1
     *
     * @param jarPath 目标文件路径 (不能为空)
     * @return 文件大小 (字节), 读取失败时返回 -1
     */
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
    private String normalizeBase(@NotNull String baseUrl) {
        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
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

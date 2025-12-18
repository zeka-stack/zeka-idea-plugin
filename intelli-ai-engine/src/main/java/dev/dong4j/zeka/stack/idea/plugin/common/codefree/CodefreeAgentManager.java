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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.dong4j.zeka.stack.idea.plugin.common.config.CodefreeAgentSettings;
import lombok.SneakyThrows;

/**
 * 管理 Codefree 本地代理的下载与启动
 */
@Service(Service.Level.APP)
public final class CodefreeAgentManager {
    private static final Logger LOG = Logger.getInstance(CodefreeAgentManager.class);
    public static final String DEFAULT_JAR_NAME = "codefree-chat-mvp-1.0.0-SNAPSHOT.jar";

    private final Object processLock = new Object();
    @Nullable
    private OSProcessHandler processHandler;
    @Nullable
    private Path runningJarPath;

    @NotNull
    public static CodefreeAgentManager getInstance() {
        return ApplicationManager.getApplication().getService(CodefreeAgentManager.class);
    }

    /**
     * 下载 jar 文件
     */
    public void downloadJar(@NotNull CodefreeAgentSettings settings, @NotNull ProgressIndicator indicator) throws IOException {
        String url = settings.downloadUrl != null ? settings.downloadUrl.trim() : "";
        if (url.isEmpty()) {
            throw new IOException("未配置下载地址");
        }
        if (isLocalPath(url)) {
            Path local = resolveJarPath(settings);
            if (Files.notExists(local)) {
                throw new IOException("本地 Jar 不存在: " + local);
            }
            return;
        }
        Path jarPath = getDefaultJarPath();
        Path parent = jarPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        indicator.setText("正在下载 Codefree 代理...");
        HttpRequests.request(url).productNameAsUserAgent().saveToFile(jarPath.toFile(), indicator);
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
     * 默认 jar 路径
     */
    @NotNull
    public Path getDefaultJarPath() {
        return getWorkDir().resolve(DEFAULT_JAR_NAME);
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
        return getDefaultJarPath();
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

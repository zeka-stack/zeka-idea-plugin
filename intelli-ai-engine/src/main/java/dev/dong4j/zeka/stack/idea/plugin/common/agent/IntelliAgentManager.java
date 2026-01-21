package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.kit.StorageUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 智能代理管理器类
 * <p> 用于管理和控制 IntelliAI Agent 的生命周期, 包括启动, 停止, 检查状态, 下载和配置等操作.
 * 提供了获取最新版本信息, 远程 jar 文件大小, 构建下载 URL 等功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.24
 * @since 1.0.0
 */
@Slf4j
@Service(Service.Level.APP)
public final class IntelliAgentManager {

    /** 默认的 IntelliAI Agent JAR 文件名称. */
    public static final String DEFAULT_JAR_NAME = "intelli-ai-agent.jar";
    /** 默认 jar 前缀 */
    public static final String DEFAULT_JAR_PREFIX = "intelli-ai-agent";
    /** 默认端口号 */
    public static final int DEFAULT_PORT = 8765;
    /** 默认的本地 OpenAI API 地址 */
    public static final String DEFAULT_OPENAI_ENDPOINT = System.getProperty("intelli.agent.api", "http://127.0.0.1:8765/v1");
    /** PID 文件名 */
    private static final String PID_FILE_NAME = "intelli-ai-agent.pid";

    /** 用于同步对 processHandler 的访问, 防止多线程冲突 */
    private final Object processLock = new Object();
    /** 当前运行的 IntelliAI Agent 代理进程处理器, 如果代理未运行则为 null */
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
     * 获取 {@code IntelliAgentManager} 的单例实例.
     * <p>
     * 通过 IntelliJ 平台的 {@code ApplicationManager} 获取已注册的 {@code IntelliAgentManager} 服务对象.
     *
     * @return {@code IntelliAgentManager} 单例实例
     */
    @NotNull
    public static IntelliAgentManager getInstance() {
        return ApplicationManager.getApplication().getService(IntelliAgentManager.class);
    }

    /**
     * 获取最新可用的 jar 名称
     * <p> 通过提供的基础 URL 拼接版本端点, 发送 HTTP 请求获取 IntelliAI Agent 的最新版本信息.
     *
     * @param baseUrl 基础 URL, 用于构建完整的版本检查地址
     * @return 最新可用的 jar 名称, 如果获取失败则返回空字符串
     */
    @NotNull
    public String fetchLatestJarName(@NotNull String baseUrl) {
        String versionEndpoint = normalizeBase(baseUrl) + "/agent/version";
        try {
            String version = HttpRequests.request(versionEndpoint).productNameAsUserAgent().readString();
            return version.trim();
        } catch (Exception e) {
            log.debug("获取 IntelliAI Agent 最新版本失败", e);
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
    public long fetchRemoteJarSize(@NotNull String baseUrl, @NotNull String jarFileName) {
        String url = buildDownloadUrl(baseUrl, jarFileName);
        try {
            return HttpRequests.request(url).productNameAsUserAgent().connect(request -> {
                URLConnection connection = request.getConnection();
                long length = connection.getContentLengthLong();
                return length > 0 ? length : 0;
            });
        } catch (Exception e) {
            log.debug("获取远端 jar 大小失败: " + url, e);
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
     * 下载 IntelliAI Agent 的 jar 文件.
     * <p>
     * 根据配置的下载地址下载 jar 文件到本地工作目录. 如果下载地址指向本地文件, 则直接返回本地路径.
     * 下载过程中会显示进度指示, 并支持进度监听.
     *
     * @param settings         IntelliAI Agent 配置, 包含下载地址等信息
     * @param jarFileName      要下载的 jar 文件名
     * @param indicator        进度指示器, 用于显示下载进度和取消检查
     * @param progressListener 下载进度监听器, 可选参数, 用于接收下载进度回调
     * @return 下载的 jar 文件路径
     * @throws IOException 当下载地址未配置, 本地文件不存在或下载过程中发生 I/O 错误时抛出
     */
    public Path downloadJar(@NotNull IntelliAgentSettings settings,
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
        indicator.setText(AICommonBundle.message("settings.agent.download.progress.text"));
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
     * 启动 IntelliAI Agent 进程
     * <p>
     * 该方法会执行以下操作:
     * 1. 检查默认端口 8765 是否被占用且是否为 IntelliAI Agent 服务
     * 2. 如果是 IntelliAI Agent 服务, 则直接使用该服务
     * 3. 否则检查指定的 JAR 文件是否存在
     * 4. 解析 Java 可执行文件路径
     * 5. 构建并配置启动命令
     * 6. 启动进程并返回其 PID
     *
     * @param settings IntelliAI Agent 配置, 包含 JAR 文件路径等信息
     * @return 启动的进程 PID, 如果无法获取 PID 则返回 -1, 如果使用已有服务则返回 0
     */
    @SneakyThrows
    public long startAgent(@NotNull IntelliAgentSettings settings) {
        // 先检查默认端口是否已有 IntelliAI Agent 服务运行
        if (isIntelliAgentRunningOnPort()) {
            log.debug("检测到端口 " + DEFAULT_PORT + " 上已有 IntelliAI Agent 服务运行, 直接使用该服务");
            return 0; // 返回 0 表示使用已有服务
        }

        // 端口未被占用或不是 IntelliAI Agent 服务, 启动新服务
        log.debug("端口 " + DEFAULT_PORT + " 上未检测到 IntelliAI Agent 服务, 启动新服务");
        Path jarPath = resolveJarPath(settings);
        if (Files.notExists(jarPath)) {
            throw new IOException("Jar 不存在: " + jarPath);
        }
        Path javaPath = resolveJavaExecutable();
        final OSProcessHandler handler = getOsProcessHandler(javaPath, jarPath);
        synchronized (processLock) {
            processHandler = handler;
        }
        Process process = handler.getProcess();
        long pid = process.pid();
        // 创建 PID 文件
        createPidFile(pid);
        return pid;
    }

    /**
     * 获取操作系统进程处理器
     * <p> 根据给定的 Java 可执行文件路径和 JAR 文件路径, 创建并配置 OSProcessHandler.
     * 在进程中添加一个监听器, 以便在进程终止时清理资源.
     *
     * @param javaPath Java 可执行文件路径
     * @param jarPath  JAR 文件路径
     * @return 配置好的 OSProcessHandler 实例
     * @throws ExecutionException 如果进程配置或启动失败
     */
    private @NotNull OSProcessHandler getOsProcessHandler(Path javaPath, Path jarPath) throws ExecutionException {
        final OSProcessHandler handler = getProcessHandler(javaPath, jarPath);
        handler.addProcessListener(new ProcessListener() {
            /**
             * 处理进程终止事件
             * <p> 在进程终止时, 同步释放当前进程处理器引用, 并删除 PID 文件
             *
             * @param event 进程事件对象, 不能为空
             */
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                // 进程终止时清理 processHandler 引用
                // 注意: 需要同步检查, 因为 stopAgent() 可能已经清空了引用
                synchronized (processLock) {
                    // 只有当 handler 是当前 processHandler 时才清空, 避免重复处理
                    if (processHandler == handler) {
                        processHandler = null;
                    }
                }
                // 进程终止时清理 PID 文件
                deletePidFile();
            }
        });
        handler.startNotify();
        return handler;
    }

    /**
     * 获取操作系统进程处理器
     * <p> 根据给定的 Java 可执行文件路径和 JAR 文件路径, 构建并返回一个 OSProcessHandler 实例.
     *
     * @param javaPath Java 可执行文件的路径
     * @param jarPath  JAR 文件的路径
     * @return OSProcessHandler 实例, 用于处理进程
     * @throws ExecutionException 当构建命令行或创建进程处理器失败时抛出
     * @since 1.0.0
     */
    private static @NotNull OSProcessHandler getProcessHandler(Path javaPath, Path jarPath) throws ExecutionException {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(javaPath.toString());
        // macOS 隐藏 Dock 图标
        if (SystemInfo.isMac) {
            commandLine.addParameter("-Dapple.awt.UIElement=true");
        }
        commandLine.addParameters("-jar", jarPath.toString());
        // 指定端口参数
        commandLine.addParameter("--port=" + DEFAULT_PORT);
        if (jarPath.getParent() != null) {
            commandLine.setWorkDirectory(jarPath.getParent().toFile());
        }

        return new OSProcessHandler(commandLine);
    }

    /**
     * 停止运行中的 IntelliAI Agent 进程
     *
     * <p> 该方法会安全地停止当前正在运行的代理进程. 如果进程处理器存在且进程尚未终止,
     * 则会尝试销毁进程, 并在成功停止后将处理器置为 null. 如果停止过程中发生异常,
     * 会记录警告日志但不会抛出异常.
     *
     * <p> 该方法还会检查 PID 文件, 如果存在外部进程, 也会尝试停止它.
     *
     * <p> 该方法使用同步锁确保多线程环境下的线程安全.
     * <p>
     * 修复死锁问题: 不在持有锁的情况下调用 destroyProcess(), 因为这会触发 ProcessListener
     * 回调, 而回调中也会尝试获取同一个锁, 导致死锁.
     */
    public void stopAgent() {
        OSProcessHandler handlerToDestroy;
        synchronized (processLock) {
            handlerToDestroy = processHandler;
            processHandler = null; // 先清空引用, 避免回调中重复处理
        }
        // 在锁外调用 destroyProcess(), 避免死锁
        if (handlerToDestroy != null) {
            try {
                handlerToDestroy.destroyProcess();
            } catch (Exception e) {
                log.debug("停止 IntelliAI Agent 失败", e);
            }
        }
        // 尝试停止外部进程（通过 PID 文件）
        stopExternalProcess();
        // 删除 PID 文件
        deletePidFile();
    }

    /**
     * 检查 IntelliAI Agent 是否正在运行
     * <p>
     * 该方法会检查以下情况:
     * 1. 当前插件实例管理的进程 (processHandler)
     * 2. PID 文件中记录的进程
     * 3. 实际进程是否真的存在
     * 4. 端口上是否有 IntelliAI Agent 服务运行
     *
     * @return 如果代理进程存在且未终止则返回 true, 否则返回 false
     */
    public boolean isRunning() {
        synchronized (processLock) {
            // 首先检查当前插件实例管理的进程
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                return true;
            }
        }
        // 检查 PID 文件和实际进程
        return checkExternalProcess();
    }

    /**
     * 快速检查 IntelliAI Agent 是否在本地运行
     * <p>仅检查本地进程和 PID 文件，不访问网络端口。</p>
     *
     * @return 如果检测到本地进程则返回 true, 否则返回 false
     */
    public boolean isRunningQuick() {
        synchronized (processLock) {
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                return true;
            }
        }
        Long pid = readPidFromFile();
        if (pid != null) {
            if (isProcessAlive(pid)) {
                return true;
            }
            deletePidFile();
        }
        return false;
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
     * 查找本地已经存在的 IntelliAI Agent jar 文件
     * <p> 在指定的工作目录中查找符合条件的 jar 文件, 按修改时间排序, 返回最新的 jar 文件路径
     *
     * @return 本地已存在的最新 IntelliAI Agent jar 文件路径, 如果未找到则返回 null
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
            log.debug("扫描本地 IntelliAI Agent jar 失败: {}", dir, e);
            return null;
        }
    }

    /**
     * 解析用户配置的 jar 路径, 支持文件名或绝对路径
     */
    @NotNull
    public Path resolveJarPath(@NotNull IntelliAgentSettings settings) {
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
     * @param settings IntelliAgentSettings 对象, 包含 jar 文件的配置信息
     * @return JarInfo 对象, 包含 jar 文件的名称, 路径和大小; 如果找不到, 则返回 null
     */
    @Nullable
    public JarInfo resolveLocalJarInfo(@NotNull IntelliAgentSettings settings) {
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
            log.debug("读取 jar 大小失败: {}", jarPath, e);
            return -1;
        }
    }

    /**
     * 获取 IntelliAI Agent 的工作目录路径
     * <p> 从插件存储目录中构建并返回名为 "agent" 的子目录路径. 如果目录不存在, 则自动创建.
     * 若创建过程中发生 I/O 错误, 将记录调试日志但不抛出异常.
     *
     * @return 工作目录的路径, 已规范化且确保存在
     */
    @NotNull
    private Path getWorkDir() {
        Path dir = StorageUtil.getPluginStorageDir(EngineContents.PLUGIN_SIMPLE_NAME).resolve("agent");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.debug("创建 IntelliAI Agent 目录失败: {}", dir, e);
        }
        return dir;
    }

    /**
     * 解析并返回本地 Java 可执行文件的路径
     * <p>
     * 该方法根据系统属性 {@code java.home} 确定 Java 安装目录, 并在 {@code bin} 子目录下查找可执行文件.
     * 在 Windows 系统中优先查找 {@code javaw.exe}, 若不存在则查找 {@code java.exe}; 在其他系统中查找 {@code java}.
     * 若未找到可执行文件, 则抛出 {@code IOException}.
     *
     * @return Java 可执行文件的完整路径
     * @throws IOException 当无法定位 {@code java.home} 或未找到 Java 可执行文件时抛出
     */
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

    /**
     * 判断指定的 URL 是否为本地路径
     * <p>该方法通过解析 URL 的 Scheme 来判断是否为本地文件路径. 如果 URL 为空或解析失败, 则默认认为是本地路径.
     *
     * @param url 要判断的 URL 字符串, 不能为空
     * @return 如果 URL 为本地文件路径 (Scheme 为空或为 "file") 或解析失败时返回 true, 否则返回 false
     */
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

    /**
     * 标准化基础 URL, 移除末尾的斜杠
     * <p> 该方法用于清理传入的基础 URL 字符串, 确保其不以斜杠结尾, 便于后续拼接路径.
     *
     * @param baseUrl 基础 URL 字符串, 不能为空
     * @return 标准化后的基础 URL, 移除末尾斜杠
     */
    @NotNull
    private String normalizeBase(@NotNull String baseUrl) {
        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 将字符串 URL 转换为本地文件路径
     * <p> 该方法尝试将输入的 URL 字符串解析为 URI, 若其协议为 "file", 则直接转换为路径对象; 否则, 若字符串以 "~/" 开头, 则替换为用户主目录路径; 否则, 直接作为本地路径解析.
     *
     * @param url 要转换的 URL 字符串, 不能为空
     * @return 转换后的本地路径对象, 如果解析失败则返回由原始字符串构建的路径
     */
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

    /**
     * 检查指定端口上的服务是否是 IntelliAI Agent 服务
     * <p> 通过访问 /health 端点并检查返回的健康检查信息来判断
     * 如果端口未被占用或服务不是 IntelliAI Agent, 则返回 false
     *
     * @return 如果是 IntelliAI Agent 服务则返回 true, 否则返回 false
     */
    private boolean isIntelliAgentRunningOnPort() {
        // 尝试访问 /health 端点
        String url = "http://127.0.0.1:" + IntelliAgentManager.DEFAULT_PORT + "/health";
        try {
            String response = HttpRequests.request(url)
                .productNameAsUserAgent()
                .readString();
            // 检查响应中是否包含健康检查标识
            // 响应格式: {"status":"ok"}
            boolean isIntelliAgent = response.contains("\"status\"") && response.contains("\"ok\"");
            if (isIntelliAgent) {
                log.debug("检测到端口 " + IntelliAgentManager.DEFAULT_PORT + " 上运行的是 IntelliAI Agent 服务");
            }
            return isIntelliAgent;
        } catch (Exception e) {
            log.debug("检查端口 " + IntelliAgentManager.DEFAULT_PORT + " 上的服务失败: {} {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * 获取 PID 文件路径
     *
     * @return PID 文件路径
     */
    @NotNull
    private Path getPidFilePath() {
        return getWorkDir().resolve(PID_FILE_NAME);
    }

    /**
     * 创建 PID 文件
     * <p> 在启动进程时创建 PID 文件, 写入进程的 PID
     *
     * @param pid 进程 ID
     */
    private void createPidFile(long pid) {
        Path pidFile = getPidFilePath();
        try {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(pidFile), StandardCharsets.UTF_8)) {
                writer.write(String.valueOf(pid));
            }
            log.debug("创建 PID 文件: {}, PID: {}", pidFile, pid);
        } catch (IOException e) {
            log.debug("创建 PID 文件失败: {}", pidFile, e);
        }
    }

    /**
     * 删除 PID 文件
     * <p> 在停止进程时删除 PID 文件
     */
    private void deletePidFile() {
        Path pidFile = getPidFilePath();
        try {
            if (Files.exists(pidFile)) {
                Files.delete(pidFile);
                log.debug("删除 PID 文件: {}", pidFile);
            }
        } catch (IOException e) {
            log.debug("删除 PID 文件失败: {}", pidFile, e);
        }
    }

    /**
     * 读取 PID 文件中的进程 ID
     *
     * @return 进程 ID, 如果文件不存在或读取失败则返回 null
     */
    @Nullable
    private Long readPidFromFile() {
        Path pidFile = getPidFilePath();
        if (!Files.exists(pidFile)) {
            return null;
        }
        try {
            String content = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
            return Long.parseLong(content);
        } catch (Exception e) {
            log.debug("读取 PID 文件失败: {}", pidFile, e);
            return null;
        }
    }

    /**
     * 检查进程是否真的存在
     * <p> 通过系统命令检查指定 PID 的进程是否正在运行
     *
     * @param pid 进程 ID
     * @return 如果进程存在则返回 true, 否则返回 false
     */
    private boolean isProcessAlive(long pid) {
        try {
            ProcessBuilder pb;
            if (SystemInfo.isWindows) {
                // Windows: tasklist 命令即使进程不存在也可能返回退出码 0
                // 需要检查输出内容来判断进程是否存在
                pb = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid, "/FO", "CSV", "/NH");
            } else {
                // Unix/Linux/Mac: ps 命令如果进程不存在会返回非 0 退出码
                pb = new ProcessBuilder("/bin/sh", "-c", "ps -p " + pid);
            }
            Process process = pb.start();

            if (SystemInfo.isWindows) {
                // Windows: 需要检查输出内容
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    // 如果输出不为空且包含 PID，说明进程存在
                    boolean exists = line != null && !line.trim().isEmpty() && !line.contains("INFO:");
                    int exitCode = process.waitFor();
                    return exists && exitCode == 0;
                }
            } else {
                // Unix/Linux/Mac: 直接检查退出码
                int exitCode = process.waitFor();
                return exitCode == 0;
            }
        } catch (Exception e) {
            log.debug("检查进程是否存在失败, PID: {}", pid, e);
            return false;
        }
    }

    /**
     * 检查外部进程（通过 PID 文件和端口）
     * <p>
     * 该方法会:
     * 1. 检查 PID 文件是否存在
     * 2. 如果存在, 读取 PID 并验证进程是否真的存在
     * 3. 如果进程不存在, 清理 PID 文件
     * 4. 检查端口上是否有 IntelliAI Agent 服务运行
     *
     * @return 如果检测到外部进程运行则返回 true, 否则返回 false
     */
    private boolean checkExternalProcess() {
        // 首先检查端口上是否有服务运行
        if (isIntelliAgentRunningOnPort()) {
            // 如果端口上有服务, 尝试读取 PID 文件
            Long pid = readPidFromFile();
            if (pid != null) {
                // 验证进程是否真的存在
                if (isProcessAlive(pid)) {
                    log.debug("检测到外部 IntelliAI Agent 进程运行, PID: {}", pid);
                    return true;
                } else {
                    // PID 文件存在但进程不存在, 清理 PID 文件
                    log.debug("PID 文件存在但进程不存在, 清理 PID 文件, PID: {}", pid);
                    deletePidFile();
                }
            } else {
                // 端口上有服务但没有 PID 文件, 可能是外部启动的, 仍然返回 true
                log.debug("检测到端口 " + DEFAULT_PORT + " 上有 IntelliAI Agent 服务运行, 但没有 PID 文件");
                return true;
            }
        }

        // 检查 PID 文件是否存在
        Long pid = readPidFromFile();
        if (pid != null) {
            // 验证进程是否真的存在
            if (isProcessAlive(pid)) {
                log.debug("检测到外部 IntelliAI Agent 进程运行, PID: {}", pid);
                return true;
            } else {
                // PID 文件存在但进程不存在, 清理 PID 文件
                log.debug("PID 文件存在但进程不存在, 清理 PID 文件, PID: {}", pid);
                deletePidFile();
            }
        }

        return false;
    }

    /**
     * 停止外部进程（通过 PID 文件找到的进程）
     * <p> 读取 PID 文件, 如果进程存在则尝试停止它
     */
    private void stopExternalProcess() {
        Long pid = readPidFromFile();
        if (pid != null && isProcessAlive(pid)) {
            try {
                ProcessBuilder pb;
                if (SystemInfo.isWindows) {
                    pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
                } else {
                    pb = new ProcessBuilder("kill", String.valueOf(pid));
                }
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    log.debug("成功停止外部 IntelliAI Agent 进程, PID: {}", pid);
                } else {
                    log.debug("停止外部 IntelliAI Agent 进程失败, PID: {}, 退出码: {}", pid, exitCode);
                }
            } catch (Exception e) {
                log.debug("停止外部 IntelliAI Agent 进程时发生异常, PID: {}", pid, e);
            }
        }
    }
}

package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.HttpRequests;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Git-cliff 下载管理器
 * <p>
 * 负责检测系统架构、获取最新版本、下载和解压 git-cliff 二进制文件。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class GitCliffDownloadManager {
    /** GitHub Releases API 地址, 用于获取 git-cliff 最新版本信息 */
    private static final String GITHUB_API_URL = "https://api.github.com/repos/orhun/git-cliff/releases/latest";
    /** GitHub 发布下载基础 URL */
    private static final String GITHUB_DOWNLOAD_BASE_URL = "https://github.com/orhun/git-cliff/releases/download";
    /** 插件目录名称, 用于标识插件相关文件的根目录 */
    private static final String PLUGIN_DIR_NAME = "changelog";
    /** Git-cliff 二进制文件所在目录名称 */
    private static final String GIT_CLIFF_DIR_NAME = "git-cliff";
    /** 下载文件存放目录名称 */
    private static final String DISTS_DIR_NAME = "dists";

    /**
     * 下载进度监听器接口
     */
    @FunctionalInterface
    public interface DownloadProgressListener {
        /**
         * 进度回调方法
         *
         * @param downloaded 已下载字节数
         * @param totalBytes 总字节数
         */
        void onProgress(long downloaded, long totalBytes);
    }

    /**
     * 获取 git-cliff 安装目录
     *
     * @return git-cliff 安装目录路径
     */
    @NotNull
    public static Path getGitCliffDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".zeka-stack", "plugin", PLUGIN_DIR_NAME, GIT_CLIFF_DIR_NAME);
    }

    /**
     * 获取下载目录
     *
     * @return 下载目录路径
     */
    @NotNull
    public static Path getDistsDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".zeka-stack", "plugin", PLUGIN_DIR_NAME, DISTS_DIR_NAME);
    }

    /**
     * 检查 git-cliff 是否已安装
     *
     * @return 如果已安装返回 true
     */
    public static boolean isInstalled() {
        Path gitCliffDir = getGitCliffDir();
        Path binary = gitCliffDir.resolve("git-cliff");
        if (SystemInfo.isWindows) {
            binary = gitCliffDir.resolve("git-cliff.exe");
        }
        return Files.exists(binary);
    }

    /**
     * 获取 git-cliff 二进制文件路径
     *
     * @return 二进制文件路径，如果不存在则返回 null
     */
    @Nullable
    public static Path getBinaryPath() {
        Path gitCliffDir = getGitCliffDir();
        Path binary = gitCliffDir.resolve("git-cliff");
        if (SystemInfo.isWindows) {
            binary = gitCliffDir.resolve("git-cliff.exe");
        }
        if (Files.exists(binary)) {
            return binary;
        }
        return null;
    }

    /**
     * 从 GitHub API 获取最新版本标签
     * <p>
     * 超时时间设置为 3 秒。
     *
     * @return 版本标签（如 "v2.11.0"），如果获取失败则返回 null
     */
    @Nullable
    public static String fetchLatestVersion() {
        try {
            String response = HttpRequests.request(GITHUB_API_URL)
                .productNameAsUserAgent()
                .tuner(connection -> {
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) connection;
                    // 设置连接超时和读取超时都为 3 秒
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                })
                .readString();
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.get("tag_name").getAsString();
        } catch (Exception e) {
            log.debug("获取 git-cliff 最新版本失败", e);
            return null;
        }
    }

    /**
     * 根据系统信息生成压缩包名称
     *
     * @param version 版本号（不含 v 前缀，如 "2.11.0"）
     * @return 压缩包名称
     */
    @NotNull
    public static String generatePackageName(@NotNull String version) {
        String arch = System.getProperty("os.arch");
        String osArch = normalizeArch(arch);
        String extension = SystemInfo.isWindows ? ".zip" : ".tar.gz";

        if (SystemInfo.isMac) {
            return String.format("git-cliff-%s-%s-apple-darwin%s", version, osArch, extension);
        } else if (SystemInfo.isWindows) {
            // Windows 默认使用 msvc，如果需要 gnu 可以后续扩展
            return String.format("git-cliff-%s-%s-pc-windows-msvc%s", version, osArch, extension);
        } else {
            // Linux 默认使用 gnu，如果需要 musl 可以后续扩展
            return String.format("git-cliff-%s-%s-unknown-linux-gnu%s", version, osArch, extension);
        }
    }

    /**
     * 规范化架构名称
     *
     * @param arch 原始架构名称
     * @return 规范化后的架构名称
     */
    @NotNull
    private static String normalizeArch(@NotNull String arch) {
        // 将常见的架构名称转换为标准格式
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return "aarch64";
        } else if (arch.equals("x86_64") || arch.equals("amd64")) {
            return "x86_64";
        }
        return arch;
    }

    /**
     * 构建下载 URL
     *
     * @param tagName     版本标签（如 "v2.11.0"）
     * @param packageName 压缩包名称
     * @return 下载 URL
     */
    @NotNull
    public static String buildDownloadUrl(@NotNull String tagName, @NotNull String packageName) {
        return String.format("%s/%s/%s", GITHUB_DOWNLOAD_BASE_URL, tagName, packageName);
    }

    /**
     * 下载 git-cliff 压缩包
     *
     * @param url              下载 URL
     * @param indicator        进度指示器
     * @param progressListener 进度监听器
     * @return 下载的文件路径
     * @throws IOException 下载失败时抛出
     */
    @NotNull
    public static Path downloadPackage(@NotNull String url,
                                       @NotNull ProgressIndicator indicator,
                                       @Nullable DownloadProgressListener progressListener) throws IOException {
        Path distsDir = getDistsDir();
        Files.createDirectories(distsDir);

        // 从 URL 中提取文件名
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path targetFile = distsDir.resolve(fileName);

        indicator.setText("Downloading git-cliff...");
        HttpRequests.request(url)
            .productNameAsUserAgent()
            .tuner(connection -> {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) connection;
                // 设置连接超时为 5 秒，读取超时为 30 秒
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(30000);
            })
            .connect(request -> {
                URLConnection connection = request.getConnection();
                long total = connection.getContentLengthLong();
                indicator.setIndeterminate(total <= 0);

                try (InputStream inputStream = request.getInputStream();
                     OutputStream outputStream = Files.newOutputStream(targetFile)) {
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

        return targetFile;
    }

    /**
     * 解压压缩包到目标目录
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @throws IOException 解压失败时抛出
     */
    public static void extractPackage(@NotNull Path archivePath, @NotNull Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        String fileName = archivePath.getFileName().toString();
        if (fileName.endsWith(".tar.gz")) {
            extractTarGz(archivePath, targetDir);
        } else if (fileName.endsWith(".zip")) {
            extractZip(archivePath, targetDir);
        } else {
            throw new IOException("不支持的压缩格式: " + fileName);
        }
    }

    /**
     * 解压 tar.gz 文件
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @throws IOException 解压失败时抛出
     */
    private static void extractTarGz(@NotNull Path archivePath, @NotNull Path targetDir) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(archivePath);
             GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {
            extractEntries(
                tarInputStream,
                targetDir,
                tarInputStream::getNextEntry,
                TarArchiveEntry::getName,
                TarArchiveEntry::isDirectory,
                name -> name.equals("git-cliff.exe") || name.equals("git-cliff")
                          );
        }
    }

    /**
     * 解压 zip 文件
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @throws IOException 解压失败时抛出
     */
    private static void extractZip(@NotNull Path archivePath, @NotNull Path targetDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archivePath))) {
            extractEntries(
                zipInputStream,
                targetDir,
                zipInputStream::getNextEntry,
                ZipEntry::getName,
                ZipEntry::isDirectory,
                name -> name.equals("git-cliff.exe") || name.equals("git-cliff")
                          );
        }
    }

    /**
     * 条目数据提供者接口
     * <p> 定义了从数据源获取条目数据的契约, 适用于需要延迟加载或动态获取数据的场景. 该接口为函数式接口, 仅包含一个抽象方法 {@code get()}, 用于统一数据获取逻辑. 实现类应确保在发生 I/O 错误时抛出
     * {@link java.io.IOException}, 若无数据或发生错误则返回 {@code null}.</p>
     * <p> 本接口不负责请求处理, 仅专注于数据获取, 符合面向对象设计原则, 避免基础设施关注, 适用于内部系统组件间的数据委托与注入场景.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.14
     * @since 1.0.0
     */
    @FunctionalInterface
    private interface EntrySupplier<T> {
        /**
         * 获取条目数据
         * <p> 通过此方法从数据源中获取条目数据, 若发生 I/O 错误则抛出异常
         *
         * @return 条目数据, 若无数据或发生错误则返回 null
         * @throws IOException 当数据读取过程中发生 I/O 错误时抛出
         */
        @Nullable T get() throws IOException;
    }

    /**
     * 从输入流中提取归档文件条目并写入目标目录
     * <p>
     * 该方法遍历归档文件中的每个条目, 跳过目录项, 对文件项进行名称规范化后写入目标目录.
     * 若条目名称匹配可执行文件名, 则在非 Windows 系统下设置可执行权限.
     *
     * @param inputStream    归档文件输入流
     * @param targetDir      目标目录路径
     * @param entrySupplier  条目提供器, 用于获取下一个条目
     * @param nameProvider   条目名称提供器, 用于获取条目名称
     * @param isDirectory    判断条目是否为目录的谓词
     * @param executableName 判断条目名称是否为可执行文件的谓词
     * @throws IOException 当读取或写入文件时发生错误
     */
    private static <T> void extractEntries(@NotNull InputStream inputStream,
                                           @NotNull Path targetDir,
                                           @NotNull EntrySupplier<T> entrySupplier,
                                           @NotNull Function<T, String> nameProvider,
                                           @NotNull Predicate<T> isDirectory,
                                           @NotNull Predicate<String> executableName) throws IOException {
        T entry;
        while ((entry = entrySupplier.get()) != null) {
            if (isDirectory.test(entry)) {
                continue;
            }

            String entryName = normalizeEntryName(nameProvider.apply(entry));
            if (entryName.isEmpty()) {
                continue;
            }

            writeEntry(inputStream, targetDir, entryName, executableName.test(entryName));
        }
    }

    /**
     * 规范化压缩包内文件路径名称
     * <p>
     * 移除压缩包内顶层目录 (如 "git-cliff-2.11.0/"), 仅保留文件或子目录的相对路径.
     *
     * @param entryName 原始文件路径名称 (可能包含顶层目录前缀)
     * @return 规范化后的文件路径名称, 若无顶层目录则原样返回
     */
    private static String normalizeEntryName(@NotNull String entryName) {
        // 跳过顶层目录（通常压缩包内有一个 git-cliff-{version} 目录）
        int firstSlash = entryName.indexOf('/');
        return firstSlash > 0 ? entryName.substring(firstSlash + 1) : entryName;
    }

    /**
     * 将输入流中的数据写入目标文件, 并根据平台设置可执行权限
     * <p>
     * 该方法负责将压缩包中的单个条目 (文件或目录) 写入目标目录. 如果目标文件是可执行文件且当前系统不是 Windows, 则尝试设置其可执行权限.
     *
     * @param inputStream 输入流, 包含要写入的数据
     * @param targetDir   目标目录路径, 用于构建目标文件路径
     * @param entryName   条目名称(文件或目录名), 用于构建目标文件路径
     * @param executable  是否为可执行文件, 若为 true 且非 Windows 系统, 则尝试设置可执行权限
     * @throws IOException 写入文件或设置权限时发生错误
     */
    private static void writeEntry(@NotNull InputStream inputStream,
                                   @NotNull Path targetDir,
                                   @NotNull String entryName,
                                   boolean executable) throws IOException {
        Path targetFile = targetDir.resolve(entryName);
        Files.createDirectories(targetFile.getParent());

        try (OutputStream outputStream = Files.newOutputStream(targetFile)) {
            inputStream.transferTo(outputStream);
        }

        // 设置可执行权限（非 Windows）
        if (!SystemInfo.isWindows && executable) {
            final boolean b = targetFile.toFile().setExecutable(true);
            if (!b) {
                log.debug("设置可执行权限失败: {}", targetFile);
            }
        }
    }

    /**
     * 查找本地可用的压缩包
     * <p>
     * 在 dists 目录下查找匹配当前系统的压缩包文件。
     *
     * @return 压缩包路径，如果不存在则返回 null
     */
    @Nullable
    public static Path findLocalPackage() {
        Path distsDir = getDistsDir();
        if (!Files.exists(distsDir)) {
            return null;
        }

        try {
            // 获取最新版本，用于匹配压缩包名称
            String tagName = fetchLatestVersion();
            if (tagName == null || tagName.isEmpty()) {
                // 如果无法获取最新版本，尝试查找任何匹配的压缩包
                return findAnyMatchingPackage(distsDir);
            }

            // 生成压缩包名称
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            String packageName = generatePackageName(version);
            Path packagePath = distsDir.resolve(packageName);

            if (Files.exists(packagePath)) {
                return packagePath;
            }

            // 如果精确匹配不存在，尝试查找任何匹配的压缩包
            return findAnyMatchingPackage(distsDir);
        } catch (Exception e) {
            log.debug("查找本地压缩包失败", e);
            return null;
        }
    }

    /**
     * 查找任何匹配当前系统的压缩包
     *
     * @param distsDir dists 目录
     * @return 压缩包路径，如果不存在则返回 null
     */
    @Nullable
    private static Path findAnyMatchingPackage(@NotNull Path distsDir) {
        try {
            String arch = System.getProperty("os.arch");
            String osArch = normalizeArch(arch);
            String extension = SystemInfo.isWindows ? ".zip" : ".tar.gz";

            // 构建匹配模式
            String osPattern;
            if (SystemInfo.isMac) {
                osPattern = "apple-darwin";
            } else if (SystemInfo.isWindows) {
                osPattern = "pc-windows-msvc";
            } else {
                osPattern = "unknown-linux-gnu";
            }

            // 遍历目录查找匹配的文件
            try (java.util.stream.Stream<Path> files = Files.list(distsDir)) {
                return files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.contains(osArch) &&
                               fileName.contains(osPattern) &&
                               fileName.endsWith(extension);
                    })
                    .findFirst()
                    .orElse(null);
            }
        } catch (Exception e) {
            log.debug("查找匹配的压缩包失败", e);
            return null;
        }
    }

    /**
     * 从本地压缩包安装 git-cliff
     *
     * @param archivePath 压缩包路径
     * @param indicator   进度指示器
     * @throws IOException 安装失败时抛出
     */
    public static void installFromLocalPackage(@NotNull Path archivePath,
                                               @NotNull ProgressIndicator indicator) throws IOException {
        if (!Files.exists(archivePath)) {
            throw new IOException("压缩包不存在: " + archivePath);
        }

        getPath(indicator, "Installing from local archive...", archivePath);
    }

    /**
     * 下载并安装 git-cliff
     *
     * @param indicator        进度指示器
     * @param progressListener 进度监听器
     * @return 安装的二进制文件路径
     * @throws IOException 下载或安装失败时抛出
     */
    @NotNull
    public static Path downloadAndInstall(@NotNull ProgressIndicator indicator,
                                          @Nullable DownloadProgressListener progressListener) throws IOException {
        // 1. 获取最新版本
        indicator.setText("Getting the latest version...");
        String tagName = fetchLatestVersion();
        if (tagName == null || tagName.isEmpty()) {
            throw new IOException("无法获取 git-cliff 最新版本");
        }

        // 2. 生成压缩包名称
        String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        String packageName = generatePackageName(version);
        String downloadUrl = buildDownloadUrl(tagName, packageName);

        // 3. 下载压缩包
        Path archivePath = downloadPackage(downloadUrl, indicator, progressListener);

        // 4. 解压到目标目录
        return getPath(indicator, "Extracting...", archivePath);
    }

    /**
     * 从压缩包中提取并安装 git-cliff 二进制文件
     * <p>
     * 该方法负责将指定的压缩包解压到 git-cliff 安装目录, 并返回解压后二进制文件的路径.
     * 若目标目录已存在, 则先删除其内容. 解压完成后, 检查是否成功找到可执行文件.
     * 若未找到, 则抛出异常.
     *
     * @param indicator   进度指示器, 用于更新当前操作状态
     * @param text        操作描述文本, 用于设置进度指示器的显示内容
     * @param archivePath 压缩包路径, 包含待解压的 git-cliff 二进制文件
     * @return 解压后 git-cliff 二进制文件的路径
     * @throws IOException 当解压失败, 未找到可执行文件或发生其他 I/O 错误时抛出
     */
    private static @NotNull Path getPath(@NotNull ProgressIndicator indicator, String text, Path archivePath) throws IOException {
        indicator.setText(text);
        Path targetDir = getGitCliffDir();
        // 如果目录已存在，先删除
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
        extractPackage(archivePath, targetDir);

        // 5. 返回二进制文件路径
        Path binary = getBinaryPath();
        if (binary == null) {
            throw new IOException("解压后未找到 git-cliff 二进制文件");
        }
        return binary;
    }

    /**
     * 获取已安装的 git-cliff 版本号
     *
     * @return 版本号（如 "2.11.0"），如果获取失败则返回 null
     */
    @Nullable
    public static String getInstalledVersion() {
        Path binary = getBinaryPath();
        if (binary == null || !Files.exists(binary)) {
            return null;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(binary.toString(), "--version");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (java.io.InputStream input = process.getInputStream();
                 java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.debug("获取 git-cliff 版本失败，退出码: {}", exitCode);
                return null;
            }

            // 解析版本号，格式通常是 "git-cliff 2.11.0" 或 "git-cliff 2.11.0 (xxx)"
            String versionOutput = output.toString().trim();
            // 使用正则表达式提取版本号
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("git-cliff\\s+(\\d+\\.\\d+\\.\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(versionOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }

            log.debug("无法从输出中解析版本号: {}", versionOutput);
            return null;
        } catch (Exception e) {
            log.debug("获取 git-cliff 版本失败", e);
            return null;
        }
    }

    /**
     * 递归删除目录
     *
     * @param directory 要删除的目录
     * @throws IOException 删除失败时抛出
     */
    private static void deleteDirectory(@NotNull Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()) // 先删除文件，再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.debug("删除文件失败: {}", path, e);
                        }
                    });
            }
        }
    }
}

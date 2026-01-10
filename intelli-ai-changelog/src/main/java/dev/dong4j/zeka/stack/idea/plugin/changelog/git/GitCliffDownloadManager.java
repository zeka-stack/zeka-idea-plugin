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
    private static final String GITHUB_API_URL = "https://api.github.com/repos/orhun/git-cliff/releases/latest";
    private static final String GITHUB_DOWNLOAD_BASE_URL = "https://github.com/orhun/git-cliff/releases/download";
    private static final String PLUGIN_DIR_NAME = "changelog";
    private static final String GIT_CLIFF_DIR_NAME = "git-cliff";
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
        String osName = normalizeOs();
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
     * 规范化操作系统名称
     *
     * @return 操作系统标识
     */
    @NotNull
    private static String normalizeOs() {
        if (SystemInfo.isMac) {
            return "apple-darwin";
        } else if (SystemInfo.isWindows) {
            return "pc-windows-msvc";
        } else {
            return "unknown-linux-gnu";
        }
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

        indicator.setText("正在下载 git-cliff...");
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

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                // 跳过顶层目录（通常压缩包内有一个 git-cliff-{version} 目录）
                int firstSlash = entryName.indexOf('/');
                if (firstSlash > 0) {
                    entryName = entryName.substring(firstSlash + 1);
                }

                Path targetFile = targetDir.resolve(entryName);
                Files.createDirectories(targetFile.getParent());

                try (OutputStream outputStream = Files.newOutputStream(targetFile)) {
                    tarInputStream.transferTo(outputStream);
                }

                // 设置可执行权限（非 Windows）
                if (!SystemInfo.isWindows && entryName.equals("git-cliff")) {
                    targetFile.toFile().setExecutable(true);
                }
            }
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
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                // 跳过顶层目录
                int firstSlash = entryName.indexOf('/');
                if (firstSlash > 0) {
                    entryName = entryName.substring(firstSlash + 1);
                }

                Path targetFile = targetDir.resolve(entryName);
                Files.createDirectories(targetFile.getParent());

                try (OutputStream outputStream = Files.newOutputStream(targetFile)) {
                    zipInputStream.transferTo(outputStream);
                }

                // 设置可执行权限（非 Windows）
                if (!SystemInfo.isWindows && (entryName.equals("git-cliff.exe") || entryName.equals("git-cliff"))) {
                    targetFile.toFile().setExecutable(true);
                }
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
     * @return 安装的二进制文件路径
     * @throws IOException 安装失败时抛出
     */
    @NotNull
    public static Path installFromLocalPackage(@NotNull Path archivePath,
                                               @NotNull ProgressIndicator indicator) throws IOException {
        if (!Files.exists(archivePath)) {
            throw new IOException("压缩包不存在: " + archivePath);
        }

        indicator.setText("正在从本地压缩包安装...");

        // 解压到目标目录
        Path targetDir = getGitCliffDir();
        // 如果目录已存在，先删除
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
        extractPackage(archivePath, targetDir);

        // 返回二进制文件路径
        Path binary = getBinaryPath();
        if (binary == null) {
            throw new IOException("解压后未找到 git-cliff 二进制文件");
        }

        return binary;
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
        indicator.setText("正在获取最新版本...");
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
        indicator.setText("正在解压...");
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
                log.debug("获取 git-cliff 版本失败，退出码: " + exitCode);
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

            log.debug("无法从输出中解析版本号: " + versionOutput);
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
                paths.sorted((a, b) -> b.compareTo(a)) // 先删除文件，再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.debug("删除文件失败: " + path, e);
                        }
                    });
            }
        }
    }
}


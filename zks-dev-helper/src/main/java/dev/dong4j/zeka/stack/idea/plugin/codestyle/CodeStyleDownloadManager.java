package dev.dong4j.zeka.stack.idea.plugin.codestyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Code Style Download Manager
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-01 23:42:16
 * @since hello.world
 */
@Slf4j
public final class CodeStyleDownloadManager {
    private static final Logger LOG = Logger.getInstance(CodeStyleDownloadManager.class);
    private static final String PLUGIN_DIR_NAME = "helper";
    private static final String CODE_STYLE_FILE_PREFIX = "zeka-stack-codestyle-";
    private static final String CODE_STYLE_FILE_SUFFIX = ".xml";
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        CODE_STYLE_FILE_PREFIX + "(\\d+\\.\\d+\\.\\d+)" + CODE_STYLE_FILE_SUFFIX
                                                                  );

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
     * 获取代码样式文件目录
     *
     * @return 代码样式文件目录路径
     */
    @NotNull
    public static Path getCodeStyleDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".zeka-stack", "plugin", PLUGIN_DIR_NAME);
    }

    /**
     * 获取本地代码样式文件路径
     * <p>
     * 扫描目录，查找最新的代码样式文件（按版本号排序）
     *
     * @return 本地代码样式文件路径，如果不存在则返回 null
     */
    @Nullable
    public static Path getLocalCodeStyleFile() {
        Path codeStyleDir = getCodeStyleDir();
        if (!Files.exists(codeStyleDir)) {
            return null;
        }

        try (Stream<Path> paths = Files.list(codeStyleDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith(CODE_STYLE_FILE_PREFIX) &&
                           fileName.endsWith(CODE_STYLE_FILE_SUFFIX);
                })
                .max(Comparator.comparing(path -> {
                    String version = extractVersionFromFileName(path.getFileName().toString());
                    return version != null ? version : "";
                }))
                .orElse(null);
        } catch (IOException e) {
            LOG.warn("Failed to list code style directory", e);
            return null;
        }
    }

    /**
     * 获取本地代码样式文件版本号
     *
     * @return 版本号（如 "1.0.0"），如果不存在则返回 null
     */
    @Nullable
    public static String getLocalVersion() {
        Path localFile = getLocalCodeStyleFile();
        if (localFile == null) {
            return null;
        }
        return extractVersionFromFileName(localFile.getFileName().toString());
    }

    /**
     * 从文件名中提取版本号
     *
     * @param fileName 文件名
     * @return 版本号，如果无法提取则返回 null
     */
    @Nullable
    public static String extractVersionFromFileName(@NotNull String fileName) {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 规范化基础 URL
     * <p>
     * 移除 URL 末尾的斜杠，确保 URL 格式统一
     *
     * @param baseUrl 基础 URL
     * @return 规范化后的 URL
     */
    @NotNull
    private static String normalizeBase(@NotNull String baseUrl) {
        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 获取最新代码样式文件名
     * <p>
     * 通过提供的基础 URL 拼接版本端点，发送 HTTP 请求获取代码样式的最新文件名（如 zeka-stack-codestyle-1.0.0.xml）
     *
     * @param baseUrl 基础 URL，用于构建完整的版本检查地址
     * @return 最新文件名（如 "zeka-stack-codestyle-1.0.0.xml"），如果获取失败则返回 null
     */
    @Nullable
    public static String fetchLatestFileName(@NotNull String baseUrl) {
        String versionEndpoint = normalizeBase(baseUrl) + "/codestyle/version";
        try {
            String fileName = HttpRequests.request(versionEndpoint)
                .productNameAsUserAgent()
                .tuner(connection -> {
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) connection;
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                })
                .readString();
            return fileName.trim();
        } catch (Exception e) {
            log.warn("Failed to fetch latest code style file name from: {}", versionEndpoint, e);
            return null;
        }
    }

    /**
     * 获取最新代码样式版本号
     * <p>
     * 通过提供的基础 URL 拼接版本端点，发送 HTTP 请求获取代码样式的最新文件名，然后提取版本号
     *
     * @param baseUrl 基础 URL，用于构建完整的版本检查地址
     * @return 最新版本号（如 "1.0.1"），如果获取失败则返回 null
     */
    @Nullable
    public static String fetchLatestVersion(@NotNull String baseUrl) {
        String fileName = fetchLatestFileName(baseUrl);
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        // 从文件名中提取版本号
        return extractVersionFromFileName(fileName);
    }

    /**
     * 构建下载 URL
     * <p>
     * 根据基础 URL 和文件名构建完整的下载地址
     *
     * @param baseUrl  基础 URL，将进行规范化处理（移除末尾的斜杠）
     * @param fileName 文件名（如 "zeka-stack-codestyle-1.0.0.xml"）
     * @return 完整的下载 URL
     */
    @NotNull
    public static String buildDownloadUrl(@NotNull String baseUrl, @NotNull String fileName) {
        return normalizeBase(baseUrl) + "/" + fileName;
    }

    /**
     * 下载代码样式文件
     *
     * @param downloadUrl      完整的下载地址
     * @param version          版本号（用于生成文件名）
     * @param indicator        进度指示器
     * @param progressListener 进度监听器
     * @return 下载的文件路径
     * @throws IOException 下载失败时抛出
     */
    @NotNull
    public static Path downloadCodeStyle(@NotNull String downloadUrl,
                                         @NotNull String version,
                                         @NotNull ProgressIndicator indicator,
                                         @Nullable DownloadProgressListener progressListener) throws IOException {
        Path codeStyleDir = getCodeStyleDir();
        Files.createDirectories(codeStyleDir);

        // 根据版本号生成文件名
        String fileName = CODE_STYLE_FILE_PREFIX + version + CODE_STYLE_FILE_SUFFIX;
        Path targetFile = codeStyleDir.resolve(fileName);

        indicator.setText("正在下载代码样式文件...");
        HttpRequests.request(downloadUrl)
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

        log.info("Code style file downloaded successfully: {}", targetFile);
        return targetFile;
    }


    /**
     * 检查并更新代码样式
     * <p>
     * 先调用版本接口获取远程版本，比较本地版本与远程版本，如果版本不一致或本地文件不存在，则下载最新版本
     *
     * @param project          项目对象，可以为 null（在设置页面中）
     * @param baseUrl          基础下载地址（如 https://download.dong4j.site）
     * @param indicator        进度指示器
     * @param progressListener 进度监听器
     * @return 是否进行了更新
     * @throws IOException 下载或更新失败时抛出
     */
    public static boolean checkAndUpdate(@Nullable Project project,
                                         @NotNull String baseUrl,
                                         @NotNull ProgressIndicator indicator,
                                         @Nullable DownloadProgressListener progressListener) throws IOException {
        // 获取本地版本
        String localVersion = getLocalVersion();
        log.info("Local code style version: {}", localVersion);

        // 获取远程文件名
        indicator.setText("正在检查远程版本...");
        String remoteFileName = fetchLatestFileName(baseUrl);
        if (remoteFileName == null || remoteFileName.isEmpty()) {
            throw new IOException("无法获取远程代码样式文件名，请检查下载地址是否正确");
        }
        log.info("Remote code style file name: {}", remoteFileName);

        // 从远程文件名中提取版本号
        String remoteVersion = extractVersionFromFileName(remoteFileName);
        if (remoteVersion == null) {
            throw new IOException("无法从远程文件名中提取版本号: " + remoteFileName);
        }
        log.info("Remote code style version: {}", remoteVersion);

        // 比较版本
        if (localVersion == null || !localVersion.equals(remoteVersion)) {
            // 版本不一致，需要下载
            // 检查本地是否已有该版本的文件
            Path existingFile = getLocalCodeStyleFile();
            boolean needDownload = true;
            if (existingFile != null) {
                String existingVersion = extractVersionFromFileName(existingFile.getFileName().toString());
                if (remoteVersion.equals(existingVersion)) {
                    // 文件已存在且版本匹配，无需重新下载
                    log.info("File already exists with correct version: {}", existingFile);
                    needDownload = false;
                }
            }

            if (needDownload) {
                // 构建下载地址并下载（使用文件名）
                String downloadUrl = buildDownloadUrl(baseUrl, remoteFileName);
                indicator.setText("正在下载最新代码样式...");
                downloadCodeStyle(downloadUrl, remoteVersion, indicator, progressListener);
            }

            // 更新代码样式配置
            indicator.setText("正在更新代码样式配置...");
            UniformCodeStyleSchemeProvider.provideUniformCodeStyleScheme(project);
            log.info("Code style updated from version {} to {}", localVersion, remoteVersion);
            return true;
        } else {
            log.info("Code style is already up to date (version: {})", localVersion);
            // 即使版本相同，如果项目不为 null，也更新一下配置
            if (project != null) {
                UniformCodeStyleSchemeProvider.provideUniformCodeStyleScheme(project);
            }
            return false;
        }
    }
}


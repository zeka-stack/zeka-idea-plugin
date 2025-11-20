package dev.dong4j.zeka.stack.idea.plugin.nacos.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 注册中心服务工具类
 * 提供操作系统判断、文件下载、解压等通用工具方法
 *
 * @author dong4j
 * @since 1.0.0
 */
public class RegistryUtils {

    private static ExecutorService executor;

    /**
     * 判断是否为 macOS 系统
     *
     * @return true 如果是 macOS
     */
    public static boolean isMacOS() {
        return getOS().contains("Mac");
    }

    /**
     * 判断是否为 ARM64 架构
     *
     * @return true 如果是 ARM64
     */
    public static boolean isArm64() {
        String osArch = System.getProperty("os.arch");
        return "aarch64".equals(osArch);
    }

    /**
     * 判断是否为 Windows 系统
     *
     * @return true 如果是 Windows
     */
    public static boolean isWindows() {
        return getOS().contains("Win");
    }

    /**
     * 判断是否为 Linux 系统
     *
     * @return true 如果是 Linux
     */
    public static boolean isLinux() {
        return !isMacOS() && !isWindows();
    }

    /**
     * 获取操作系统名称
     *
     * @return 操作系统名称
     */
    private static String getOS() {
        return System.getProperty("os.name");
    }

    /**
     * 从指定 URL 下载文件
     *
     * @param url    下载 URL
     * @param path   保存路径
     * @param logger 日志记录器
     * @throws IOException IO异常
     */
    public static void download(String url, String path, RegistryLogger logger) throws IOException {
        HttpURLConnection c = null;
        InputStream is = null;
        FileOutputStream os = null;
        File file = new File(path);

        try {
            c = (HttpURLConnection) (new URL(url)).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            if (c.getResponseCode() == 200) {
                FileUtils.deleteQuietly(file);
                FileUtils.touch(file);
                is = c.getInputStream();
                os = new FileOutputStream(file);
                IOUtils.copy(new InterruptDetectInputStream(is), os);
            }
        } catch (Exception ex) {
            logger.info("Download error: " + ex.getMessage());
        } finally {
            close(is);
            close(os);
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception var15) {
                }
            }
        }
    }

    /**
     * 创建文件（如果不存在）
     *
     * @param file 文件对象
     */
    public static void touch(File file) {
        try {
            FileUtils.touch(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取系统行分隔符
     *
     * @return 行分隔符
     */
    public static String getLineSeperator() {
        return isWindows() ? "\r\n" : "\n";
    }

    /**
     * 关闭可关闭资源
     *
     * @param closeable 可关闭资源
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解压 ZIP 文件
     *
     * @param zipFilePath ZIP 文件路径
     * @param destDir     目标目录
     */
    public static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        byte[] buffer = new byte[1024];

        try {
            FileInputStream fis = new FileInputStream(zipFilePath);

            ZipInputStream zis;
            ZipEntry ze;
            for (zis = new ZipInputStream(fis); (ze = zis.getNextEntry()) != null; zis.closeEntry()) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                if (fileName.endsWith(File.separator)) {
                    newFile.mkdirs();
                } else {
                    File parentFile = new File(newFile.getParent());
                    if (parentFile.isFile()) {
                        parentFile.delete();
                        parentFile.mkdir();
                    } else if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }

                    FileOutputStream fos = new FileOutputStream(newFile);

                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    fos.close();
                }
            }

            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 可中断检测的输入流
     * 用于在下载过程中检测线程中断
     */
    static class InterruptDetectInputStream extends InputStream {

        private final InputStream is;

        public InterruptDetectInputStream(InputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Thread interrupted");
            } else {
                return this.is.read();
            }
        }
    }
}

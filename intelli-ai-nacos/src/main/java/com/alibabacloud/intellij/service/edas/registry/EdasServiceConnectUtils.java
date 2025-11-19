package com.alibabacloud.intellij.service.edas.registry;


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

public class EdasServiceConnectUtils {

    private static ExecutorService executor;

    public static boolean isMacOS() {
        return getOS().contains("Mac");
    }

    public static boolean isArm64() {
        String osArch = System.getProperty("os.arch");
        return "aarch64".equals(osArch);
    }

    public static boolean isWindows() {
        return getOS().contains("Win");
    }

    public static boolean isLinux() {
        return !isMacOS() && !isWindows();
    }

    private static String getOS() {
        return System.getProperty("os.name");
    }

    public static void download(String url, String path, EdasServiceConnectLogger logger) throws IOException {
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


    public static void touch(File file) {
        try {
            FileUtils.touch(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getLineSeperator() {
        return isWindows() ? "\r\n" : "\n";
    }


    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

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


    static class InterruptDetectInputStream extends InputStream {
        private final InputStream is;

        public InterruptDetectInputStream(InputStream is) {
            this.is = is;
        }

        public int read() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Thread interrupted");
            } else {
                return this.is.read();
            }
        }
    }
}

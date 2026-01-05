package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * <p>Description : CRC32 校验工具.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public final class Crc32Utils {

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该构造函数为私有, 确保 Crc32Utils 类不能被外部直接实例化
     */
    private Crc32Utils() {
    }

    /**
     * 计算字节数组的 CRC32 值
     *
     * @param bytes 字节数组
     * @return CRC32 值
     */
    public static int calculate(byte[] bytes) {
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        return (int) checksum.getValue();
    }

    /**
     * 计算文件的 CRC32(跳过文件头 20 字节)
     *
     * @param filePath 文件路径
     * @return CRC32 值
     * @throws IOException IO 异常
     */
    public static int calculateFile(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            // 跳过文件头 20 字节
            byte[] header = new byte[20];
            int headerRead = is.read(header);
            if (headerRead < 20) {
                return 0;
            }

            Checksum checksum = new CRC32();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                checksum.update(buffer, 0, bytesRead);
            }
            return (int) checksum.getValue();
        }
    }

    /**
     * 计算文件的 CRC32(全部内容)
     *
     * @param filePath 文件路径
     * @return CRC32 值
     * @throws IOException IO 异常
     */
    public static int calculateFullFile(Path filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath.toFile())) {
            Checksum checksum = new CRC32();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                checksum.update(buffer, 0, bytesRead);
            }
            return (int) checksum.getValue();
        }
    }
}

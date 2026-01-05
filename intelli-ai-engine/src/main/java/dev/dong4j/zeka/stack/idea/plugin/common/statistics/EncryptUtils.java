package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.nio.charset.StandardCharsets;

/**
 * XOR 加密工具.
 *
 * @author dong4j
 * @version 1.4.0
 * @email dong4j@gmail.com
 * @date 2025.01.05
 */
public final class EncryptUtils {

    /** 加密密钥 ("INTELLAI" 的字节值) */
    private static final byte[] ENCRYPT_KEY = {
        0x49, 0x4E, 0x54, 0x45, 0x4C, 0x4C, 0x41, 0x49
    };

    /**
     * 私有构造方法, 防止类被实例化
     * <p> 该工具类仅包含静态方法, 因此不提供公共构造函数 </p>
     */
    private EncryptUtils() {
    }

    /**
     * 加密字节数组
     *
     * @param data 原始数据
     * @return 加密后的数据
     */
    public static byte[] encrypt(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ ENCRYPT_KEY[i % ENCRYPT_KEY.length]);
        }
        return result;
    }

    /**
     * 解密字节数组
     *
     * @param data 加密数据
     * @return 解密后的数据
     */
    public static byte[] decrypt(byte[] data) {
        // XOR 加密是对称的，解密和加密相同
        return encrypt(data);
    }

    /**
     * 加密字符串并转换为十六进制字符串
     *
     * @param str 原始字符串
     * @return 十六进制字符串
     */
    public static String encryptToHex(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        byte[] encrypted = encrypt(str);
        StringBuilder sb = new StringBuilder();
        for (byte b : encrypted) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 解密十六进制字符串
     *
     * @param hex 十六进制字符串
     * @return 解密后的字符串
     */
    public static String decryptFromHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return "";
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                  + Character.digit(hex.charAt(i + 1), 16));
        }
        String decrypted = decryptToString(data);
        // 如果解密后为空字符串，说明原始数据也是空
        return decrypted;
    }

    /**
     * 加密字符串
     *
     * @param str 原始字符串
     * @return 加密后的字节数组
     */
    public static byte[] encrypt(String str) {
        if (str == null || str.isEmpty()) {
            return new byte[0];
        }
        return encrypt(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密字节数组为字符串
     *
     * @param data 加密数据
     * @return 解密后的字符串
     */
    public static String decryptToString(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        return new String(decrypt(data), StandardCharsets.UTF_8);
    }
}

package dev.dong4j.zeka.stack.feedback.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * 签名验证工具类
 * <p>
 * 用于验证 HTTP 请求的 HMAC-SHA256 签名，防止请求被伪造和重放攻击。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.23
 * @since 1.0.0
 */
@Slf4j
public class SignatureVerifier {
    /** 时间戳允许的误差范围（秒） */
    private static final long TIMESTAMP_TOLERANCE = 300;

    /**
     * 常量时间比较两个字符串（防止时序攻击）
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return 是否相等
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }

    /**
     * 计算 SHA256 哈希（十六进制）
     *
     * @param data 数据
     * @return SHA256 哈希值（十六进制字符串）
     * @throws Exception 计算失败
     */
    public static String sha256Hex(byte[] data) throws Exception {
        if (data == null) {
            data = new byte[0];
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 计算 HMAC-SHA256 签名（Base64 编码）
     *
     * @param secret  密钥
     * @param message 消息
     * @return Base64 编码的签名
     * @throws Exception 计算失败
     */
    public static String hmacBase64(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig);
    }

    /**
     * 验证签名
     *
     * @param secret        密钥
     * @param method        HTTP 方法
     * @param pathWithQuery 路径和查询参数
     * @param body          请求体（原始字节）
     * @param timestamp     时间戳（字符串）
     * @param nonce         随机数
     * @param bodySha256    请求体 SHA256（十六进制）
     * @param signature     签名
     * @return 验证是否通过
     * @throws Exception 验证失败
     */
    public static boolean verify(
        String secret,
        String method,
        String pathWithQuery,
        byte[] body,
        String timestamp,
        String nonce,
        String bodySha256,
        String signature
                                ) throws Exception {
        // 1. 验证时间戳（必须在 ±300 秒内）
        long now = Instant.now().getEpochSecond();
        long t;
        try {
            t = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
        if (Math.abs(now - t) > TIMESTAMP_TOLERANCE) {
            log.warn("Timestamp out of tolerance: now={}, request={}, diff={}", now, t, Math.abs(now - t));
            return false;
        }

        // 2. 验证请求体 SHA256（如果提供了）
        if (bodySha256 != null && !bodySha256.isEmpty()) {
            String computedBodySha = sha256Hex(body == null ? new byte[0] : body);
            if (!computedBodySha.equalsIgnoreCase(bodySha256)) {
                log.warn("Body SHA256 mismatch: expected={}, computed={}", bodySha256, computedBodySha);
                return false;
            }
        }

        // 3. 重新计算签名
        String computedBodySha = sha256Hex(body == null ? new byte[0] : body);
        String canonical = method.toUpperCase() + "\n"
                           + pathWithQuery + "\n"
                           + computedBodySha + "\n"
                           + timestamp + "\n"
                           + nonce;

        String expected = hmacBase64(secret, canonical);

        // 4. 常量时间比较签名
        boolean isValid = constantTimeEquals(expected, signature);
        if (!isValid) {
            log.warn("Signature mismatch for client");
        }
        return isValid;
    }
}


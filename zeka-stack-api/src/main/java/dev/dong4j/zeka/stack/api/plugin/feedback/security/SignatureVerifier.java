package dev.dong4j.zeka.stack.api.plugin.feedback.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * 签名验证器类
 * <p> 提供用于验证请求签名的工具方法, 适用于需要通过 HMAC-SHA256 算法验证请求签名的场景, 如 API 接口安全校验.
 * 该类不负责请求处理, 仅专注于签名验证逻辑, 确保请求的时间戳, 请求体哈希, 方法, 路径, 随机数等参数符合预期.
 * 通过常量时间比较避免时序攻击, 支持对请求体 SHA256 值的校验, 确保数据完整性.
 * 适用于内部组件调用, 用于保障接口调用的安全性与一致性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
public class SignatureVerifier {
    /** 时间戳允许的误差范围 (秒) */
    private static final long TIMESTAMP_TOLERANCE = 300;

    /**
     * 以常量时间比较两个字符串, 防止时序攻击
     * <p> 通过逐字节异或并累加结果, 确保比较耗时与字符串长度无关, 避免因比较时间差异泄露信息
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return 是否相等, 若任一参数为 null 则返回 false
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
     * 计算字节数组的 SHA256 哈希值并返回十六进制字符串
     * <p> 该方法使用 {@code MessageDigest} 计算输入字节数组的 SHA256 哈希值, 并将结果转换为小写十六进制字符串表示.
     * 若输入数据为 null, 则默认使用空字节数组进行计算.
     *
     * @param data 输入字节数组
     * @return SHA256 哈希值的十六进制字符串表示
     * @throws Exception 当无法获取 SHA-256 算法实例或计算失败时抛出
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
     * 使用 HMAC-SHA256 算法计算签名并进行 Base64 编码
     * <p> 该方法通过指定的密钥和消息, 生成符合 HMAC-SHA256 标准的签名, 并以 Base64 格式返回.
     * 适用于需要生成请求签名以验证身份或防篡改的场景.
     *
     * @param secret  密钥, 用于 HMAC 算法的密钥材料
     * @param message 消息内容, 用于与密钥共同计算签名
     * @return Base64 编码的 HMAC-SHA256 签名字符串
     * @throws Exception 当算法实例化, 密钥初始化或签名计算失败时抛出
     */
    public static String hmacBase64(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig);
    }

    /**
     * 验证 HTTP 请求的 HMAC-SHA256 签名, 防止请求被伪造和重放攻击
     * <p> 该方法通过密钥,HTTP 方法, 路径, 请求体, 时间戳, 随机数和签名值计算并比对签名, 确保请求来源合法且未被篡改.
     * 时间戳允许的误差范围由常量 {@code TIMESTAMP_TOLERANCE} 定义 (默认 300 秒).
     * 请求体的 SHA256 哈希值若提供, 将被验证是否与实际请求体一致.
     * 签名比较使用常量时间比较算法, 防止时序攻击.
     *
     * @param secret        密钥, 用于生成 HMAC-SHA256 签名
     * @param method        HTTP 方法, 如 "GET","POST" 等, 将转换为大写用于签名计算
     * @param pathWithQuery 路径和查询参数, 如 "/api/v1/user?name=jack"
     * @param body          请求体原始字节数据, 若为 null 则视为空字节数组
     * @param timestamp     时间戳, 以秒为单位的字符串格式, 用于防止重放攻击
     * @param nonce         随机数, 用于防止重放攻击
     * @param bodySha256    请求体的 SHA256 哈希值 (十六进制字符串), 若提供则用于验证请求体一致性
     * @param signature     签名值,Base64 编码的 HMAC-SHA256 结果, 用于与计算值比对
     * @return 验证是否通过, 若所有校验通过则返回 true, 否则返回 false
     * @throws Exception 验证过程中发生异常, 如时间戳格式错误,SHA256 计算失败, 签名不匹配等
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
            log.debug("Invalid timestamp format: {}", timestamp);
            return false;
        }
        if (Math.abs(now - t) > TIMESTAMP_TOLERANCE) {
            log.debug("Timestamp out of tolerance: now={}, request={}, diff={}", now, t, Math.abs(now - t));
            return false;
        }

        // 2. 验证请求体 SHA256（如果提供了）
        if (bodySha256 != null && !bodySha256.isEmpty()) {
            String computedBodySha = sha256Hex(body == null ? new byte[0] : body);
            if (!computedBodySha.equalsIgnoreCase(bodySha256)) {
                log.debug("Body SHA256 mismatch: expected={}, computed={}", bodySha256, computedBodySha);
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
            log.debug("Signature mismatch for client");
        }
        return isValid;
    }
}


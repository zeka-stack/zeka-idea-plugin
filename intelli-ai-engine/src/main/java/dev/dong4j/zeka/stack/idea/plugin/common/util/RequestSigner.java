package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Request Signer
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-24 18:32:45
 * @since hello.world
 */
public class RequestSigner {

    /**
     * 签名头信息
     */
    public record SignedHeaders(
        @NotNull String clientId,
        @NotNull String timestamp,
        @NotNull String nonce,
        @NotNull String bodySha256,
        @NotNull String signature
    ) {
    }

    /**
     * 计算 SHA256 哈希（十六进制）
     *
     * @param data 数据
     * @return SHA256 哈希值（十六进制字符串）
     * @throws Exception 计算失败
     */
    @NotNull
    public static String sha256Hex(byte @Nullable [] data) throws Exception {
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
    @NotNull
    public static String hmacBase64(@NotNull String secret, @NotNull String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig);
    }


    /**
     * 为请求生成签名头
     *
     * @param clientId      客户端 ID
     * @param secret        密钥
     * @param method        HTTP 方法（GET、POST 等）
     * @param pathWithQuery 路径和查询参数（例如：/api/feedback?x=1）
     * @param bodyOrNull    请求体（可以为 null）
     * @return 签名头信息
     * @throws Exception 签名失败
     */
    @NotNull
    public static SignedHeaders sign(
        @NotNull String clientId,
        @NotNull String secret,
        @NotNull String method,
        @NotNull String pathWithQuery,
        byte @Nullable [] bodyOrNull
                                    ) throws Exception {
        long ts = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString();

        byte[] body = (bodyOrNull == null) ? new byte[0] : bodyOrNull;
        String bodySha = sha256Hex(body);

        // 构建规范字符串（使用 \n 分隔）
        String canonical = method.toUpperCase() + "\n"
                           + pathWithQuery + "\n"
                           + bodySha + "\n"
                           + ts + "\n"
                           + nonce;

        String signature = hmacBase64(secret, canonical);

        return new SignedHeaders(clientId, String.valueOf(ts), nonce, bodySha, signature);
    }

}


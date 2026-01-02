package dev.dong4j.zeka.stack.idea.javadoc.util;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 提供者配置工具类
 * <p>
 * 提供生成唯一标识符和比较提供者配置是否相等的工具方法, 主要用于 AI 服务提供者的配置管理
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ProviderConfigUtils {
    /**
     * 生成提供商配置的唯一标识符 (MD5)
     * <p>
     * 将提供商 ID, 模型名称,API 密钥和基础 URL 等参数拼接后, 使用 MD5 算法生成唯一标识符.
     * 如果 MD5 算法不可用, 则使用字符串的哈希码作为替代.
     *
     * @param providerId 提供商 ID, 可以为 null
     * @param modelName  模型名称, 可以为 null
     * @param apiKey     API 密钥, 可以为 null
     * @param baseUrl    基础 URL, 可以为 null
     * @return 基于所有参数的 MD5 哈希值, 若 MD5 不可用则返回字符串的哈希码
     */
    public static String generateUniqueId(@Nullable String providerId,
                                          @Nullable String modelName,
                                          @Nullable String apiKey,
                                          @Nullable String baseUrl) {
        String combined = (providerId != null ? providerId : "") +
                          (modelName != null ? modelName : "") +
                          (apiKey != null ? apiKey : "") +
                          (baseUrl != null ? baseUrl : "");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 这种情况理论上不会发生，因为MD5是Java标准算法
            return Integer.toString(combined.hashCode());
        }
    }

    /**
     * 比较两个提供商配置是否相等
     * <p>
     * 通过生成唯一标识符来比较两个提供商配置是否相等. 如果两个配置生成的唯一标识符相同, 则认为配置相等.
     *
     * @param providerId1 第一个提供商 ID, 可以为 null
     * @param modelName1  第一个模型名称, 可以为 null
     * @param apiKey1     第一个 API 密钥, 可以为 null
     * @param baseUrl1    第一个基础 URL, 可以为 null
     * @param providerId2 第二个提供商 ID, 可以为 null
     * @param modelName2  第二个模型名称, 可以为 null
     * @param apiKey2     第二个 API 密钥, 可以为 null
     * @param baseUrl2    第二个基础 URL, 可以为 null
     * @return 如果两个配置相等返回 true, 否则返回 false
     */
    public static boolean areEqual(@Nullable String providerId1,
                                   @Nullable String modelName1,
                                   @Nullable String apiKey1,
                                   @Nullable String baseUrl1,
                                   @Nullable String providerId2,
                                   @Nullable String modelName2,
                                   @Nullable String apiKey2,
                                   @Nullable String baseUrl2) {
        String id1 = generateUniqueId(providerId1, modelName1, apiKey1, baseUrl1);
        String id2 = generateUniqueId(providerId2, modelName2, apiKey2, baseUrl2);
        return id1.equals(id2);
    }
}

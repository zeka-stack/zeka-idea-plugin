package dev.dong4j.zeka.stack.idea.plugin.util;

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
     * 生成提供商配置的唯一标识符（MD5）
     * <p>
     * 将提供商ID、模型名称、API密钥和基础URL等参数拼接后，使用MD5算法生成唯一标识符。
     * 如果MD5算法不可用，则使用字符串的哈希码作为替代。
     *
     * @param providerId 提供商ID，可以为null
     * @param modelName  模型名称，可以为null
     * @param apiKey     API密钥，可以为null
     * @param baseUrl    基础URL，可以为null
     * @return 基于所有参数的MD5哈希值，若MD5不可用则返回字符串的哈希码
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
     * 通过生成唯一标识符来比较两个提供商配置是否相等。如果两个配置生成的唯一标识符相同，则认为配置相等。
     *
     * @param providerId1 第一个提供商ID
     * @param modelName1  第一个模型名称
     * @param apiKey1     第一个API密钥
     * @param baseUrl1    第一个基础URL
     * @param providerId2 第二个提供商ID
     * @param modelName2  第二个模型名称
     * @param apiKey2     第二个API密钥
     * @param baseUrl2    第二个基础URL
     * @return 如果两个配置相等返回true，否则返回false
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

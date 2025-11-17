package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 提供与 Provider 配置相关的工具方法
 * <p>
 * 该类封装了与 Provider 配置相关的实用方法, 主要提供唯一标识生成和配置信息比较功能.
 * <p>
 * generateUniqueId 方法根据给定的 providerId,modelName,apiKey 和 baseUrl 生成一个唯一的 MD5 标识符, 若 MD5 生成失败则使用默认的 hashCode 值.
 * areEqual 方法用于比较两个 Provider 配置是否相等, 通过生成唯一标识符进行比较.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public final class ProviderConfigUtils {
    /**
     * 私有构造函数, 防止外部实例化 {@code ProviderConfigUtils}.
     * 该类仅提供静态工具方法, 不需要创建对象.
     */
    private ProviderConfigUtils() {}

    /**
     * 生成唯一的标识符
     * <p>
     * 根据提供的参数拼接字符串并使用 MD5 算法生成唯一标识符. 如果 MD5 算法不可用, 则使用字符串的 hashCode 方法生成一个字符串形式的哈希值.
     *
     * @param providerId 服务提供商 ID, 可为空
     * @param modelName  模型名称, 可为空
     * @param apiKey     API 密钥, 可为空
     * @param baseUrl    基础 URL, 可为空
     * @return 生成的唯一标识符字符串
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
            return Integer.toString(combined.hashCode());
        }
    }

    /**
     * 判断两个模型配置是否相等
     * <p>
     * 通过生成唯一的标识字符串并比较这两个标识来判断两个模型配置是否一致.
     *
     * @param providerId1 第一个模型的提供商 ID
     * @param modelName1  第一个模型的名称
     * @param apiKey1     第一个模型的 API 密钥
     * @param baseUrl1    第一个模型的基础 URL
     * @param providerId2 第二个模型的提供商 ID
     * @param modelName2  第二个模型的名称
     * @param apiKey2     第二个模型的 API 密钥
     * @param baseUrl2    第二个模型的基础 URL
     * @return 如果两个模型配置生成的唯一标识相同则返回 true, 否则返回 false
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

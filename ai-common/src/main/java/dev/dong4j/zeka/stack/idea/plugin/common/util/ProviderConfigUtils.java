package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 提供商配置工具类。
 */
public final class ProviderConfigUtils {
    private ProviderConfigUtils() {}

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

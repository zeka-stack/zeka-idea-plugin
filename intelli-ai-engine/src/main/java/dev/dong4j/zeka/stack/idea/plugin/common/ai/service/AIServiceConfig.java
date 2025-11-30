package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * AI 服务配置类
 * <p>
 * 用于配置 AI 服务的相关参数, 包括提供商类型, 模型名称, 基础 URL,API 密钥,
 * 温度参数, 最大令牌数, 最大重试次数和超时时间等. 提供创建默认配置的静态方法.
 * 注意: 此类已被标记为废弃, 建议使用新的配置方式.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 * @deprecated 使用 {@link AIProviderConfig}
 */
@Deprecated
public class AIServiceConfig {
    /** AI 服务提供商类型 */
    @NotNull
    public AIProviderType providerType = AIProviderType.QIANWEN;

    /** 模型名称 */
    public String modelName;

    /** 基础 URL（可选，使用默认值） */
    @Nullable
    public String baseUrl;

    /** API 密钥 */
    public String apiKey;

    /** 温度参数（0.0-2.0，默认 0.7） */
    public double temperature = 0.7;

    /** 最大 Token 数（默认 2000） */
    public int maxTokens = 2000;

    /** 最大重试次数（默认 3） */
    public int maxRetries = 3;

    /** 请求超时时间（毫秒，默认 30000） */
    public int timeout = 30000;

    /**
     * 快速创建配置（使用默认值）
     */
    public static AIServiceConfig createDefault(@NotNull AIProviderType providerType,
                                                @NotNull String apiKey) {
        AIServiceConfig config = new AIServiceConfig();
        config.providerType = providerType;
        config.apiKey = apiKey;
        config.modelName = providerType.getDefaultModel();
        config.baseUrl = providerType.getDefaultBaseUrl();
        return config;
    }
}


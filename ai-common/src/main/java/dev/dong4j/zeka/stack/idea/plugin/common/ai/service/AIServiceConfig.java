package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI 服务配置（简化版）
 * <p>
 * 为外部开发者提供简化的配置选项，内部自动转换为完整的配置对象。
 */
public class AIServiceConfig {
    /** AI 服务提供商类型 */
    @NotNull
    public AIProviderType providerType = AIProviderType.QIANWEN;

    /** 模型名称 */
    @NotNull
    public String modelName;

    /** 基础 URL（可选，使用默认值） */
    @Nullable
    public String baseUrl;

    /** API 密钥 */
    @NotNull
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


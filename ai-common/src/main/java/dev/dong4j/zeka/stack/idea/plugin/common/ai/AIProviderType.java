package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * AI 服务提供商类型枚举
 *
 * <p>统一管理所有支持的 AI 服务提供商配置信息，包含默认地址、默认模型和常用模型列表。
 */
public enum AIProviderType {

    CUSTOM(
        "custom",
        "OpenAI API",
        "https://api.openai.com/v1",
        "gpt-3.5-turbo",
        true,
        true,
        List.of("gpt-3.5-turbo", "gpt-4o-mini")
    ),

    QIANWEN(
        "qianwen",
        "通义千问",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen3-8b",
        true,
        false,
        Arrays.asList("qwen3-32b", "qwen3-14b", "qwen3-8b", "qwen3-4b")
    ),

    SILICONFLOW(
        "siliconflow",
        "硅基流动",
        "https://api.siliconflow.cn/v1",
        "Qwen/Qwen3-8B",
        true,
        false,
        List.of("Qwen/Qwen3-8B", "Qwen/Qwen2.5-14B-Instruct", "THUDM/glm-4-9b-chat")
    ),

    OLLAMA(
        "ollama",
        "Ollama",
        "http://localhost:11434/v1",
        "qwen:7b",
        false,
        true,
        Arrays.asList("gpt-oss:120b-cloud", "deepseek-r1:14b", "qwen3-8b")
    ),

    LM_STUDIO(
        "lmstudio",
        "LM Studio",
        "http://localhost:1234/v1",
        "gpt-3.5-turbo",
        false,
        true,
        List.of("qwen3-8b")
    );

    private final String providerId;
    private final String displayName;
    private final String defaultBaseUrl;
    private final String defaultModel;
    private final boolean requiresApiKey;
    private final boolean baseUrlEditable;
    private final List<String> supportedModels;

    AIProviderType(@NotNull String providerId,
                   @NotNull String displayName,
                   @NotNull String defaultBaseUrl,
                   @NotNull String defaultModel,
                   boolean requiresApiKey,
                   boolean baseUrlEditable,
                   @NotNull List<String> supportedModels) {
        this.providerId = providerId;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.requiresApiKey = requiresApiKey;
        this.baseUrlEditable = baseUrlEditable;
        this.supportedModels = supportedModels;
    }

    @NotNull
    public String getProviderId() {
        return providerId;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    @NotNull
    public String getDefaultModel() {
        return defaultModel;
    }

    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    public boolean isBaseUrlEditable() {
        return baseUrlEditable;
    }

    @NotNull
    public List<String> getSupportedModels() {
        return supportedModels;
    }

    @Nullable
    public static AIProviderType fromProviderId(@NotNull String providerId) {
        for (AIProviderType type : values()) {
            if (type.providerId.equals(providerId)) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    public static AIProviderType fromDisplayName(@NotNull String displayName) {
        for (AIProviderType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    public static String getProviderIdByDisplayName(@NotNull String displayName) {
        AIProviderType type = fromDisplayName(displayName);
        return type != null ? type.providerId : null;
    }

    @Nullable
    public static String getDisplayNameByProviderId(@NotNull String providerId) {
        AIProviderType type = fromProviderId(providerId);
        return type != null ? type.displayName : null;
    }

    @NotNull
    public static List<String> getAllProviderIds() {
        return Arrays.stream(values()).map(AIProviderType::getProviderId).toList();
    }

    @NotNull
    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values()).map(AIProviderType::getDisplayName).toList();
    }
}

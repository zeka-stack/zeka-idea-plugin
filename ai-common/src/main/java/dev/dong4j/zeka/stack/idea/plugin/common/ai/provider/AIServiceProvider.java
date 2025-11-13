package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;

/**
 * AI 服务提供商接口。
 */
public interface AIServiceProvider {

    @NotNull
    AIProviderType getProviderType();

    @NotNull
    String getModelName();

    @NotNull
    String getBaseUrl();

    @NotNull
    String generateContent(@NotNull AIChatRequest request,
                           @Nullable String apiKey,
                           @Nullable AIResponseListener listener) throws AIServiceException;

    @NotNull
    ValidationResult validateConfiguration(@Nullable String apiKey);

    @NotNull
    List<String> getAvailableModels(@Nullable String apiKey);
}

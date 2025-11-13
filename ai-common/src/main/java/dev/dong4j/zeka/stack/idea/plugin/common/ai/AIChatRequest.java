package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;

/**
 * AI 对话请求，包含系统提示词和用户提示词。
 */
public record AIChatRequest(@NotNull String systemPrompt,
                            @NotNull String userPrompt,
                            int promptTokenEstimate) {
    public AIChatRequest(@NotNull String systemPrompt, @NotNull String userPrompt) {
        this(systemPrompt, userPrompt, 0);
    }
}

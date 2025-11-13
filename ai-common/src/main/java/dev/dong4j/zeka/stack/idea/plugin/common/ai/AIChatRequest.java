package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;

/**
 * AI 对话请求，包含系统提示词和用户提示词。
 */
public record AIChatRequest(@NotNull String systemPrompt,
                            @NotNull String userPrompt,
                            int promptTokenEstimate) {
    /**
     * 创建一个 AI 聊天请求对象
     * <p>
     * 使用指定的系统提示和用户提示初始化 AI 聊天请求, 并设置默认的超时时间为 0
     *
     * @param systemPrompt 系统提示内容
     * @param userPrompt   用户提示内容
     * @throws NullPointerException 如果 systemPrompt 或 userPrompt 为 null 时抛出
     */
    public AIChatRequest(@NotNull String systemPrompt, @NotNull String userPrompt) {
        this(systemPrompt, userPrompt, 0);
    }
}

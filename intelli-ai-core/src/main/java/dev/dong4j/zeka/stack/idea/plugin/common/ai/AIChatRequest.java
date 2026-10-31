package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;

/**
 * AI 聊天请求记录类
 * <p>
 * 用于封装 AI 聊天的请求参数, 包括系统提示词, 用户提示词和提示词 token 预估数量
 * 该记录类提供了创建 AI 聊天请求的标准化数据结构
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
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

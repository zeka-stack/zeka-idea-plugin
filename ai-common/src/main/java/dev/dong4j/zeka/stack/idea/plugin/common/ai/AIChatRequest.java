package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;

/**
 * AI 聊天请求记录类
 * <p>
 * 用于封装 AI 聊天请求的相关参数, 包括系统提示语, 用户提示语以及预估的提示词令牌数.
 * 该类提供了构造方法, 用于初始化请求对象, 支持通过系统提示语和用户提示语创建实例, 并默认设置提示词令牌数为 0.
 *
 * @author 作者名
 * @version 1.0.0
 * @date 2025.10.24
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

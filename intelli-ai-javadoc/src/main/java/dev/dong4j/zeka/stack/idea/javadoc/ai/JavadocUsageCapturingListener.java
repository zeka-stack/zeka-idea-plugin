package dev.dong4j.zeka.stack.idea.javadoc.ai;

import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import lombok.Getter;

/**
 * AI 使用量捕获监听器
 * <p>
 * 用于捕获 token 使用情况，并可选地委托给其他监听器进行日志输出。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.01
 * @since 1.0.0
 */
public class JavadocUsageCapturingListener implements AIResponseListener {

    /** 用于委托实际的 AI 响应监听逻辑, 可选地将事件转发给其他监听器进行日志或统计处理. */
    private final AIResponseListener delegate;
    /** promptTokens 值, 表示提示词使用的 token 数量, 用于统计 AI 请求的输入成本. */
    @Getter
    private volatile int promptTokens;
    /** 完成令牌数量, 用于记录模型生成响应所消耗的令牌数 */
    @Getter
    private volatile int completionTokens;
    /** 总 token 数量, 用于记录请求和响应过程中消耗的总 tokens 数量. */
    @Getter
    private volatile int totalTokens;

    /**
     * 初始化 AI 使用量捕获监听器
     * <p> 构造函数, 用于创建一个监听器实例, 可选地委托给其他监听器处理请求和响应事件.
     *
     * @param delegate 可选的委托监听器, 用于将事件转发给其他处理器, 若为 null 则不转发
     */
    public JavadocUsageCapturingListener(@Nullable AIResponseListener delegate) {
        this.delegate = delegate;
    }

    /**
     * 处理请求事件, 将请求信息转发给委托监听器
     * <p> 当存在委托监听器时, 将提供者名称, 模型名称, 请求体和验证状态传递给委托监听器的 onRequest 方法
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容
     * @param validation   是否进行验证
     */
    @Override
    public void onRequest(String providerName, String modelName, String requestBody, boolean validation) {
        if (delegate != null) {
            delegate.onRequest(providerName, modelName, requestBody, validation);
        }
    }

    /**
     * 处理 AI 响应事件
     * <p> 在接收到 AI 响应后, 如果存在委托监听器, 则将响应信息转发给委托监听器进行后续处理.
     * 该方法允许在不修改原始监听器逻辑的前提下, 扩展响应处理能力.
     *
     * @param providerName 提供商名称, 如 "OpenAI","Anthropic" 等
     * @param modelName    模型名称, 如 "gpt-4","claude-3" 等
     * @param responseBody 响应内容的原始字符串
     * @param validation   是否已验证响应数据的有效性
     */
    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        if (delegate != null) {
            delegate.onResponse(providerName, modelName, responseBody, validation);
        }
    }

    /**
     * 处理 AI 使用量统计回调
     * <p> 记录当前请求的 promptTokens,completionTokens 和 totalTokens, 并可选地将统计信息转发给委托监听器.
     *
     * @param providerName     服务提供商名称
     * @param modelName        模型名称
     * @param promptTokens     提示词 token 数量
     * @param completionTokens 完成词 token 数量
     * @param totalTokens      总 token 数量
     */
    @Override
    public void onUsage(String providerName,
                        String modelName,
                        int promptTokens,
                        int completionTokens,
                        int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        if (delegate != null) {
            delegate.onUsage(providerName, modelName, promptTokens, completionTokens, totalTokens);
        }
    }

}

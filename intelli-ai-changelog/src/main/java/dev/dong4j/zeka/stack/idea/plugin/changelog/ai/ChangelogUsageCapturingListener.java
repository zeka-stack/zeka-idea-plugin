package dev.dong4j.zeka.stack.idea.plugin.changelog.ai;

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
 * @date 2026.01.19
 * @since 1.0.0
 */
public class ChangelogUsageCapturingListener implements AIResponseListener {

    /** 被委托的 AI 响应监听器, 用于将事件转发到其他监听器进行日志或处理. */
    private final AIResponseListener delegate;
    /** promptTokens 用于记录提示词所消耗的 token 数量, 支持并发安全更新 */
    @Getter
    private volatile int promptTokens;
    /** 完成阶段使用的 token 数量, 用于统计 AI 请求总消耗 */
    @Getter
    private volatile int completionTokens;
    /** 总 token 数量, 用于记录请求和响应过程中消耗的总 token 数量 */
    @Getter
    private volatile int totalTokens;

    /**
     * 初始化使用量捕获监听器
     * <p> 创建一个监听器实例, 用于捕获 AI 请求的 token 使用情况, 并可选地将事件委托给其他监听器.
     *
     * @param delegate 可选的委托监听器, 用于将事件转发给其他处理器, 如果为 null 则不进行委托
     */
    public ChangelogUsageCapturingListener(@Nullable AIResponseListener delegate) {
        this.delegate = delegate;
    }

    /**
     * 处理请求事件, 将请求信息转发给委托监听器
     * <p> 当存在委托监听器时, 将提供者名称, 模型名称, 请求体和验证状态传递给委托监听器的 onRequest 方法 </p>
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
     * 处理 AI 响应, 将响应数据传递给委托监听器
     * <p> 当存在委托监听器时, 将提供方名称, 模型名称, 响应体和验证状态转发给委托监听器的 onResponse 方法 </p>
     *
     * @param providerName 提供方名称
     * @param modelName    模型名称
     * @param responseBody 响应体内容
     * @param validation   是否已验证
     */
    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        if (delegate != null) {
            delegate.onResponse(providerName, modelName, responseBody, validation);
        }
    }

    /**
     * 处理 AI 使用量统计回调
     * <p> 记录当前请求的 promptTokens,completionTokens 和 totalTokens, 并将数据传递给委托监听器 (如果存在).
     * 该方法用于在 AI 响应完成后更新内部计数器, 并可选地将使用量数据转发给其他监听器.
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

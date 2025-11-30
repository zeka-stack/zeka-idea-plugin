package dev.dong4j.zeka.stack.idea.plugin.common.ai;

/**
 * AI 响应监听器接口
 * <p>
 * 用于监听 AI 服务的请求, 响应和使用情况, 提供默认的空实现方法,
 * 实现类可以选择性地重写需要的方法来处理特定的事件回调
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public interface AIResponseListener {

    /**
     * 处理请求方法
     * <p>
     * 用于处理来自指定提供者和模型的请求, 可选择是否进行参数校验
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容
     * @param validation   是否进行参数校验
     */
    default void onRequest(String providerName, String modelName, String requestBody, boolean validation)   {}

    /**
     * 处理来自指定提供者的响应数据
     * <p>
     * 该方法用于接收并处理由指定提供者返回的模型响应数据, 包含响应体和验证结果.
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param responseBody 响应体内容
     * @param validation   验证结果标志
     */
    default void onResponse(String providerName, String modelName, String responseBody, boolean validation) {}

    /**
     * 处理模型使用事件的回调方法
     * <p>
     * 当模型被使用时触发此方法, 用于记录或处理模型的使用情况, 包括提供者名称, 模型名称以及使用的令牌数.
     *
     * @param providerName     提供者名称
     * @param modelName        模型名称
     * @param promptTokens     提示部分使用的令牌数
     * @param completionTokens 完成部分使用的令牌数
     * @param totalTokens      总共使用的令牌数
     */
    default void onUsage(String providerName,
                         String modelName,
                         int promptTokens,
                         int completionTokens,
                         int totalTokens) {}
}

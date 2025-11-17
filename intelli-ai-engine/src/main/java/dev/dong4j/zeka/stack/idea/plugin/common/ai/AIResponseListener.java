package dev.dong4j.zeka.stack.idea.plugin.common.ai;

/**
 * AI 响应监听器接口
 * <p>
 * 该接口定义了在 AI 服务请求, 响应以及使用量统计过程中的回调方法. 实现类可以通过覆盖这些默认方法来接收并处理相应事件, 例如记录日志, 更新 UI 或进行业务逻辑处理.
 * <p>
 * 典型使用场景:
 * <ul>
 *   <li>在调用第三方 AI 提供商 (如 OpenAI,Azure AI 等) 时, 捕获请求与响应信息.</li>
 *   <li>统计 token 使用量, 用于计费或监控.</li>
 *   <li>实现统一的错误处理或重试机制.</li>
 * </ul>
 * <p>
 * 该接口采用 Observer(观察者)模式, 允许多处注册监听器并在事件发生时统一通知.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.11.14
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

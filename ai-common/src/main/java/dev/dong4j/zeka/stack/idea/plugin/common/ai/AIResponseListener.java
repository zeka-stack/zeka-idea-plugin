package dev.dong4j.zeka.stack.idea.plugin.common.ai;

/**
 * AI 请求过程监听器。
 */
public interface AIResponseListener {

    default void onRequest(String providerName, String modelName, String requestBody, boolean validation)   {}

    default void onResponse(String providerName, String modelName, String responseBody, boolean validation) {}

    default void onUsage(String providerName,
                         String modelName,
                         int promptTokens,
                         int completionTokens,
                         int totalTokens) {}
}

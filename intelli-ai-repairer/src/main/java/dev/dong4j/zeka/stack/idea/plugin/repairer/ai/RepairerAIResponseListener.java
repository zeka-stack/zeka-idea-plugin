package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * AI 响应日志监听器
 * <p> 输出请求、响应与 token 使用信息到控制台。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.02.02
 * @since 1.0.0
 */
public class RepairerAIResponseListener implements AIResponseListener {
    /** 当前项目实例, 用于日志输出和上下文管理 */
    private final Project project;

    /**
     * 构造函数
     *
     * @param project 当前项目
     */
    public RepairerAIResponseListener(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理 AI 请求事件
     * <p> 在接收到 AI 请求时, 输出请求信息到控制台, 包括提供者名称, 模型名称和请求体内容 (如果存在).
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容, 可能为 null 或空字符串
     * @param validation   是否进行验证
     */
    @Override
    public void onRequest(String providerName, String modelName, String requestBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("请求: %s - %s", providerName, modelName));
        if (requestBody != null && !requestBody.isEmpty()) {
            AIConsoleLoggerUtil.print(project, requestBody);
        }
    }

    /**
     * 处理 AI 响应事件
     * <p> 在接收到 AI 响应时, 打印响应标识信息到控制台, 并在响应内容非空时输出完整响应体
     *
     * @param providerName 服务提供商名称
     * @param modelName    模型名称
     * @param responseBody 响应内容字符串, 可能为 null 或空字符串
     * @param validation   是否已验证响应内容
     */
    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("响应: %s - %s", providerName, modelName));
        if (responseBody != null && !responseBody.isEmpty()) {
            AIConsoleLoggerUtil.print(project, responseBody);
        }
    }

    /**
     * 记录 Token 使用情况到控制台
     * <p> 输出当前 AI 服务提供商名称, 模型名称, 提示词 Token 数, 完成 Token 数, 总 Token 数
     *
     * @param providerName     服务提供商名称
     * @param modelName        模型名称
     * @param promptTokens     提示词使用的 Token 数量
     * @param completionTokens 完成响应使用的 Token 数量
     * @param totalTokens      总共使用的 Token 数量
     */
    @Override
    public void onUsage(String providerName, String modelName,
                        int promptTokens, int completionTokens, int totalTokens) {
        AIConsoleLoggerUtil.print(project,
                                  String.format("Token 使用: %s | %s | Prompt: %d | Completion: %d | Total: %d",
                                                providerName, modelName, promptTokens, completionTokens, totalTokens));
    }
}

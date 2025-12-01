package dev.dong4j.zeka.stack.idea.plugin.changelog.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * 日志监听器类, 用于监听 AI 请求, 响应及 Token 使用情况
 * <p>
 * 该类实现了 AIResponseListener 接口, 主要职责是在 AI 请求和响应过程中打印相关信息到控制台, 便于调试和监控 AI 交互过程.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public class ChangelogAIResponseListener implements AIResponseListener {

    /**
     * 当前处理的项目对象
     * <p>
     * 用于存储和操作与当前任务相关的项目信息
     */
    private final Project project;

    /**
     * 初始化 ChangelogAIResponseListener 实例
     * <p>
     * 为监听器设置关联的项目信息
     *
     * @param project 关联的项目对象, 不能为空
     * @throws IllegalArgumentException 如果提供的项目为 null
     */
    public ChangelogAIResponseListener(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理请求并记录相关信息
     * <p>
     * 该方法用于在接收到请求时, 记录请求的提供者名称, 模型名称以及请求体内容.
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容, 若为 null 或空字符串则不记录
     * @param validation   是否进行验证 (当前方法中未使用该参数)
     * @since 1.0
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
     * 处理来自指定提供者的模型响应
     * <p>
     * 在接收到响应后, 记录响应信息及响应体内容 (如果存在).
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param responseBody 响应体内容, 可能为 null 或空字符串
     * @param validation   是否进行了验证 (该参数在方法中未使用)
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
     * 记录指定模型的 Token 使用情况
     * <p>
     * 当模型被使用时, 记录提供者名称, 模型名称以及 Prompt,Completion 和 Total 的 Token 数量, 并打印到控制台.
     *
     * @param providerName     提供者名称
     * @param modelName        模型名称
     * @param promptTokens     Prompt 部分使用的 Token 数量
     * @param completionTokens Completion 部分使用的 Token 数量
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


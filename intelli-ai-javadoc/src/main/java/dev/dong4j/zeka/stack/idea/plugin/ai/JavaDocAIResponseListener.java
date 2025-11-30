package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;

/**
 * JavaDoc AI 响应监听器
 * <p>
 * 实现 AI 响应监听器接口, 用于监听和处理 AI 服务的请求, 响应和使用情况,
 * 支持详细日志记录功能, 可以输出请求和响应的详细信息以及 Token 使用情况
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class JavaDocAIResponseListener implements AIResponseListener {

    private final Project project;
    private final boolean verboseLogging;

    public JavaDocAIResponseListener(@NotNull Project project, boolean verboseLogging) {
        this.project = project;
        this.verboseLogging = verboseLogging;
    }

    @Override
    public void onRequest(String providerName, String modelName, String requestBody, boolean validation) {
        if (verboseLogging) {
            JavaDocConsoleView.printWithTimestamp(project,
                                                  String.format("请求: %s - %s", providerName, modelName));
            if (requestBody != null && !requestBody.isEmpty()) {
                JavaDocConsoleView.print(project, requestBody);
            }
        }
    }

    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        if (verboseLogging) {
            JavaDocConsoleView.printWithTimestamp(project,
                                                  String.format("响应: %s - %s", providerName, modelName));
            if (responseBody != null && !responseBody.isEmpty()) {
                JavaDocConsoleView.print(project, responseBody);
            }
        }
    }

    @Override
    public void onUsage(String providerName, String modelName,
                        int promptTokens, int completionTokens, int totalTokens) {
        if (verboseLogging) {
            JavaDocConsoleView.print(project,
                                     String.format("Token 使用: %s | %s | Prompt: %d | Completion: %d | Total: %d",
                                                   providerName, modelName, promptTokens, completionTokens, totalTokens));
        }
    }
}


package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;

/**
 * AI Javadoc 的 AI 响应监听器实现
 * <p>
 * 将 AI 响应事件转换为控制台日志输出
 */
public class JavaDocAIResponseListener implements AIResponseListener {

    private final Project project;
    private final boolean verboseLogging;

    public JavaDocAIResponseListener(@NotNull Project project) {
        this.project = project;
        this.verboseLogging = AIProviderSettings.getInstance().runtimeSettings.verboseLogging;
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


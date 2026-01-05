package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

final class AutocompleteCompletionDispatcher {
    private final Project project;
    private final AutocompleteProviderResolver providerResolver = new AutocompleteProviderResolver();
    private final NextEditResponseParser responseParser = new NextEditResponseParser();
    private final ScheduledExecutorService scheduler = AppExecutorUtil.getAppScheduledExecutorService();

    AutocompleteCompletionDispatcher(@NotNull Project project) {
        this.project = project;
    }

    CompletableFuture<NextEditCompletionResponse> request(@NotNull AutocompleteCompletionRequest request) {
        AutocompleteSettings settings = AutocompleteSettings.getInstance();
        CompletableFuture<NextEditCompletionResponse> future = CompletableFuture.supplyAsync(() -> {
            AIProviderConfig config = providerResolver.resolvePrimary();
            AIConsoleLoggerUtil.printWithTimestamp(project, "=== Autocomplete 请求 ===");
            AIConsoleLoggerUtil.print(project, "触发模式: " + request.triggerMode());
            AIConsoleLoggerUtil.print(project, "服务商: " + config.providerType.getDisplayName() + " | 模型: " + config.modelName);
            AIConsoleLoggerUtil.print(project, "用户提示词:\n" + request.userPrompt());
            String content = callProvider(config, request);
            if (content == null || content.isBlank()) {
                AIConsoleLoggerUtil.printWarning(project, "Autocomplete 返回空响应");
                return new NextEditCompletionResponse(List.of(), "");
            }
            List<NextEditAutocompletion> parsed = responseParser.parse(content);
            AIConsoleLoggerUtil.print(project, "Autocomplete 响应条数: " + parsed.size());
            AIConsoleLoggerUtil.print(project, "Autocomplete 原始响应:\n" + content);
            return new NextEditCompletionResponse(parsed, content);
        }, AppExecutorUtil.getAppExecutorService());

        scheduleTimeout(future, settings.timeoutMs);
        return future;
    }

    private void scheduleTimeout(@NotNull CompletableFuture<?> future, long timeoutMs) {
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new RuntimeException("Autocomplete request timeout"));
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Nullable
    private String callProvider(@NotNull AIProviderConfig config, @NotNull AutocompleteCompletionRequest request) {
        AIChatRequest chatRequest = new AIChatRequest(request.systemPrompt(), request.userPrompt());
        try {
            return AIServiceImpl.getInstance().generateContent(project, chatRequest, config, null);
        } catch (AIServiceException e) {
            AIConsoleLoggerUtil.printWarning(project, "Autocomplete 主服务商失败: " + e.getMessage());
            AIProviderConfig fallback = providerResolver.resolveFallback();
            if (fallback != null) {
                AIConsoleLoggerUtil.printWarning(project, "切换到备用服务商: " + fallback.providerType.getDisplayName());
                try {
                    return AIServiceImpl.getInstance().generateContent(project, chatRequest, fallback, null);
                } catch (AIServiceException exc) {
                    AIConsoleLoggerUtil.printError(project, "Autocomplete 备用服务商失败: " + exc.getMessage());
                    return null;
                }
            }
            return null;
        }
    }
}

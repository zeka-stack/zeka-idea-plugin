package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.util.TokenCounter;

/**
 * 负责根据当前设置与任务数据构建 {@link AIChatRequest}。
 */
public final class AIRequestComposer {

    private AIRequestComposer() {
    }

    @NotNull
    public static AIChatRequest compose(@NotNull SettingsState settings,
                                        @NotNull DocumentationTask task) {
        String systemPrompt = resolveSystemPrompt(settings);
        String userPrompt = buildUserPrompt(settings, task);
        int tokenEstimate = TokenCounter.estimateTokens(systemPrompt) + TokenCounter.estimateTokens(userPrompt);
        return new AIChatRequest(systemPrompt, userPrompt, tokenEstimate);
    }

    private static String resolveSystemPrompt(@NotNull SettingsState settings) {
        String userSystemPrompt = settings.systemPromptTemplate;
        if (userSystemPrompt == null || userSystemPrompt.trim().isEmpty()) {
            return SettingsState.getDefaultSystemPromptTemplate();
        }
        return userSystemPrompt;
    }

    private static String buildUserPrompt(@NotNull SettingsState settings, @NotNull DocumentationTask task) {
        String template = switch (task.getType()) {
            case CLASS, INTERFACE, ENUM -> resolveTemplate(settings.classPromptTemplate,
                                                           SettingsState.getDefaultClassPromptTemplate());
            case FIELD -> resolveTemplate(settings.fieldPromptTemplate,
                                          SettingsState.getDefaultFieldPromptTemplate());
            case TEST_METHOD -> resolveTemplate(settings.testPromptTemplate,
                                                SettingsState.getDefaultTestPromptTemplate());
            default -> resolveTemplate(settings.methodPromptTemplate,
                                       SettingsState.getDefaultMethodPromptTemplate());
        };
        return String.format(template, task.getCode());
    }

    private static String resolveTemplate(String userTemplate, String defaultTemplate) {
        if (userTemplate == null || userTemplate.isBlank()) {
            return defaultTemplate;
        }
        return userTemplate;
    }
}

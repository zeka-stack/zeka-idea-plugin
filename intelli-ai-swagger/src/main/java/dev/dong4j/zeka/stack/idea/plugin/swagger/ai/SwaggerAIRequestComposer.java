package dev.dong4j.zeka.stack.idea.plugin.swagger.ai;

import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerPromptBuilder;

/**
 * Swagger AI 请求构建器
 */
public final class SwaggerAIRequestComposer {

    private SwaggerAIRequestComposer() {
    }

    @NotNull
    public static AIChatRequest compose(@NotNull SettingsState settings, @NotNull PsiMethod method) {
        String systemPrompt = settings.systemPrompt;
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = SettingsState.getDefaultSystemPrompt();
        }

        String template = settings.swaggerTemplate;
        if (template == null || template.isBlank()) {
            template = SettingsState.getDefaultSwaggerTemplate();
        }

        String content = SwaggerPromptBuilder.buildMethodContext(method);
        String userPrompt = template.replace("{content}", content);
        return new AIChatRequest(systemPrompt, userPrompt);
    }
}

package dev.dong4j.zeka.stack.idea.plugin.swagger.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.swagger.ai.SwaggerAIRequestComposer;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerAnnotationUtil;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerAnnotationWriter;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;

/**
 * Swagger 生成服务
 */
public class SwaggerGenerationService {

    public void generateForMethod(@NotNull Project project,
                                  @NotNull PsiMethod method,
                                  @NotNull SettingsState settings,
                                  @NotNull AIProviderConfig providerConfig) {
        if (!settings.overrideExisting && SwaggerAnnotationUtil.hasSwaggerAnnotations(method)) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.swagger.exists"));
            return;
        }

        AIChatRequest request = SwaggerAIRequestComposer.compose(settings, method);
        AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
        try {
            AIConsoleLoggerUtil.printWithTimestamp(project, "=== Swagger AI Request ===");
            String result = aiService.generateContent(project, request, providerConfig, null);
            AIConsoleLoggerUtil.printSuccess(project, "=== Swagger AI Response ===");
            AIConsoleLoggerUtil.print(project, result);

            SwaggerAnnotationWriter writer = new SwaggerAnnotationWriter(project, settings);
            writer.insertAnnotations(method, result);

            NotificationUtil.showInfo(project, SwaggerBundle.message("success.swagger.written"));
        } catch (AIServiceException ex) {
            String message = AIServiceException.build(ex);
            AIConsoleLoggerUtil.printError(project, message);
            NotificationUtil.showError(project, SwaggerBundle.message("error.ai.failed", message));
        }
    }
}

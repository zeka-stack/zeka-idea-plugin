package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.apply.PatchApplier;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 通用 AI 修复执行器.
 */
public final class ViolationFixer {
    private ViolationFixer() {
    }

    public static void apply(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull Document document,
                             @NotNull TextRange range,
                             @NotNull CodeViolation violation) {
        String originalSnippet = document.getText(range);
        new Task.Backgroundable(project, RepairerBundle.message("task.ai.generic"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
                if (aiService == null) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.unavailable"));
                    return;
                }
                AIProviderConfig config = selectProviderConfig(aiService);
                if (config == null) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.no.provider"));
                    return;
                }
                String userPrompt = FixPromptBuilder.userPrompt(violation, originalSnippet);
                AIChatRequest request = new AIChatRequest(FixPromptBuilder.systemPrompt(), userPrompt);
                String fixedCode;
                try {
                    fixedCode = aiService.generateContent(project, request, config, null);
                } catch (AIServiceException e) {
                    NotificationUtil.showError(project, RepairerBundle.message("error.ai.failed", e.getMessage()));
                    return;
                }
                String normalized = FixResponseValidator.normalize(fixedCode);
                if (normalized.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.empty"));
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() ->
                                                                    PatchApplier.apply(project, file, range, originalSnippet, normalized));
            }
        }.queue();
    }

    private static AIProviderConfig selectProviderConfig(@NotNull AIService aiService) {
        AIProviderSettings settings = aiService.getGlobalSettings();
        List<AIProviderConfig> verified = settings.getVerifiedProviders();
        if (!verified.isEmpty()) {
            return verified.get(0);
        }
        return settings.getDefaultProviderConfig(settings.aiProviderType);
    }
}

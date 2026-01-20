package dev.dong4j.zeka.stack.idea.plugin.repairer.checkstyle;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.ai.FixPromptBuilder;
import dev.dong4j.zeka.stack.idea.plugin.repairer.ai.FixResponseValidator;
import dev.dong4j.zeka.stack.idea.plugin.repairer.apply.PatchApplier;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 使用 AI 自动修复 Checkstyle 违规问题.
 */
public class AICheckstyleFix implements LocalQuickFix {

    private final Problem problem;

    public AICheckstyleFix(@NotNull Problem problem) {
        this.problem = problem;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RepairerBundle.message("fix.ai.checkstyle.family");
    }

    @NotNull
    @Override
    public String getName() {
        return RepairerBundle.message("fix.ai.checkstyle.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (element == null) {
            return;
        }
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        TextRange range = computeRange(document, element, problem.line(), problem.column());
        if (range == null) {
            return;
        }
        String originalSnippet = document.getText(range);
        CodeViolation violation = toViolation();
        FixContext context = buildContext(violation, originalSnippet);
        if (context == null) {
            return;
        }

        new Task.Backgroundable(project, RepairerBundle.message("task.ai.checkstyle"), true) {
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

                String systemPrompt = FixPromptBuilder.systemPrompt();
                String userPrompt = FixPromptBuilder.userPrompt(context.violation(), context.targetText());
                AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

                String fixedCode;
                try {
                    fixedCode = aiService.generateContent(project, request, config, null);
                } catch (AIServiceException e) {
                    NotificationUtil.showError(project, RepairerBundle.message("error.ai.failed", e.getMessage()));
                    return;
                }

                if (fixedCode == null || fixedCode.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.empty"));
                    return;
                }

                String result = FixResponseValidator.normalize(fixedCode);
                if (result.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.empty"));
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() ->
                                                                    PatchApplier.apply(project, file, range, originalSnippet, result));
            }
        }.queue();
    }

    private AIProviderConfig selectProviderConfig(@NotNull AIService aiService) {
        AIProviderSettings settings = aiService.getGlobalSettings();
        List<AIProviderConfig> verified = settings.getVerifiedProviders();
        if (!verified.isEmpty()) {
            return verified.get(0);
        }
        return settings.getDefaultProviderConfig(settings.aiProviderType);
    }

    private FixContext buildContext(@NotNull CodeViolation violation, @NotNull String originalSnippet) {
        return new FixContext(violation, originalSnippet);
    }

    private TextRange computeRange(@NotNull Document document,
                                   @NotNull PsiElement element,
                                   int line,
                                   int column) {
        TextRange elementRange = element.getTextRange();
        if (elementRange == null) {
            return null;
        }
        int startOffset = elementRange.getStartOffset();
        if (line > 0 && line <= document.getLineCount()) {
            int lineStart = document.getLineStartOffset(line - 1);
            int columnOffset = Math.max(0, column - 1);
            startOffset = lineStart + columnOffset;
            if (startOffset < elementRange.getStartOffset()) {
                startOffset = elementRange.getStartOffset();
            }
            if (startOffset > elementRange.getEndOffset()) {
                startOffset = elementRange.getStartOffset();
            }
        }
        return new TextRange(startOffset, elementRange.getEndOffset());
    }

    private CodeViolation toViolation() {
        CodeViolation v = new CodeViolation();
        v.tool = "CHECKSTYLE";
        v.ruleId = problem.sourceName();
        v.message = problem.message();
        v.startLine = problem.line();
        v.startColumn = problem.column();
        v.endLine = problem.line();
        v.endColumn = problem.column();
        return v;
    }
}

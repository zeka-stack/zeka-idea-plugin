package dev.dong4j.zeka.stack.idea.plugin.example.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.example.icons.ExampleIcons;
import dev.dong4j.zeka.stack.idea.plugin.example.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import dev.dong4j.zeka.stack.idea.plugin.example.util.NotificationUtil;

/**
 * 示例 Action - 右键菜单触发
 * <p>
 * 这是一个最小 AI 调用示例：读取设置里的提示词，
 * 使用 Engine 的 AIService 生成内容并输出到通知/控制台。
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ExampleAction extends AnAction {

    public ExampleAction() {
        super(
            ExampleBundle.message("action.example.title"),
            ExampleBundle.message("action.example.description"),
            ExampleIcons.EXAMPLE_16
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.project"));
            return;
        }

        if (psiFile == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.file"));
            return;
        }

        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerConfig = resolveProviderConfig(settings);
        if (providerConfig == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.ai.provider"));
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            ExampleBundle.message("action.example.progress"),
            true
        ) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(ExampleBundle.message("action.example.progress"));

                String fileName = psiFile.getName();
                String userPrompt = settings.exampleTemplate.replace("{content}", fileName);
                AIChatRequest request = new AIChatRequest(settings.systemPrompt, userPrompt);

                AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
                try {
                    AIConsoleLoggerUtil.printWithTimestamp(project, "=== Example AI Request ===");
                    String result = aiService.generateContent(project, request, providerConfig, null);
                    AIConsoleLoggerUtil.printSuccess(project, "=== Example AI Response ===");
                    AIConsoleLoggerUtil.print(project, result);

                    String summary = shorten(result, 200);
                    NotificationUtil.showInfo(project,
                        ExampleBundle.message("success.ai.generated", summary));
                } catch (AIServiceException ex) {
                    String message = AIServiceException.build(ex);
                    AIConsoleLoggerUtil.printError(project, message);
                    NotificationUtil.showError(project,
                        ExampleBundle.message("error.ai.failed", message));
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(project != null && psiFile != null);
    }

    @NotNull
    private static String shorten(@NotNull String text, int maxLength) {
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    @Nullable
    private static AIProviderConfig resolveProviderConfig(@NotNull SettingsState settings) {
        if (settings.providerConfig != null) {
            return settings.providerConfig.copy();
        }
        AIProviderSettings global = AIProviderSettings.getInstance();
        List<AIProviderConfig> verified = global.getVerifiedProviders();
        if (verified.isEmpty()) {
            return null;
        }
        return verified.get(0).copy();
    }
}

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
import dev.dong4j.zeka.stack.idea.plugin.example.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import dev.dong4j.zeka.stack.idea.plugin.example.util.NotificationUtil;
import icons.ExampleIcons;

/**
 * 示例操作类
 * <p> 该插件操作类用于在 IntelliJ IDEA 环境中触发与 AI 交互的功能, 如根据当前文件内容生成 AI 响应.
 * <p> 主要功能包括:
 * <ul>
 *     <li> 获取当前项目和文件上下文 </li>
 *     <li> 检查是否配置了可用的 AI 提供商 </li>
 *     <li> 调用 AI 服务生成内容并展示结果 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class ExampleAction extends AnAction {

    /**
     * 构造函数, 初始化示例操作
     * <p> 设置操作的标题, 描述和图标, 使用 ExampleBundle 提供的国际化字符串和 ExampleIcons 提供的图标
     */
    public ExampleAction() {
        super(
            ExampleBundle.message("action.example.title"),
            ExampleBundle.message("action.example.description"),
            ExampleIcons.EXAMPLE_16
        );
    }

    /**
     * 执行用户操作的动作方法
     * <p> 此方法在用户触发右键菜单中的“示例”动作时被调用. 它会检查项目和文件是否存在, 并根据配置调用 AI 服务生成内容.
     * <p> 具体步骤如下:
     * <ol>
     * <li> 获取当前项目和 PSI 文件 </li>
     * <li> 检查项目和文件是否存在, 如果不存在则显示错误通知 </li>
     * <li> 解析 AI 提供者配置, 如果配置不存在则显示错误通知 </li>
     * <li> 启动后台任务, 在任务中执行 AI 内容生成 </li>
     * <li> 捕获 AI 服务异常并处理 </li>
     * </ol>
     *
     * @param e 动作事件对象, 提供项目和文件信息
     */
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
            /**
             * 执行示例 AI 功能
             * <p> 在后台线程中执行 AI 请求, 使用当前文件名作为内容生成提示, 并通过 AIService 获取响应结果.
             * <p> 该方法会显示进度指示器, 记录 AI 请求和响应日志, 并根据结果展示成功或失败的通知.
             *
             * @param indicator 进度指示器, 用于显示进度和设置文本
             */
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

    /**
     * 更新操作的可用状态
     * <p> 根据当前上下文中是否存在有效的项目和 PSI 文件, 设置该操作是否启用
     *
     * @param e AnActionEvent 事件对象, 包含当前上下文信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(project != null && psiFile != null);
    }

    /**
     * 截断字符串以确保其长度不超过指定最大值
     * <p> 如果原始字符串长度小于等于最大值, 则直接返回原字符串; 否则截取前部分并添加省略号 (...)
     *
     * @param text      要截断的字符串, 不能为 null
     * @param maxLength 最大允许长度 (包括省略号在内)
     * @return 截断后的字符串, 不会超过指定的最大长度
     * @since 1.0.0
     */
    @NotNull
    private static String shorten(@NotNull String text, int maxLength) {
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    /**
     * 解析并返回当前设置或全局验证的 AI 提供商配置
     * <p> 首先检查设置中是否存在有效的提供商配置, 如果存在则返回其副本.
     * 如果不存在, 则从全局 AI 提供商设置中获取已验证的提供商列表, 并返回第一个已验证的提供商的副本.
     * 如果没有已验证的提供商, 则返回 null.
     *
     * @param settings 当前设置对象, 不能为 null
     * @return AIProviderConfig 对象的副本, 如果不存在有效配置则返回 null
     */
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

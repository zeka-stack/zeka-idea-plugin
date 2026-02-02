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
 * 违规修复工具类
 * <p> 用于在 IDE 环境中自动修复代码违规项, 通过调用 AI 服务对指定代码片段进行智能修复.
 * 该类封装了从检测到的违规项出发, 调用 AI 服务生成修复代码, 并应用到原始文档中的完整流程.
 * 支持异步执行, 确保 UI 线程不阻塞, 同时提供错误通知机制.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public final class ViolationFixer {
    /**
     * 私有构造函数, 防止外部实例化该类
     * <p> 此构造函数被声明为私有, 确保 ViolationFixer 类只能通过静态方法访问, 而不能被实例化
     */
    private ViolationFixer() {
    }

    /**
     * 应用 AI 修复建议到指定代码范围
     * <p> 该方法会启动一个后台任务, 调用 AI 服务对指定代码片段进行修复, 并将修复结果应用到文件中.
     * 修复过程包括: 获取原始代码片段, 构建用户提示, 调用 AI 服务生成修复内容, 验证并标准化结果, 最后在 UI 线程中应用补丁.
     *
     * @param project   当前项目上下文
     * @param file      目标文件对象
     * @param document  文件对应的文档对象
     * @param range     代码修复的文本范围
     * @param violation 代码违规信息对象, 用于构建修复提示
     */
    public static void apply(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull Document document,
                             @NotNull TextRange range,
                             @NotNull CodeViolation violation) {
        String originalSnippet = document.getText(range);
        new Task.Backgroundable(project, RepairerBundle.message("task.ai.generic"), true) {
            /**
             * 执行后台任务, 利用 AI 服务生成代码修复补丁并应用到项目中
             * <p>该方法首先获取 AI 服务实例及其配置, 随后构建用于代码修复的系统及用户提示词.
             * 调用 AI 服务生成修复后的代码内容, 并对结果进行标准化校验.
             * 若校验通过, 则在事件分发线程 (EDT) 中调用补丁应用器应用修复.
             * 如果在过程中发生错误(例如服务不可用, 配置缺失或生成失败), 将通过通知组件显示警告或错误信息并终止任务.
             *
             * @param indicator 进度指示器, 必须非空, 用于显示任务执行的进度状态
             */
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
                String diff = FixResponseValidator.extractUnifiedDiff(fixedCode);
                if (diff.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.diff.invalid"));
                    return;
                }
                String patched = FixResponseValidator.applyUnifiedDiffToSnippet(originalSnippet, diff);
                if (patched.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.diff.invalid"));
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() ->
                                                                    PatchApplier.apply(project, file, range, originalSnippet, patched));
            }
        }.queue();
    }

    /**
     * 选择 AI 提供者配置
     * <p> 从 AI 服务的全局设置中获取已验证的提供者配置列表,
     * 如果列表不为空则返回第一个配置, 否则返回默认配置 </p>
     *
     * @param aiService AI 服务实例, 用于获取全局设置和提供者配置
     * @return AI 提供者配置, 如果存在已验证的配置则返回第一个, 否则返回默认配置
     */
    private static AIProviderConfig selectProviderConfig(@NotNull AIService aiService) {
        AIProviderSettings settings = aiService.getGlobalSettings();
        List<AIProviderConfig> verified = settings.getVerifiedProviders();
        if (!verified.isEmpty()) {
            return verified.get(0);
        }
        return settings.getDefaultProviderConfig(settings.aiProviderType);
    }
}

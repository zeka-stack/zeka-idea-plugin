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
 * AI 代码检查修复工具类
 * <p>实现本地快速修复接口, 通过调用 AI 服务自动修复 Checkstyle 规则违反的代码片段.
 * 该类在 IDE 插件环境中运行, 负责从问题描述中提取上下文, 构建 AI 对话请求, 调用 AI 服务生成修复代码, 并应用到原始文件中.
 * 适用于在代码编辑器中自动修复静态代码分析工具 (如 Checkstyle) 报告的违规项.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class AICheckstyleFix implements LocalQuickFix {

    /** 用于存储当前需要修复的 Checkstyle 问题信息 */
    private final Problem problem;

    /**
     * 构造一个 AI 检查风格修复对象.
     * <p> 将给定的 {@link Problem} 关联到此修复实例, 以便在后续的修复操作中使用.
     *
     * @param problem 描述检查到的代码违规信息的 {@code Problem} 对象
     */
    public AICheckstyleFix(@NotNull Problem problem) {
        this.problem = problem;
    }

    /**
     * 返回修复建议的家族名称
     * <p> 此方法返回与 AI 自动修复 Checkstyle 相关的修复建议家族名称
     *
     * @return 家族名称字符串
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return RepairerBundle.message("fix.ai.checkstyle.family");
    }

    /**
     * 获取修复操作的名称
     * <p> 返回 AI Checkstyle 修复操作的显示名称, 用于在用户界面中标识此快速修复
     *
     * @return 修复操作的名称
     */
    @NotNull
    @Override
    public String getName() {
        return RepairerBundle.message("fix.ai.checkstyle.name");
    }

    /**
     * 应用修复操作以解决 Checkstyle 违规问题
     * <p> 该方法根据提供的 ProblemDescriptor 获取 PsiElement 和 PsiFile,
     * 然后获取文档内容并计算需要修复的文本范围. 接着创建 CodeViolation 和 FixContext 对象,
     * 并通过 AI 服务生成修复后的代码. 最后使用 PatchApplier 应用修复结果.
     *
     * @param project    项目对象, 用于获取服务和执行操作
     * @param descriptor 问题描述符, 用于获取 PsiElement 和 PsiFile
     */
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
            /**
             * 运行背景任务以修复代码问题
             * <p> 此方法在后台运行, 调用 AI 服务来生成修复后的代码, 并应用到项目文件中.
             * 具体步骤包括:
             * 1. 获取 AI 服务实例
             * 2. 选择合适的 AI 提供商配置
             * 3. 构建系统提示和用户提示
             * 4. 创建 AI 聊天请求
             * 5. 调用 AI 服务生成修复内容
             * 6. 验证生成的内容是否为空或无效
             * 7. 应用修复内容到指定文件范围
             *
             * @param indicator 进度指示器, 用于显示任务进度
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

    /**
     * 选择可用的 AI 提供商配置
     * <p> 优先返回已验证的提供商配置, 若无则返回默认配置
     *
     * @param aiService AI 服务实例, 用于获取全局设置
     * @return 已验证的 AI 提供商配置 (第一个), 若无则返回默认配置
     */
    private AIProviderConfig selectProviderConfig(@NotNull AIService aiService) {
        AIProviderSettings settings = aiService.getGlobalSettings();
        List<AIProviderConfig> verified = settings.getVerifiedProviders();
        if (!verified.isEmpty()) {
            return verified.get(0);
        }
        return settings.getDefaultProviderConfig(settings.aiProviderType);
    }

    /**
     * 构建修复上下文对象
     * <p> 使用指定的代码违规信息和原始代码片段创建一个新的 {@code FixContext} 实例, 用于后续的修复操作.
     *
     * @param violation       代码违规信息, 包含工具, 规则 ID, 消息和位置等信息
     * @param originalSnippet 需要修复的原始代码片段
     * @return 返回构建好的修复上下文对象
     */
    private FixContext buildContext(@NotNull CodeViolation violation, @NotNull String originalSnippet) {
        return new FixContext(violation, originalSnippet);
    }

    /**
     * 计算文本范围, 用于定位代码中需要修复或修改的位置
     * <p> 根据指定的行号和列号计算在文档中的起始偏移量, 并返回一个 TextRange 对象.
     * 如果元素没有文本范围, 则直接返回 null.
     *
     * @param document 文档对象, 用于获取行和列的偏移信息
     * @param element  PsiElement 对象, 表示代码中的某个元素
     * @param line     指定的行号 (从 1 开始)
     * @param column   指定的列号 (从 1 开始)
     * @return 返回计算后的 TextRange 对象, 如果无法计算则返回 null
     */
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

    /**
     * 将当前检查问题转换为 CodeViolation 对象
     * <p> 根据当前问题的源信息构建一个表示 Checkstyle 违规的 CodeViolation 实例, 包含工具名称, 规则 ID, 消息内容及起始 / 结束行和列坐标 </p>
     *
     * @return 返回一个填充了当前问题信息的 CodeViolation 对象
     */
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

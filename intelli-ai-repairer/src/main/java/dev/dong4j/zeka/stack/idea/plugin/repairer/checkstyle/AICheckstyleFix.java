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
import dev.dong4j.zeka.stack.idea.plugin.repairer.apply.EnhancedPatchApplier;
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

        TextRange range = computePreciseRange(document, element, problem.line(), problem.column());
        if (range == null) {
            return;
        }
        String originalSnippet = document.getText(range);
        String surroundingContext = getSurroundingContext(document, range);
        CodeViolation violation = toViolation();
        FixContext context = buildContext(violation, originalSnippet, surroundingContext);

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
                String userPrompt = FixPromptBuilder.enhancedUserPrompt(
                    context.violation(),
                    context.targetText(),
                    context.surroundingContext()
                );
                AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

                String fixedCode;
                try {
                    fixedCode = aiService.generateContent(project, request, config, null);
                } catch (AIServiceException e) {
                    NotificationUtil.showError(project, RepairerBundle.message("error.ai.failed", e.getMessage()));
                    return;
                }

                if (fixedCode.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.empty"));
                    return;
                }

                String result = FixResponseValidator.normalize(fixedCode);
                if (result.isBlank()) {
                    NotificationUtil.showWarning(project, RepairerBundle.message("error.ai.empty"));
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() ->
                    EnhancedPatchApplier.apply(project, file, element, range, originalSnippet, result));
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
            return verified.getFirst();
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
        return new FixContext(violation, originalSnippet, "");
    }

    /**
     * 构建修复上下文对象
     * <p> 使用指定的代码违规信息和原始代码片段创建一个新的 {@code FixContext} 实例, 用于后续的修复操作.
     *
     * @param violation          代码违规信息, 包含工具, 规则 ID, 消息和位置等信息
     * @param originalSnippet    需要修复的原始代码片段
     * @param surroundingContext 代码片段周围的上下文内容, 用于增强 AI 修复的语境理解
     * @return 返回构建好的修复上下文对象
     */
    private FixContext buildContext(@NotNull CodeViolation violation, @NotNull String originalSnippet, @NotNull String surroundingContext) {
        return new FixContext(violation, originalSnippet, surroundingContext);
    }

    /**
     * 计算精确的文本范围, 用于定位代码中需要修复或修改的位置
     * <p> 该方法首先尝试通过递归查找找到与指定行, 列位置匹配的最精确的 PsiElement 元素.
     * 如果未找到, 则回退到当前元素的文本范围, 并根据指定的行, 列信息计算起始偏移量.
     * 最终返回一个 TextRange 对象, 表示需要修复或修改的代码文本范围.
     *
     * @param document 文档对象, 用于获取行和列的偏移信息
     * @param element  代码元素对象, 表示当前处理的代码节点
     * @param line     指定的行号 (从 1 开始)
     * @param column   指定的列号 (从 1 开始)
     * @return 返回计算后的 TextRange 对象, 如果无法计算则返回 null
     */
    private TextRange computePreciseRange(@NotNull Document document, @NotNull PsiElement element, int line, int column) {
        // 1. 尝试获取最小的有意义的代码单元
        PsiElement targetElement = findTargetElement(element, line, column);

        // 2. 如果找不到合适的元素，回退到原始逻辑
        if (targetElement == null) {
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

        // 3. 使用目标元素的范围
        return targetElement.getTextRange();
    }

    /**
     * 递归查找指定位置对应的代码元素
     * <p> 从给定的 PsiElement 开始, 遍历其子元素, 检查指定行和列是否位于子元素范围内. 如果在, 则递归进入该子元素继续查找; 否则返回当前元素.
     *
     * @param element 起始的代码元素, 用于遍历其子元素
     * @param line    指定的行号 (从 1 开始)
     * @param column  指定的列号 (从 1 开始)
     * @return 匹配位置的最深层代码元素, 若未找到则返回原始元素
     */
    private PsiElement findTargetElement(@NotNull PsiElement element, int line, int column) {
        // 遍历元素的子元素，找到最精确的匹配
        for (PsiElement child : element.getChildren()) {
            TextRange childRange = child.getTextRange();
            if (childRange != null) {
                // 检查子元素是否包含问题位置
                // 这里需要根据具体的 PSI 结构进行调整
                if (isPositionInElement(child, line, column)) {
                    return findTargetElement(child, line, column);
                }
            }
        }

        // 如果没有子元素包含问题位置，返回当前元素
        return element;
    }

    /**
     * 判断指定位置是否位于元素内部
     * <p> 当前实现直接返回 true, 表示默认认为指定行和列位置位于该元素内. 该方法可能用于定位代码中违规位置的精确范围, 但当前版本未做实际位置校验.</p>
     *
     * @param element 要检查的 PsiElement 对象
     * @param line    指定的行号 (从 1 开始)
     * @param column  指定的列号 (从 1 开始)
     * @return 始终返回 true, 表示位置被认为在元素内
     */
    private boolean isPositionInElement(@NotNull PsiElement element, int line, int column) {
        // 简化实现：假设元素的文本范围包含问题位置
        // 实际实现可能需要更复杂的逻辑
        return true;
    }

    /**
     * 获取指定文本范围周围的上下文内容
     * <p> 从文档中提取指定文本范围前后各 200 个字符的上下文内容, 总长度不超过 400 个字符. 若内容超过 400 字符, 则截取前 200 字符和后 200 字符, 并用“...”连接中间部分.</p>
     *
     * @param document 文档对象, 用于获取文本内容和偏移量
     * @param range    文本范围, 用于确定提取上下文的起始和结束位置
     * @return 返回提取的上下文字符串, 长度不超过 400 字符, 若超过则截断并添加省略号
     */
    private String getSurroundingContext(@NotNull Document document, @NotNull TextRange range) {
        // 获取范围前后的上下文信息
        int startOffset = Math.max(0, range.getStartOffset() - 200);
        int endOffset = Math.min(document.getTextLength(), range.getEndOffset() + 200);

        String context = document.getText(new TextRange(startOffset, endOffset));
        // 确保上下文不包含太多无关信息
        return context.length() > 400 ? context.substring(0, 200) + "..." + context.substring(context.length() - 200) : context;
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

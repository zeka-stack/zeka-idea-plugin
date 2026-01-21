package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * AI 违规修复器
 * <p> 实现了 IntelliJ IDEA 的本地快速修复接口, 专门用于处理代码违规修复
 * <p> 该类接收一个 {@link CodeViolation} 对象, 通过计算违规代码的文本范围,
 * 应用相应的修复方案来解决问题. 主要用于 AI 辅助的代码质量检查和自动修复场景
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 2025.3.1200
 */
public class AIViolationFix implements LocalQuickFix {
    /**
     * 违规信息对象, 用于表示当前需要修复的代码问题
     * <p> 该对象包含违规的起始和结束行号等信息 </p>
     */
    private final CodeViolation violation;

    /**
     * 基于指定的代码违规信息创建 AI 修复器实例
     * <p> 使用提供的违规信息初始化修复器, 以便后续执行自动化修复操作
     *
     * @param violation 代码违规信息, 包含违规的具体位置, 类型和描述数据
     */
    public AIViolationFix(@NotNull CodeViolation violation) {
        this.violation = violation;
    }

    /**
     * 获取修复项的家族名称
     * <p> 返回用于在用户界面中显示的修复项家族名称, 通常用于分类或分组显示
     *
     * @return 修复项家族名称, 由资源文件中键值 "fix.ai.generic.family" 对应的本地化字符串
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return RepairerBundle.message("fix.ai.generic.family");
    }

    /**
     * 获取修复方案的名称
     * <p> 返回用于标识该修复方案的名称, 通常用于在用户界面中展示
     *
     * @return 修复方案名称
     */
    @NotNull
    @Override
    public String getName() {
        return RepairerBundle.message("fix.ai.generic.name");
    }

    /**
     * 应用修复操作以修正代码违规问题
     * <p> 根据提供的项目和问题描述符, 获取对应的 PSI 元素和文件, 加载文档并计算需要修改的文本范围, 最后调用 ViolationFixer 执行具体的修复逻辑.
     *
     * @param project    项目实例, 用于获取文档管理器和其他项目相关资源
     * @param descriptor 问题描述符, 用于获取需要修复的 PSI 元素和文件信息
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
        TextRange range = computeRange(document, violation);
        if (range == null) {
            return;
        }
        ViolationFixer.apply(project, file, document, range, violation);
    }

    /**
     * 计算文档中违规代码的文本范围
     * <p> 根据给定的违规对象和文档, 计算出起始行与结束行对应的文本偏移范围.
     * 如果起始行超出文档行数, 则返回 null. 默认起始行为 1, 若未指定.</p>
     *
     * @param document 文档对象, 用于获取行号对应的偏移量
     * @param v        违规对象, 包含起始行和结束行信息
     * @return 返回一个 TextRange 对象, 表示违规代码在文档中的位置范围; 如果范围无效则返回 null
     */
    private TextRange computeRange(@NotNull Document document, @NotNull CodeViolation v) {
        int line = v.startLine > 0 ? v.startLine : 1;
        if (line > document.getLineCount()) {
            return null;
        }
        int startLine = line - 1;
        int startOffset = document.getLineStartOffset(startLine);
        int endLine = v.endLine > 0 ? v.endLine - 1 : startLine;
        if (endLine >= document.getLineCount()) {
            endLine = document.getLineCount() - 1;
        }
        int endOffset = document.getLineEndOffset(endLine);
        return new TextRange(startOffset, endOffset);
    }
}

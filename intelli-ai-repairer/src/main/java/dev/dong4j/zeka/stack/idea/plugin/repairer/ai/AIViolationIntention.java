package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * AI 违规意图动作类
 * <p> 该类实现了 IntentionAction 接口, 用于处理代码违规的修复操作. 它根据给定的 CodeViolation 对象,
 * 提供相应的文本和家族名称, 并在满足条件的情况下调用修复器来修正代码违规问题.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class AIViolationIntention implements IntentionAction {
    /** 代码违规信息, 用于标识需要修复的代码问题 */
    private final CodeViolation violation;

    /**
     * 构造一个新的 AIViolationIntention 实例
     * <p> 用于注入代码违规问题的意图操作, 通过指定的代码违规信息进行初始化
     *
     * @param violation 代码违规信息, 不能为 null
     */
    public AIViolationIntention(@NotNull CodeViolation violation) {
        this.violation = violation;
    }

    /**
     * 获取此意图操作的显示文本
     * <p> 返回用于在 IDE 中展示的意图操作名称, 通常为 "fix.ai.generic.name" 对应的本地化字符串
     *
     * @return 显示文本内容
     */
    @NotNull
    @Override
    public String getText() {
        return RepairerBundle.message("fix.ai.generic.name");
    }

    /**
     * 获取修复操作的家族名称
     * <p> 返回用于标识该修复操作类型的家族名称, 通常用于在 IDE 中分类显示
     *
     * @return 修复操作的家族名称
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return RepairerBundle.message("fix.ai.generic.family");
    }

    /**
     * 判断当前意图操作是否可用
     * <p> 检查给定的项目, 编辑器和文件对象是否非空, 以确定该意图操作是否可以执行
     *
     * @param project 项目对象, 用于上下文信息
     * @param editor  编辑器对象, 用于获取当前编辑状态
     * @param file    文件对象, 用于判断是否在有效的文件上下文中
     * @return 如果项目, 编辑器和文件对象均不为空, 则返回 true, 否则返回 false
     */
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file != null && editor != null;
    }

    /**
     * 执行代码违规修复操作
     * <p> 根据当前编辑器上下文, 计算违规范围并应用修复器
     *
     * @param project 当前项目对象
     * @param editor  当前编辑器对象
     * @param file    当前文件对象
     * @since 1.0
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        Document document = editor.getDocument();
        TextRange range = computeRange(document, violation);
        if (range == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitDocument(document);
        ViolationFixer.apply(project, file, document, range, violation);
    }

    /**
     * 判断该意图操作是否应在写入操作中启动
     * <p> 此方法返回 false, 表示该意图操作不应在写入操作中启动
     *
     * @return 始终返回 false
     */
    @Override
    public boolean startInWriteAction() {
        return false;
    }

    /**
     * 根据代码违规信息计算文档中的文本范围
     * <p> 基于违规代码的起始行和结束行信息, 在给定的文档中计算对应的文本范围.
     * 该方法会处理边界情况, 如行号为 0 或超出文档行数的情况.
     *
     * @param document 目标文档, 用于计算文本范围
     * @param v        代码违规信息, 包含违规代码的起始和结束行号
     * @return 计算得到的文本范围, 如果行号无效则返回 null
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

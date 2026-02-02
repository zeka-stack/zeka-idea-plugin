package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.analysis.problemsView.toolWindow.ProblemsTreeModel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.treeStructure.Tree;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;

import javax.swing.SwingUtilities;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.ai.ViolationFixer;
import dev.dong4j.zeka.stack.idea.plugin.repairer.problems.RepairerProblemsRoot;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Fix all Repairer problems in the current project.
 */
public class RepairerFixAllProblemsAction extends AnAction implements DumbAware {
    /**
     * 执行修复操作, 遍历当前项目中所有违规项并应用修复
     * <p> 从项目中获取违规项列表, 若无违规项则显示提示信息; 否则逐个调用修复方法
     *
     * @param e 操作事件对象, 包含当前操作的上下文信息
     * @since 1.0
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        if (!isRepairerTab(e)) {
            return;
        }
        var violations = ViolationCache.getInstance(project).getAll();
        if (violations.isEmpty()) {
            NotificationUtil.showInfo(project, "No problems to fix.");
            return;
        }
        for (CodeViolation violation : violations) {
            applyFix(project, violation);
        }
    }

    /**
     * 更新操作按钮的状态
     * <p> 根据当前项目和上下文判断是否启用并显示该操作按钮
     *
     * @param e 动作事件对象
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && isRepairerTab(e);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    /**
     * 检查当前操作事件是否在 Repairer 问题选项卡中触发
     * <p> 通过查找关联的树组件并检查其模型类型来判断是否处于 Repairer 问题选项卡上下文 </p>
     *
     * @param e 动作事件对象, 不能为 null
     * @return 如果当前处于 Repairer 问题选项卡则返回 true, 否则返回 false
     */
    private static boolean isRepairerTab(@NotNull AnActionEvent e) {
        Tree tree = findTree(e);
        if (tree == null) {
            return false;
        }
        if (!(tree.getModel() instanceof ProblemsTreeModel)) {
            return false;
        }
        return ((ProblemsTreeModel) tree.getModel()).getRoot() instanceof RepairerProblemsRoot;
    }

    /**
     * 应用修复操作到指定的代码违规
     * <p> 根据给定的项目和代码违规信息, 找到对应的文件并应用修复操作.
     * 如果文件路径为空或无效, 则不会执行任何操作.
     *
     * @param project   项目对象
     * @param violation 代码违规信息
     */
    private static void applyFix(@NotNull Project project, @NotNull CodeViolation violation) {
        if (violation.filePath == null || violation.filePath.isBlank()) {
            return;
        }
        VirtualFile file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(violation.filePath);
        if (file == null) {
            return;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        var document = documentManager.getDocument(psiFile);
        if (document == null) {
            return;
        }
        TextRange range = computeRange(document, violation);
        if (range == null) {
            return;
        }
        documentManager.commitDocument(document);
        ViolationFixer.apply(project, psiFile, document, range, violation);
    }

    /**
     * 根据代码违规信息计算文本范围
     * <p> 根据文档的行数和违规信息中的起始行, 结束行, 计算出对应的文本范围 (TextRange).
     * 若行号超出文档范围, 则自动调整为文档边界.
     *
     * @param document  文档对象, 用于获取行偏移量
     * @param violation 违规信息对象, 包含起始行号和结束行号
     * @return 文本范围对象, 若行号无效或文档为空则返回 null
     */
    private static TextRange computeRange(@NotNull com.intellij.openapi.editor.Document document,
                                          @NotNull CodeViolation violation) {
        int lineCount = document.getLineCount();
        if (lineCount == 0) {
            return null;
        }
        int startLine = violation.startLine > 0 ? violation.startLine - 1 : 0;
        if (startLine >= lineCount) {
            return null;
        }
        int endLine = violation.endLine > 0 ? violation.endLine - 1 : startLine;
        if (endLine >= lineCount) {
            endLine = lineCount - 1;
        }
        int startOffset = document.getLineStartOffset(startLine);
        int endOffset = document.getLineEndOffset(endLine);
        return new TextRange(startOffset, endOffset);
    }

    /**
     * 查找组件树中的 Tree 组件
     * <p> 根据给定的 AnActionEvent 获取上下文组件, 并尝试找到其中的 Tree 组件. 如果组件本身就是 Tree, 则直接返回; 否则, 递归查找其父组件中的 Tree 组件.
     *
     * @param e AnActionEvent 对象, 用于获取上下文组件
     * @return 找到的 Tree 组件, 如果没有找到则返回 null
     */
    private static Tree findTree(@NotNull AnActionEvent e) {
        Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        if (component instanceof Tree) {
            return (Tree) component;
        }
        if (component == null) {
            return null;
        }
        Component tree = SwingUtilities.getAncestorOfClass(Tree.class, component);
        return tree instanceof Tree ? (Tree) tree : null;
    }
}

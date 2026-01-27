package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.analysis.problemsView.toolWindow.ProblemsTreeModel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.openapi.util.TextRange;

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

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && isRepairerTab(e);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

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

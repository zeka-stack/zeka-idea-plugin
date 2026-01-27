package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.analysis.problemsView.toolWindow.ProblemNode;
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
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.ai.ViolationFixer;
import dev.dong4j.zeka.stack.idea.plugin.repairer.problems.RepairerProblem;
import dev.dong4j.zeka.stack.idea.plugin.repairer.problems.RepairerProblemsRoot;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Fix selected problems in the Problems view using AI.
 */
public class RepairerFixSelectedProblemsAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Tree tree = findTree(e);
        if (tree == null) {
            return;
        }
        ProblemsTreeModel model = getProblemsTreeModel(tree);
        if (model == null || !(model.getRoot() instanceof RepairerProblemsRoot)) {
            return;
        }
        Set<RepairerProblem> problems = collectSelectedProblems(tree);
        if (problems.isEmpty()) {
            NotificationUtil.showInfo(project, "No problems selected.");
            return;
        }
        for (RepairerProblem problem : problems) {
            applyFix(project, problem);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Tree tree = findTree(e);
        boolean enabled = project != null && tree != null;
        if (enabled) {
            ProblemsTreeModel model = getProblemsTreeModel(tree);
            enabled = model != null && model.getRoot() instanceof RepairerProblemsRoot;
        }
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    private static void applyFix(@NotNull Project project, @NotNull RepairerProblem problem) {
        CodeViolation violation = problem.getViolation();
        VirtualFile file = problem.getFile();
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

    private static Set<RepairerProblem> collectSelectedProblems(@NotNull Tree tree) {
        Set<RepairerProblem> problems = new LinkedHashSet<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return problems;
        }
        for (TreePath path : paths) {
            Object node = path.getLastPathComponent();
            collectProblems(node, problems);
        }
        return problems;
    }

    private static void collectProblems(Object node, Set<RepairerProblem> problems) {
        if (node instanceof ProblemNode) {
            Problem problem = ((ProblemNode) node).getProblem();
            if (problem instanceof RepairerProblem) {
                problems.add((RepairerProblem) problem);
            }
            return;
        }
        if (node instanceof Node) {
            for (Object child : ((Node) node).getChildren()) {
                collectProblems(child, problems);
            }
        }
    }

    private static ProblemsTreeModel getProblemsTreeModel(@NotNull Tree tree) {
        if (tree.getModel() instanceof ProblemsTreeModel) {
            return (ProblemsTreeModel) tree.getModel();
        }
        return null;
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

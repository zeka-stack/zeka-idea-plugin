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
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.treeStructure.Tree;

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
    /**
     * 修复在问题视图中选定的问题
     * <p> 通过收集选定的问题并逐个应用修复操作来处理问题
     *
     * @param e 动作事件, 包含项目上下文和 UI 组件信息
     */
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

    /**
     * 更新操作的可用状态
     * <p> 根据当前上下文判断操作是否可用, 当项目非空且树组件存在, 并且树模型的根节点为 RepairerProblemsRoot 类型时, 操作可用 </p>
     *
     * @param e 操作事件对象, 包含当前上下文信息
     */
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

    /**
     * 应用修复方案到指定问题
     * <p> 根据问题对象获取对应的代码违规信息和文件, 计算修复范围并提交文档, 最后调用修复器应用修复.
     *
     * @param project 当前项目对象, 用于获取相关服务和上下文
     * @param problem 问题对象, 包含违规信息和文件引用
     */
    private static void applyFix(@NotNull Project project, @NotNull RepairerProblem problem) {
        CodeViolation violation = problem.violation();
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

    /**
     * 根据代码违规信息计算文档中的文本范围
     * <p> 基于违规信息的起始行和结束行, 将行号转换为文档中的偏移量范围. 如果指定的行号超出文档范围, 则返回 null.
     *
     * @param document  包含代码违规的文档
     * @param violation 代码违规信息, 包含 startLine 和 endLine 属性
     * @return 计算得出的文本范围, 如果文档为空或行号无效则返回 null
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
     * 收集选中的修复问题
     * <p> 遍历选中的树路径, 收集所有选中的修复问题
     *
     * @param tree 树组件
     * @return 包含选中修复问题的集合
     */
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

    /**
     * 递归收集指定节点树中所有 {@link RepairerProblem} 实例.
     * <p> 若当前节点是 {@link ProblemNode} 并且其关联的 {@link Problem} 为 {@link RepairerProblem}, 则将其加入结果集;
     * 否则若节点实现了 {@link Node} 接口, 则遍历其子节点继续递归收集.
     *
     * @param node     待检测的节点对象, 可能是 {@link ProblemNode} 或 {@link Node}
     * @param problems 用于存放收集到的 {@link RepairerProblem}, 此集合在方法内被逐步填充
     */
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

    /**
     * 获取问题树模型
     * <p> 从指定树组件中提取并返回其模型, 如果模型类型为 {@code ProblemsTreeModel}, 则强制转换并返回; 否则返回 null
     *
     * @param tree 需要获取模型的树组件, 不能为空
     * @return 如果树模型是 {@code ProblemsTreeModel} 类型, 则返回该类型实例, 否则返回 null
     */
    private static ProblemsTreeModel getProblemsTreeModel(@NotNull Tree tree) {
        if (tree.getModel() instanceof ProblemsTreeModel) {
            return (ProblemsTreeModel) tree.getModel();
        }
        return null;
    }

    /**
     * 从动作事件中查找树组件
     * <p> 首先尝试从事件数据中直接获取组件, 如果它是 {@code Tree} 实例则直接返回.
     * 如果直接获取的组件不是树组件且不为空, 则通过 {@code SwingUtilities} 在其父级组件中查找树组件.
     *
     * @param e 动作事件, 包含上下文信息
     * @return 找到的 {@code Tree} 组件, 如果未找到则返回 {@code null}
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

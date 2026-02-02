package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.analysis.problemsView.toolWindow.ProblemsTreeModel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.CheckstyleXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.PmdXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.problems.RepairerProblemsRoot;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ReportPathCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Refresh cached static analysis reports.
 */
public class RefreshStaticReportsAction extends AnAction implements DumbAware {
    public RefreshStaticReportsAction() {
        super(
            RepairerBundle.message("action.refresh.report.title"),
            RepairerBundle.message("action.refresh.report.description"),
            AllIcons.Actions.Refresh
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ReportPathCache cache = ReportPathCache.getInstance(project);
        List<String> checkstylePaths = cache.getCheckstylePaths();
        List<String> pmdPaths = cache.getPmdPaths();
        if ((checkstylePaths == null || checkstylePaths.isEmpty())
            && (pmdPaths == null || pmdPaths.isEmpty())) {
            NotificationUtil.showWarning(project, RepairerBundle.message("notify.refresh.missing"));
            return;
        }

        List<CodeViolation> violations = new ArrayList<>();
        LocalFileSystem fs = LocalFileSystem.getInstance();
        if (checkstylePaths != null) {
            for (String path : checkstylePaths) {
                VirtualFile vf = fs.findFileByPath(path);
                if (vf != null) {
                    violations.addAll(new CheckstyleXmlAdapter().parse(new File(vf.getPath())));
                }
            }
        }
        for (String path : pmdPaths) {
            VirtualFile vf = fs.findFileByPath(path);
            if (vf != null) {
                violations.addAll(new PmdXmlAdapter().parse(new File(vf.getPath())));
            }
        }

        ViolationCache.getInstance(project).setAll(violations);
        com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
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

    private static Tree findTree(@NotNull AnActionEvent e) {
        return tree(e);
    }

    @Nullable
    static Tree tree(@NotNull AnActionEvent e) {
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

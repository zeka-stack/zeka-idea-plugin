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
    /**
     * 构造 RefreshStaticReportsAction 实例
     * <p> 使用默认的标题, 描述和图标初始化操作, 标题和描述通过 RepairerBundle 获取本地化字符串
     */
    public RefreshStaticReportsAction() {
        super(
            RepairerBundle.message("action.refresh.report.title"),
            RepairerBundle.message("action.refresh.report.description"),
            AllIcons.Actions.Refresh
             );
    }

    /**
     * 处理刷新静态分析报告的操作.
     *
     * <p> 当用户触发该操作时, 方法会获取当前项目, 并依据 {@link ReportPathCache} 中缓存的 Checkstyle 与 PMD 路径列表进行工作:</p>
     *
     * <ul>
     *   <li> 若项目或路径列表不存在, 直接返回.</li>
     *   <li> 若 Checkstyle 与 PMD 路径均为空, 弹出警告提示用户缺少报表文件.</li>
     *   <li> 遍历 Checkstyle 路径, 利用 {@link CheckstyleXmlAdapter} 解析每个 XML 报告文件并收集违规记录.</li>
     *   <li> 同样遍历 PMD 路径, 利用 {@link PmdXmlAdapter} 解析每个 XML 报告文件并收集违规记录.</li>
     *   <li> 将所有违规记录存入 {@link ViolationCache}, 随后重启 IntelliJ IDEA 的代码分析器以更新视图.</li>
     * </ul>
     *
     * @param e 触发操作的事件对象, 包含当前项目及上下文信息
     * @since 1.0
     */
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

    /**
     * 指定用于更新操作状态的后台线程
     * <p>返回 {@link ActionUpdateThread#EDT} 表示该操作的状态更新必须在事件分发线程 (EDT) 上执行
     *
     * @return 更新操作所使用的线程(EDT)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * 更新操作按钮的启用状态和可见性
     * <p> 根据当前项目和是否处于 Repairer 标签页中, 设置操作按钮的启用状态和可见性.
     *
     * @param e 行动事件对象, 包含与操作相关的上下文信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && isRepairerTab(e);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    /**
     * 判断当前操作事件是否关联到修复器标签页
     * <p> 通过查找事件中的树组件, 验证其模型是否为 ProblemsTreeModel 类型, 并进一步判断根节点是否为 RepairerProblemsRoot 类型 </p>
     *
     * @param e 操作事件对象, 不能为空
     * @return 如果树组件存在, 模型类型匹配且根节点类型为 RepairerProblemsRoot, 则返回 true, 否则返回 false
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
     * 查找给定事件相关的树组件
     * <p> 通过给定的 AnActionEvent 查找关联的树组件
     *
     * @param e AnActionEvent 事件对象
     * @return 关联的树组件, 如果没有找到则返回 null
     */
    private static Tree findTree(@NotNull AnActionEvent e) {
        return tree(e);
    }

    /**
     * 根据动作事件查找其上下文中的 Tree 组件
     * <p> 从动作事件中获取上下文组件, 若为 Tree 类型则直接返回; 若为 null 则返回 null; 否则向上查找最近的 Tree 父组件并返回, 若未找到则返回 null
     *
     * @param e 动作事件对象, 不能为空
     * @return 找到的 Tree 组件, 若未找到或输入参数为 null 则返回 null
     */
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

package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Problems tool window tab for IntelliAI Repairer.
 */
public class RepairerProblemsViewPanel extends ProblemsViewPanel {
    /** Tab 标识符, 用于唯一标识 IntelliAI Repairer 问题工具窗口标签页 */
    public static final String TAB_ID = "IntelliAI.Repairer.Problems";
    /** 标签页显示名称 */
    public static final String TAB_NAME = "IntelliAI Repairer";
    /**
     * 修复器问题视图的根节点
     * <p> 用于管理修复器问题树结构的数据源
     *
     * @see RepairerProblemsRoot
     */
    private final @NotNull RepairerProblemsRoot root;

    /**
     * 构造并初始化 RepairerProblemsViewPanel 实例
     * <p> 使用指定的项目信息创建问题视图面板, 并设置相关属性和初始状态.
     *
     * @param project 用于初始化面板的项目对象, 不能为空
     */
    public RepairerProblemsViewPanel(@NotNull Project project) {
        super(project, TAB_ID, ProblemsViewState.getInstance(project), () -> TAB_NAME);
        this.root = new RepairerProblemsRoot(this, project);
        getTreeModel().setRoot(root);
    }

    /**
     * 根据给定的标签索引返回标签名称.
     *
     * <p> 此方法返回 {@link #TAB_NAME}, 表示“IntelliAI Repairer”标签的名称.
     *
     * @param tabIndex 标签索引, 当前实现不使用该参数
     * @return 固定的标签名称 {@code IntelliAI Repairer}
     */
    @Override
    public @NotNull String getName(int tabIndex) {
        return TAB_NAME;
    }

    /**
     * 获取选项卡的唯一标识符
     *
     * @return 选项卡的 ID 字符串
     */
    @Override
    public @NotNull String getTabId() {
        return TAB_ID;
    }

    /**
     * 释放资源并清理视图面板
     * <p> 重写父类 dispose 方法, 先设置树模型根节点为新的 RepairerProblemsRoot 实例, 然后调用 root 的 dispose 方法, 最后调用父类的 dispose 方法完成清理
     *
     * @see RepairerProblemsRoot#dispose()* @see ProblemsViewPanel#dispose()
     */
    @Override
    public void dispose() {
        getTreeModel().setRoot(new RepairerProblemsRoot(this, getProject()));
        root.dispose();
        super.dispose();
    }
}

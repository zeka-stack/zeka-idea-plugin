package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Summary node for Repairer problems.
 */
public final class RepairerSummaryNode extends Node {
    /** 摘要节点的名称 */
    private final String name;
    /** 包含此节点的子节点列表 */
    private final List<Node> children;

    /**
     * Create a summary node with the given label and children.
     *
     * @param project  current project
     * @param name     summary label
     * @param children child nodes
     */
    public RepairerSummaryNode(@NotNull Project project,
                               @NotNull String name,
                               @NotNull List<Node> children) {
        super(project);
        this.name = name;
        this.children = children;
    }

    /**
     * 返回该节点的子节点集合
     * <p> 此方法覆盖父类方法, 用于获取当前节点的所有子节点.
     *
     * @return 不可变的子节点集合
     */
    @Override
    public @NotNull Collection<Node> getChildren() {
        return children;
    }

    /**
     * 获取节点名称
     * <p> 返回此节点的名称
     *
     * @return 节点名称
     */
    @Override
    public @NotNull String getName() {
        return name;
    }

    /**
     * 获取节点的叶子状态
     * <p> 当子节点列表为空时返回 {@link LeafState#ALWAYS}, 否则返回 {@link LeafState#NEVER}
     *
     * @return 节点的叶子状态
     */
    @Override
    public @NotNull LeafState getLeafState() {
        return children.isEmpty() ? LeafState.ALWAYS : LeafState.NEVER;
    }

    /**
     * 更新节点的展示数据
     * <p> 设置展示文本为节点名称, 并设置图标为信息图标
     *
     * @param presentation 展示数据对象
     */
    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(name);
        presentation.setIcon(AllIcons.General.Information);
    }

    /**
     * 更新展示数据
     * <p> 调用父类的 update 方法, 传入项目和展示数据对象, 用于更新界面显示内容
     *
     * @param project          当前项目对象
     * @param presentationData 展示数据对象, 用于设置界面显示内容
     */
    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentationData) {
        update(presentationData);
    }

    /**
     * 判断当前对象与传入对象是否相等
     * <p> 比较当前对象与传入对象的类类型及名称字段值, 若两者均相等则返回 true, 否则返回 false.
     *
     * @param o 需要比较的对象
     * @return 如果对象为当前类实例且名称字段值相等, 则返回 true; 否则返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairerSummaryNode that)) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    /**
     * 计算此节点的哈希码值
     * <p> 基于节点的名称计算哈希码
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

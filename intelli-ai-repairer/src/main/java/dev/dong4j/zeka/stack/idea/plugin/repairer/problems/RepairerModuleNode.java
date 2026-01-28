package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Module group node for Repairer problems.
 */
public final class RepairerModuleNode extends Node {
    /** 模块节点名称 */
    private final String name;
    /** 关联的模块 */
    private final Module module;
    /**
     * 子节点列表
     * <p> 包含该模块节点的所有子节点
     */
    private final List<Node> children;

    /**
     * 构造一个 RepairerModuleNode 实例
     * <p> 用于创建表示修复器模块节点的对象, 包含项目, 名称, 模块和子节点列表信息
     *
     * @param project  项目对象, 不能为空
     * @param name     模块节点的名称, 不能为空
     * @param module   关联的模块对象
     * @param children 子节点列表, 不能为空
     */
    public RepairerModuleNode(@NotNull Project project,
                              @NotNull String name,
                              Module module,
                              @NotNull List<Node> children) {
        super(project);
        this.name = name;
        this.module = module;
        this.children = children;
    }

    /**
     * 获取子节点集合
     * <p> 返回此节点的所有子节点集合. 返回值非空, 且当节点不存在子节点时返回空集合.
     *
     * @return 子节点集合
     */
    @Override
    public @NotNull Collection<Node> getChildren() {
        return children;
    }

    /**
     * 获取节点名称
     *
     * @return 节点名称, 不为 null
     */
    @Override
    public @NotNull String getName() {
        return name;
    }

    /**
     * 获取节点的叶子状态
     * <p> 当子节点列表为空时返回 {@link LeafState#ALWAYS}, 否则返回 {@link LeafState#NEVER}
     *
     * @return 节点的叶子状态, 根据子节点是否存在决定
     */
    @Override
    public @NotNull LeafState getLeafState() {
        return children.isEmpty() ? LeafState.ALWAYS : LeafState.NEVER;
    }

    /**
     * 更新节点的展示数据
     * <p> 设置节点的展示文本和图标, 根据模块是否存在来选择不同的图标
     *
     * @param presentation 展示数据对象
     */
    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(name);
        presentation.setIcon(module != null ? AllIcons.Nodes.Module : AllIcons.General.Warning);
    }

    /**
     * 判断当前对象与指定对象是否相等
     * <p> 比较两个 RepairerModuleNode 实例是否相等, 仅比较名称字段, 忽略其他属性 </p>
     * <p> 当对象为同一引用, 或类型相同且名称相等时返回 true</p>
     *
     * @param o 待比较的对象
     * @return 如果对象相等则返回 true, 否则返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairerModuleNode)) {
            return false;
        }
        RepairerModuleNode that = (RepairerModuleNode) o;
        return Objects.equals(name, that.name);
    }

    /**
     * 计算当前对象的哈希码
     * <p> 根据对象的 name 字段计算哈希值, 使用 Objects.hash 方法确保一致性
     *
     * @return 基于 name 字段计算的整数哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * 更新节点的展示数据
     * <p> 此方法用于根据当前项目和展示数据对象更新节点的显示信息.
     *
     * @param project          当前项目实例
     * @param presentationData 要更新的展示数据对象
     */
    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentationData) {
        update(presentationData);
    }
}

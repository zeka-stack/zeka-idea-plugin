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
 * Package group node for Repairer problems.
 */
public final class RepairerPackageNode extends Node {
    /** 包节点名称 */
    private final String name;
    /**
     * 子节点列表
     * <p> 包含当前包节点下的所有直接子节点
     *
     * @see Node
     */
    private final List<Node> children;

    /**
     * 构造函数, 初始化包节点
     * <p> 创建一个新的包节点对象, 设置父节点, 名称和子节点列表
     *
     * @param parent   父节点, 不能为空
     * @param name     包节点名称, 不能为空
     * @param children 子节点列表, 不能为空
     */
    public RepairerPackageNode(@NotNull Node parent,
                               @NotNull String name,
                               @NotNull List<Node> children) {
        super(parent);
        this.name = name;
        this.children = children;
    }

    /**
     * 返回此节点的子节点集合
     * <p> 该方法覆盖父类方法, 返回存储在当前节点中的子节点列表.
     *
     * @return 子节点的不可变集合
     */
    @Override
    public @NotNull Collection<Node> getChildren() {
        return children;
    }

    /**
     * 返回该节点的名称.
     *
     * @return 节点名称 (非空).
     */
    @Override
    public @NotNull String getName() {
        return name;
    }

    /**
     * 获取节点的叶子状态
     * <p> 根据子节点列表的状态判断当前节点是否为叶子节点
     *
     * @return 如果子节点列表为空返回 {@link LeafState#ALWAYS}, 否则返回 {@link LeafState#NEVER}
     */
    @Override
    public @NotNull LeafState getLeafState() {
        return children.isEmpty() ? LeafState.ALWAYS : LeafState.NEVER;
    }

    /**
     * 更新呈现数据, 设置可显示文本和图标
     * <p> 根据当前节点名称设置呈现数据的可显示文本, 并设置包节点图标 </p>
     *
     * @param presentation 需要更新的呈现数据对象
     */
    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(name);
        presentation.setIcon(AllIcons.Nodes.Package);
    }

    /**
     * 检查对象是否相等
     * <p> 比较此节点与指定对象是否相等. 两个节点被认为相等, 当且仅当它们是 {@code RepairerPackageNode} 的实例且名称相同.
     *
     * @param o 要与此对象进行比较的引用对象
     * @return 如果指定对象与此节点相等则返回 {@code true}, 否则返回 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairerPackageNode)) {
            return false;
        }
        RepairerPackageNode that = (RepairerPackageNode) o;
        return Objects.equals(name, that.name);
    }

    /**
     * 计算当前对象的哈希码
     * <p> 该方法基于对象的名称属性生成哈希码, 用于在集合中快速识别对象.
     *
     * @return 对象的哈希码值
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * 更新节点的显示数据
     * <p> 此方法在项目发生变化时被调用, 用于更新节点的显示信息
     *
     * @param project          当前项目对象
     * @param presentationData 节点的显示数据对象
     */
    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentationData) {

    }
}

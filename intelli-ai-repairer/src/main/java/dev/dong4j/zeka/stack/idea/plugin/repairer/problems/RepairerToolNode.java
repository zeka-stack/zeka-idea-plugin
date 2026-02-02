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
 * Tool group node for Repairer problems.
 */
public final class RepairerToolNode extends Node {
    /** 节点的唯一标识符 */
    private final String key;
    /** 节点显示名称 */
    private final String name;
    /**
     * 子节点列表
     * <p> 表示当前工具节点下的所有子节点
     */
    private final List<Node> children;

    /**
     * 构造一个 RepairerToolNode 实例
     * <p> 使用指定的项目, 键, 名称和子节点列表创建一个新的工具节点
     *
     * @param project  与该节点关联的项目
     * @param key      节点的唯一标识键
     * @param name     节点的显示名称
     * @param children 子节点列表
     */
    public RepairerToolNode(@NotNull Project project,
                            @NotNull String key,
                            @NotNull String name,
                            @NotNull List<Node> children) {
        super(project);
        this.key = key;
        this.name = name;
        this.children = children;
    }

    /**
     * 获取此节点的子节点集合
     * <p> 返回此工具节点包含的所有子节点 </p>
     *
     * @return 子节点的不可修改集合
     */
    @Override
    public @NotNull Collection<Node> getChildren() {
        return children;
    }

    /**
     * 获取节点名称
     * <p> 返回当前节点的名称
     *
     * @return 节点名称
     */
    @Override
    public @NotNull String getName() {
        return name;
    }

    /**
     * 获取工具节点的唯一标识键
     * <p> 返回当前 RepairerToolNode 实例的 key 字段值, 该键用于标识工具节点的唯一性
     *
     * @return 工具节点的唯一标识键, 非空字符串
     */
    public @NotNull String getKey() {
        return key;
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
     * 更新节点展示数据
     * <p> 设置节点的可显示文本和图标
     *
     * @param presentation 展示数据对象
     */
    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(name);
        presentation.setIcon(AllIcons.Nodes.Folder);
    }

    /**
     * 更新展示数据
     * <p> 调用重载的 update 方法, 传入项目和展示数据对象, 用于更新节点的显示内容
     *
     * @param project          项目上下文
     * @param presentationData 展示数据对象, 用于设置节点的显示文本和图标等
     */
    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentationData) {
        update(presentationData);
    }

    /**
     * 判断当前对象与指定对象是否相等
     * <p> 比较两个对象是否为同一实例, 或是否为同类型的 RepairerToolNode 实例且 key 字段相等
     *
     * @param o 待比较的对象
     * @return 如果对象相等则返回 true, 否则返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairerToolNode that)) {
            return false;
        }
        return Objects.equals(key, that.key);
    }

    /**
     * 计算对象的哈希码值
     * <p> 基于 {@code key} 属性生成哈希码
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}

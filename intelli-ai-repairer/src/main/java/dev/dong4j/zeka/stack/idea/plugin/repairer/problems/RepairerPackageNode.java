package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.tree.LeafState;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Package group node for Repairer problems.
 */
public final class RepairerPackageNode extends Node {
    private final String name;
    private final List<Node> children;

    public RepairerPackageNode(@NotNull Node parent,
                               @NotNull String name,
                               @NotNull List<Node> children) {
        super(parent);
        this.name = name;
        this.children = children;
    }

    @Override
    public @NotNull Collection<Node> getChildren() {
        return children;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return children.isEmpty() ? LeafState.ALWAYS : LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(name);
        presentation.setIcon(AllIcons.Nodes.Package);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

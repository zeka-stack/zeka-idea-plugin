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
    private final String name;
    private final Module module;
    private final List<Node> children;

    public RepairerModuleNode(@NotNull Project project,
                              @NotNull String name,
                              Module module,
                              @NotNull List<Node> children) {
        super(project);
        this.name = name;
        this.module = module;
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
        presentation.setIcon(module != null ? AllIcons.Nodes.Module : AllIcons.General.Warning);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

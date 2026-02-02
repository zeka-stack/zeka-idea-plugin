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
    private final String key;
    private final String name;
    private final List<Node> children;

    public RepairerToolNode(@NotNull Project project,
                            @NotNull String key,
                            @NotNull String name,
                            @NotNull List<Node> children) {
        super(project);
        this.key = key;
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

    public @NotNull String getKey() {
        return key;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return children.isEmpty() ? LeafState.ALWAYS : LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(name);
        presentation.setIcon(AllIcons.Nodes.Folder);
    }

    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentationData) {
        update(presentationData);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}

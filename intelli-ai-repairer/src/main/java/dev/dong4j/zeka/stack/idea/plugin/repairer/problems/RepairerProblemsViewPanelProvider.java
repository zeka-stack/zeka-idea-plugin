package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * Provides the IntelliAI Repairer tab for the Problems tool window.
 */
public class RepairerProblemsViewPanelProvider implements ProblemsViewPanelProvider {
    private final Project project;

    public RepairerProblemsViewPanelProvider(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull ProblemsViewTab create() {
        return new RepairerProblemsViewPanel(project);
    }
}

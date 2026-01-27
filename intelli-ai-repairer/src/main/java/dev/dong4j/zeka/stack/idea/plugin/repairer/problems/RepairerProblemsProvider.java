package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * Problems provider for IntelliAI Repairer problems.
 */
public final class RepairerProblemsProvider implements ProblemsProvider {
    private final Project project;

    public RepairerProblemsProvider(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull Project getProject() {
        return project;
    }

    @Override
    public void dispose() {
        // Nothing to dispose.
    }
}

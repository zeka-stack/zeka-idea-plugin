package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * Problems tool window tab for IntelliAI Repairer.
 */
public class RepairerProblemsViewPanel extends ProblemsViewPanel {
    public static final String TAB_ID = "IntelliAI.Repairer.Problems";
    public static final String TAB_NAME = "IntelliAI Repairer";

    public RepairerProblemsViewPanel(@NotNull Project project) {
        super(project, TAB_ID, ProblemsViewState.getInstance(project), () -> new RepairerProblemsRoot(this, project));
    }

    @Override
    public @NotNull String getName(int tabIndex) {
        return TAB_NAME;
    }

    @Override
    public @NotNull String getTabId() {
        return TAB_ID;
    }
}

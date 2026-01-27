package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.FileProblem;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.codeInsight.multiverse.CodeInsightContext;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Problems view entry for a single Repairer violation.
 */
public final class RepairerProblem implements FileProblem {
    private final ProblemsProvider provider;
    private final CodeViolation violation;
    private final VirtualFile file;

    public RepairerProblem(@NotNull ProblemsProvider provider,
                           @NotNull CodeViolation violation,
                           @NotNull VirtualFile file) {
        this.provider = provider;
        this.violation = violation;
        this.file = file;
    }

    public @NotNull CodeViolation getViolation() {
        return violation;
    }

    @Override
    public @NotNull ProblemsProvider getProvider() {
        return provider;
    }

    @Override
    public @NotNull String getText() {
        return violation.ruleId == null || violation.ruleId.isBlank() ? "Rule" : violation.ruleId;
    }

    @Override
    public @Nullable String getGroup() {
        return violation.tool;
    }

    @Override
    public @Nullable CodeInsightContext getContextGroup() {
        return null;
    }

    @Override
    public @NotNull String getDescription() {
        return violation.message == null ? "" : violation.message;
    }

    @Override
    public @Nullable Icon getIcon() {
        int severity = violation.severity;
        if (severity <= 1) {
            return AllIcons.General.Error;
        }
        if (severity == 2) {
            return AllIcons.General.Warning;
        }
        if (severity >= 4) {
            return AllIcons.General.Information;
        }
        return AllIcons.General.Warning;
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public int getLine() {
        int line = violation.startLine > 0 ? violation.startLine - 1 : 0;
        return Math.max(0, line);
    }

    @Override
    public int getColumn() {
        int column = violation.startColumn > 0 ? violation.startColumn - 1 : 0;
        return Math.max(0, column);
    }
}

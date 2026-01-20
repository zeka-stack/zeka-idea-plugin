package dev.dong4j.zeka.stack.idea.plugin.repairer.checkstyle;

import com.intellij.codeInspection.LocalQuickFix;

import org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider;
import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 为 Checkstyle 违规问题提供 AI 自动修复入口.
 */
public class AICheckstyleQuickFixProvider implements CheckstyleQuickFixProvider {

    @Override
    @Nullable
    public LocalQuickFix[] getQuickFixes(@NotNull Problem problem) {
        if (!isFixable(problem)) {
            return null;
        }
        return new LocalQuickFix[] {new AICheckstyleFix(problem)};
    }

    private boolean isFixable(@NotNull Problem problem) {
        return problem.sourceName() != null && problem.message() != null && problem.target() != null;
    }
}

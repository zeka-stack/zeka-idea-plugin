package dev.dong4j.zeka.stack.idea.plugin.repairer.maven;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * Maven 执行完成后自动刷新报告.
 */
public class StaticAnalysisMavenStartupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        MavenReportListener.register(project);
        return Unit.INSTANCE;
    }
}

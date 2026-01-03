package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * AI Console Startup Activity
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-03 17:19:14
 * @since hello.world
 */
public class AIConsoleStartupActivity implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault() || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }
        ApplicationManager.getApplication().invokeLater(() ->
                                                            AIConsoleView.getInstance(project).ensureTabVisible()
                                                       );
        return Unit.INSTANCE;
    }
}

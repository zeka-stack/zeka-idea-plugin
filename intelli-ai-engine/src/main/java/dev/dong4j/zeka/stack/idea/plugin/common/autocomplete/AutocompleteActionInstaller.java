package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class AutocompleteActionInstaller implements ProjectActivity {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    @Override
    public @NotNull Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 只在非默认项目且非单元测试模式下运行
        if (project.isDefault() || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }

        if (!INSTALLED.compareAndSet(false, true)) {
            return Unit.INSTANCE;
        }
        EditorActionManager manager = EditorActionManager.getInstance();
        EditorActionHandler tabHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_TAB);
        manager.setActionHandler(IdeActions.ACTION_EDITOR_TAB, new AutocompleteAcceptActionHandler(tabHandler));

        EditorActionHandler escapeHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_ESCAPE);
        manager.setActionHandler(IdeActions.ACTION_EDITOR_ESCAPE, new AutocompleteRejectActionHandler(escapeHandler));

        return Unit.INSTANCE;
    }
}

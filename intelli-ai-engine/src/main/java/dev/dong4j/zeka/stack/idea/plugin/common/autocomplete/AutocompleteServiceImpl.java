package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
public final class AutocompleteServiceImpl implements AutocompleteService, Disposable {
    private final Project project;
    private final AutocompleteCoordinator coordinator;

    public AutocompleteServiceImpl(@NotNull Project project) {
        this.project = project;
        this.coordinator = new AutocompleteCoordinator(project);
    }

    @Override
    public void setEnabled(boolean enabled) {
        AutocompleteSettings settings = AutocompleteSettings.getInstance();
        settings.enabled = enabled;
        if (enabled) {
            enableAllOpenEditors();
        } else {
            coordinator.disposeAll();
        }
    }

    @Override
    public boolean isEnabled() {
        return AutocompleteSettings.getInstance().enabled;
    }

    @Override
    public void enableEditor(@NotNull Editor editor) {
        if (!isEnabled() || editor.getProject() == null || editor.getProject() != project) {
            return;
        }
        coordinator.enableEditor(editor);
    }

    @Override
    public void disableEditor(@NotNull Editor editor) {
        coordinator.disableEditor(editor);
    }

    @Override
    public @Nullable AutocompleteTracker getTracker(@NotNull Editor editor) {
        return coordinator.getTracker(editor);
    }

    private void enableAllOpenEditors() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (project.equals(editor.getProject())) {
                coordinator.enableEditor(editor);
            }
        }
    }

    @Override
    public void dispose() {
        coordinator.dispose();
    }
}

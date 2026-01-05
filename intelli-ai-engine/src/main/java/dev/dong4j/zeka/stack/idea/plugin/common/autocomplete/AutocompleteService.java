package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.Nullable;

public interface AutocompleteService {
    static AutocompleteService getInstance(Project project) {
        return project.getService(AutocompleteService.class);
    }

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void enableEditor(Editor editor);

    void disableEditor(Editor editor);

    @Nullable
    AutocompleteTracker getTracker(Editor editor);
}

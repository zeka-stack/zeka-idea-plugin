package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AutocompleteCoordinator implements Disposable {
    private final Project project;
    private final Map<Editor, AutocompleteTracker> trackers = new ConcurrentHashMap<>();

    AutocompleteCoordinator(@NotNull Project project) {
        this.project = project;
    }

    void enableEditor(@NotNull Editor editor) {
        trackers.computeIfAbsent(editor, ed -> new AutocompleteTracker(project, ed));
    }

    void disableEditor(@NotNull Editor editor) {
        AutocompleteTracker tracker = trackers.remove(editor);
        if (tracker != null) {
            tracker.dispose();
        }
    }

    @Nullable
    AutocompleteTracker getTracker(@NotNull Editor editor) {
        return trackers.get(editor);
    }

    void disposeAll() {
        for (AutocompleteTracker tracker : trackers.values()) {
            tracker.dispose();
        }
        trackers.clear();
    }

    @Override
    public void dispose() {
        disposeAll();
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AutocompleteAcceptActionHandler extends EditorActionHandler {
    private final EditorActionHandler originalHandler;

    public AutocompleteAcceptActionHandler(@NotNull EditorActionHandler originalHandler) {
        super(true);
        this.originalHandler = originalHandler;
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, @NotNull DataContext dataContext) {
        AutocompleteTracker tracker = getTracker(editor);
        if (tracker != null && tracker.hasSuggestion()) {
            tracker.acceptSuggestion();
            return;
        }
        originalHandler.execute(editor, caret, dataContext);
    }

    private @Nullable AutocompleteTracker getTracker(@NotNull Editor editor) {
        if (editor.getProject() == null) {
            return null;
        }
        return AutocompleteService.getInstance(editor.getProject()).getTracker(editor);
    }
}

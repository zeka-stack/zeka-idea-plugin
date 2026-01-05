package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class GhostTextSuggestion extends AutocompleteSuggestion {
    private final String content;
    private final int startOffset;
    private final String autocompleteId;
    private final Document document;
    private Inlay<?> inlay;
    private boolean disposed;

    GhostTextSuggestion(@NotNull String content,
                        int startOffset,
                        @NotNull String autocompleteId,
                        @NotNull Document document) {
        this.content = content;
        this.startOffset = startOffset;
        this.autocompleteId = autocompleteId;
        this.document = document;
    }

    @NotNull
    @Override
    String getContent() {
        return content;
    }

    @Override
    int getStartOffset() {
        return startOffset;
    }

    @Override
    int getEndOffset() {
        return startOffset;
    }

    @NotNull
    @Override
    String getAutocompleteId() {
        return autocompleteId;
    }

    @Override
    void show(@NotNull Editor editor, boolean isPostJumpSuggestion) {
        if (content.isBlank()) {
            return;
        }
        String displayText = content;
        int newlineIndex = content.indexOf('\n');
        if (newlineIndex >= 0) {
            displayText = content.substring(0, newlineIndex);
        }
        if (displayText.isBlank()) {
            return;
        }
        GhostTextInlayRenderer renderer = new GhostTextInlayRenderer(editor, displayText);
        inlay = editor.getInlayModel().addInlineElement(startOffset, true, renderer);
    }

    @Nullable
    @Override
    Disposable accept(@NotNull Editor editor) {
        if (content.isBlank() || editor.getProject() == null) {
            dispose();
            return null;
        }
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            document.insertString(startOffset, content);
            editor.getCaretModel().moveToOffset(startOffset + content.length());
        });
        dispose();
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (inlay != null) {
            inlay.dispose();
        }
        inlay = null;
        markDisposed();
    }

    @Override
    public String toString() {
        return "GhostTextSuggestion{startOffset=" + startOffset + ", contentLength=" + content.length() + "}";
    }
}

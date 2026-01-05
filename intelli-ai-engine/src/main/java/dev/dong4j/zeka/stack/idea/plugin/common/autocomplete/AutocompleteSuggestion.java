package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class AutocompleteSuggestion implements Disposable {
    private final String rawContent;
    private final String insertionText;
    private final String displayText;
    private final int startOffset;
    private Inlay<?> inlay;
    private boolean disposed;

    AutocompleteSuggestion(@NotNull String rawContent,
                           @NotNull String insertionText,
                           @NotNull String displayText,
                           int startOffset) {
        this.rawContent = rawContent;
        this.insertionText = insertionText;
        this.displayText = displayText;
        this.startOffset = startOffset;
    }

    @Nullable
    Inlay<?> show(@NotNull Editor editor) {
        if (displayText.isBlank()) {
            return null;
        }
        GhostTextInlayRenderer renderer = new GhostTextInlayRenderer(editor, displayText);
        inlay = editor.getInlayModel().addInlineElement(startOffset, true, renderer);
        return inlay;
    }

    boolean accept(@NotNull Editor editor) {
        if (insertionText.isBlank()) {
            dispose();
            return false;
        }
        if (editor.getProject() == null) {
            dispose();
            return false;
        }
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            Document document = editor.getDocument();
            int caretOffset = editor.getCaretModel().getOffset();
            String text = insertionText;
            int cursorIndex = text.indexOf("<|cursor|>");
            if (cursorIndex >= 0) {
                text = text.replace("<|cursor|>", "");
            } else {
                cursorIndex = text.length();
            }
            document.insertString(caretOffset, text);
            editor.getCaretModel().moveToOffset(caretOffset + cursorIndex);
        });
        dispose();
        return true;
    }

    int getStartOffset() {
        return startOffset;
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
    }

    @Override
    public String toString() {
        return "AutocompleteSuggestion{startOffset=" + startOffset + ", contentLength=" + rawContent.length() + "}";
    }
}

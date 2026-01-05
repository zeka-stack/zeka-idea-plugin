package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MultipleGhostTextSuggestion extends AutocompleteSuggestion {
    private final String content;
    private final int startOffset;
    private final int endOffset;
    private final String autocompleteId;
    private final List<GhostTextSuggestion> ghostTextSuggestions;

    MultipleGhostTextSuggestion(@NotNull String content,
                                int startOffset,
                                int endOffset,
                                @NotNull String autocompleteId,
                                @NotNull List<GhostTextSuggestion> ghostTextSuggestions) {
        this.content = content;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.autocompleteId = autocompleteId;
        this.ghostTextSuggestions = new ArrayList<>(ghostTextSuggestions);
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
        return endOffset;
    }

    @NotNull
    @Override
    String getAutocompleteId() {
        return autocompleteId;
    }

    @Override
    void show(@NotNull Editor editor, boolean isPostJumpSuggestion) {
        for (GhostTextSuggestion suggestion : ghostTextSuggestions) {
            suggestion.show(editor, isPostJumpSuggestion);
        }
    }

    @Nullable
    @Override
    Disposable accept(@NotNull Editor editor) {
        if (editor.getProject() == null) {
            dispose();
            return null;
        }
        Document document = editor.getDocument();
        List<GhostTextSuggestion> sorted = new ArrayList<>(ghostTextSuggestions);
        if (sorted.isEmpty()) {
            dispose();
            return null;
        }
        sorted.sort(Comparator.comparingInt(GhostTextSuggestion::getStartOffset).reversed());
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            for (GhostTextSuggestion suggestion : sorted) {
                document.insertString(suggestion.getStartOffset(), suggestion.getContent());
            }
            GhostTextSuggestion last = sorted.get(0);
            editor.getCaretModel().moveToOffset(last.getStartOffset() + last.getContent().length());
        });
        dispose();
        return null;
    }

    @Override
    public void dispose() {
        for (GhostTextSuggestion suggestion : ghostTextSuggestions) {
            suggestion.dispose();
        }
        markDisposed();
    }

    int getTotalInsertionLength() {
        int total = 0;
        for (GhostTextSuggestion suggestion : ghostTextSuggestions) {
            total += suggestion.getContent().length();
        }
        return total;
    }

    int getMaxStartOffset() {
        int max = startOffset;
        for (GhostTextSuggestion suggestion : ghostTextSuggestions) {
            max = Math.max(max, suggestion.getStartOffset());
        }
        return max;
    }
}

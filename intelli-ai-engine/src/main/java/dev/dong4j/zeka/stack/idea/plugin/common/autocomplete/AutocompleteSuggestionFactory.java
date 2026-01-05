package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class AutocompleteSuggestionFactory {
    @Nullable
    AutocompleteSuggestion build(@NotNull Editor editor, @NotNull String content) {
        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();
        int line = document.getLineNumber(caretOffset);
        int lineStart = document.getLineStartOffset(line);
        String linePrefix = document.getText(TextRange.create(lineStart, caretOffset));

        String insertionText = content;
        if (insertionText.startsWith(linePrefix)) {
            insertionText = insertionText.substring(linePrefix.length());
        }
        insertionText = insertionText.replace("\r\n", "\n");
        if (insertionText.isBlank()) {
            return null;
        }

        String displayText = insertionText;
        int newlineIndex = insertionText.indexOf('\n');
        if (newlineIndex >= 0) {
            displayText = insertionText.substring(0, newlineIndex);
        }

        return new AutocompleteSuggestion(content, insertionText, displayText, caretOffset);
    }
}

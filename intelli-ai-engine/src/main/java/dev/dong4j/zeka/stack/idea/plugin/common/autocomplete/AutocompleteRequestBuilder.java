package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class AutocompleteRequestBuilder {
    private static final int CONTEXT_WINDOW = 2000;

    @NotNull
    AutocompleteCompletionRequest build(@NotNull Editor editor, @NotNull AutocompleteTriggerMode triggerMode) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int start = Math.max(0, offset - CONTEXT_WINDOW);
        int end = Math.min(document.getTextLength(), offset + CONTEXT_WINDOW);

        String prefix = document.getText(TextRange.create(start, offset));
        String suffix = document.getText(TextRange.create(offset, end));

        int line = document.getLineNumber(offset);
        int column = offset - document.getLineStartOffset(line);

        String filePath = "unknown";
        String language = "unknown";
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            filePath = file.getPath();
            language = file.getFileType().getName();
        }

        AutocompleteContext context = new AutocompleteContext(filePath, language, prefix, suffix, line, column);
        AutocompletePromptBuilder promptBuilder = new AutocompletePromptBuilder();
        return new AutocompleteCompletionRequest(
            UUID.randomUUID().toString(),
            context,
            promptBuilder.buildSystemPrompt(),
            promptBuilder.buildUserPrompt(context),
            triggerMode
        );
    }
}

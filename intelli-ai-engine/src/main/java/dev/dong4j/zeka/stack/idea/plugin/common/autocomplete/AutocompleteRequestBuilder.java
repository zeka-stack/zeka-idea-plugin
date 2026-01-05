package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class AutocompleteRequestBuilder {
    private static final int CONTEXT_WINDOW = 2000;
    private static final int MAX_ALLOWED_OCCURRENCES = 12;
    private final OccurrenceCandidateFinder candidateFinder = new OccurrenceCandidateFinder();

    @NotNull
    AutocompleteCompletionRequest build(@NotNull Editor editor,
                                        @NotNull AutocompleteTriggerMode triggerMode,
                                        @NotNull EditRecord lastEdit) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int line = document.getLineNumber(offset);
        int column = offset - document.getLineStartOffset(line);

        String filePath = "unknown";
        String language = "unknown";
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            filePath = file.getPath();
            language = file.getFileType().getName();
        }

        String fullText = document.getText();
        String documentText = trimDocument(fullText, lastEdit);
        String allowedOccurrences = candidateFinder.buildAllowedOccurrences(fullText, lastEdit.oldText(), lastEdit,
                                                                            MAX_ALLOWED_OCCURRENCES);
        AutocompleteContext context = new AutocompleteContext(filePath, language, documentText, allowedOccurrences, lastEdit, line, column);
        AutocompletePromptBuilder promptBuilder = new AutocompletePromptBuilder();
        return new AutocompleteCompletionRequest(
            UUID.randomUUID().toString(),
            context,
            promptBuilder.buildSystemPrompt(),
            promptBuilder.buildUserPrompt(context),
            triggerMode
        );
    }

    @NotNull
    private String trimDocument(@NotNull String text, @NotNull EditRecord lastEdit) {
        if (text.length() <= CONTEXT_WINDOW * 2) {
            return text;
        }
        int center = Math.max(0, lastEdit.startOffset());
        int start = Math.max(0, center - CONTEXT_WINDOW);
        int end = Math.min(text.length(), center + CONTEXT_WINDOW);
        return text.substring(start, end);
    }
}

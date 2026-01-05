package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

final class AutocompletePromptBuilder {
    private static final String SYSTEM_PROMPT = "You are a code edit prediction engine. "
                                                + "Given the current file content and the latest user edit, propose the next edits "
                                                + "the user likely wants to apply elsewhere in the file. "
                                                + "Return ONLY valid JSON (no markdown).";

    @NotNull
    String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @NotNull
    String buildUserPrompt(@NotNull AutocompleteContext context) {
        EditRecord lastEdit = context.lastEdit();
        String oldText = lastEdit.oldText();
        String newText = lastEdit.newText();
        String sb = "File: " + context.filePath() + '\n' +
                    "Language: " + context.language() + '\n' +
                    "Cursor: line " + context.line() + ", column " + context.column() + '\n' +
                    "Latest edit: start=" + lastEdit.startOffset() +
                    ", end=" + lastEdit.endOffset() + '\n' +
                    "Old text:\n" + oldText + '\n' +
                    "New text:\n" + newText + '\n' +
                    "Current file content:\n" + context.documentText() + '\n' +
                    "Allowed occurrences (excluding latest edit):\n" + context.allowedOccurrences() + '\n' +
                    "Task: Suggest the next edits that apply the SAME transformation elsewhere in the file.\n" +
                    "Rules:\n" +
                    "- Do NOT propose edits inside the latest edit range.\n" +
                    "- Do NOT return the same text as old text at that range.\n" +
                    "- Prefer replacing other occurrences of the old text with the new text.\n" +
                    "- You MUST choose start/end from the allowed occurrences list. If list is empty, return [].\n" +
                    "- If no other candidates, return [].\n" +
                    "Return JSON array of edit objects. Each object schema:\n" +
                    "{ \"start_index\": int, \"end_index\": int, \"completion\": string, \"confidence\": number, \"autocomplete_id\": " +
                    "string }";
        return sb;
    }
}

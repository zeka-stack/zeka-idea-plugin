package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

final class AutocompletePromptBuilder {
    private static final String SYSTEM_PROMPT = "You are a code completion engine. "
                                                + "Return only the code to insert at the cursor, no explanations, no markdown.";

    @NotNull
    String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @NotNull
    String buildUserPrompt(@NotNull AutocompleteContext context) {
        String sb = "File: " + context.filePath() + '\n' +
                    "Language: " + context.language() + '\n' +
                    "Cursor: line " + context.line() + ", column " + context.column() + '\n' +
                    "Prefix:\n" + context.prefix() + '\n' +
                    "Suffix:\n" + context.suffix() + '\n' +
                    "Return the continuation to insert at cursor only.";
        return sb;
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AutocompleteResponseParser {
    private static final Pattern CODE_FENCE = Pattern.compile("```(?:[a-zA-Z0-9]+)?\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);

    @NotNull
    String parse(@NotNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        Matcher matcher = CODE_FENCE.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }
}

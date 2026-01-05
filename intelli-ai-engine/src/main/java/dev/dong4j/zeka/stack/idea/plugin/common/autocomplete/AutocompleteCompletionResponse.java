package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

public record AutocompleteCompletionResponse(@NotNull String content,
                                             boolean finished) {
}

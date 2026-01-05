package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

public record AutocompleteContext(@NotNull String filePath,
                                  @NotNull String language,
                                  @NotNull String documentText,
                                  @NotNull String allowedOccurrences,
                                  @NotNull EditRecord lastEdit,
                                  int line,
                                  int column) {
}

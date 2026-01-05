package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

public record AutocompleteContext(@NotNull String filePath,
                                  @NotNull String language,
                                  @NotNull String prefix,
                                  @NotNull String suffix,
                                  int line,
                                  int column) {
}

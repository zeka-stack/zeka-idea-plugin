package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

import java.util.List;

record NextEditCompletionResponse(@NotNull List<NextEditAutocompletion> completions,
                                  @NotNull String rawContent) {
}

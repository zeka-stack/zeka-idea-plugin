package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

public record AutocompleteCompletionRequest(@NotNull String requestId,
                                            @NotNull AutocompleteContext context,
                                            @NotNull String systemPrompt,
                                            @NotNull String userPrompt,
                                            @NotNull AutocompleteTriggerMode triggerMode) {
}

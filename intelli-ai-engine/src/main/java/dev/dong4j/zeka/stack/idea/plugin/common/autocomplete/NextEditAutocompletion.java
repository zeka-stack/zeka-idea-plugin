package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

final class NextEditAutocompletion {
    private int startIndex;
    private int endIndex;
    private String completion;
    private final float confidence;
    private final String autocompleteId;

    NextEditAutocompletion(int startIndex, int endIndex, @NotNull String completion, float confidence, @NotNull String autocompleteId) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.completion = completion;
        this.confidence = confidence;
        this.autocompleteId = autocompleteId;
    }

    int getStartIndex() {
        return startIndex;
    }

    int getEndIndex() {
        return endIndex;
    }

    @NotNull
    String getCompletion() {
        return completion;
    }

    float getConfidence() {
        return confidence;
    }

    @NotNull
    String getAutocompleteId() {
        return autocompleteId;
    }

    void setCompletion(@NotNull String completion) {
        this.completion = completion;
    }

    void adjustOffsets(int delta) {
        startIndex += delta;
        endIndex += delta;
    }

    @NotNull
    String applyChangesTo(@NotNull String original) {
        return original.substring(0, startIndex) + completion + original.substring(endIndex);
    }
}

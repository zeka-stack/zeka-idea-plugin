package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

record EditRecord(int startOffset, int endOffset, String oldText, String newText, long timestamp) {
    EditRecord(int startOffset, int endOffset, @NotNull String oldText, @NotNull String newText, long timestamp) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.oldText = oldText;
        this.newText = newText;
        this.timestamp = timestamp;
    }

    @Override
    @NotNull
    public String oldText() {
        return oldText;
    }

    @Override
    @NotNull
    public String newText() {
        return newText;
    }
}

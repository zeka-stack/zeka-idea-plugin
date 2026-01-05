package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

record OccurrenceCandidate(int startIndex, int endIndex, int line, double score, String preview) {
    OccurrenceCandidate(int startIndex, int endIndex, int line, double score, @NotNull String preview) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.line = line;
        this.score = score;
        this.preview = preview;
    }

    @Override
    @NotNull
    public String preview() {
        return preview;
    }
}

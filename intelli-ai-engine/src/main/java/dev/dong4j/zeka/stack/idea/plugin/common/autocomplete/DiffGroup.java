package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

record DiffGroup(String additions, String deletions, int index) {
    DiffGroup(@NotNull String additions, @NotNull String deletions, int index) {
        this.additions = additions;
        this.deletions = deletions;
        this.index = index;
    }

    @Override
    @NotNull
    public String additions() {
        return additions;
    }

    @Override
    @NotNull
    public String deletions() {
        return deletions;
    }

    boolean hasAdditions() {
        return !additions.isEmpty();
    }

    boolean hasDeletions() {
        return !deletions.isEmpty();
    }
}

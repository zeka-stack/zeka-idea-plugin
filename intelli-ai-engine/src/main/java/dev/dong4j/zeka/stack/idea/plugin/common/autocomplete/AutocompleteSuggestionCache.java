package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

final class AutocompleteSuggestionCache {
    private String lastKey;

    boolean isDuplicate(String key) {
        return key != null && key.equals(lastKey);
    }

    void update(String key) {
        this.lastKey = key;
    }
}

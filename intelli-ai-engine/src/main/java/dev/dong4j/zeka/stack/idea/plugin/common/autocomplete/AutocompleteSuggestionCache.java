package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

final class AutocompleteSuggestionCache {
    private String lastContent;

    boolean isDuplicate(String content) {
        return content != null && content.equals(lastContent);
    }

    void update(String content) {
        this.lastContent = content;
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

final class LargeFileRule implements TriggerRule {
    @Override
    public boolean check(TriggerContext context) {
        int length = context.getDocument().getTextLength();
        return length <= AutocompleteSettings.getInstance().maxFileLength;
    }
}

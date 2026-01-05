package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

final class MultiCaretRule implements TriggerRule {
    @Override
    public boolean check(TriggerContext context) {
        return context.getEditor().getCaretModel().getCaretCount() <= 1;
    }
}

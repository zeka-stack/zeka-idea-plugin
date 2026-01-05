package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import java.util.List;

final class TriggerEngine {
    private final List<TriggerRule> rules;

    TriggerEngine(List<TriggerRule> rules) {
        this.rules = rules;
    }

    boolean shouldTrigger(TriggerContext context) {
        for (TriggerRule rule : rules) {
            if (!rule.check(context)) {
                if (!rule.ignoreWhenNotPass()) {
                    return false;
                }
            } else if (rule.passAllWhenPassOne()) {
                return true;
            }
        }
        return true;
    }
}

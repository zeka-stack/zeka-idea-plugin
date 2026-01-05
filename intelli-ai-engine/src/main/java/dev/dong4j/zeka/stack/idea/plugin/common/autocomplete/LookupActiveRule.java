package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.codeInsight.lookup.LookupManager;

final class LookupActiveRule implements TriggerRule {
    @Override
    public boolean check(TriggerContext context) {
        if (context.getTriggerMode() == AutocompleteTriggerMode.MANUAL) {
            return true;
        }
        return LookupManager.getActiveLookup(context.getEditor()) == null;
    }
}

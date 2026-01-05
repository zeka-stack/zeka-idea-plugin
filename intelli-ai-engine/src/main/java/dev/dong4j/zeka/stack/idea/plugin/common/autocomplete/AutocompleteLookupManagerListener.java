package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManagerListener;
import com.intellij.openapi.editor.Editor;

import org.jetbrains.annotations.Nullable;

public class AutocompleteLookupManagerListener implements LookupManagerListener {
    @Override
    public void activeLookupChanged(@Nullable Lookup oldLookup, @Nullable Lookup newLookup) {
        Lookup lookup = newLookup != null ? newLookup : oldLookup;
        if (lookup == null) {
            return;
        }
        Editor editor = lookup.getEditor();
        if (editor.getProject() == null) {
            return;
        }
        AutocompleteTracker tracker = AutocompleteService.getInstance(editor.getProject()).getTracker(editor);
        if (tracker == null) {
            return;
        }
        if (newLookup != null) {
            tracker.rejectSuggestion();
            return;
        }
        if (AutocompleteSettings.getInstance().lookupTrigger) {
            tracker.trigger(AutocompleteTriggerMode.LOOKUP);
        }
    }
}

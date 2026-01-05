package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Editor;

final class EditorAvailableRule implements TriggerRule {
    @Override
    public boolean check(TriggerContext context) {
        Editor editor = context.getEditor();
        if (editor.isDisposed()) {
            return false;
        }
        if (editor.isViewer() || editor.isOneLineMode()) {
            return false;
        }
        return editor.getProject() != null && !editor.getProject().isDisposed();
    }
}

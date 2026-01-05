package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

import org.jetbrains.annotations.NotNull;

final class TriggerContext {
    private final Editor editor;
    private final Document document;
    private final int caretOffset;
    private final AutocompleteTriggerMode triggerMode;

    TriggerContext(@NotNull Editor editor, @NotNull AutocompleteTriggerMode triggerMode) {
        this.editor = editor;
        this.document = editor.getDocument();
        this.caretOffset = editor.getCaretModel().getOffset();
        this.triggerMode = triggerMode;
    }

    Editor getEditor() {
        return editor;
    }

    Document getDocument() {
        return document;
    }

    int getCaretOffset() {
        return caretOffset;
    }

    AutocompleteTriggerMode getTriggerMode() {
        return triggerMode;
    }
}

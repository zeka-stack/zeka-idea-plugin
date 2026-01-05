package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Document;

final class IMEChineseRule implements TriggerRule {
    @Override
    public boolean check(TriggerContext context) {
        if (context.getTriggerMode() == AutocompleteTriggerMode.MANUAL) {
            return true;
        }
        int offset = context.getCaretOffset();
        if (offset <= 0) {
            return true;
        }
        Document document = context.getDocument();
        if (offset > document.getTextLength()) {
            return true;
        }
        char c = document.getCharsSequence().charAt(offset - 1);
        return !(c >= 0x4E00 && c <= 0x9FFF);
    }
}

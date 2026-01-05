package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayProperties;
import com.intellij.openapi.util.Disposer;

import org.jetbrains.annotations.NotNull;

final class TabHintManager implements Disposable {
    private final Editor editor;
    private final int lineEndOffset;
    private Inlay<?> inlineInlay;

    TabHintManager(@NotNull Editor editor, int lineEndOffset, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.lineEndOffset = lineEndOffset;
        Disposer.register(parentDisposable, this);
    }

    void show() {
        if (inlineInlay != null) {
            return;
        }
        InlayProperties properties = new InlayProperties();
        properties.relatesToPrecedingText(true);
        properties.disableSoftWrapping(true);
        TabHintRenderer renderer = new TabHintRenderer(editor, this);
        inlineInlay = editor.getInlayModel().addInlineElement(lineEndOffset, properties, renderer);
    }

    @Override
    public void dispose() {
        if (inlineInlay != null) {
            inlineInlay.dispose();
        }
        inlineInlay = null;
    }
}

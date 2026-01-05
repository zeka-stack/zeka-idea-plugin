package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayProperties;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBViewport;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;

final class JumpHintManager implements Disposable {
    private final Editor editor;
    private final int targetLineNumber;
    private final int lineStartOffset;
    private final boolean wasVisibleOnCreation;
    private JBPopup jumpPopup;
    private VisibleAreaListener scrollListener;
    private Editor currentEditor;
    private Inlay<?> inlineInlay;

    JumpHintManager(@NotNull Editor editor, int targetLineNumber, int lineStartOffset, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.targetLineNumber = targetLineNumber;
        this.lineStartOffset = lineStartOffset;
        this.wasVisibleOnCreation = isLineVisible(editor, lineStartOffset);
        Disposer.register(parentDisposable, this);
    }

    void showIfNeeded() {
        createJumpInlay();
        scrollListener = this::onVisibleAreaChanged;
        ScrollingModel scrollingModel = editor.getScrollingModel();
        scrollingModel.addVisibleAreaListener(scrollListener);
        currentEditor = editor;
        updateVisibility(editor, targetLineNumber, lineStartOffset);
    }

    private void createJumpInlay() {
        if (inlineInlay != null) {
            return;
        }
        int lineEndOffset = editor.getDocument().getLineEndOffset(targetLineNumber);
        InlayProperties properties = new InlayProperties();
        properties.relatesToPrecedingText(true);
        properties.disableSoftWrapping(true);
        JumpInlineRenderer renderer = new JumpInlineRenderer(editor, this);
        inlineInlay = editor.getInlayModel().addInlineElement(lineEndOffset, properties, renderer);
    }

    private void updateVisibility(Editor editor, int lineNumber, int lineStartOffset) {
        boolean isVisible = wasVisibleOnCreation || isLineVisible(editor, lineStartOffset);
        if (isVisible) {
            if (jumpPopup != null) {
                jumpPopup.dispose();
            }
            jumpPopup = null;
        } else if (jumpPopup == null) {
            showJumpPopup(editor, lineNumber);
        }
    }

    private boolean isLineVisible(Editor editor, int lineStartOffset) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        double lineStartY = editor.offsetToPoint2D(lineStartOffset).getY();
        int lineHeight = editor.getLineHeight();
        double lineEndY = lineStartY + lineHeight;
        return lineStartY <= visibleArea.y + visibleArea.height && lineEndY >= visibleArea.y;
    }

    private void showJumpPopup(Editor editor, int targetLineNumber) {
        if (jumpPopup != null) {
            jumpPopup.dispose();
        }
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        int targetLineY = editor.visualLineToY(targetLineNumber);
        boolean isTargetBelow = targetLineY > visibleArea.y + visibleArea.height;
        JumpHintRenderer renderer = new JumpHintRenderer(editor, isTargetBelow, this);
        JComponent component = renderer.createJumpHintComponent();
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(component, null)
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(false)
            .setCancelOnClickOutside(true)
            .setShowBorder(false)
            .createPopup();
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                // no-op
            }
        });
        jumpPopup = popup;
        JComponent editorComponent = editor.getContentComponent();
        Component parent = editorComponent.getParent();
        if (!(parent instanceof JBViewport viewport)) {
            return;
        }
        Point point = new Point(viewport.getWidth() / 2 - component.getPreferredSize().width / 2,
                                isTargetBelow ? viewport.getHeight() - 20 - component.getPreferredSize().height : 20);
        jumpPopup.show(new RelativePoint(viewport, point));
    }

    @Override
    public void dispose() {
        if (jumpPopup != null) {
            Disposer.dispose(jumpPopup);
        }
        jumpPopup = null;
        if (scrollListener != null && currentEditor != null) {
            currentEditor.getScrollingModel().removeVisibleAreaListener(scrollListener);
        }
        scrollListener = null;
        currentEditor = null;
        if (inlineInlay != null) {
            inlineInlay.dispose();
        }
        inlineInlay = null;
    }

    private void onVisibleAreaChanged(@NotNull VisibleAreaEvent event) {
        updateVisibility(event.getEditor(), targetLineNumber, lineStartOffset);
    }
}

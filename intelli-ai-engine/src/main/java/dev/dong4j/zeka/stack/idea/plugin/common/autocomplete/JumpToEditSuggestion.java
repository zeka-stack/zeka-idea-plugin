package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class JumpToEditSuggestion extends AutocompleteSuggestion {
    private final String content;
    private final int startOffset;
    private final int endOffset;
    private final NextEditAutocompletion originalCompletion;
    private final String autocompleteId;
    private final String oldContent;
    private final Document document;
    private final int lineNumber;
    private final Editor editor;
    private JumpHintManager jumpHintManager;

    JumpToEditSuggestion(@NotNull String content,
                         int startOffset,
                         int endOffset,
                         @NotNull NextEditAutocompletion originalCompletion,
                         @NotNull String autocompleteId,
                         @NotNull String oldContent,
                         @NotNull Editor editor) {
        this.content = content;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.originalCompletion = originalCompletion;
        this.autocompleteId = autocompleteId;
        this.oldContent = oldContent;
        this.editor = editor;
        this.document = editor.getDocument();
        this.lineNumber = document.getLineNumber(Math.max(0, startOffset - 1));
    }

    @NotNull
    @Override
    String getContent() {
        return content;
    }

    @Override
    int getStartOffset() {
        return startOffset;
    }

    @Override
    int getEndOffset() {
        return endOffset;
    }

    @NotNull
    @Override
    String getAutocompleteId() {
        return autocompleteId;
    }

    @NotNull
    NextEditAutocompletion getOriginalCompletion() {
        return originalCompletion;
    }

    @Override
    void show(@NotNull Editor editor, boolean isPostJumpSuggestion) {
        jumpHintManager = new JumpHintManager(editor, lineNumber, startOffset, this);
        jumpHintManager.showIfNeeded();
    }

    @Nullable
    @Override
    Disposable accept(@NotNull Editor editor) {
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);
        String lineText = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset).toString();
        int firstNonWhitespace = findFirstNonWhitespaceOffset(lineText);
        int targetOffset = lineStartOffset + Math.max(0, firstNonWhitespace);
        double targetY = editor.offsetToPoint2D(targetOffset).getY();
        boolean isTargetVisible = isTargetVisible(editor, targetY);
        editor.getCaretModel().moveToOffset(targetOffset);
        editor.getSelectionModel().setSelection(targetOffset, targetOffset);
        if (!isTargetVisible) {
            editor.getScrollingModel().disableAnimation();
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            editor.getScrollingModel().enableAnimation();
        }
        return null;
    }

    @NotNull
    @Override
    String rejectionCacheKey() {
        return "jump_to_edit_offset:" + startOffset;
    }

    @Override
    public void dispose() {
        if (jumpHintManager != null) {
            jumpHintManager.dispose();
        }
        jumpHintManager = null;
        editor.getComponent().repaint();
        markDisposed();
    }

    @NotNull
    String getOldContent() {
        return oldContent;
    }

    private int findFirstNonWhitespaceOffset(@NotNull String lineText) {
        for (int i = 0; i < lineText.length(); i++) {
            if (!Character.isWhitespace(lineText.charAt(i))) {
                return i;
            }
        }
        return 0;
    }

    private boolean isTargetVisible(@NotNull Editor editor, double targetY) {
        java.awt.Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        int lineHeight = editor.getLineHeight();
        return targetY >= visibleArea.y + lineHeight && targetY <= visibleArea.y + visibleArea.height - lineHeight;
    }
}

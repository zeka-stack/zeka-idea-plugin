package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

final class PopupSuggestion extends AutocompleteSuggestion {
    private final String content;
    private final int startOffset;
    private final int endOffset;
    private final String autocompleteId;
    private final String oldContent;
    private final Editor editor;
    private JBPopup popup;
    private RangeHighlighter highlighter;

    PopupSuggestion(@NotNull String content,
                    int startOffset,
                    int endOffset,
                    @NotNull String autocompleteId,
                    @NotNull String oldContent,
                    @NotNull Editor editor) {
        this.content = content;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.autocompleteId = autocompleteId;
        this.oldContent = oldContent;
        this.editor = editor;
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

    @Override
    void show(@NotNull Editor editor, boolean isPostJumpSuggestion) {
        if (popup != null) {
            popup.dispose();
        }
        JEditorPane pane = new JEditorPane("text/html", buildDiffHtml());
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(editor.getColorsScheme().getDefaultBackground());
        pane.setBorder(JBUI.Borders.empty(6));
        pane.setFont(editor.getColorsScheme().getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN));
        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(JBUI.Borders.empty());
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, pane)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(false)
            .setShowBorder(true)
            .createPopup();
        popup.addListener(new com.intellij.openapi.ui.popup.JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                dispose();
            }
        });
        com.intellij.openapi.editor.VisualPosition visualPosition = editor.offsetToVisualPosition(startOffset);
        java.awt.Point point = editor.visualPositionToXY(visualPosition);
        popup.showInScreenCoordinates(editor.getContentComponent(), point);
        highlightRange(editor);
    }

    @Nullable
    @Override
    Disposable accept(@NotNull Editor editor) {
        if (editor.getProject() == null) {
            dispose();
            return null;
        }
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            editor.getDocument().replaceString(startOffset, endOffset, content);
            editor.getCaretModel().moveToOffset(startOffset + content.length());
        });
        dispose();
        return null;
    }

    @Override
    public void dispose() {
        if (popup != null) {
            Disposer.dispose(popup);
        }
        popup = null;
        if (highlighter != null) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
        highlighter = null;
        markDisposed();
    }

    @NotNull
    String getOldContent() {
        return oldContent;
    }

    private void highlightRange(@NotNull Editor editor) {
        TextAttributes attrs = new TextAttributes();
        attrs.setBackgroundColor(new JBColor(new Color(255, 237, 200), new Color(80, 60, 20)));
        highlighter = editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 1, attrs,
                                                                  com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE);
    }

    private String buildDiffHtml() {
        String prefix = commonPrefix(oldContent, content);
        String suffix = commonSuffix(oldContent.substring(prefix.length()), content.substring(prefix.length()));
        String oldMid = oldContent.substring(prefix.length(), oldContent.length() - suffix.length());
        String newMid = content.substring(prefix.length(), content.length() - suffix.length());
        String oldHtml = escapeHtml(prefix) + wrapRemoved(oldMid) + escapeHtml(suffix);
        String newHtml = escapeHtml(prefix) + wrapAdded(newMid) + escapeHtml(suffix);
        String titleStyle = "font-family: " + Font.MONOSPACED + "; font-size: 12px;";
        return "<html><body style='" + titleStyle + "'>" +
               "<div><b>OLD</b>: " + oldHtml + "</div>" +
               "<div><b>NEW</b>: " + newHtml + "</div>" +
               "</body></html>";
    }

    private String commonPrefix(String a, String b) {
        int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return a.substring(0, i);
    }

    private String commonSuffix(String a, String b) {
        int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
            i++;
        }
        return a.substring(a.length() - i);
    }

    private String wrapRemoved(String text) {
        if (text.isEmpty()) {
            return "";
        }
        return "<span style='background:#ffd8d8;text-decoration:line-through;'>" + escapeHtml(text) + "</span>";
    }

    private String wrapAdded(String text) {
        if (text.isEmpty()) {
            return "";
        }
        return "<span style='background:#d8ffd8;'>" + escapeHtml(text) + "</span>";
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}

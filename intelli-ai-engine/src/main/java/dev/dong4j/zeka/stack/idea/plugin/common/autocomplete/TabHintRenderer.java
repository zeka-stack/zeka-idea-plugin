package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

final class TabHintRenderer implements EditorCustomElementRenderer, Disposable {
    private final Editor editor;
    private final String actionText;

    TabHintRenderer(@NotNull Editor editor, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.actionText = " to apply";
        Disposer.register(parentDisposable, this);
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        FontMetrics fontMetrics = editor.getContentComponent().getFontMetrics(font);
        int tabWidth = fontMetrics.stringWidth(getTabText());
        int actionWidth = fontMetrics.stringWidth(actionText);
        int horizontalPadding = 8;
        int spacing = 4;
        return tabWidth + horizontalPadding * 2 + spacing + actionWidth + 12;
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return editor.getLineHeight();
    }

    @Override
    public void paint(@NotNull Inlay inlay,
                      @NotNull Graphics g,
                      @NotNull Rectangle targetRegion,
                      @NotNull TextAttributes textAttributes) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = JBUI.Fonts.label().deriveFont(getFontSize());
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int tabWidth = fm.stringWidth(getTabText());
        int actionWidth = fm.stringWidth(actionText);
        int tabHeight = fm.getHeight() - 2;
        int tabHorizontalPadding = 4;
        int spacing = 2;
        int py = 2;
        int px = 8;
        int totalWidth = tabWidth + tabHorizontalPadding * 2 + spacing + actionWidth;
        int totalHeight = tabHeight + py * 2;
        int startX = targetRegion.x + px;
        int startY = targetRegion.y + (targetRegion.height - totalHeight) / 2;
        Color backgroundColor = withAlpha(editor.getColorsScheme().getDefaultBackground().brighter(), 0.7f);
        Color borderColor = withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.25f);
        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(startX - px, startY, totalWidth + px * 2, totalHeight, 8, 8);
        g2d.setColor(borderColor);
        g2d.drawRoundRect(startX - px, startY, totalWidth + px * 2, totalHeight, 8, 8);
        int tabY = startY + py;
        g2d.setColor(withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.1f));
        g2d.fillRoundRect(startX, tabY, tabWidth + tabHorizontalPadding * 2, tabHeight, 4, 4);
        boolean isDarkMode = !JBColor.isBright();
        g2d.setColor(isDarkMode ? withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.8f)
                                : editor.getColorsScheme().getDefaultForeground());
        int tabTextY = tabY + tabHeight / 2 + fm.getAscent() / 2 - fm.getDescent() / 2;
        g2d.drawString(getTabText(), startX + tabHorizontalPadding, tabTextY);
        g2d.setColor(isDarkMode ? withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.8f)
                                : editor.getColorsScheme().getDefaultForeground());
        g2d.drawString(actionText, startX + tabWidth + tabHorizontalPadding * 2 + spacing, tabTextY);
        g2d.dispose();
    }

    @Override
    public void dispose() {
    }

    private String getTabText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank() ? "Tab" : shortcutText;
    }

    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a), new Color());
    }
}

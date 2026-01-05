package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

final class GhostTextInlayRenderer implements EditorCustomElementRenderer {
    private final Editor editor;
    private final String text;
    private final Color color;

    GhostTextInlayRenderer(@NotNull Editor editor, @NotNull String text) {
        this.editor = editor;
        this.text = text;
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color fg = scheme.getColor(EditorColors.TABS_COLOR);
        this.color = fg != null ? fg : JBColor.GRAY;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        FontMetrics metrics = getFontMetrics();
        return metrics.stringWidth(text);
    }

    @Override
    public void paint(@NotNull Inlay inlay,
                      @NotNull Graphics g,
                      @NotNull Rectangle targetRegion,
                      @NotNull TextAttributes textAttributes) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.setFont(getFont());
        FontMetrics metrics = getFontMetrics();
        int x = targetRegion.x;
        int y = targetRegion.y + metrics.getAscent();
        g2d.drawString(text, x, y);
    }

    private Font getFont() {
        return editor.getColorsScheme().getFont(EditorFontType.PLAIN);
    }

    private FontMetrics getFontMetrics() {
        return editor.getContentComponent().getFontMetrics(getFont());
    }
}

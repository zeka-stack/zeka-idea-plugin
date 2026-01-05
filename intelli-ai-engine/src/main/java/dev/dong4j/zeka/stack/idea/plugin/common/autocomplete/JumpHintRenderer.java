package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

final class JumpHintRenderer implements Disposable {
    private static final Dimension PREFERRED_SIZE = new Dimension(160, 30);
    private final Editor editor;
    private final boolean isTargetBelow;
    private final String actionText;

    JumpHintRenderer(@NotNull Editor editor, boolean isTargetBelow, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.isTargetBelow = isTargetBelow;
        this.actionText = isTargetBelow ? " to next move ↓" : " to next move ↑";
        Disposer.register(parentDisposable, this);
    }

    @NotNull
    JComponent createJumpHintComponent() {
        JComponent component = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                JBFont font = JBUI.Fonts.label().deriveFont(getFontSize());
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics();
                int tabWidth = fm.stringWidth(getTabText());
                int actionWidth = fm.stringWidth(actionText);
                int tabHeight = fm.getHeight() - 2;
                int tabHorizontalPadding = 4;
                int spacing = 2;
                int totalWidth = tabWidth + tabHorizontalPadding * 2 + spacing + actionWidth;
                int startX = (getWidth() - totalWidth) / 2;
                int tabY = (getHeight() - tabHeight) / 2;
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
        };
        component.setBackground(contrastWithTheme(editor.getColorsScheme().getDefaultBackground()));
        component.setPreferredSize(PREFERRED_SIZE);
        return component;
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

    private Color contrastWithTheme(Color color) {
        int delta = JBColor.isBright() ? -10 : 10;
        return new JBColor(new Color(
            clamp(color.getRed() + delta),
            clamp(color.getGreen() + delta),
            clamp(color.getBlue() + delta),
            color.getAlpha()), new Color());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

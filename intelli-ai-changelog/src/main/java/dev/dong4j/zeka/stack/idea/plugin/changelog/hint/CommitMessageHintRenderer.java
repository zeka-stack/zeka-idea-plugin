package dev.dong4j.zeka.stack.idea.plugin.changelog.hint;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
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

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;

/**
 * Commit Message Inlay Hint 渲染器
 * <p>
 * 用于在 Commit Message 编辑器中渲染 Tab 提示，显示"⇥ 生成提交信息"的提示内容。
 * 参考 {@code dev.dong4j.zeka.stack.idea.plugin.common.autocomplete.TabHintRenderer} 的实现。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
final class CommitMessageHintRenderer implements EditorCustomElementRenderer, Disposable {
    /**
     * 编辑器实例
     */
    private final Editor editor;

    /**
     * 构造函数
     *
     * @param editor           编辑器实例，不能为 null
     * @param parentDisposable 父级可释放对象，不能为 null
     */
    CommitMessageHintRenderer(@NotNull Editor editor, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        Disposer.register(parentDisposable, this);
    }

    /**
     * 计算提示元素的宽度
     *
     * @param inlay Inlay 对象，不能为 null
     * @return 宽度（像素）
     */
    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        Font font = JBUI.Fonts.label().deriveFont(getFontSize());
        FontMetrics fm = editor.getContentComponent().getFontMetrics(font);
        String tabText = getTabShortcutText();
        String actionText = ChangelogBundle.message("commit.hint.action.text");
        int tabWidth = fm.stringWidth(tabText);
        int actionWidth = fm.stringWidth(actionText);
        int horizontalPadding = JBUI.scale(8);
        int spacing = JBUI.scale(4);
        return tabWidth + horizontalPadding * 2 + spacing + actionWidth + JBUI.scale(12);
    }

    /**
     * 计算提示元素的高度
     *
     * @param inlay Inlay 对象，不能为 null
     * @return 高度（像素），等于编辑器行高
     */
    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return editor.getLineHeight();
    }

    /**
     * 绘制提示元素
     *
     * @param inlay          Inlay 对象，不能为 null
     * @param g              Graphics 对象，不能为 null
     * @param targetRegion   绘制区域，不能为 null
     * @param textAttributes 文本属性，不能为 null
     */
    @Override
    public void paint(@NotNull Inlay inlay,
                      @NotNull Graphics g,
                      @NotNull Rectangle targetRegion,
                      @NotNull com.intellij.openapi.editor.markup.TextAttributes textAttributes) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font font = JBUI.Fonts.label().deriveFont(getFontSize());
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            String tabText = getTabShortcutText();
            String actionText = ChangelogBundle.message("commit.hint.action.text");
            int tabWidth = fm.stringWidth(tabText);
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

            // 背景色：半透明
            Color backgroundColor = withAlpha(
                editor.getColorsScheme().getDefaultBackground().brighter(),
                0.7f
                                             );
            // 边框色：浅色
            Color borderColor = withAlpha(
                editor.getColorsScheme().getDefaultForeground(),
                0.25f
                                         );

            // 绘制背景
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(startX - px, startY, totalWidth + px * 2, totalHeight, 8, 8);

            // 绘制边框
            g2d.setColor(borderColor);
            g2d.drawRoundRect(startX - px, startY, totalWidth + px * 2, totalHeight, 8, 8);

            // Tab 键背景
            int tabY = startY + py;
            g2d.setColor(withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.1f));
            g2d.fillRoundRect(startX, tabY, tabWidth + tabHorizontalPadding * 2, tabHeight, 4, 4);

            // 文本颜色：根据主题调整
            boolean isDarkMode = !JBColor.isBright();
            Color textColor = isDarkMode
                              ? withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.8f)
                              : editor.getColorsScheme().getDefaultForeground();

            // 绘制文本
            int tabTextY = tabY + tabHeight / 2 + fm.getAscent() / 2 - fm.getDescent() / 2;
            g2d.setColor(textColor);
            g2d.drawString(tabText, startX + tabHorizontalPadding, tabTextY);
            g2d.drawString(actionText, startX + tabWidth + tabHorizontalPadding * 2 + spacing, tabTextY);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * 释放资源
     */
    @Override
    public void dispose() {
        // 无需特殊清理
    }

    /**
     * 获取 Tab 键的快捷键文本
     *
     * @return Tab 键的快捷键文本，如果未配置则返回 "Tab"
     */
    private String getTabShortcutText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank()
               ? ChangelogBundle.message("commit.hint.tab.text")
               : shortcutText;
    }

    /**
     * 获取字体大小
     *
     * @return 字体大小（比默认标签字体小 2 像素）
     */
    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    /**
     * 为颜色添加透明度
     *
     * @param color 原始颜色，不能为 null
     * @param alpha 透明度（0.0f - 1.0f）
     * @return 带透明度的颜色
     */
    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(
            new Color(color.getRed(), color.getGreen(), color.getBlue(), a),
            new Color(color.getRed(), color.getGreen(), color.getBlue(), a)
        );
    }
}

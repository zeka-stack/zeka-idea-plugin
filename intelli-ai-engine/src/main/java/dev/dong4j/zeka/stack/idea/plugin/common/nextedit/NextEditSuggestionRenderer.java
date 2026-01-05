package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.keymap.KeymapUtil;
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

/**
 * 下一步编辑建议渲染器
 * <p> 用于在编辑器中渲染“下一步编辑建议”提示, 通常用于代码编辑时的智能提示或快捷替换建议.
 * <p> 该渲染器负责计算提示的宽度和高度, 并在编辑器中绘制包含快捷键提示, 操作说明和替换内容的视觉元素.
 * <p> 支持自定义字体, 颜色, 圆角边框和透明度效果, 适配暗色 / 亮色主题.
 * <p> 使用示例:
 * <pre>{@code
 * NextEditSuggestionRenderer renderer = new NextEditSuggestionRenderer(editor, "new content", " to replace", true);
 * // 该渲染器将被注册到编辑器的内联元素中, 自动绘制提示
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditSuggestionRenderer implements EditorCustomElementRenderer {
    /** 编辑器实例, 用于渲染建议项的上下文环境 */
    private final Editor editor;
    /** 用于替换的文本内容 */
    private final String replacement;
    /** 操作提示文本 */
    private final String actionText;
    /** 是否展示替换内容 */
    private final boolean showReplacement;

    /**
     * 构造函数, 用于创建编辑建议渲染器实例
     * <p> 初始化编辑器和替换文本, 用于后续的建议渲染操作
     *
     * @param editor      编辑器实例, 不能为 null
     * @param replacement 替换文本内容, 不能为 null
     */
    NextEditSuggestionRenderer(@NotNull Editor editor,
                               @NotNull String replacement,
                               @NotNull String actionText,
                               boolean showReplacement) {
        this.editor = editor;
        this.replacement = replacement;
        this.actionText = actionText;
        this.showReplacement = showReplacement;
    }

    /**
     * 计算内嵌元素的宽度 (以像素为单位)
     * <p> 根据字体, 文本内容以及一些固定间距计算内嵌元素在编辑器中的宽度
     *
     * @param inlay 内嵌元素对象, 不能为 null
     * @return 内嵌元素的宽度 (以像素为单位)
     */
    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        FontMetrics fontMetrics = editor.getContentComponent().getFontMetrics(font);
        int tabWidth = fontMetrics.stringWidth(getTabText());
        int actionWidth = fontMetrics.stringWidth(actionText);
        int hintWidth = showReplacement ? fontMetrics.stringWidth(renderText()) : 0;
        int horizontalPadding = 8;
        return tabWidth + actionWidth + hintWidth + horizontalPadding * 2 + 12;
    }

    /**
     * 计算内联元素在像素中的高度
     * <p> 根据编辑器当前行高返回内联元素所需的高度, 通常用于自定义编辑器元素的渲染尺寸计算
     *
     * @param inlay 内联元素对象, 不能为 null
     * @return 编辑器中单行的高度 (像素单位)
     */
    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return editor.getLineHeight();
    }

    /**
     * 绘制编辑器中的补全建议提示框
     * <p> 该方法用于在编辑器中绘制替换建议的图形界面元素, 包括背景色块, 标签文本和提示信息.
     *
     * @param inlay          补全建议的内嵌元素对象, 不能为 null
     * @param g              用于绘制的 Graphics 对象, 不能为 null
     * @param targetRegion   目标绘制区域, 表示该提示框在编辑器中的位置和大小, 不能为 null
     * @param textAttributes 文本样式属性, 用于设置颜色等文本外观, 不能为 null
     */
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
        String tabText = getTabText();
        String hintText = renderText();
        int tabWidth = fm.stringWidth(tabText);
        int actionWidth = fm.stringWidth(actionText);
        int hintWidth = showReplacement ? fm.stringWidth(hintText) : 0;
        int tabHeight = fm.getHeight() - 2;
        int tabHorizontalPadding = 4;
        int py = 2;
        int px = 8;
        int totalWidth = tabWidth + actionWidth + hintWidth + tabHorizontalPadding * 2 + 8;
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
        int textY = tabY + tabHeight / 2 + fm.getAscent() / 2 - fm.getDescent() / 2;
        g2d.drawString(tabText, startX + tabHorizontalPadding, textY);
        g2d.drawString(actionText, startX + tabWidth + tabHorizontalPadding * 2 + 4, textY);
        if (showReplacement) {
            g2d.setColor(withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.7f));
            g2d.drawString(hintText, startX + tabWidth + actionWidth + tabHorizontalPadding * 2 + 6, textY);
        }
        g2d.dispose();
    }

    /**
     * 渲染替换文本的显示格式
     * <p> 将原始替换文本进行格式化, 如果文本长度超过 24 个字符, 则截取前 21 个字符并追加“...”, 并在前后添加双引号
     * <p> 示例:
     * <pre>{@code
     * renderText("Hello World") // 返回 ""Hello World""* renderText("This is a very long text that exceeds 24 characters") // 返回" "This is a very long text..."
     * }</pre>
     *
     * @return 格式化后的文本, 长度不超过 24 字符时完整显示, 否则截断并添加省略号
     */
    private String renderText() {
        if (!showReplacement) {
            return "";
        }
        String text = replacement.replace("\n", "\\n");
        if (text.length() > 24) {
            return " \"" + text.substring(0, 21) + "...\"";
        }
        return " \"" + text + "\"";
    }

    /**
     * 获取编辑器标签的快捷键文本
     * <p> 从 ActionManager 获取编辑器标签动作 (ACTION_EDITOR_TAB), 并返回其第一个快捷键文本.
     * 如果没有快捷键或文本为空, 则返回默认值 "Tab".
     *
     * @return 快捷键文本, 若无快捷键或文本为空则返回 "Tab"
     */
    private String getTabText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank() ? "Tab" : shortcutText;
    }

    /**
     * 获取字体大小
     * <p> 根据默认标签字体的大小减去 2 来计算最终的字体大小
     *
     * @return 计算后的字体大小
     */
    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    /**
     * 根据指定的透明度调整颜色
     * <p> 该方法将原始颜色与给定的 alpha 值进行组合, 返回一个新的带有透明度的颜色对象.
     *
     * @param color 原始颜色
     * @param alpha 透明度值, 范围为 [0.0, 1.0], 其中 0 表示完全透明,1 表示完全不透明
     * @return 带有指定透明度的新颜色对象
     */
    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a), new Color(color.getRed(), color.getGreen(),
                                                                                                      color.getBlue(), a));
    }
}

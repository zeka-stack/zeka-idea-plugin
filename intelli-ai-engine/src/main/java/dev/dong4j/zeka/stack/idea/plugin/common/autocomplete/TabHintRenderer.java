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

/**
 * Tab 提示渲染器类
 * <p> 该类实现了 {@link EditorCustomElementRenderer} 接口, 用于在编辑器中绘制自定义的 Tab 提示元素.
 * <p> 通过计算宽度和高度, 并绘制带有阴影效果的矩形区域来显示 Tab 提示文本及其操作文本.
 * <p> 此类确保在编辑器中正确渲染 Tab 提示, 并在父组件被销毁时进行资源清理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class TabHintRenderer implements EditorCustomElementRenderer, Disposable {
    /** 编辑器实例, 用于获取样式, 字体和绘制相关信息 */
    private final Editor editor;
    /**
     * 操作提示文本
     * <p> 表示操作的动作文本, 默认值为 "to apply"
     */
    private final String actionText;

    /**
     * 构造函数, 初始化 TabHintRenderer 对象
     * <p> 构造函数接收一个编辑器实例和一个父级可释放对象作为参数
     * <p> 构造函数会设置 actionText 为 "to apply" 并注册当前对象到父级可释放对象中
     *
     * @param editor           编辑器实例, 不能为 null
     * @param parentDisposable 父级可释放对象, 不能为 null
     */
    TabHintRenderer(@NotNull Editor editor, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.actionText = " to apply";
        Disposer.register(parentDisposable, this);
    }

    /**
     * 计算标签提示的宽度 (以像素为单位)
     * <p> 根据字体和文本内容计算标签提示的总宽度, 包括内边距和间距
     *
     * @param inlay 标签提示对象, 不能为 null
     * @return 标签提示的总宽度 (以像素为单位)
     */
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

    /**
     * 计算内联元素在像素中的高度
     * <p> 根据编辑器当前行高返回内联元素的高度, 用于布局计算
     *
     * @param inlay 内联元素对象, 不能为 null
     * @return 内联元素的高度 (像素单位), 等于编辑器当前行高
     */
    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return editor.getLineHeight();
    }

    /**
     * 绘制标签提示元素
     * <p> 该方法用于在编辑器中绘制一个带有标签文本和操作文本的提示框, 通常用于显示快捷键提示.
     *
     * @param inlay          Inlay 对象, 表示当前绘制的内嵌元素
     * @param g              Graphics 对象, 用于绘制图形
     * @param targetRegion   目标绘制区域, 定义了绘制的位置和大小
     * @param textAttributes 文本属性, 用于控制文本的颜色, 字体等样式
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

    /**
     * 释放资源, 清理当前实例占用的系统资源
     * <p> 该方法在对象生命周期结束时被调用, 用于释放与当前实例相关的资源, 如注册的 Disposable 对象
     *
     */
    @Override
    public void dispose() {
    }

    /**
     * 获取 Tab 键的快捷方式文本
     * <p> 尝试从 ActionManager 中获取编辑器 Tab 操作的快捷键, 若无法获取或为空, 则返回默认值 "Tab"
     *
     * @return 表示 Tab 操作的快捷方式文本, 如果未配置则返回 "Tab"
     */
    private String getTabText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank() ? "Tab" : shortcutText;
    }

    /**
     * 获取字体大小
     * <p> 根据系统默认的标签字体大小减去 2 像素, 返回调整后的字体大小
     *
     * @return 调整后的字体大小
     */
    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    /**
     * 为颜色添加透明度 (Alpha) 通道
     * <p>根据指定的透明度值 alpha, 生成一个新的 JBColor 对象, 其颜色通道保持原样, 仅透明度被修改
     * <p>透明度值 alpha 范围为 0.0f(完全透明)到 1.0f(完全不透明), 超出范围的值会被截断
     * <p>示例:
     * <pre>{@code
     * Color originalColor = new Color(255, 0, 0); // 红色
     * Color translucentRed = withAlpha(originalColor, 0.5f); // 半透明红色
     * }</pre>
     *
     * @param color 原始颜色对象, 不能为 null
     * @param alpha 透明度值, 范围为 0.0f 到 1.0f, 超出范围会被截断
     * @return 新的 JBColor 对象, 其透明度已按 alpha 值调整
     */
    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a), new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
    }
}

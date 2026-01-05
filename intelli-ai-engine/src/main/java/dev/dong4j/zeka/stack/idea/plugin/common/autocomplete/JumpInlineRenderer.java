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
 * 跳转内联渲染器类
 * <p> 用于在编辑器中渲染跳转到此处的内联元素, 提供视觉效果和交互功能. 该类负责计算宽度, 高度, 并绘制跳转按钮及其相关文本.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class JumpInlineRenderer implements EditorCustomElementRenderer, Disposable {
    /**
     * 编辑器实例
     * <p> 用于获取编辑器相关的配置和状态信息
     *
     * @see Editor
     */
    private final Editor editor;
    /** 用于显示跳转操作的提示文本 */
    private final String actionText;

    /**
     * 构造函数, 初始化 JumpInlineRenderer 实例
     *
     * @param editor           编辑器实例, 用于获取字体, 颜色等渲染相关的信息
     * @param parentDisposable 父级 Disposable 对象, 用于注册当前对象的生命周期管理
     */
    JumpInlineRenderer(@NotNull Editor editor, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.actionText = " to jump here";
        Disposer.register(parentDisposable, this);
    }


    /**
     * 计算内联元素的宽度 (以像素为单位)
     * <p> 根据标签文本和操作文本的宽度, 加上内边距, 间距等计算总宽度
     *
     * @param inlay 内联元素对象, 当前未被使用
     * @return 返回内联元素所需的总宽度 (像素)
     */
    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        FontMetrics fontMetrics = editor.getContentComponent().getFontMetrics(font);
        int tabWidth = fontMetrics.stringWidth(getTabText());
        int actionWidth = fontMetrics.stringWidth(actionText);
        int horizontalPadding = 8;
        int spacing = 4;
        return tabWidth + horizontalPadding * 2 + spacing + actionWidth + 16;
    }

    /**
     * 计算内嵌元素的高度 (以像素为单位)
     * <p> 返回编辑器中单行文字的高度, 用于确定内嵌元素在界面中的显示高度
     *
     * @param inlay 内嵌元素对象, 不能为 null
     * @return 编辑器中单行文字的高度
     */
    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return editor.getLineHeight();
    }

    /**
     * 绘制内联提示的图形界面
     * <p> 根据指定区域和样式绘制一个带有标签和操作文本的圆角矩形框, 用于表示跳转位置的提示信息.
     *
     * @param inlay          内联元素对象, 提供上下文信息
     * @param g              图形上下文对象, 用于绘图
     * @param targetRegion   目标绘制区域
     * @param textAttributes 文本属性, 影响字体, 颜色等样式
     */
    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
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
        int py = 4;
        int px = 12;
        int leftMargin = px * 2;
        int totalWidth = tabWidth + tabHorizontalPadding * 2 + spacing + actionWidth;
        int totalHeight = tabHeight + py * 2;
        int startX = targetRegion.x + leftMargin;
        int startY = targetRegion.y + (targetRegion.height - totalHeight) / 2;
        Color backgroundColor = withAlpha(editor.getColorsScheme().getDefaultBackground().brighter(), 0.8f);
        Color borderColor = withAlpha(editor.getColorsScheme().getDefaultForeground(), 0.3f);
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
        int cursorX = startX - px * 2;
        g2d.setColor(new JBColor(new Color(0x7AB2), new Color(0x7AB2)));
        g2d.fillRoundRect(cursorX, startY, 2, totalHeight, 2, 2);
        g2d.dispose();
    }


    /**
     * 释放资源或执行清理操作
     * <p> 此方法用于释放由该对象占用的资源或执行必要的清理工作, 确保不会发生资源泄漏.
     *
     * @since 1.0
     */
    @Override
    public void dispose() {
    }

    /**
     * 获取用于显示的标签文本, 通常是编辑器中的 Tab 快捷键文本
     * <p> 该方法尝试从 ActionManager 中获取编辑器 Tab 动作的快捷键文本, 如果获取不到或为空, 则返回默认值 "Tab"
     *
     * @return 编辑器 Tab 的快捷键文本, 如果未找到则返回 "Tab"
     */
    private String getTabText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank() ? "Tab" : shortcutText;
    }

    /**
     * 获取用于绘制的字体大小
     * <p> 该方法通过 {@link JBUI.Fonts#label()} 获取默认标签字体, 并返回其字号减去 2 的值, 用于界面渲染时调整字体大小.
     *
     * @return 返回字体大小 (单位: 点), 值为默认标签字体大小减去 2
     */
    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    /**
     * 根据给定的颜色和透明度值, 返回一个新的颜色对象, 其透明度已调整
     * <p> 该方法用于创建一个带有指定透明度的新颜色, 适用于界面渲染等场景
     *
     * @param color 原始颜色对象, 不能为 null
     * @param alpha 透明度值, 范围应在 0.0 到 1.0 之间
     * @return 新的颜色对象, 包含调整后的透明度
     */
    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a), new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
    }


}

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

/**
 * 跳转提示渲染器类
 * <p> 用于在编辑器中渲染跳转操作的提示组件, 显示快捷键和方向指示 (上 / 下), 支持自定义主题颜色和字体样式
 * <p> 该组件通过自定义绘制实现视觉效果, 包括圆角背景, 文字高亮和主题对比色适配, 适用于 IDE 插件中跳转操作的视觉反馈
 * <p> 使用示例:
 * <pre>{@code
 * JumpHintRenderer renderer = new JumpHintRenderer(editor, isTargetBelow, parentDisposable);
 * JComponent component = renderer.createJumpHintComponent();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class JumpHintRenderer implements Disposable {
    /** 预期的组件首选大小 */
    private static final Dimension PREFERRED_SIZE = new Dimension(160, 30);
    /** 编辑器实例, 用于操作和获取编辑器相关的信息 */
    private final Editor editor;
    /** 是否目标在下方 */
    private final boolean isTargetBelow;
    /** 跳转提示文本, 显示“to next move ↑”或“to next move ↓”根据目标位置上下而定 */
    private final String actionText;

    /**
     * 初始化跳转提示渲染器
     * <p> 用于创建并注册一个可释放的跳转提示组件, 根据目标位置决定提示文本方向
     *
     * @param editor           编辑器实例, 不能为 null
     * @param isTargetBelow    是否目标在下方, 决定提示文本方向 (↓ 或 ↑)
     * @param parentDisposable 父级可释放资源, 用于自动管理生命周期
     */
    JumpHintRenderer(@NotNull Editor editor, boolean isTargetBelow, @NotNull Disposable parentDisposable) {
        this.editor = editor;
        this.isTargetBelow = isTargetBelow;
        this.actionText = isTargetBelow ? " to next move ↓" : " to next move ↑";
        Disposer.register(parentDisposable, this);
    }

    /**
     * 创建跳转提示组件
     * <p>该方法构建一个自定义的 JComponent, 用于在编辑器中显示跳转提示, 包含快捷键提示和方向箭头(↑/↓), 并根据主题颜色自动调整显示效果.
     * <p>组件绘制逻辑包括:
     * <ul>
     *   <li>设置抗锯齿渲染</li>
     *   <li>根据当前主题 (亮色 / 暗色) 调整文字颜色</li>
     *   <li>绘制圆角矩形背景</li>
     *   <li>绘制快捷键文本和方向动作文本</li>
     * </ul>
     * <p>组件尺寸固定为 {@code PREFERRED_SIZE}, 背景色根据主题对比调整.
     *
     * @return 包含跳转提示信息的 JComponent, 永不为 null
     */
    @NotNull
    JComponent createJumpHintComponent() {
        JComponent component = new JComponent() {
            /**
             * 绘制组件的自定义外观, 包含标签文本和操作文本的圆形背景及文字渲染
             * <p> 该方法重写父类的绘制逻辑, 通过 Graphics2D 绘制带有圆角背景的标签区域, 并在背景上绘制标签文本和操作文本, 支持暗色模式适配
             * <p> 绘制流程包括:
             * <ul>
             *   <li> 创建抗锯齿的 Graphics2D 上下文 </li>
             *   <li> 根据当前字体计算标签和操作文本的宽度与高度 </li>
             *   <li> 计算总宽度并居中绘制背景矩形 </li>
             *   <li> 根据是否为暗色模式设置文本颜色 </li>
             *   <li> 在背景矩形内绘制标签文本和操作文本, 位置居中对齐 </li>
             * </ul>
             * <p> 示例绘制效果:
             * <pre>{@code
             * // 绘制一个包含标签和操作按钮的横向控件, 背景为半透明圆角矩形, 文字颜色根据主题自动切换
             * }</pre>
             *
             * @param g 绘制上下文, 不能为 null
             */
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

    /**
     * 释放资源或执行清理操作
     * <p> 该方法用于释放由当前对象占用的资源, 或执行必要的清理工作.
     *
     * @since 1.0
     */
    @Override
    public void dispose() {
    }

    /**
     * 获取用于跳转的快捷键文本
     * <p> 尝试从操作管理器中获取 "Tab" 操作的快捷键文本, 如果无法获取或为空, 则返回默认值 "Tab"
     *
     * @return 快捷键文本, 例如 "Tab" 或具体的按键组合如 "Ctrl+I"
     */
    private String getTabText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank() ? "Tab" : shortcutText;
    }

    /**
     * 获取字体大小 (减去 2)
     * <p> 从默认标签字体中获取当前字体大小, 并减去 2.0f 作为调整值
     *
     * @return 调整后的字体大小, 为原字体大小减 2.0f
     */
    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    /**
     * 为给定的颜色添加透明度
     * <p> 根据指定的透明度参数 alpha, 计算并返回一个新的颜色对象, 该对象具有调整后的透明度.
     * <p> 透明度参数 alpha 的取值范围为 0 到 1, 其中 0 表示完全透明,1 表示完全不透明.
     * <p> 该方法确保生成的颜色透明度值在 0 到 255 之间.
     *
     * @param color 原始颜色
     * @param alpha 透明度参数, 取值范围为 0 到 1
     * @return 具有调整后透明度的新颜色对象
     */
    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a), new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
    }

    /**
     * 根据主题亮度调整颜色对比度
     * <p> 根据当前主题是否为亮色模式, 对颜色的 RGB 分量进行偏移调整, 以增强颜色在不同主题背景下的对比度
     * <p> 若主题为亮色模式, 则减小 RGB 值 (偏移 -10); 若为暗色模式, 则增大 RGB 值 (偏移 +10)
     * <p> 透明度通道保持不变
     *
     * @param color 输入的颜色对象, 不能为 null
     * @return 调整后对比度增强的颜色对象, 包含亮色和暗色主题下的两种颜色实现
     */
    private Color contrastWithTheme(Color color) {
        int delta = JBColor.isBright() ? -10 : 10;
        return new JBColor(new Color(
            clamp(color.getRed() + delta),
            clamp(color.getGreen() + delta),
            clamp(color.getBlue() + delta),
            color.getAlpha()), new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
    }

    /**
     * 将整数值限制在 0 到 255 的范围内
     * <p> 如果值小于 0, 则返回 0; 如果值大于 255, 则返回 255;
     * 否则返回原始值.
     *
     * @param value 需要被限制的整数值
     * @return 介于 0 和 255 之间的整数值
     */
    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

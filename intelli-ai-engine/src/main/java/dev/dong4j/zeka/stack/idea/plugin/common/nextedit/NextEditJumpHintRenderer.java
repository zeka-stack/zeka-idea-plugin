package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.JBColor;
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
 * NextEditJumpHintRenderer 类
 * <p> 用于渲染编辑器中的跳转提示组件, 显示跳转方向的提示信息 (↑ 或 ↓)
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditJumpHintRenderer {
    /** 默认首选大小, 用于绘制跳转提示组件的尺寸 */
    private static final Dimension PREFERRED_SIZE = new Dimension(160, 30);
    /** 编辑器实例, 用于获取颜色方案和绘制相关设置 */
    private final Editor editor;
    /** 操作文本, 用于显示跳转方向提示 */
    private final String actionText;

    /**
     * 构造一个用于显示编辑器跳转提示的组件渲染器
     * <p> 根据传入的编辑器实例和目标位置方向 (上方或下方), 初始化跳转提示文本并设置相关属性
     *
     * @param editor        编辑器实例, 不能为空
     * @param isTargetBelow 目标位置是否在当前光标下方,true 表示下方,false 表示上方
     */
    NextEditJumpHintRenderer(@NotNull Editor editor, boolean isTargetBelow) {
        this.editor = editor;
        this.actionText = isTargetBelow ? " to jump ↓" : " to jump ↑";
    }

    /**
     * 创建跳转提示组件
     * <p> 该组件用于在编辑器中显示跳转提示, 包含标签快捷键和跳转方向文本 (如“ to jump ↓”或“ to jump ↑”), 并绘制圆角背景和文字.
     * <p> 组件绘制逻辑包括:
     * <ul>
     *   <li> 设置抗锯齿渲染 </li>
     *   <li> 根据当前主题颜色调整文字和背景透明度 </li>
     *   <li> 计算并绘制标签文本和方向文本的位置 </li>
     *   <li> 使用主题对比色调整背景 </li>
     * </ul>
     * <p> 使用示例:
     * <pre>{@code
     * JComponent hintComponent = createJumpHintComponent();
     * // 将组件添加到面板或工具栏中
     * }</pre>
     *
     * @return 绘制完成的跳转提示组件, 始终返回非空对象
     */
    @NotNull
    JComponent createJumpHintComponent() {
        JComponent component = new JComponent() {
            /**
             * 重写父类的 paintComponent 方法以自定义绘制组件的内容
             * <p> 此方法在组件需要重新绘制时被调用, 例如当组件的可见性, 尺寸或状态发生变化时
             * <p> 该方法绘制了一个带有标签文本和操作文本的圆角矩形区域, 适用于标签组件的绘制
             * <p> 具体步骤如下:
             * <ol>
             * <li> 设置抗锯齿渲染提示以提高绘制质量 </li>
             * <li> 根据字体大小创建字体对象并设置给 Graphics2D 对象 </li>
             * <li> 计算标签文本和操作文本的宽度及高度 </li>
             * <li> 计算圆角矩形的位置和大小 </li>
             * <li> 绘制背景圆角矩形 </li>
             * <li> 根据暗模式设置标签文本和操作文本的颜色并绘制 </li>
             * </ol>
             *
             * @param g Graphics 对象, 用于绘制组件内容
             */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
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
     * 获取编辑器标签的快捷键文本
     * <p> 从 ActionManager 获取编辑器标签动作 (IdeActions.ACTION_EDITOR_TAB), 并获取其第一个键盘快捷键文本.
     * 如果未找到快捷键或文本为空, 则返回 "Tab".
     * <p> 示例:
     * <pre>{@code
     * String shortcut = getTabText(); // 可能返回 "Ctrl+Tab" 或 "Tab"
     * }</pre>
     *
     * @return 快捷键文本, 若无快捷键或文本为空则返回 "Tab"
     */
    private String getTabText() {
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_TAB);
        String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
        return shortcutText == null || shortcutText.isBlank() ? "Tab" : shortcutText;
    }

    /**
     * 获取用于绘制的字体大小
     * <p> 该方法从系统标签默认字体中获取字体大小, 并减去 2 以适配 UI 显示效果
     *
     * @return 调整后的字体大小, 单位为点 (points)
     */
    private float getFontSize() {
        Font font = JBUI.Fonts.label();
        return font.getSize() - 2f;
    }

    /**
     * 为颜色添加透明度
     * <p> 根据给定的透明度值, 创建一个新的颜色对象, 其中包含指定的透明度.
     *
     * @param color 原始颜色对象
     * @param alpha 透明度值, 范围在 0.0 到 1.0 之间
     * @return 新的颜色对象, 包含指定的透明度
     */
    private Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255 * alpha)));
        return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a), new Color(color.getRed(), color.getGreen(),
                                                                                                      color.getBlue(), a));
    }

    /**
     * 根据当前主题调整颜色对比度
     * <p> 如果当前是浅色主题, 则降低颜色亮度; 如果是深色主题, 则增加颜色亮度, 以确保与背景的对比度.
     *
     * @param color 需要调整的颜色对象
     * @return 调整后的颜色对象 (用于不同主题下的显示优化)
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
     * <p> 如果输入值小于 0, 则返回 0; 如果输入值大于 255, 则返回 255; 否则返回原值
     *
     * @param value 待限制的整数值
     * @return 限制在 [0, 255] 范围内的整数值
     */
    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

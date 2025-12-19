package dev.dong4j.zeka.stack.idea.plugin.common.ui.component;

import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.Timer;

/**
 * Breathing Dot Icon
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-19 13:51:00
 * @since hello.world
 */
public class BreathingDotIcon implements Icon {
    private static final int SIZE = JBUI.scale(8);
    private static final int TIMER_DELAY = 50;
    private float phase;
    private Color color;

    /**
     * 构造函数
     *
     * @param owner        拥有该图标的组件，用于触发重绘
     * @param initialColor 初始颜色
     */
    public BreathingDotIcon(@NotNull Component owner, @NotNull Color initialColor) {
        this.color = initialColor;
        // todo-dong4j: 共享定时器
        Timer timer = new Timer(TIMER_DELAY, e -> {
            phase += 0.08f;
            if (phase > Math.PI * 2) {
                phase -= (float) (Math.PI * 2);
            }
            owner.repaint();
        });
        timer.start();
    }

    /**
     * 设置图标颜色
     *
     * @param color 新的颜色
     */
    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        float alpha = 0.5f + 0.5f * (float) Math.sin(phase);
        int a = (int) (alpha * 255);
        @SuppressWarnings("UseJBColor")
        Color drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(60, Math.min(255, a)));

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(drawColor);
        g2.fillOval(x, y, SIZE, SIZE);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}


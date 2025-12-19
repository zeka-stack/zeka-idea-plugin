package dev.dong4j.zeka.stack.idea.plugin.common.ui.component;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * 带状态指示灯的按钮组件
 * <p>
 * 该按钮组件内置了带呼吸效果的状态指示灯，支持三种状态：
 * - 成功（绿色）
 * - 失败（红色）
 * - 警告/进行中（黄色）
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public class StatusIndicatorButton extends JButton {
    /** 成功状态颜色（绿色） */
    public static final JBColor STATUS_SUCCESS = new JBColor(new Color(76, 175, 80), new Color(76, 175, 80));
    /** 失败状态颜色（红色） */
    public static final JBColor STATUS_ERROR = new JBColor(new Color(244, 67, 54), new Color(244, 67, 54));
    /** 警告/进行中状态颜色（黄色） */
    public static final JBColor STATUS_WARNING = new JBColor(new Color(255, 193, 7), new Color(255, 193, 7));

    /** 带呼吸效果的图标 */
    private final BreathingDotIcon statusIcon;

    /**
     * 构造函数
     *
     * @param text 按钮文本
     */
    public StatusIndicatorButton(@NotNull String text) {
        super(text);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(JBUI.scale(6));

        // 创建默认的呼吸效果图标（红色）
        statusIcon = new BreathingDotIcon(this, STATUS_ERROR);
        setIcon(statusIcon);
        setDisabledIcon(statusIcon);
    }

    /**
     * 设置状态颜色
     *
     * @param color 状态颜色
     */
    public void setStatusColor(@NotNull Color color) {
        statusIcon.setColor(color);
    }

    /**
     * 设置为成功状态（绿色）
     */
    public void setSuccessStatus() {
        setStatusColor(STATUS_SUCCESS);
    }

    /**
     * 设置为失败状态（红色）
     */
    public void setErrorStatus() {
        setStatusColor(STATUS_ERROR);
    }

    /**
     * 设置为警告/进行中状态（黄色）
     */
    public void setWarningStatus() {
        setStatusColor(STATUS_WARNING);
    }

}


package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 空状态面板类, 用于在“新功能”对话框中显示空状态信息
 * <p> 该面板用于当没有新功能时显示提示信息, 包含居中的标签和适当的边距设置
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.24
 * @since 1.0.0
 */
public class WhatsNewEmptyPanel extends JPanel {
    /**
     * 构造函数, 初始化空的新功能面板
     * <p> 创建一个空的新功能面板, 用于显示提示信息, 居中对齐, 设置边距, 并添加垂直弹性空间
     *
     * @since hello.world
     */
    public WhatsNewEmptyPanel() {
        var label = new JBLabel(AICommonBundle.message("whatsnew.dialog.empty"));
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setForeground(UIUtil.getInactiveTextColor());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        add(label);
        add(Box.createVerticalGlue());
        setBorder(JBUI.Borders.empty(40, 30));
    }
}

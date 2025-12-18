package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * Codefree 代理配置面板
 * <p>
 * 独立的 Codefree 代理配置面板，提供高内聚低耦合的设计。
 * 包含下载地址、自动启动选项、状态显示和操作按钮等功能。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CodefreePanel {
    /** 主面板 */
    @NotNull
    private final JPanel content;
    /** 自动启动复选框 */
    @NotNull
    private final JBCheckBox autoStartCheckBox;
    /** 下载地址输入框 */
    @NotNull
    private final JBTextField downloadUrlField;
    /** 状态标签 */
    @NotNull
    private final JBLabel statusLabel;
    /** 下载按钮 */
    @NotNull
    private final JButton downloadButton;
    /** 启动/停止按钮 */
    @NotNull
    private final JButton startButton;
    /** 内容面板（可折叠） */
    @NotNull
    private final JPanel mainPanel;
    /** 状态更新定时器 */
    @Nullable
    private Timer statusUpdateTimer;
    /** 状态更新回调 */
    @Nullable
    private Runnable statusUpdateCallback;

    /**
     * 构造函数
     */
    public CodefreePanel() {
        // 初始化组件
        autoStartCheckBox = new JBCheckBox(AICommonBundle.message("settings.codefree.auto.start"));
        downloadUrlField = new JBTextField();
        downloadUrlField.setToolTipText(AICommonBundle.message("settings.codefree.download.url.hint"));
        // 限制输入框宽度，防止超长 URL 拉长界面
        Dimension urlFieldSize = new Dimension(500, downloadUrlField.getPreferredSize().height);
        downloadUrlField.setPreferredSize(urlFieldSize);
        downloadUrlField.setMaximumSize(new Dimension(600, downloadUrlField.getPreferredSize().height));

        statusLabel = new JBLabel(AICommonBundle.message("settings.codefree.status.not.ready"));
        downloadButton = new JButton(AICommonBundle.message("settings.codefree.download"));
        startButton = new JButton(AICommonBundle.message("settings.codefree.start"));

        // 创建按钮面板
        JPanel buttonsPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        buttonsPanel.add(downloadButton);
        buttonsPanel.add(startButton);

        // 创建主内容面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(autoStartCheckBox, "settings.codefree.auto.start.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.codefree.download.url")), downloadUrlField)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.codefree.status")), statusLabel)
            .addComponent(buttonsPanel)
            .getPanel();

        // 创建可折叠容器
        content = createCollapsiblePanel();

        // 启动状态更新定时器（每 2 秒检查一次服务状态）
        startStatusUpdateTimer();
    }

    /**
     * 启动状态更新定时器
     * <p>
     * 定期检查 Codefree 代理服务状态，并更新 UI。
     */
    private void startStatusUpdateTimer() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
        }
        statusUpdateTimer = new Timer(2000, e -> {
            if (statusUpdateCallback != null) {
                statusUpdateCallback.run();
            }
        });
        statusUpdateTimer.setRepeats(true);
        statusUpdateTimer.start();
    }

    /**
     * 停止状态更新定时器
     */
    public void stopStatusUpdateTimer() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
            statusUpdateTimer = null;
        }
    }

    /**
     * 设置状态更新回调
     *
     * @param callback 状态更新回调
     */
    public void setStatusUpdateCallback(@Nullable Runnable callback) {
        this.statusUpdateCallback = callback;
    }

    /**
     * 创建可折叠面板
     *
     * @return 可折叠面板
     */
    @NotNull
    private JPanel createCollapsiblePanel() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        // 创建可折叠的标题栏
        String titleText = AICommonBundle.message("settings.codefree.title");
        JPanel titlePanel = createCollapsibleTitle("▶ " + titleText);

        // 默认折叠：隐藏内容面板
        mainPanel.setVisible(false);

        // 使用包装面板确保内容居中
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(mainPanel, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);

        // 将标题栏和内容面板添加到主面板
        content.add(titlePanel, BorderLayout.NORTH);
        content.add(contentWrapper, BorderLayout.CENTER);

        // 为标题栏添加点击事件
        titlePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = mainPanel.isVisible();
                mainPanel.setVisible(!isVisible);
                updateCollapsibleTitle(titlePanel, titleText, !isVisible);
                content.revalidate();
                content.repaint();
            }
        });

        return content;
    }

    /**
     * 创建可折叠的标题栏
     *
     * @param title 标题文本（包含箭头）
     * @return 标题栏面板
     */
    @NotNull
    private JPanel createCollapsibleTitle(@NotNull String title) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        // 默认折叠状态，使用右箭头
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
        configureTitledBorder(titledBorder);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            JBUI.Borders.empty(5)
                                                               ));
        titlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        titlePanel.setOpaque(true);
        titlePanel.setBackground(UIUtil.getPanelBackground());
        return titlePanel;
    }

    /**
     * 更新可折叠标题栏的箭头图标
     *
     * @param titlePanel 标题栏面板
     * @param title      标题文本（不包含箭头）
     * @param expanded   是否展开
     */
    private void updateCollapsibleTitle(@NotNull JPanel titlePanel, @NotNull String title, boolean expanded) {
        String arrow = expanded ? "▼ " : "▶ ";
        TitledBorder titledBorder = BorderFactory.createTitledBorder(arrow + title);
        configureTitledBorder(titledBorder);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            JBUI.Borders.empty(5)
                                                               ));
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 创建一个包含复选框和提示标签的面板
     *
     * @param checkBox 要添加到面板中的复选框
     * @param hintKey  用于获取提示信息的键
     * @return 包含复选框和提示标签的面板
     */
    @NotNull
    private JPanel createCheckBoxWithHint(@NotNull JBCheckBox checkBox, @NotNull String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(checkBox, BorderLayout.WEST);

        JBLabel hintLabel = new JBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        // 根据复选框状态更新提示标签颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        return panel;
    }

    /**
     * 更新提示标签的字体颜色
     *
     * @param hintLabel 提示标签对象
     * @param selected  是否选中状态
     */
    private void updateHintLabelColor(@NotNull JBLabel hintLabel, boolean selected) {
        if (selected) {
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    // ==================== Getter 方法 ====================

    /**
     * 获取主面板
     *
     * @return 主面板
     */
    @NotNull
    public JPanel getContent() {
        return content;
    }

    /**
     * 获取自动启动复选框
     *
     * @return 自动启动复选框
     */
    @NotNull
    public JBCheckBox getAutoStartCheckBox() {
        return autoStartCheckBox;
    }

    /**
     * 获取下载地址输入框
     *
     * @return 下载地址输入框
     */
    @NotNull
    public JBTextField getDownloadUrlField() {
        return downloadUrlField;
    }

    /**
     * 获取状态标签
     *
     * @return 状态标签
     */
    @NotNull
    public JBLabel getStatusLabel() {
        return statusLabel;
    }

    /**
     * 获取下载按钮
     *
     * @return 下载按钮
     */
    @NotNull
    public JButton getDownloadButton() {
        return downloadButton;
    }

    /**
     * 获取启动/停止按钮
     *
     * @return 启动/停止按钮
     */
    @NotNull
    public JButton getStartButton() {
        return startButton;
    }
}


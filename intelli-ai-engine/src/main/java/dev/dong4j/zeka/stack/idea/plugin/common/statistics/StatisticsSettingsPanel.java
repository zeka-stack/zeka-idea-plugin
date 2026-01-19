package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 统计设置面板
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public class StatisticsSettingsPanel {

    /** 统计设置面板主容器 */
    private final JPanel panel;
    /** 隐私协议复选框, 用于用户确认已阅读并同意隐私政策 */
    private final JBCheckBox privacyAgreementCheckBox;
    /** 是否启用统计功能复选框 */
    private final JBCheckBox enableStatistics;
    /** 是否允许上传统计数据, 仅在用户同意隐私协议且启用统计功能后生效 */
    private final JBCheckBox allowUploadCheckBox;
    /** 设备唯一标识输入框, 用于显示和编辑设备 ID */
    private final JBTextField deviceIdField;
    /** 复制设备 ID 按钮, 点击后将设备 ID 复制到剪贴板并显示成功提示 */
    private final JButton copyButton;

    /**
     * 初始化统计设置面板, 构建包含隐私协议, 统计开关, 数据上传选项, 设备 ID 输入及复制功能的用户界面
     * <p>
     * 面板包含以下组件:
     * <ul>
     *   <li> 隐私协议复选框, 用于用户同意隐私政策 </li>
     *   <li> 启用统计复选框, 控制是否启用数据统计 </li>
     *   <li> 允许上传复选框, 控制是否允许上传统计数据 </li>
     *   <li> 设备 ID 输入框, 用于显示和编辑设备唯一标识 </li>
     *   <li> 复制按钮, 用于复制设备 ID 到剪贴板 </li>
     * </ul>
     * <p>
     * 面板支持折叠展开功能, 通过点击标题区域切换内容可见性.
     * <p>
     * 隐私协议复选框状态影响其他组件的可用性: 当未勾选时, 统计开关和上传开关将被禁用.
     * <p>
     * 设备 ID 复制功能通过点击复制按钮实现, 复制后弹出提示.
     * <p>
     * 隐私政策链接点击后将打开 <a href="https://api.dong4j.site/plugin/privacy">https://api.dong4j.site/plugin/privacy</a>
     * 数据查看链接点击后将打开 <a href="https://api.dong4j.site/plugin/datas">https://api.dong4j.site/plugin/datas</a>
     *
     * @see #updateComponentsEnabled()* @see #showCopySuccessTip(JComponent)
     * @see #createCollapsiblePanel(String, JPanel)
     */
    public StatisticsSettingsPanel() {
        this.privacyAgreementCheckBox = new JBCheckBox(AICommonBundle.message("settings.statistics.privacy.agreement"));
        this.enableStatistics = new JBCheckBox(AICommonBundle.message("settings.statistics.enable"));
        this.allowUploadCheckBox = new JBCheckBox(AICommonBundle.message("settings.statistics.allow.upload"));
        this.deviceIdField = new JBTextField();
        // 控制输入框长度，参考 baseUrlField 的方式
        Dimension deviceIdFieldSize = new Dimension(400, deviceIdField.getPreferredSize().height);
        deviceIdField.setPreferredSize(deviceIdFieldSize);
        deviceIdField.setMaximumSize(deviceIdFieldSize);
        this.copyButton = new JButton(AICommonBundle.message("settings.statistics.copy"));
        // 设置按钮宽度，参考测试按钮/刷新按钮的方式
        // 创建一个临时按钮来获取测试按钮/刷新按钮的宽度
        JButton tempTestButton = new JButton(AICommonBundle.message("settings.test.connection"));
        JButton tempRefreshButton = new JButton(AICommonBundle.message("settings.refresh.models"));
        int buttonWidth = Math.max(
            tempTestButton.getPreferredSize().width,
            tempRefreshButton.getPreferredSize().width
                                  );
        Dimension copyButtonSize = new Dimension(buttonWidth, copyButton.getPreferredSize().height);
        copyButton.setPreferredSize(copyButtonSize);
        copyButton.setMaximumSize(copyButtonSize);

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.empty(10));

        // 隐私协议复选框和超链接同一行
        JPanel privacyPanel = new JPanel(new BorderLayout(8, 0));
        privacyPanel.add(privacyAgreementCheckBox, BorderLayout.WEST);
        HyperlinkLabel privacyPolicyLink = new HyperlinkLabel(AICommonBundle.message("settings.statistics.view.privacy.policy"));
        privacyPolicyLink.setHyperlinkText(AICommonBundle.message("settings.statistics.view.privacy.policy"));
        privacyPolicyLink.addHyperlinkListener(e -> BrowserUtil.browse("https://api.dong4j.site/plugin/privacy"));
        privacyPanel.add(privacyPolicyLink, BorderLayout.CENTER);
        content.add(privacyPanel);
        content.add(Box.createVerticalStrut(8));

        // 启用统计复选框和查看统计数据超链接同一行
        JPanel enableCheckboxPanel = new JPanel(new BorderLayout(8, 0));
        enableCheckboxPanel.add(enableStatistics, BorderLayout.WEST);
        HyperlinkLabel viewDataLink = new HyperlinkLabel(AICommonBundle.message("settings.statistics.view.data"));
        viewDataLink.setHyperlinkText(AICommonBundle.message("settings.statistics.view.data"));
        viewDataLink.addHyperlinkListener(e -> BrowserUtil.browse("https://api.dong4j.site/plugin/datas"));
        enableCheckboxPanel.add(viewDataLink, BorderLayout.CENTER);
        content.add(enableCheckboxPanel);
        content.add(Box.createVerticalStrut(8));

        // 允许上报数据复选框左对齐
        JPanel allowUploadPanel = new JPanel(new BorderLayout());
        allowUploadPanel.add(allowUploadCheckBox, BorderLayout.WEST);
        content.add(allowUploadPanel);
        content.add(Box.createVerticalStrut(8));

        JPanel deviceRow = new JPanel(new BorderLayout(5, 0));
        deviceRow.add(new JBLabel(AICommonBundle.message("settings.statistics.device.id")), BorderLayout.WEST);
        deviceRow.add(deviceIdField, BorderLayout.CENTER);
        deviceRow.add(copyButton, BorderLayout.EAST);

        content.add(deviceRow);
        content.add(Box.createVerticalStrut(6));

        // hint 左对齐
        JPanel hintPanel = new JPanel(new BorderLayout());
        JBLabel hint = new JBLabel(AICommonBundle.message("settings.statistics.device.id.hint"));
        hint.setForeground(UIUtil.getContextHelpForeground());
        hintPanel.add(hint, BorderLayout.WEST);
        content.add(hintPanel);

        this.panel = createCollapsiblePanel(AICommonBundle.message("settings.statistics.title"), content);

        // 隐私协议复选框监听器：控制其他组件的可用性
        privacyAgreementCheckBox.addActionListener(e -> updateComponentsEnabled());

        // 启用统计复选框监听器：只有同意隐私协议后才能开启，并控制允许上报数据复选框
        enableStatistics.addActionListener(e -> {
            if (enableStatistics.isSelected() && !privacyAgreementCheckBox.isSelected()) {
                enableStatistics.setSelected(false);
            }
            updateComponentsEnabled();
        });

        // 初始化组件可用性
        updateComponentsEnabled();

        // 复制按钮：复制成功后显示 tip
        copyButton.addActionListener(e -> {
            String deviceId = deviceIdField.getText().trim();
            CopyPasteManager.getInstance().setContents(new StringSelection(deviceId));
            showCopySuccessTip(copyButton);
        });
    }

    /**
     * 更新组件的可用性
     * <p>
     * 根据隐私协议复选框的状态，控制面板其他组件的可用性
     * 允许上报数据复选框由启用统计复选框控制
     */
    private void updateComponentsEnabled() {
        boolean privacyEnabled = privacyAgreementCheckBox.isSelected();
        enableStatistics.setEnabled(privacyEnabled);
        deviceIdField.setEnabled(privacyEnabled);
        copyButton.setEnabled(privacyEnabled);

        // 如果未同意隐私协议，禁用统计功能
        if (!privacyEnabled && enableStatistics.isSelected()) {
            enableStatistics.setSelected(false);
        }

        // 允许上报数据复选框由启用统计复选框控制
        boolean statisticsEnabled = privacyEnabled && enableStatistics.isSelected();
        allowUploadCheckBox.setEnabled(statisticsEnabled);

        // 如果禁用统计功能，取消选中允许上报数据
        if (!statisticsEnabled && allowUploadCheckBox.isSelected()) {
            allowUploadCheckBox.setSelected(false);
        }
    }

    /**
     * 显示复制成功提示
     * <p>
     * 在按钮下方显示一个 tip 提示，表示复制成功
     *
     * @param component 显示 tip 的组件
     */
    private void showCopySuccessTip(@NotNull JComponent component) {
        Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                AICommonBundle.message("settings.statistics.copy.success"),
                com.intellij.openapi.ui.MessageType.INFO,
                null)
            .setFadeoutTime(2000)
            .createBalloon();
        balloon.show(
            new RelativePoint(component, new Point(component.getWidth() / 2, component.getHeight())),
            Balloon.Position.below);
    }

    /**
     * 获取面板组件
     * <p>
     * 返回当前统计设置面板的主容器组件, 用于在界面中显示和布局.
     *
     * @return 面板组件, 非空 (@NotNull)
     */
    @NotNull
    public JComponent getPanel() {
        return panel;
    }

    /**
     * 加载设置数据到面板组件
     * <p>
     * 根据传入的统计设置对象, 更新面板中各个复选框和文本字段的值, 包括隐私协议同意状态, 统计功能启用状态, 数据上传允许状态以及设备 ID.
     * 更新后调用 {@link #updateComponentsEnabled()} 以同步组件的可用性状态.
     *
     * @param settings 统计设置对象, 包含用户配置的隐私协议, 统计功能, 数据上传及设备 ID 等信息
     */
    public void loadSettings(@NotNull StatisticsSettings settings) {
        privacyAgreementCheckBox.setSelected(settings.isPrivacyAgreementAccepted());
        enableStatistics.setSelected(settings.isEnableStatistics());
        allowUploadCheckBox.setSelected(settings.isAllowUpload());
        deviceIdField.setText(settings.getDeviceId());
        updateComponentsEnabled();
    }

    /**
     * 检查当前面板设置是否与传入的配置对象存在差异
     * <p>
     * 该方法用于判断用户在界面中修改的设置是否与传入的 {@code StatisticsSettings} 对象不一致.
     * 比较内容包括: 隐私协议是否已勾选, 是否启用统计, 是否允许上传数据, 设备 ID 是否变更.
     * 若任一字段不一致, 则返回 true, 表示设置已被修改.
     *
     * @param settings 用于对比的统计设置对象, 不能为 null
     * @return 如果当前面板设置与传入的 settings 对象存在差异, 则返回 true; 否则返回 false
     */
    public boolean isModified(@NotNull StatisticsSettings settings) {
        if (privacyAgreementCheckBox.isSelected() != settings.isPrivacyAgreementAccepted()) {
            return true;
        }
        if (enableStatistics.isSelected() != settings.isEnableStatistics()) {
            return true;
        }
        if (allowUploadCheckBox.isSelected() != settings.isAllowUpload()) {
            return true;
        }
        String currentId = settings.getDeviceId();
        return !deviceIdField.getText().trim().equals(currentId == null ? "" : currentId);
    }

    /**
     * 应用设置并同步到指定的统计设置对象
     * <p>
     * 根据当前界面组件的状态, 将隐私协议同意状态, 统计功能启用状态, 数据上传权限以及设备 ID 等信息写入传入的 {@code settings} 对象.
     * 若设备 ID 为空, 则自动生成一个设备 ID 并赋值.
     *
     * @param settings 需要被更新的统计设置对象, 不能为 null
     * @since 1.4.0
     */
    public void apply(@NotNull StatisticsSettings settings) {
        settings.setPrivacyAgreementAccepted(privacyAgreementCheckBox.isSelected());
        // 只有同意隐私协议后才能启用统计
        boolean enableStats = privacyAgreementCheckBox.isSelected() && enableStatistics.isSelected();
        settings.setEnableStatistics(enableStats);
        // 只有启用统计后才能允许上报数据
        boolean allowUpload = enableStats && allowUploadCheckBox.isSelected();
        settings.setAllowUpload(allowUpload);
        String deviceId = deviceIdField.getText().trim();
        if (deviceId.isEmpty()) {
            deviceId = DeviceIdGenerator.generateDeviceId();
        }
        settings.setDeviceId(deviceId);
    }

    /**
     * 创建一个可折叠的面板容器
     * <p>
     * 该方法用于构建一个带有标题和折叠 / 展开功能的面板容器, 标题前显示箭头图标 (▶ 或 ▼), 点击可切换内容区域的可见性.
     * 内容区域默认不可见, 点击标题区域后会切换其显示状态, 并更新标题图标.
     * 面板背景为默认面板背景色, 标题边框使用指定字体和颜色.
     *
     * @param title   标题文本, 用于显示在面板标题栏中
     * @param content 内容面板, 将被添加到容器中, 初始状态为不可见
     * @return 包含折叠功能的 JPanel 容器
     */
    @NotNull
    private JPanel createCollapsiblePanel(@NotNull String title, @NotNull JPanel content) {
        JPanel container = new JPanel(new BorderLayout());
        content.setVisible(false);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(content, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);

        container.add(contentWrapper, BorderLayout.CENTER);
        container.setOpaque(true);
        container.setBackground(UIUtil.getPanelBackground());

        TitledBorder titledBorder = BorderFactory.createTitledBorder("▶ " + title);
        titledBorder.setTitleFont(UIUtil.getLabelFont());
        titledBorder.setTitleColor(UIUtil.getLabelForeground());
        container.setBorder(BorderFactory.createCompoundBorder(titledBorder, JBUI.Borders.empty(5)));

        container.addMouseListener(new java.awt.event.MouseAdapter() {
            /**
             * 处理鼠标点击事件, 用于切换内容区域的可见性并更新标题图标
             * <p> 点击时切换 content 组件的可见状态, 根据当前状态显示不同的箭头图标 (▶ 或 ▼), 并重新设置边框和重绘组件 </p>
             *
             * @param e 鼠标点击事件对象
             */
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean visible = content.isVisible();
                content.setVisible(!visible);
                String arrow = visible ? "▶ " : "▼ ";
                TitledBorder newBorder = BorderFactory.createTitledBorder(arrow + title);
                newBorder.setTitleFont(UIUtil.getLabelFont());
                newBorder.setTitleColor(UIUtil.getLabelForeground());
                container.setBorder(BorderFactory.createCompoundBorder(newBorder, JBUI.Borders.empty(5)));
                container.revalidate();
                container.repaint();
            }
        });

        return container;
    }
}

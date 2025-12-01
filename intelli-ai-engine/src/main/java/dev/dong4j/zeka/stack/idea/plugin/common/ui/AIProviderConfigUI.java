package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import icons.AICommonIcons;

/**
 * AI 提供商配置 UI 类
 * <p>
 * 该类负责创建和管理 AI 服务提供商的配置界面, 包括提供商选择, 模型配置,API 密钥设置,
 * 连接测试, 可用提供商管理以及高级参数配置等功能. 提供完整的 UI 组件和交互逻辑,
 * 支持多种 AI 服务提供商的配置和管理.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class AIProviderConfigUI {
    /** 主界面主面板, 用于承载主要功能组件和布局 */
    private JPanel mainPanel;
    /** 提供商下拉选择框 */
    private ComboBox<String> providerComboBox;
    /** 下拉框组件, 用于选择模型 */
    private ComboBox<String> modelComboBox;
    /** 基础 URL 输入框 */
    private JBTextField baseUrlField;
    /** API 密钥输入框 */
    private JBPasswordField apiKeyField;
    /** 测试连接按钮 */
    private JButton testConnectionButton;
    /** 刷新模型按钮 */
    private JButton refreshModelsButton;
    /** 显示可用提供者的复选框 */
    private JBCheckBox showAvailableProvidersCheckBox;
    /** 可用提供者面板, 用于展示和选择可用的提供者组件 */
    private JPanel availableProvidersPanel;
    /** 可用提供者的描述标签 */
    private JBLabel availableProvidersDescriptionLabel;
    /** 可用提供者的表格组件, 用于展示和选择可用的提供者列表 */
    private JBTable availableProvidersTable;
    /** 可用提供者模型表 */
    private AvailableProvidersTableModel availableProvidersTableModel;
    /** 日志详细输出选项复选框 */
    private JBCheckBox verboseLoggingCheckBox;
    /** 显示高级设置的复选框 */
    private JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置内容面板, 用于展示和管理高级配置选项 */
    private JPanel advancedSettingsContentPanel;
    /** 最大重试次数选择器 */
    private JSpinner maxRetriesSpinner;
    /** 超时时间选择器, 用于设置请求超时时间 */
    private JSpinner timeoutSpinner;
    /** 温度设置控件, 用于用户输入和调整温度值 */
    private JSpinner temperatureSpinner;
    /** 最大令牌数输入控件 */
    private JSpinner maxTokensSpinner;
    /** 顶部参数的下拉选择器控件 */
    private JSpinner topPSpinner;
    /** 用于设置和获取 topK 值的下拉选择框 */
    private JSpinner topKSpinner;
    /** 偏差惩罚值调节器, 用于设置生成文本时的偏差惩罚参数 */
    private JSpinner presencePenaltySpinner;
    /** checkBoxHintLabelMap 用于映射复选框与对应的提示标签 */
    private final Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new HashMap<>();

    /**
     * 初始化并创建用户界面组件
     * <p>
     * 该方法用于初始化和创建所有用户界面组件, 包括下拉框, 文本字段, 按钮, 复选框,
     * 旋钮控件以及表格等, 并设置它们的属性和布局. 同时, 为表格添加工具栏装饰器,
     * 用于提供删除和清除所有可用提供者的操作.
     *
     * @param removeAvailableProviderCallback    删除可用提供者时的回调
     * @param clearAllAvailableProvidersCallback 清除所有可用提供者时的回调
     */
    public void createUI(@Nullable Runnable removeAvailableProviderCallback,
                         @Nullable Runnable clearAllAvailableProvidersCallback) {
        // 初始化连接配置组件
        providerComboBox = new ComboBox<>(AIProviderType.getAllDisplayNames().toArray(new String[0]));
        providerComboBox.setRenderer(new ProviderListCellRenderer());

        modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true);

        baseUrlField = new JBTextField();
        baseUrlField.setToolTipText(AICommonBundle.message("settings.base.url.tooltip"));

        apiKeyField = new JBPasswordField();
        apiKeyField.setToolTipText(AICommonBundle.message("settings.api.key.tooltip"));

        testConnectionButton = new JButton(AICommonBundle.message("settings.test.connection"));
        refreshModelsButton = new JButton(AICommonBundle.message("settings.refresh.models"));

        // 初始化基础配置组件
        verboseLoggingCheckBox = new JBCheckBox(AICommonBundle.message("settings.verbose.logging"));

        // 初始化高级配置组件
        showAdvancedSettingsCheckBox = new JBCheckBox(AICommonBundle.message("settings.advanced.settings.show"));
        maxRetriesSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 10, 1));
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 600, 1));
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 2.0, 0.1));
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(4.0, 0.1, 256.0, 0.1));
        topPSpinner = new JSpinner(new SpinnerNumberModel(0.9, 0.0, 1.0, 0.1));
        topKSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100, 1));
        presencePenaltySpinner = new JSpinner(new SpinnerNumberModel(0.0, -2.0, 2.0, 0.1));

        // 设置所有 JSpinner 的长度一致
        Dimension spinnerSize = new Dimension(120, maxRetriesSpinner.getPreferredSize().height);
        maxRetriesSpinner.setPreferredSize(spinnerSize);
        timeoutSpinner.setPreferredSize(spinnerSize);
        temperatureSpinner.setPreferredSize(spinnerSize);
        maxTokensSpinner.setPreferredSize(spinnerSize);
        topPSpinner.setPreferredSize(spinnerSize);
        topKSpinner.setPreferredSize(spinnerSize);
        presencePenaltySpinner.setPreferredSize(spinnerSize);

        // 初始化可用服务商表格
        availableProvidersTableModel = new AvailableProvidersTableModel();
        availableProvidersTable = new JBTable(availableProvidersTableModel);
        availableProvidersTable.setPreferredScrollableViewportSize(new Dimension(480, 120));
        availableProvidersTable.getColumnModel().getColumn(0).setCellRenderer(new ProviderTableCellRenderer());

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(availableProvidersTable)
            .setRemoveAction(button -> {
                if (removeAvailableProviderCallback != null) {
                    removeAvailableProviderCallback.run();
                }
            })
            .addExtraAction(new AnAction(AICommonBundle.message("settings.available.providers.clear.all"),
                                         AICommonBundle.message("settings.available.providers.clear.all.description"),
                                         AllIcons.Actions.GC) {
                /**
                 * 处理动作事件, 调用清除所有可用提供者的回调方法
                 * <p>
                 * 当接收到动作事件时, 如果存在清除所有可用提供者的回调方法, 则执行该回调
                 *
                 * @param e 动作事件对象
                 */
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    if (clearAllAvailableProvidersCallback != null) {
                        clearAllAvailableProvidersCallback.run();
                    }
                }

                /**
                 * 获取动作更新线程类型
                 * <p>
                 * 返回动作更新所使用的线程类型, 该方法覆盖了父类方法以提供特定实现.
                 *
                 * @return 动作更新线程类型, 保证不为 null
                 */
                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.EDT;
                }
            });
        availableProvidersPanel = decorator.createPanel();
        availableProvidersPanel.setVisible(false);

        showAvailableProvidersCheckBox = new JBCheckBox(AICommonBundle.message("settings.show.available.providers"));

        // 创建 4 个子面板
        JPanel connectionPanel = createConnectionPanel();
        JPanel availableProvidersSectionPanel = createAvailableProvidersPanel();
        JPanel basicPanel = createBasicPanel();
        JPanel advancedPanel = createAdvancedPanel();

        // 组合成主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(connectionPanel)
            .addSeparator(10)
            .addComponent(availableProvidersSectionPanel)
            .addSeparator(10)
            .addComponent(basicPanel)
            .addSeparator(10)
            .addComponent(advancedPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        mainPanel.setBorder(JBUI.Borders.empty(8));
    }

    /**
     * 获取主面板
     */
    @NotNull
    public JPanel getMainPanel() {
        return mainPanel;
    }

    // ==================== Getter 方法 ====================

    @NotNull
    public ComboBox<String> getProviderComboBox() {
        return providerComboBox;
    }

    @NotNull
    public ComboBox<String> getModelComboBox() {
        return modelComboBox;
    }

    @NotNull
    public JBTextField getBaseUrlField() {
        return baseUrlField;
    }

    @NotNull
    public JBPasswordField getApiKeyField() {
        return apiKeyField;
    }

    @NotNull
    public JButton getTestConnectionButton() {
        return testConnectionButton;
    }

    @NotNull
    public JButton getRefreshModelsButton() {
        return refreshModelsButton;
    }

    @NotNull
    public JBCheckBox getShowAvailableProvidersCheckBox() {
        return showAvailableProvidersCheckBox;
    }

    @NotNull
    public JPanel getAvailableProvidersPanel() {
        return availableProvidersPanel;
    }

    @Nullable
    public JBLabel getAvailableProvidersDescriptionLabel() {
        return availableProvidersDescriptionLabel;
    }

    @NotNull
    public AvailableProvidersTableModel getAvailableProvidersTableModel() {
        return availableProvidersTableModel;
    }

    @NotNull
    public JBCheckBox getVerboseLoggingCheckBox() {
        return verboseLoggingCheckBox;
    }

    @NotNull
    public JBCheckBox getShowAdvancedSettingsCheckBox() {
        return showAdvancedSettingsCheckBox;
    }

    @Nullable
    public JPanel getAdvancedSettingsContentPanel() {
        return advancedSettingsContentPanel;
    }

    @NotNull
    public JSpinner getMaxRetriesSpinner() {
        return maxRetriesSpinner;
    }

    @NotNull
    public JSpinner getTimeoutSpinner() {
        return timeoutSpinner;
    }

    @NotNull
    public JSpinner getTemperatureSpinner() {
        return temperatureSpinner;
    }

    @NotNull
    public JSpinner getMaxTokensSpinner() {
        return maxTokensSpinner;
    }

    @NotNull
    public JSpinner getTopPSpinner() {
        return topPSpinner;
    }

    @NotNull
    public JSpinner getTopKSpinner() {
        return topKSpinner;
    }

    @NotNull
    public JSpinner getPresencePenaltySpinner() {
        return presencePenaltySpinner;
    }

    // ==================== UI 创建方法 ====================

    /**
     * 创建连接配置面板, 用于显示和配置 AI 服务的连接相关信息
     * <p>
     * 该方法构建一个包含提供商选择, 基础 URL 输入,API 密钥输入以及模型选择的面板, 用于设置 AI 服务的基本连接配置.
     *
     * @return 包含连接配置组件的面板
     */
    private JPanel createConnectionPanel() {
        Dimension providerComboBoxSize = new Dimension(300, providerComboBox.getPreferredSize().height);
        providerComboBox.setPreferredSize(providerComboBoxSize);
        providerComboBox.setMaximumSize(providerComboBoxSize);

        JPanel apiKeyPanel = new JPanel(new BorderLayout(5, 0));
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);
        apiKeyPanel.add(refreshModelsButton, BorderLayout.EAST);

        JPanel modelPanel = new JPanel(new BorderLayout(5, 0));
        modelPanel.add(modelComboBox, BorderLayout.CENTER);
        modelPanel.add(testConnectionButton, BorderLayout.EAST);

        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.provider.label")), providerComboBox)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.base.url.label")), baseUrlField)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.api.key.label")), apiKeyPanel)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.model.label")), modelPanel)
            .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setBorder(new TitledBorder(AICommonBundle.message("settings.basic.connection.config")));
        return wrapper;
    }

    /**
     * 创建可用提供者面板
     * <p>
     * 用于构建显示可用提供者的面板, 包含描述标签和相关组件.
     *
     * @return 包含可用提供者信息的面板
     */
    private JPanel createAvailableProvidersPanel() {
        availableProvidersDescriptionLabel = new JBLabel();
        String descriptionText = AICommonBundle.message("settings.available.providers.description");
        descriptionText = "<html>" + descriptionText.replace("\n", "<br>") + "</html>";
        availableProvidersDescriptionLabel.setText(descriptionText);
        availableProvidersDescriptionLabel.setFont(availableProvidersDescriptionLabel.getFont().deriveFont(availableProvidersDescriptionLabel.getFont().getSize() - 1f));
        availableProvidersDescriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        availableProvidersDescriptionLabel.setBorder(JBUI.Borders.empty(5, 0, 10, 0));
        availableProvidersDescriptionLabel.setVisible(false);

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showAvailableProvidersCheckBox, "settings.show.available.providers.hint"))
            .addComponent(availableProvidersDescriptionLabel)
            .addComponent(availableProvidersPanel)
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.setBorder(new TitledBorder(
            BorderFactory.createEtchedBorder(),
            AICommonBundle.message("settings.show.available.providers")
        ));

        return panel;
    }

    /**
     * 创建基础配置面板
     * <p>
     * 用于生成包含日志详细设置选项的基础配置面板, 包含一个复选框用于控制日志详细级别.
     *
     * @return 包含日志详细设置选项的面板
     */
    private JPanel createBasicPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(verboseLoggingCheckBox, "settings.verbose.logging.hint"))
            .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setBorder(new TitledBorder(AICommonBundle.message("settings.basic.config")));
        return wrapper;
    }

    /**
     * 创建高级设置面板, 用于显示 AI 模型的高级配置选项
     * <p>
     * 该方法构建一个包含多个可配置参数的面板, 包括最大重试次数, 超时时间, 最大令牌数, 温度值,Top-p,Top-k, 存在惩罚等设置项.
     * 面板中还包含一个复选框, 用于控制是否显示高级设置内容.
     *
     * @return 包含高级设置内容的面板
     */
    private JPanel createAdvancedPanel() {
        advancedSettingsContentPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.max.retries")),
                                 createSpinnerWithHint(maxRetriesSpinner, "settings.max.retries.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.timeout")),
                                 createSpinnerWithHint(timeoutSpinner, "settings.timeout.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.max.tokens")),
                                 createSpinnerWithHint(maxTokensSpinner, "settings.max.tokens.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.temperature")),
                                 createSpinnerWithHint(temperatureSpinner, "settings.temperature.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.top.p")),
                                 createSpinnerWithHint(topPSpinner, "settings.top.p.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.top.k")),
                                 createSpinnerWithHint(topKSpinner, "settings.top.k.hint"))
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.presence.penalty")),
                                 createSpinnerWithHint(presencePenaltySpinner, "settings.presence.penalty.hint"))
            .getPanel();
        advancedSettingsContentPanel.setVisible(false);

        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showAdvancedSettingsCheckBox, "settings.advanced.settings.show.hint"))
            .addComponent(advancedSettingsContentPanel)
            .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setBorder(new TitledBorder(AICommonBundle.message("settings.advanced.config")));
        return wrapper;
    }

    /**
     * 创建包含提示标签的面板, 用于与 JSpinner 组件结合使用
     * <p>
     * 该方法创建一个 JPanel, 其中包含一个 JSpinner 组件和一个提示标签. 提示标签使用指定的提示键获取国际化消息, 并设置为较暗的字体颜色和较小的字体大小, 以实现提示效果.
     *
     * @param spinner 要添加到面板中的 JSpinner 组件
     * @param hintKey 国际化提示消息的键, 用于获取提示文本
     * @return 包含 JSpinner 和提示标签的 JPanel
     */
    private JPanel createSpinnerWithHint(JSpinner spinner, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(spinner, BorderLayout.WEST);

        JBLabel hintLabel = new JBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(300, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建一个包含复选框和提示标签的面板
     * <p>
     * 该方法用于创建一个包含复选框和提示标签的面板, 提示标签根据提供的键获取对应的提示信息, 并设置样式和布局.
     *
     * @param checkBox 要添加到面板中的复选框
     * @param hintKey  用于获取提示信息的键
     * @return 包含复选框和提示标签的面板
     */
    private JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(checkBox, BorderLayout.WEST);

        JBLabel hintLabel = new JBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        checkBoxHintLabelMap.put(checkBox, hintLabel);
        updateHintLabelColor(hintLabel, checkBox.isSelected());
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        return panel;
    }

    /**
     * 更新提示标签的字体颜色
     * <p>
     * 根据传入的选中状态, 设置提示标签的字体颜色为正常或禁用状态的颜色.
     *
     * @param hintLabel 提示标签对象
     * @param selected  是否选中状态, 为 true 时使用正常颜色, 为 false 时使用禁用颜色
     */
    private void updateHintLabelColor(JBLabel hintLabel, boolean selected) {
        if (selected) {
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    /**
     * 更新所有复选框的提示标签颜色
     * <p>
     * 遍历复选框与提示标签的映射关系, 根据复选框的选中状态更新对应的提示标签颜色
     *
     * @since 1.0
     */
    public void updateCheckBoxHintColors() {
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) -> updateHintLabelColor(hintLabel, checkBox.isSelected()));
    }

    /**
     * 创建一个状态点图标, 用于表示某种状态
     * <p>
     * 根据指定颜色生成一个圆形图标, 常用于状态指示
     *
     * @param color 图标填充颜色
     * @return 新创建的图标对象
     */
    @NotNull
    public Icon createStatusDotIcon(Color color) {
        int size = 4;
        BufferedImage image = ImageUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * 获取选中的可用服务提供商行索引
     * <p>
     * 返回可用服务提供商表格中当前选中的行的索引
     *
     * @return 选中的行索引, 若未选中则返回 -1
     */
    public int getSelectedAvailableProviderRow() {
        return availableProvidersTable.getSelectedRow();
    }

    // ==================== 内部类 ====================

    /**
     * 自定义的 Provider 列表单元格渲染器
     * <p>
     * 用于在 JList 中渲染 AI Provider 的显示名称和对应的图标, 支持根据显示名称获取对应的 Provider 类型, 并设置相应的图标.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.28
     * @since 1.0.0
     */
    private static class ProviderListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String displayName) {
                label.setText(displayName);
                AIProviderType providerType = AIProviderType.fromDisplayName(displayName);
                if (providerType != null) {
                    Icon icon = AICommonIcons.getProviderIcon(providerType);
                    label.setIcon(icon);
                }
            }
            return label;
        }
    }

    /**
     * 表格单元格渲染器, 用于自定义 AI 提供商配置信息的显示样式
     * <p>
     * 该渲染器继承自 DefaultTableCellRenderer, 用于在表格中渲染 AIProviderConfig 对象, 显示其对应的显示名称和图标.
     * 支持根据配置类型获取对应的显示名称和图标, 提升表格数据的可读性和可视化效果.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.28
     * @since 1.0.0
     */
    private static class ProviderTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label && value instanceof AIProviderConfig config) {
                String displayName = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message(
                    "settings.available.providers.unknown");
                label.setText(displayName);
                if (config.providerType != null) {
                    Icon icon = AICommonIcons.getProviderIcon(config.providerType);
                    label.setIcon(icon);
                }
            }
            return component;
        }
    }

    /**
     * 可用提供者表格模型类
     * <p>
     * 用于展示和操作可用 AI 提供者的配置信息, 支持数据的增删改查以及表格的显示与编辑功能.
     * 该模型继承自 AbstractTableModel, 实现了表格数据的绑定和操作接口.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.28
     * @since 1.0.0
     */
    public static class AvailableProvidersTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            AICommonBundle.message("settings.available.providers.column.provider"),
            AICommonBundle.message("settings.available.providers.column.model"),
            AICommonBundle.message("settings.available.providers.column.remark")
        };
        private final List<AIProviderConfig> data = new java.util.ArrayList<>();

        public void setData(List<AIProviderConfig> configs) {
            data.clear();
            configs.forEach(config -> data.add(config.copy()));
            fireTableDataChanged();
        }

        public List<AIProviderConfig> getData() {
            List<AIProviderConfig> copy = new java.util.ArrayList<>();
            data.forEach(config -> copy.add(config.copy()));
            return copy;
        }

        public AIProviderConfig getProviderConfig(int index) {
            if (index >= 0 && index < data.size()) {
                return data.get(index);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AIProviderConfig config = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> config;
                case 1 -> config.modelName != null ? config.modelName : "";
                case 2 -> config.remark != null ? config.remark : "";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 2 && rowIndex >= 0 && rowIndex < data.size()) {
                data.get(rowIndex).remark = aValue != null ? aValue.toString() : "";
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}


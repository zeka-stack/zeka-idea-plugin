package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 可复用的 AI 提供商配置面板。
 */
@SuppressWarnings("D")
public final class AIProviderConfigPanel {

    private final AICredentialManager credentialManager;
    private final AIResponseListener responseListener;

    private JPanel mainPanel;

    private ComboBox<String> providerComboBox;
    private ComboBox<String> modelComboBox;
    private JBTextField baseUrlField;
    private JBPasswordField apiKeyField;
    private JButton testConnectionButton;
    private JButton refreshModelsButton;

    private JBCheckBox showAvailableProvidersCheckBox;
    private JPanel availableProvidersPanel;
    private JBTable availableProvidersTable;
    private AvailableProvidersTableModel availableProvidersTableModel;

    // 基础配置组件
    private JBCheckBox verboseLoggingCheckBox;
    private JBCheckBox performanceModeCheckBox;
    private JBCheckBox showProviderStatisticsCheckBox;

    // 高级配置组件
    private JBCheckBox showAdvancedSettingsCheckBox;
    private JPanel advancedSettingsContentPanel;
    private JSpinner maxRetriesSpinner;
    private JSpinner timeoutSpinner;
    private JSpinner temperatureSpinner;
    private JSpinner maxTokensSpinner;
    private JSpinner topPSpinner;
    private JSpinner topKSpinner;
    private JSpinner presencePenaltySpinner;

    // 保存复选框和提示标签的映射关系，用于更新提示文本颜色
    private final java.util.Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new java.util.HashMap<>();

    private Boolean configurationVerified = Boolean.FALSE;
    private Boolean refreshModelsSuccess = null;

    private AIProviderSettings workingSettings = new AIProviderSettings();

    public AIProviderConfigPanel(@NotNull AICredentialManager credentialManager) {
        this(credentialManager, null);
    }

    public AIProviderConfigPanel(@NotNull AICredentialManager credentialManager,
                                 @Nullable AIResponseListener responseListener) {
        this.credentialManager = credentialManager;
        this.responseListener = responseListener;
        createUI();
        setupListeners();
    }

    @NotNull
    public JPanel getPanel() {
        return mainPanel;
    }

    public void loadSettings(@NotNull AIProviderSettings settings) {
        this.workingSettings = settings.copy();

        // 加载连接配置
        providerComboBox.setSelectedItem(workingSettings.providerType.getDisplayName());
        updateModelList();

        AIProviderConfig defaultConfig = workingSettings.getDefaultProviderConfig(workingSettings.providerType);
        modelComboBox.setSelectedItem(defaultConfig.modelName);
        baseUrlField.setText(defaultConfig.baseUrl);
        configurationVerified = defaultConfig.configurationVerified;
        updateTestButtonState();

        loadApiKeyAsync(defaultConfig.credentialId, workingSettings.providerType.getProviderId());

        refreshModelsSuccess = null;
        updateRefreshButtonState();

        // 加载基础配置
        AIRuntimeSettings runtimeSettings = workingSettings.runtimeSettings;
        verboseLoggingCheckBox.setSelected(runtimeSettings.verboseLogging);
        performanceModeCheckBox.setSelected(workingSettings.performanceMode);
        showProviderStatisticsCheckBox.setSelected(workingSettings.showProviderStatistics);
        updatePerformanceModeSubConfigEnabled();

        // 更新提示文本颜色（根据复选框的选中状态）
        updateCheckBoxHintColors();

        // 加载高级配置
        showAdvancedSettingsCheckBox.setSelected(workingSettings.showAdvancedSettings);
        if (advancedSettingsContentPanel != null) {
            advancedSettingsContentPanel.setVisible(workingSettings.showAdvancedSettings);
        }
        maxRetriesSpinner.setValue(runtimeSettings.maxRetries);
        timeoutSpinner.setValue(runtimeSettings.timeout);
        AIModelParameters modelParameters = workingSettings.modelParameters;
        temperatureSpinner.setValue(modelParameters.temperature);
        maxTokensSpinner.setValue(modelParameters.maxTokens);
        topPSpinner.setValue(modelParameters.topP);
        topKSpinner.setValue(modelParameters.topK);
        presencePenaltySpinner.setValue(modelParameters.presencePenalty);

        // 加载可用服务商
        availableProvidersTableModel.setData(workingSettings.availableProviders);
        showAvailableProvidersCheckBox.setSelected(workingSettings.showAvailableProviders);
        availableProvidersPanel.setVisible(workingSettings.showAvailableProviders);
    }

    @NotNull
    public AIProviderSettings getSettings() {
        AIProviderSettings copy = workingSettings.copy();

        // 保存连接配置
        AIProviderType providerType = resolveSelectedProviderType();
        copy.providerType = providerType;

        AIProviderConfig defaultConfig = copy.getDefaultProviderConfig(providerType);
        String modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        defaultConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        defaultConfig.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        defaultConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);
        defaultConfig.updateCredentialId(getCurrentApiKey());
        copy.updateDefaultProviderConfig(providerType, defaultConfig);

        copy.availableProviders.clear();
        availableProvidersTableModel.getData().forEach(copy::addAvailableProvider);

        // 保存基础配置
        AIRuntimeSettings runtimeSettings = copy.runtimeSettings;
        runtimeSettings.verboseLogging = verboseLoggingCheckBox.isSelected();
        copy.performanceMode = performanceModeCheckBox.isSelected();
        copy.showProviderStatistics = showProviderStatisticsCheckBox.isSelected();
        copy.showAvailableProviders = showAvailableProvidersCheckBox.isSelected();

        // 保存高级配置
        copy.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();
        runtimeSettings.maxRetries = ((Number) maxRetriesSpinner.getValue()).intValue();
        runtimeSettings.timeout = ((Number) timeoutSpinner.getValue()).intValue();
        AIModelParameters modelParameters = copy.modelParameters;
        modelParameters.temperature = ((Number) temperatureSpinner.getValue()).doubleValue();
        modelParameters.maxTokens = ((Number) maxTokensSpinner.getValue()).intValue();
        modelParameters.topP = ((Number) topPSpinner.getValue()).doubleValue();
        modelParameters.topK = ((Number) topKSpinner.getValue()).intValue();
        modelParameters.presencePenalty = ((Number) presencePenaltySpinner.getValue()).doubleValue();

        return copy;
    }

    public boolean isModified(@NotNull AIProviderSettings baseline) {
        AIProviderSettings latest = getSettings();
        // 使用 contentEquals 进行完整比较，包括基础配置和高级配置
        return !latest.contentEquals(baseline);
    }

    @NotNull
    public String getCurrentApiKey() {
        return new String(apiKeyField.getPassword()).trim();
    }

    private void createUI() {
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
        updateRefreshButtonState();

        // 初始化基础配置组件
        verboseLoggingCheckBox = new JBCheckBox(AICommonBundle.message("settings.verbose.logging"));
        performanceModeCheckBox = new JBCheckBox(AICommonBundle.message("settings.performance.mode"));
        showProviderStatisticsCheckBox = new JBCheckBox(AICommonBundle.message("settings.show.provider.statistics"));

        // 初始化高级配置组件
        showAdvancedSettingsCheckBox = new JBCheckBox(AICommonBundle.message("settings.advanced.settings.show"));
        maxRetriesSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 10, 1));
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(10000, 1000, 300000, 1000));
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 2.0, 0.1));
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
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
                int selected = availableProvidersTable.getSelectedRow();
                if (selected >= 0) {
                    removeAvailableProvider(selected);
                }
            })
            .addExtraAction(new AnAction(AICommonBundle.message("settings.available.providers.clear.all"),
                                         AICommonBundle.message("settings.available.providers.clear.all.description"),
                                         AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllAvailableProviders();
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.EDT;
                }
            });
        availableProvidersPanel = decorator.createPanel();
        availableProvidersPanel.setVisible(false);

        showAvailableProvidersCheckBox = new JBCheckBox(AICommonBundle.message("settings.show.available.providers"));

        // 创建 3 个子面板
        JPanel connectionPanel = createConnectionPanel();
        JPanel basicPanel = createBasicPanel();
        JPanel advancedPanel = createAdvancedPanel();

        // 初始化高级设置面板的可见性
        if (advancedSettingsContentPanel != null) {
            advancedSettingsContentPanel.setVisible(workingSettings.showAdvancedSettings);
        }

        // 组合成主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(connectionPanel)
            .addSeparator(10)
            .addComponent(basicPanel)
            .addSeparator(10)
            .addComponent(advancedPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        mainPanel.setBorder(JBUI.Borders.empty(8));
    }

    private JPanel createConnectionPanel() {
        // 连接配置布局：
        // - AI 服务商下拉框（单独一行）
        // - 基础 URL（单独一行）
        // - API Key 和 刷新模型按钮（同一行）
        // - 模型选择框 和 测试按钮（同一行）

        // 设置 AI 服务商下拉框的宽度为 50 像素
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

    private JPanel createBasicPanel() {
        // 基础配置包括：
        // - 详细日志
        // - 性能模式
        // - 联动的任务结果展示（showProviderStatistics）- 子配置，缩进2个空格
        // - 显示可用服务商 - 子配置，缩进2个空格

        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(verboseLoggingCheckBox, "settings.verbose.logging.hint"))
            .addComponent(createCheckBoxWithHint(performanceModeCheckBox, "settings.performance.mode.hint"))
            .addComponent(createPerformanceModeSubConfigPanel())
            .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setBorder(new TitledBorder(AICommonBundle.message("settings.basic.config")));
        return wrapper;
    }

    private JPanel createPerformanceModeSubConfigPanel() {
        // 性能模式的子配置面板，包含显示任务统计和显示可用服务商
        // 需要向右缩进2个空格（约22像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(22));

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showProviderStatisticsCheckBox, "settings.show.provider.statistics.hint"))
            .addComponent(createCheckBoxWithHint(showAvailableProvidersCheckBox, "settings.show.available.providers.hint"))
            .addComponent(availableProvidersPanel)
            .getPanel();

        indentPanel.add(contentPanel, BorderLayout.CENTER);
        return indentPanel;
    }

    private JPanel createAdvancedPanel() {
        // 高级配置包括：
        // - 最大重试次数（在最大Token数上面）
        // - 请求超时时间（在最大Token数上面）
        // - 最大 Token 数
        // - 温度
        // - Top-p
        // - Top-k
        // - 存在惩罚

        // 创建高级配置内容面板
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
        advancedSettingsContentPanel.setVisible(false); // 默认隐藏

        // 创建主面板，包含复选框和内容面板
        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showAdvancedSettingsCheckBox, "settings.advanced.settings.show.hint"))
            .addComponent(advancedSettingsContentPanel)
            .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setBorder(new TitledBorder(AICommonBundle.message("settings.advanced.config")));
        return wrapper;
    }

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

    private JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(checkBox, BorderLayout.WEST);

        JBLabel hintLabel = new JBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        // 保存映射关系，用于后续更新颜色
        checkBoxHintLabelMap.put(checkBox, hintLabel);

        // 根据复选框状态设置提示文本颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());

        // 监听复选框状态变化，动态更新提示文本颜色
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        return panel;
    }

    private void updateHintLabelColor(JBLabel hintLabel, boolean selected) {
        if (selected) {
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    private void updateCheckBoxHintColors() {
        // 更新所有复选框的提示文本颜色
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) -> {
            updateHintLabelColor(hintLabel, checkBox.isSelected());
        });
    }


    private void setupListeners() {
        providerComboBox.addActionListener(e -> {
            updateModelList();
            loadDefaultProviderConfig();
        });

        showAvailableProvidersCheckBox.addActionListener(e -> {
            availableProvidersPanel.setVisible(showAvailableProvidersCheckBox.isSelected());
        });

        showAdvancedSettingsCheckBox.addActionListener(e -> {
            if (advancedSettingsContentPanel != null) {
                advancedSettingsContentPanel.setVisible(showAdvancedSettingsCheckBox.isSelected());
            }
        });

        performanceModeCheckBox.addActionListener(e -> updatePerformanceModeSubConfigEnabled());

        testConnectionButton.addActionListener(e -> testConnection());
        refreshModelsButton.addActionListener(e -> refreshModels());
    }

    private void updatePerformanceModeSubConfigEnabled() {
        boolean enabled = performanceModeCheckBox.isSelected();
        showProviderStatisticsCheckBox.setEnabled(enabled);
        showAvailableProvidersCheckBox.setEnabled(enabled);
        if (!enabled) {
            showProviderStatisticsCheckBox.setSelected(false);
            showAvailableProvidersCheckBox.setSelected(false);
            availableProvidersPanel.setVisible(false);
        }

        // 更新提示文本颜色
        JBLabel statisticsHintLabel = checkBoxHintLabelMap.get(showProviderStatisticsCheckBox);
        if (statisticsHintLabel != null) {
            if (enabled) {
                updateHintLabelColor(statisticsHintLabel, showProviderStatisticsCheckBox.isSelected());
            } else {
                statisticsHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }

        JBLabel availableProvidersHintLabel = checkBoxHintLabelMap.get(showAvailableProvidersCheckBox);
        if (availableProvidersHintLabel != null) {
            if (enabled) {
                updateHintLabelColor(availableProvidersHintLabel, showAvailableProvidersCheckBox.isSelected());
            } else {
                availableProvidersHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
    }

    private void updateModelList() {
        AIProviderType providerType = resolveSelectedProviderType();
        List<String> models = providerType.getSupportedModels();
        modelComboBox.removeAllItems();
        models.forEach(modelComboBox::addItem);
        if (!models.isEmpty()) {
            modelComboBox.setSelectedItem(models.get(0));
        }
        updateBaseUrlEditable(providerType);
        updateApiKeyEnabled(providerType);
    }

    private void loadDefaultProviderConfig() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        modelComboBox.setSelectedItem(config.modelName);
        baseUrlField.setText(config.baseUrl);
        configurationVerified = config.configurationVerified;
        updateTestButtonState();
        loadApiKeyAsync(config.credentialId, providerType.getProviderId());
    }

    private void loadApiKeyAsync(@Nullable String credentialId, @NotNull String expectedProviderId) {
        apiKeyField.setText("");
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        credentialManager.loadApiKeyAsync(credentialId, key -> {
            String currentProviderId = resolveSelectedProviderType().getProviderId();
            if (!Objects.equals(currentProviderId, expectedProviderId)) {
                return;
            }
            apiKeyField.setText(key != null ? key : "");
            updateTestButtonState();
        });
    }

    private void testConnection() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderSettings snapshot = workingSettings.copy();
        snapshot.providerType = providerType;
        AIProviderConfig config = snapshot.getDefaultProviderConfig(providerType);
        config.modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        config.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        config.updateCredentialId(getCurrentApiKey());
        snapshot.updateDefaultProviderConfig(providerType, config);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(config, snapshot.modelParameters, snapshot.runtimeSettings);
            if (provider == null) {
                JOptionPane.showMessageDialog(mainPanel,
                                              AICommonBundle.message("settings.error.provider.create.failed.details"),
                                              AICommonBundle.message("settings.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                                          AICommonBundle.message("settings.error.provider.create.failed"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        testConnectionButton.setEnabled(false);
        testConnectionButton.setText(AICommonBundle.message("settings.test.connection.testing"));
        testConnectionButton.setIcon(createStatusDotIcon(Gray._158));

        new Thread(() -> {
            try {
                ValidationResult result = provider.validateConfiguration(getCurrentApiKey());
                SwingUtilities.invokeLater(() -> {
                    if (result.isSuccess()) {
                        configurationVerified = true;
                        updateTestButtonState();
                        addAvailableProvider(config, providerType);
                        JOptionPane.showMessageDialog(mainPanel,
                                                      result.getMessage(),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        configurationVerified = false;
                        updateTestButtonState();
                        removeAvailableProvider(config.credentialId);
                        JOptionPane.showMessageDialog(mainPanel,
                                                      result.getFullErrorMessage(),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    configurationVerified = false;
                    updateTestButtonState();
                    removeAvailableProvider(config.credentialId);
                    JOptionPane.showMessageDialog(mainPanel,
                                                  AICommonBundle.message("settings.test.connection.error", e.getMessage()),
                                                  AICommonBundle.message("settings.test.result.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    testConnectionButton.setText(AICommonBundle.message("settings.test.connection"));
                    testConnectionButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void refreshModels() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderSettings snapshot = workingSettings.copy();
        snapshot.providerType = providerType;
        AIProviderConfig config = snapshot.getDefaultProviderConfig(providerType);
        // 确保 providerType 被正确设置
        config.providerType = providerType;
        config.modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        config.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        config.updateCredentialId(getCurrentApiKey());
        snapshot.updateDefaultProviderConfig(providerType, config);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(config, snapshot.modelParameters, snapshot.runtimeSettings);
            if (provider == null) {
                JOptionPane.showMessageDialog(mainPanel,
                                              AICommonBundle.message("settings.error.provider.create.failed"),
                                              AICommonBundle.message("settings.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                                          AICommonBundle.message("settings.error.provider.create.failed.details"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 验证配置
        String baseUrl = normalizeBaseUrl(baseUrlField.getText());
        if (baseUrl.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel,
                                          AICommonBundle.message("settings.error.base.url.missing"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        String apiKey = getCurrentApiKey();
        if (providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            JOptionPane.showMessageDialog(mainPanel,
                                          AICommonBundle.message("settings.error.api.key.missing"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshModelsButton.setEnabled(false);
        refreshModelsButton.setText(AICommonBundle.message("settings.refresh.models.testing"));
        refreshModelsButton.setIcon(createStatusDotIcon(Gray._158));

        new Thread(() -> {
            try {
                List<String> models = provider.getAvailableModels(apiKey);
                models.sort(String::compareToIgnoreCase);
                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();
                    if (!models.isEmpty()) {
                        models.forEach(modelComboBox::addItem);
                        modelComboBox.setSelectedItem(models.get(0));
                        refreshModelsSuccess = true;
                        JOptionPane.showMessageDialog(mainPanel,
                                                      AICommonBundle.message("settings.refresh.models.success", models.size()),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        refreshModelsSuccess = false;
                        // 如果模型列表为空，可能是配置错误或网络问题
                        String errorMessage = AICommonBundle.message("settings.refresh.models.empty");
                        if (providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
                            errorMessage = AICommonBundle.message("settings.error.api.key.missing");
                        } else if (baseUrl.isEmpty()) {
                            errorMessage = AICommonBundle.message("settings.error.base.url.missing");
                        }
                        JOptionPane.showMessageDialog(mainPanel,
                                                      errorMessage,
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.WARNING_MESSAGE);
                    }
                    updateRefreshButtonState();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    refreshModelsSuccess = false;
                    updateRefreshButtonState();
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.trim().isEmpty()) {
                        errorMessage = e.getClass().getSimpleName();
                    }
                    JOptionPane.showMessageDialog(mainPanel,
                                                  AICommonBundle.message("settings.refresh.models.failed", errorMessage),
                                                  AICommonBundle.message("settings.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshModelsButton.setText(AICommonBundle.message("settings.refresh.models"));
                    refreshModelsButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void addAvailableProvider(@NotNull AIProviderConfig config, @NotNull AIProviderType providerType) {
        AIProviderConfig copy = config.copy();
        if (copy.remark == null || copy.remark.isEmpty()) {
            copy.remark = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        }
        copy.providerType = providerType;
        copy.configurationVerified = true;
        workingSettings.addAvailableProvider(copy);
        availableProvidersTableModel.setData(workingSettings.availableProviders);
        showAvailableProvidersCheckBox.setSelected(true);
        availableProvidersPanel.setVisible(true);
    }

    private void removeAvailableProvider(@Nullable String credentialId) {
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        workingSettings.removeAvailableProvider(credentialId);
        availableProvidersTableModel.setData(workingSettings.availableProviders);
    }

    private void removeAvailableProvider(int rowIndex) {
        AIProviderConfig config = availableProvidersTableModel.getProviderConfig(rowIndex);
        if (config == null) {
            return;
        }
        String provider = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message("settings.available" +
                                                                                                                      ".providers.unknown");
        String model = config.modelName != null ? config.modelName : "";
        int result = JOptionPane.showConfirmDialog(mainPanel,
                                                   AICommonBundle.message("settings.available.providers.delete.confirm", provider, model),
                                                   AICommonBundle.message("settings.available.providers.delete.title"),
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            removeAvailableProvider(config.credentialId);
        }
    }

    private void clearAllAvailableProviders() {
        if (workingSettings.availableProviders.isEmpty()) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(mainPanel,
                                                   AICommonBundle.message("settings.available.providers.clear.confirm",
                                                                          workingSettings.availableProviders.size()),
                                                   AICommonBundle.message("settings.available.providers.clear.title"),
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            workingSettings.clearAvailableProviders();
            availableProvidersTableModel.setData(List.of());
        }
    }

    private void updateTestButtonState() {
        if (configurationVerified != null && configurationVerified) {
            testConnectionButton.setIcon(createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            testConnectionButton.setIcon(createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
    }

    private void updateRefreshButtonState() {
        if (refreshModelsButton == null) {
            return;
        }
        if (refreshModelsSuccess == null) {
            refreshModelsButton.setIcon(createStatusDotIcon(new JBColor(new Color(255, 193, 7), new Color(255, 193, 7))));
        } else if (refreshModelsSuccess) {
            refreshModelsButton.setIcon(createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            refreshModelsButton.setIcon(createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
    }

    private void updateApiKeyEnabled(@NotNull AIProviderType providerType) {
        apiKeyField.setEnabled(providerType.requiresApiKey());
        if (!providerType.requiresApiKey()) {
            apiKeyField.setText("");
        }
    }

    private void updateBaseUrlEditable(@NotNull AIProviderType providerType) {
        baseUrlField.setEditable(providerType.isBaseUrlEditable());
        if (!providerType.isBaseUrlEditable()) {
            baseUrlField.setText(providerType.getDefaultBaseUrl());
        }
    }

    private Icon createStatusDotIcon(Color color) {
        int size = 4;
        BufferedImage image = ImageUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    private AIProviderType resolveSelectedProviderType() {
        String displayName = (String) providerComboBox.getSelectedItem();
        AIProviderType type = displayName != null ? AIProviderType.fromDisplayName(displayName) : null;
        return type != null ? type : AIProviderType.QIANWEN;
    }

    @NotNull
    private static String normalizeBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static class ProviderListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String displayName) {
                label.setText(displayName);
            }
            return label;
        }
    }

    private static class ProviderTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label && value instanceof AIProviderConfig config) {
                String displayName = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message(
                    "settings.available.providers.unknown");
                label.setText(displayName);
            }
            return component;
        }
    }

    private static class AvailableProvidersTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            AICommonBundle.message("settings.available.providers.column.provider"),
            AICommonBundle.message("settings.available.providers.column.model"),
            AICommonBundle.message("settings.available.providers.column.remark")
        };
        private final List<AIProviderConfig> data = new ArrayList<>();

        public void setData(List<AIProviderConfig> configs) {
            data.clear();
            configs.forEach(config -> data.add(config.copy()));
            fireTableDataChanged();
        }

        public List<AIProviderConfig> getData() {
            List<AIProviderConfig> copy = new ArrayList<>();
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

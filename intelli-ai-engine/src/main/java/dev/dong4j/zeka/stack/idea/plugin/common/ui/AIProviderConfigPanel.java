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

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
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
import dev.dong4j.zeka.stack.idea.plugin.common.icons.AICommonIcons;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * AI 提供者配置面板
 * <p>
 * 用于配置和管理 AI 提供者 (如通义千问,OpenAI 等) 的相关参数, 包括基础连接信息, 模型选择, 运行时参数, 高级设置等.
 * 提供了图形化界面, 支持测试连接, 刷新模型列表, 保存配置等功能, 适用于集成多种 AI 服务提供商的配置管理.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@SuppressWarnings("D")
public final class AIProviderConfigPanel {

    /** AI 凭证管理器 */
    private final AICredentialManager credentialManager;
    /**
     * 响应监听器
     * <p>
     * 用于接收和处理 AI 响应事件的监听器实例
     */
    private final AIResponseListener responseListener;

    /** 主界面主面板, 用于承载主要功能组件和布局 */
    private JPanel mainPanel;

    /**
     * 提供者下拉选择框
     * <p>
     * 用于选择不同的服务提供者
     */
    private ComboBox<String> providerComboBox;
    /** 下拉框组件, 用于选择模型 */
    private ComboBox<String> modelComboBox;
    /** 基础 URL 输入框 */
    private JBTextField baseUrlField;
    /** API 密钥输入框 */
    private JBPasswordField apiKeyField;
    /** 测试连接按钮 */
    private JButton testConnectionButton;
    /**
     * 刷新模型的按钮
     */
    private JButton refreshModelsButton;

    /** 显示可用提供者的复选框 */
    private JBCheckBox showAvailableProvidersCheckBox;
    /** 可用提供者的面板, 用于展示和选择可用的第三方登录提供商 */
    private JPanel availableProvidersPanel;
    /** 可用服务商说明标签 */
    private JBLabel availableProvidersDescriptionLabel;
    /**
     * 可用提供者表格
     * <p>
     * 用于展示可选的提供者列表
     */
    private JBTable availableProvidersTable;
    /** 可用提供商表格模型 */
    private AvailableProvidersTableModel availableProvidersTableModel;

    // 基础配置组件
    /** 日志详细输出选项复选框 */
    private JBCheckBox verboseLoggingCheckBox;

    // 高级配置组件
    /**
     * 显示高级设置的复选框
     * <p>
     * 用于控制是否显示应用中的高级设置选项
     */
    private JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置内容面板, 用于展示和管理高级配置选项 */
    private JPanel advancedSettingsContentPanel;
    /** 最大重试次数的下拉选择器 */
    private JSpinner maxRetriesSpinner;
    /**
     * 超时设置的旋钮控件
     * <p>
     * 用于设置操作超时时间的用户界面组件
     */
    private JSpinner timeoutSpinner;
    /** 温度设置旋钮控件 */
    private JSpinner temperatureSpinner;
    /** 最大令牌数输入控件 */
    private JSpinner maxTokensSpinner;
    /** 顶部参数的下拉选择器控件 */
    private JSpinner topPSpinner;
    /**
     * 用于输入 topK 值的旋钮控件
     * <p>
     * 用户可通过此控件调整 topK 参数的数值
     */
    private JSpinner topKSpinner;
    /** 出现惩罚值选择器 */
    private JSpinner presencePenaltySpinner;

    // 保存复选框和提示标签的映射关系，用于更新提示文本颜色
    /** checkBoxHintLabelMap 用于存储 CheckBox 和 Label 的映射关系, 实现提示标签与复选框的关联 */
    private final java.util.Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new java.util.HashMap<>();

    /** 配置是否已验证的标志, 用于标识当前配置是否通过验证 */
    private Boolean configurationVerified = Boolean.FALSE;
    /**
     * 表示刷新模型是否成功的标志
     * <p>
     * 用于记录模型刷新操作的执行结果,null 表示尚未执行或结果未知
     */
    private Boolean refreshModelsSuccess = null;

    /** 当前正在使用的 AI 提供商配置信息 */
    private AIProviderSettings workingSettings = new AIProviderSettings();

    /** 监听器是否已设置的标志, 用于防止重复添加监听器 */
    private boolean listenersSetup = false;

    /**
     * 初始化 AI 提供者配置面板
     * <p>
     * 构造函数用于初始化 AI 提供者配置面板, 设置凭证管理器和响应监听器, 并创建 UI 界面和绑定监听事件.
     *
     * @param credentialManager 凭证管理器, 用于管理 AI 相关的凭证信息, 不能为空
     * @param responseListener  响应监听器, 用于接收 AI 调用的响应事件, 可以为 null
     */
    public AIProviderConfigPanel(@NotNull AICredentialManager credentialManager,
                                 @Nullable AIResponseListener responseListener) {
        this.credentialManager = credentialManager;
        this.responseListener = responseListener;
        createUI();
        setupListeners();
    }

    /**
     * 获取主面板组件
     * <p>
     * 返回用于界面展示的主面板对象
     *
     * @return 主面板组件
     */
    @NotNull
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * 初始化设置面板的用户界面组件.
     * <p>
     * 本方法负责创建并配置所有与 AI 设置相关的 UI 控件, 包括提供商选择框, 模型选择框,URL,API 密钥输入框, 测试连接按钮, 刷新模型按钮, 日志级别复选框, 性能模式复选框, 统计信息复选框, 高级设置复选框, 重试次数, 超时, 温度,
     * 最大令牌数,top-p,top-k,presence penalty 等参数的输入框, 以及可用提供商表格和相关工具栏.<p>
     * 该方法在构造器中被调用, 以确保在显示设置面板前所有组件已正确初始化.
     */
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
                int selected = availableProvidersTable.getSelectedRow();
                if (selected >= 0) {
                    removeAvailableProvider(selected);
                }
            })
            .addExtraAction(new AnAction(AICommonBundle.message("settings.available.providers.clear.all"),
                                         AICommonBundle.message("settings.available.providers.clear.all.description"),
                                         AllIcons.Actions.GC) {
                /**
                 * 执行动作时的回调方法.
                 * <p>
                 * 当用户触发对应的操作时, 该方法会被调用并执行,用于清除所有可用的提供者.
                 *
                 * @param e 触发动作的事件对象, 不能为空
                 */
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllAvailableProviders();
                }

                /**
                 * 覆盖父类方法, 获取动作更新线程
                 * <p>
                 * 返回默认的事件调度线程 (EDT), 用于在事件调度线程上执行更新操作
                 *
                 * @return 动作更新线程, 始终返回 EDT
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

        // 初始化高级设置面板的可见性
        if (advancedSettingsContentPanel != null) {
            advancedSettingsContentPanel.setVisible(workingSettings.showAdvancedSettings);
        }

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
     * 加载 AI 提供商设置
     * <p>
     * 将传入的 {@link AIProviderSettings} 对象复制到工作设置, 并更新 UI 组件的状态, 包括下拉框, 文本框, 复选框等.
     *
     * @param settings 要加载的 AI 提供商设置, 不能为空
     */
    public void loadSettings(@NotNull AIProviderSettings settings) {
        this.workingSettings = settings.copy();

        // 使用 lastSelectedProviderType 恢复上次选择的提供商，如果没有则使用 QIANWEN 作为默认值
        AIProviderType defaultProviderType = workingSettings.aiProviderType != null
                                             ? workingSettings.aiProviderType
                                             : AIProviderType.QIANWEN;

        providerComboBox.setSelectedItem(defaultProviderType.getDisplayName());
        updateBasicConnectionInfo();

        // 从 defaultProviders Map 中获取配置，如果没有则使用枚举的默认参数初始化
        AIProviderConfig defaultConfig = workingSettings.getDefaultProviderConfig(defaultProviderType);
        modelComboBox.setSelectedItem(defaultConfig.modelName);
        baseUrlField.setText(defaultConfig.baseUrl);
        configurationVerified = defaultConfig.configurationVerified;
        updateTestButtonState();

        loadApiKeyAsync(defaultConfig.credentialId, defaultProviderType.getProviderId());

        refreshModelsSuccess = null;
        updateRefreshButtonState();

        // 加载基础配置
        AIRuntimeSettings runtimeSettings = workingSettings.runtimeSettings;
        verboseLoggingCheckBox.setSelected(runtimeSettings.verboseLogging);

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
        maxTokensSpinner.setValue(Math.max(0.1d, modelParameters.maxTokens / 1000.0d));
        topPSpinner.setValue(modelParameters.topP);
        topKSpinner.setValue(modelParameters.topK);
        presencePenaltySpinner.setValue(modelParameters.presencePenalty);

        // 加载可用服务商
        availableProvidersTableModel.setData(workingSettings.availableProviders);
        showAvailableProvidersCheckBox.setSelected(workingSettings.showAvailableProviders);
        boolean showAvailableProviders = workingSettings.showAvailableProviders;
        availableProvidersPanel.setVisible(showAvailableProviders);
        if (availableProvidersDescriptionLabel != null) {
            availableProvidersDescriptionLabel.setVisible(showAvailableProviders);
        }
    }

    /**
     * 获取 AI 提供者的配置设置, 并根据当前界面配置进行更新
     * <p>
     * 该方法创建 AIProviderSettings 对象的副本, 设置默认配置, 模型名称, 基础 URL, 配置验证状态,API 密钥等信息, 并更新可用提供者列表和运行时设置.
     *
     * @return 更新后的 AI 提供者配置设置对象
     */
    @NotNull
    public AIProviderSettings getSettings() {
        // 直接修改 workingSettings，不需要 copy，因为 applyFrom() 会再次 copy
        AIProviderType providerType = resolveSelectedProviderType();

        // 保存当前编辑的供应商配置到 defaultProviders Map
        AIProviderConfig defaultConfig = workingSettings.getDefaultProviderConfig(providerType);
        String modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        defaultConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        defaultConfig.baseUrl = normalizeBaseUrl(baseUrlField.getText().trim());
        defaultConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);

        // 保存当前编辑的默认提供商的 API Key
        updateCredentialIdAndSaveApiKey(defaultConfig);
        workingSettings.updateDefaultProviderConfig(providerType, defaultConfig);

        // 保存可用提供商列表（它们的 API Key 已经在 addAvailableProvider 时保存了）
        workingSettings.availableProviders.clear();
        availableProvidersTableModel.getData().forEach(workingSettings::addAvailableProvider);

        // 保存基础配置
        AIRuntimeSettings runtimeSettings = workingSettings.runtimeSettings;
        runtimeSettings.verboseLogging = verboseLoggingCheckBox.isSelected();
        runtimeSettings.maxRetries = ((Number) maxRetriesSpinner.getValue()).intValue();
        runtimeSettings.timeout = ((Number) timeoutSpinner.getValue()).intValue();
        workingSettings.showAvailableProviders = showAvailableProvidersCheckBox.isSelected();

        // 保存高级配置
        workingSettings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();

        AIModelParameters modelParameters = workingSettings.modelParameters;
        modelParameters.temperature = ((Number) temperatureSpinner.getValue()).doubleValue();
        double maxTokensInK = ((Number) maxTokensSpinner.getValue()).doubleValue();
        modelParameters.maxTokens = (int) Math.max(100, Math.round(maxTokensInK * 1000));
        modelParameters.topP = ((Number) topPSpinner.getValue()).doubleValue();
        modelParameters.topK = ((Number) topKSpinner.getValue()).intValue();
        modelParameters.presencePenalty = ((Number) presencePenaltySpinner.getValue()).doubleValue();

        // 保存最后选中的提供商类型
        workingSettings.aiProviderType = providerType;

        return workingSettings;
    }

    /**
     * 检查当前设置是否与基准设置不同
     * <p>
     * 通过比较当前设置与传入的基准设置, 判断是否有修改
     *
     * @param baseline 用于比较的基准设置
     * @return 如果当前设置与基准设置不同则返回 true, 否则返回 false
     */
    public boolean isModified(@NotNull AIProviderSettings baseline) {
        AIProviderSettings latest = getSettings();
        // 使用 contentEquals 进行完整比较，包括基础配置和高级配置
        return !latest.contentEquals(baseline);
    }

    /**
     * 获取当前 API 密钥
     * <p>
     * 从密码字段中获取当前存储的 API 密钥并返回其字符串表示形式, 去除前后空格.
     *
     * @return 当前 API 密钥的字符串形式
     * @since 1.0
     */
    @NotNull
    public String getCurrentApiKey() {
        return new String(apiKeyField.getPassword()).trim();
    }

    /**
     * 创建连接配置面板
     * <p>
     * 构建并返回一个包含提供者选择框, 基础 URL 输入框,API 密钥输入区域, 模型选择框以及测试连接按钮的面板.
     * 面板使用表单布局, 并添加了相应的标签和组件.
     *
     * @return 包含连接配置组件的面板
     */
    private JPanel createConnectionPanel() {
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

    /**
     * 创建可用服务商面板
     * <p>
     * 创建一个独立的面板用于显示和管理已验证的可用服务商列表。
     * 包含详细的说明信息和可用的服务商列表表格。
     *
     * @return 可用服务商面板
     */
    private JPanel createAvailableProvidersPanel() {
        // 创建说明标签
        availableProvidersDescriptionLabel = new JBLabel();
        String descriptionText = AICommonBundle.message("settings.available.providers.description");
        // 将 \n 替换为 HTML 换行
        descriptionText = "<html>" + descriptionText.replace("\n", "<br>") + "</html>";
        availableProvidersDescriptionLabel.setText(descriptionText);
        availableProvidersDescriptionLabel.setFont(availableProvidersDescriptionLabel.getFont().deriveFont(availableProvidersDescriptionLabel.getFont().getSize() - 1f));
        availableProvidersDescriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        availableProvidersDescriptionLabel.setBorder(JBUI.Borders.empty(5, 0, 10, 0));
        // 初始状态：根据复选框状态设置可见性
        availableProvidersDescriptionLabel.setVisible(false);

        // 创建内容面板
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showAvailableProvidersCheckBox, "settings.show.available.providers.hint"))
            .addComponent(availableProvidersDescriptionLabel)
            .addComponent(availableProvidersPanel)
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.setBorder(new TitledBorder(
            BorderFactory.createEtchedBorder(),
            AICommonBundle.message("settings.show.available.providers")
        ));

        return panel;
    }

    /**
     * 创建基本配置面板
     * <p>
     * 用于构建包含日志详细设置的面板, 并将其包装在带有边框的容器中返回
     *
     * @return 包含基本配置项的面板
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
     * 该方法构建一个包含多个可配置参数的面板, 包括最大重试次数, 超时时间, 最大令牌数, 温度值,Top P,Top K, 存在惩罚等设置项.
     * 面板中还包含一个显示 / 隐藏高级设置的复选框.
     *
     * @return 包含高级设置内容的面板
     */
    private JPanel createAdvancedPanel() {
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

    /**
     * 创建一个包含 {@link JSpinner} 与提示标签的面板.
     * <p>
     * 该方法将给定的 {@code JSpinner} 放置在面板的左侧, 并在右侧添加一个 {@link JBLabel},
     * 该标签显示由 {@code hintKey} 指定的提示文本. 提示标签的字体会略微减小, 颜色设置为
     * “Label.disabledForeground”, 并且宽度固定为 300 像素, 以便在 UI 中保持一致的布局.
     *
     * @param spinner 需要显示的 {@link JSpinner} 组件
     * @param hintKey 用于从 {@link AICommonBundle} 资源文件中获取提示文本的键
     * @return 包含 spinner 与提示标签的 {@link JPanel}
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
     * 创建带有提示信息的复选框面板
     * <p>
     * 该方法创建一个包含复选框和提示标签的面板, 用于在用户界面中显示带提示的复选框组件.
     *
     * @param checkBox 要添加到面板中的复选框组件
     * @param hintKey  提示信息对应的资源键, 用于从资源文件中获取提示文本
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

        // 保存映射关系，用于后续更新颜色
        checkBoxHintLabelMap.put(checkBox, hintLabel);

        // 根据复选框状态设置提示文本颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());

        // 监听复选框状态变化，动态更新提示文本颜色
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        return panel;
    }

    /**
     * 更新提示标签的颜色
     * <p>
     * 根据传入的选中状态, 设置提示标签的前景色. 如果选中为 true, 则使用普通标签前景色; 否则使用禁用标签前景色.
     *
     * @param hintLabel 提示标签对象
     * @param selected  表示标签是否被选中,true 为选中,false 为未选中
     */
    private void updateHintLabelColor(JBLabel hintLabel, boolean selected) {
        if (selected) {
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    /**
     * 更新所有复选框提示标签的颜色
     * <p>
     * 遍历复选框与提示标签的映射关系, 根据复选框的选中状态更新对应提示标签的颜色
     */
    private void updateCheckBoxHintColors() {
        // 更新所有复选框的提示文本颜色
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) -> updateHintLabelColor(hintLabel, checkBox.isSelected()));
    }


    /**
     * 初始化各种监听器, 用于响应用户界面组件的事件
     * <p>
     * 为组合框, 复选框和按钮等组件添加动作监听器, 以实现界面交互功能
     * 使用 listenersSetup 标志确保监听器只添加一次, 避免重复添加
     */
    private void setupListeners() {
        if (listenersSetup) {
            return; // 防止重复添加监听器
        }
        listenersSetup = true;

        providerComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // 处理切换之后的提供者信息
                updateBasicConnectionInfo();
                loadDefaultProviderConfig();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                // 保存切换之前的提供者信息
                saveCurrentProviderConfig(String.valueOf(e.getItem()));
            }
        });

        showAvailableProvidersCheckBox.addActionListener(e -> {
            boolean selected = showAvailableProvidersCheckBox.isSelected();
            availableProvidersPanel.setVisible(selected);
            if (availableProvidersDescriptionLabel != null) {
                availableProvidersDescriptionLabel.setVisible(selected);
            }
        });

        showAdvancedSettingsCheckBox.addActionListener(e -> {
            if (advancedSettingsContentPanel != null) {
                advancedSettingsContentPanel.setVisible(showAdvancedSettingsCheckBox.isSelected());
            }
        });

        testConnectionButton.addActionListener(e -> testConnection());
        refreshModelsButton.addActionListener(e -> refreshModels());
    }

    /**
     * 更新连接信息的基本配置
     * <p>
     * 解析当前选中的提供商类型, 重新加载支持的模型列表到下拉框中, 并根据当前选中的模型或默认模型设置选中项.
     * 同时更新基础 URL 的可编辑状态和 API 密钥的启用状态.
     */
    private void updateBasicConnectionInfo() {
        AIProviderType providerType = resolveSelectedProviderType();

        // 保存当前输入的模型名称
        String currentModel = (String) modelComboBox.getSelectedItem();

        // 清空并添加推荐的模型列表（仅作为参考）
        modelComboBox.removeAllItems();
        for (String model : providerType.getSupportedModels()) {
            modelComboBox.addItem(model);
        }

        // 恢复用户之前输入的值，如果为空则使用默认值
        if (currentModel != null && !currentModel.trim().isEmpty()) {
            modelComboBox.setSelectedItem(currentModel);
        } else {
            modelComboBox.setSelectedItem(providerType.getDefaultModel());
        }

        // 加载配置到 UI
        baseUrlField.setText(providerType.getDefaultBaseUrl());

        updateBaseUrlEditable(providerType);
        updateApiKeyEnabled(providerType);
    }

    /**
     * 保存当前编辑的提供商配置到 defaultProviders Map
     * <p>
     * 在切换供应商之前调用, 确保当前编辑的配置（包括 API Key）不会丢失
     * 配置会保存到 workingSettings.defaultProviders Map 中，持久化时会自动保存
     */
    private void saveCurrentProviderConfig(String displayName) {
        AIProviderType providerType = resolveSelectedProviderType(displayName);
        // 从 defaultProviders Map 中获取配置，如果没有则使用枚举的默认参数初始化
        AIProviderConfig currentConfig = workingSettings.getDefaultProviderConfig(providerType);

        // 保存当前编辑的模型名称和基础 URL
        String modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        currentConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        currentConfig.baseUrl = normalizeBaseUrl(StringUtils.isBlank(baseUrlField.getText().trim())
                                                 ? providerType.getDefaultBaseUrl()
                                                 : baseUrlField.getText().trim());
        currentConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);

        // 保存当前编辑的 API Key（只有在需要 API Key 且不为空时才保存）
        String currentApiKey = getCurrentApiKey();
        if (providerType.requiresApiKey() && !currentApiKey.trim().isEmpty()) {
            updateCredentialIdAndSaveApiKey(currentConfig);
        }
        // 更新到 defaultProviders Map 中，持久化时会自动保存
        workingSettings.updateDefaultProviderConfig(providerType, currentConfig);
    }

    /**
     * 加载默认的 AI 服务提供商配置信息
     * <p>
     * 从 defaultProviders Map 中加载当前选中的 AI 服务提供商的配置, 并更新相关界面组件的状态.
     * 如果 Map 中没有该配置，则使用枚举的默认参数初始化
     */
    private void loadDefaultProviderConfig() {
        AIProviderType providerType = resolveSelectedProviderType();
        // 从 defaultProviders Map 中获取配置，如果没有则使用枚举的默认参数初始化
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        modelComboBox.setSelectedItem(config.modelName);
        baseUrlField.setText(config.baseUrl);
        configurationVerified = config.configurationVerified;
        updateTestButtonState();
        loadApiKeyAsync(config.credentialId, providerType.getProviderId());
    }

    /**
     * 异步加载指定凭证 ID 的 API 密钥并更新界面
     * <p>
     * 如果提供的凭证 ID 为空或无效, 则直接返回. 否则, 异步加载 API 密钥, 并在加载完成后检查当前选中的提供商 ID 是否与预期匹配. 若匹配, 则更新 API 密钥显示字段并刷新测试按钮状态.
     *
     * @param credentialId       凭证 ID, 可能为 null
     * @param expectedProviderId 预期的提供商 ID, 不能为空
     */
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

    /**
     * 测试与 AI 服务提供者的连接状态
     * <p>
     * 该方法用于验证当前配置是否能够成功连接到 AI 服务提供者. 首先解析选中的服务类型, 复制当前的工作设置并更新默认配置信息. 然后尝试创建 AI 服务提供者实例, 若创建失败则弹出错误提示. 若创建成功, 则在后台线程中验证配置,
     * 并根据验证结果更新界面状态和提示信息. 最后无论是否成功, 都会重新启用测试连接按钮.
     *
     * @since 1.0
     */
    private void testConnection() {
        AIProviderType providerType = resolveSelectedProviderType();
        // 直接使用 workingSettings，不需要 copy，因为只是读取配置
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        // 创建临时配置用于测试，不修改 workingSettings
        AIProviderConfig testConfig = config.copy();
        testConfig.modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        testConfig.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        testConfig.updateCredentialId(getCurrentApiKey());

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(testConfig, workingSettings.modelParameters, workingSettings.runtimeSettings);
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
                        addAvailableProvider(testConfig, providerType);
                        JOptionPane.showMessageDialog(mainPanel,
                                                      result.getMessage(),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        configurationVerified = false;
                        updateTestButtonState();
                        removeAvailableProvider(testConfig.credentialId);
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
                    removeAvailableProvider(testConfig.credentialId);
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

    /**
     * 刷新可用模型列表
     * <p>
     * 该方法根据当前选中的 AI 提供商类型, 配置以及凭证信息, 创建 {@link AIServiceProvider} 实例并请求可用模型列表.
     * 在请求过程中会对输入的基础 URL,API Key 等必要信息进行校验, 并在出现错误时通过对话框提示用户.
     * 成功获取模型后会更新 {@link #modelComboBox} 的内容, 并根据默认模型或当前选中模型进行选择.
     * 同时会根据请求结果更新刷新按钮的状态与文本, 确保 UI 与后台状态保持同步.
     * <p>
     * 该方法内部使用多线程执行网络请求, 避免阻塞 UI 线程, 并在完成后通过 {@link SwingUtilities#invokeLater} 更新 UI.
     *
     * @since 1.0
     */
    private void refreshModels() {
        AIProviderType providerType = resolveSelectedProviderType();
        // 直接使用 workingSettings，不需要 copy，因为只是读取配置
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        // 创建临时配置用于刷新，不修改 workingSettings
        AIProviderConfig refreshConfig = config.copy();
        refreshConfig.providerType = providerType;
        refreshConfig.modelName = Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
        refreshConfig.baseUrl = normalizeBaseUrl(baseUrlField.getText());
        refreshConfig.updateCredentialId(getCurrentApiKey());

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(refreshConfig, workingSettings.modelParameters, workingSettings.runtimeSettings);
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
        if (providerType.requiresApiKey() && apiKey.trim().isEmpty()) {
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
                // 保存当前选择的模型名称
                String currentModelName = getSelectedModelName();
                List<String> models = provider.getAvailableModels(apiKey);
                models.sort(String::compareToIgnoreCase);
                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();
                    if (!models.isEmpty()) {
                        models.forEach(modelComboBox::addItem);
                        // 优先选择默认模型名称，如果不存在则选择第一个
                        String defaultModelName = refreshConfig.modelName;
                        if (defaultModelName != null && !defaultModelName.trim().isEmpty() && models.contains(defaultModelName)) {
                            modelComboBox.setSelectedItem(defaultModelName);
                        } else if (!currentModelName.trim().isEmpty() && models.contains(currentModelName)) {
                            modelComboBox.setSelectedItem(currentModelName);
                        } else {
                            modelComboBox.setSelectedItem(models.get(0));
                        }
                        refreshModelsSuccess = true;
                        JOptionPane.showMessageDialog(mainPanel,
                                                      AICommonBundle.message("settings.refresh.models.success", models.size()),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        refreshModelsSuccess = false;
                        // 如果模型列表为空，可能是配置错误或网络问题
                        String errorMessage = AICommonBundle.message("settings.refresh.models.empty");
                        if (providerType.requiresApiKey() && apiKey.trim().isEmpty()) {
                            errorMessage = AICommonBundle.message("settings.error.api.key.missing");
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

    /**
     * 添加可用的 AI 服务提供商配置
     * <p>
     * 将传入的 AI 服务提供商配置进行复制, 并设置备注信息 (若为空则使用当前时间), 然后设置服务类型和配置验证状态, 最后将配置添加到可用提供商列表中, 并更新相关界面显示.
     *
     * @param config       要添加的 AI 服务提供商配置对象
     * @param providerType AI 服务提供商类型
     */
    private void addAvailableProvider(@NotNull AIProviderConfig config, @NotNull AIProviderType providerType) {
        AIProviderConfig copy = config.copy();
        if (copy.remark == null || copy.remark.isEmpty()) {
            copy.remark = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        }
        copy.providerType = providerType;
        copy.configurationVerified = true;

        // 确保 credentialId 已设置，并保存 API Key 到 credentialManager
        updateCredentialIdAndSaveApiKey(copy);

        // 同时更新工作设置和全局实例，确保第三方插件能够立即看到变更
        workingSettings.addAvailableProvider(copy);
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.addAvailableProvider(copy);

        availableProvidersTableModel.setData(workingSettings.availableProviders);
        showAvailableProvidersCheckBox.setSelected(true);
        availableProvidersPanel.setVisible(true);
    }

    /**
     * 更新配置的凭证 ID 并保存 API Key
     * <p>
     * 获取当前输入的 API Key，更新配置的 credentialId，并将 API Key 保存到 credentialManager 中。
     *
     * @param config 要更新的 AI 提供商配置对象
     */
    private void updateCredentialIdAndSaveApiKey(@NotNull AIProviderConfig config) {
        String apiKey = getCurrentApiKey();
        config.updateCredentialId(apiKey);
        if (!apiKey.trim().isEmpty() && config.credentialId != null) {
            credentialManager.setApiKey(config.credentialId, apiKey);
        }
    }

    /**
     * 移除指定凭证 ID 的可用提供商
     * <p>
     * 如果提供的凭证 ID 为 null 或为空字符串, 则直接返回. 否则从工作设置中移除对应的可用提供商, 并更新数据表模型.
     *
     * @param credentialId 要移除的提供商的凭证 ID, 可以为 null
     */
    private void removeAvailableProvider(@Nullable String credentialId) {
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        // 同时更新工作设置和全局实例，确保第三方插件能够立即看到变更
        workingSettings.removeAvailableProvider(credentialId);
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.removeAvailableProvider(credentialId);

        availableProvidersTableModel.setData(workingSettings.availableProviders);
    }

    /**
     * 根据指定行索引移除可用的提供商配置
     * <p>
     * 从表格模型中获取对应行的提供商配置, 若配置存在则显示确认对话框, 确认后执行实际的移除操作.
     *
     * @param rowIndex 表格中提供商配置所在的行索引
     * @throws IllegalArgumentException 如果行索引无效或配置不存在 (可能已提前处理)
     */
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

    /**
     * 清除所有可用的提供者设置
     * <p>
     * 如果当前可用提供者列表为空, 则直接返回. 否则弹出确认对话框, 确认后清除所有可用提供者并刷新表格数据.
     *
     * @since 1.0
     */
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
            // 同时更新工作设置和全局实例，确保第三方插件能够立即看到变更
            workingSettings.clearAvailableProviders();
            AIProviderSettings globalSettings = AIProviderSettings.getInstance();
            globalSettings.clearAvailableProviders();

            availableProvidersTableModel.setData(List.of());
        }
    }

    /**
     * 更新测试按钮的状态图标
     * <p>
     * 根据 {@code configurationVerified} 的值来决定按钮显示的图标颜色:
     * <ul>
     *   <li> 若 {@code configurationVerified} 为 {@code true}, 则设置为绿色状态点图标.</li>
     *   <li> 若 {@code configurationVerified} 为 {@code false} 或 {@code null}, 则设置为红色状态点图标.</li>
     * </ul>
     * 该方法不返回任何值, 也不抛出异常.
     */
    private void updateTestButtonState() {
        if (configurationVerified != null && configurationVerified) {
            testConnectionButton.setIcon(createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            testConnectionButton.setIcon(createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
    }

    /**
     * 更新刷新按钮的状态图标
     * <p>
     * 根据刷新操作的成功状态, 设置按钮的图标颜色. 若 refreshModelsSuccess 为 null, 则使用默认颜色; 若为 true, 使用成功颜色; 若为 false, 使用失败颜色.
     */
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

    /**
     * 根据指定的 AI 服务提供商类型更新 API 密钥字段的启用状态和文本内容
     * <p>
     * 如果该提供商类型不需要 API 密钥, 则禁用 API 密钥字段并清空其文本内容.
     *
     * @param providerType AI 服务提供商类型
     */
    private void updateApiKeyEnabled(@NotNull AIProviderType providerType) {
        apiKeyField.setEnabled(providerType.requiresApiKey());
        if (!providerType.requiresApiKey()) {
            apiKeyField.setText("");
        }
    }

    /**
     * 根据 AI 提供商类型更新基础 URL 字段的可编辑状态.
     * <p>
     * 若 {@link AIProviderType#isBaseUrlEditable()} 返回 {@code true}, 则保持 {@code baseUrlField} 可编辑;
     * 否则将其设为不可编辑并填充默认基础 URL.
     *
     * @param providerType AI 提供商类型, 不能为空
     */
    private void updateBaseUrlEditable(@NotNull AIProviderType providerType) {
        baseUrlField.setEditable(providerType.isBaseUrlEditable());
        if (!providerType.isBaseUrlEditable()) {
            baseUrlField.setText(providerType.getDefaultBaseUrl());
        }
    }

    /**
     * 创建一个状态点图标, 用于表示某种状态
     * <p>
     * 该方法使用指定颜色生成一个尺寸为 4x4 像素的图标, 图标内容为一个填充的圆形.
     *
     * @param color 图标颜色
     * @return 状态点图标
     */
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

    /**
     * 解析用户选择的 AI 提供者类型
     * <p>
     * 从下拉框中获取选中的显示名称, 并转换为对应的 AIProviderType 枚举值.
     * 如果显示名称为空或无法转换, 则返回默认的 AIProviderType.QIANWEN.
     *
     * @return 解析后的 AI 提供者类型, 若无法解析则返回 QIANWEN 作为默认值
     */
    private AIProviderType resolveSelectedProviderType() {
        String displayName = (String) providerComboBox.getSelectedItem();
        AIProviderType type = displayName != null ? AIProviderType.fromDisplayName(displayName) : null;
        return type != null ? type : AIProviderType.QIANWEN;
    }

    private AIProviderType resolveSelectedProviderType(String displayName) {
        AIProviderType type = displayName != null ? AIProviderType.fromDisplayName(displayName) : null;
        return type != null ? type : AIProviderType.QIANWEN;
    }

    /**
     * 获取当前选中的模型名称
     * <p>
     * 从下拉框中获取当前选中的模型名称, 若下拉框未选择项, 则从编辑器中获取.
     * 若仍未获取到名称, 则根据当前选中的提供者类型获取默认配置中的模型名称.
     *
     * @return 当前选中的模型名称
     */
    @NotNull
    private String getSelectedModelName() {
        Object selected = modelComboBox.getSelectedItem();
        if (selected != null) {
            return selected.toString().trim();
        }
        Object editorItem = modelComboBox.getEditor().getItem();
        if (editorItem != null) {
            return editorItem.toString().trim();
        }
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        return config.modelName != null ? config.modelName : "";
    }

    /**
     * 对基础 URL 进行规范化处理, 去除末尾的斜杠
     * <p>
     * 如果传入的 URL 为 null 或为空字符串, 则返回空字符串. 如果 URL 以斜杠结尾, 则移除最后一个斜杠; 否则返回原 URL.
     *
     * @param baseUrl 需要规范化的基础 URL, 可以为 null
     * @return 规范化后的基础 URL, 若输入为 null 或空则返回空字符串
     */
    @NotNull
    private static String normalizeBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 提供列表单元格渲染功能的内部类
     * <p>
     * 继承自 {@code DefaultListCellRenderer}, 用于自定义 {@code JList} 中单元格的显示样式, 主要处理 {@code AIProviderType} 对应的图标和显示名称的渲染
     *
     * @author 系统生成
     * @version 1.0.0
     * @date 2025.10.24
     * @since 1.0.0
     */
    private static class ProviderListCellRenderer extends DefaultListCellRenderer {
        /**
         * 为 {@link JList} 提供自定义的单元格渲染器.
         * <p>
         * 该实现会将列表项的文本设置为 {@code value} 的字符串表示, 并根据该字符串
         * 通过 {@link AIProviderType#fromDisplayName(String)} 解析对应的 AI 提供商类型,
         * 若解析成功则为标签设置相应的图标 {@link AICommonIcons#getProviderIcon(AIProviderType)}.
         * </p>
         *
         * @param list         正在渲染的 {@link JList} 对象
         * @param value        当前列表项的值, 若为 {@link String} 则会被视为显示名称
         * @param index        当前列表项的索引
         * @param isSelected   当前列表项是否被选中
         * @param cellHasFocus 当前列表项是否具有焦点
         * @return 用于渲染列表项的 {@link Component}, 通常为 {@link JLabel}
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String displayName) {
                label.setText(displayName);
                // 设置图标
                AIProviderType providerType = AIProviderType.fromDisplayName(displayName);
                if (providerType != null) {
                    Icon icon = AICommonIcons.getProviderIcon(providerType);
                    if (icon != null) {
                        label.setIcon(icon);
                    }
                }
            }
            return label;
        }
    }

    /**
     * 表格单元格渲染器, 用于自定义 AI 提供者配置信息的显示样式
     * <p>
     * 该渲染器继承自 DefaultTableCellRenderer, 用于在表格中渲染 AI 提供者配置信息, 包括显示名称和图标.
     * 当单元格内容为 AIProviderConfig 类型时, 会根据配置信息设置显示名称和对应的图标.
     *
     * @author 未知
     * @version 1.0.0
     * @date 2025.10.24
     * @since 1.0.0
     */
    private static class ProviderTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        /**
         * 自定义表格单元格渲染器, 用于显示 AI 提供者的名称和图标
         * <p>
         * 该方法重写父类的渲染逻辑, 根据单元格的值设置显示名称和对应的图标.
         *
         * @param table      表格组件
         * @param value      单元格的值, 应为 AIProviderConfig 类型
         * @param isSelected 单元格是否被选中
         * @param hasFocus   单元格是否获得焦点
         * @param row        行索引
         * @param column     列索引
         * @return 渲染后的组件, 通常是 JLabel
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label && value instanceof AIProviderConfig config) {
                String displayName = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message(
                    "settings.available.providers.unknown");
                label.setText(displayName);
                // 设置图标
                if (config.providerType != null) {
                    Icon icon = AICommonIcons.getProviderIcon(config.providerType);
                    if (icon != null) {
                        label.setIcon(icon);
                    }
                }
            }
            return component;
        }
    }

    /**
     * 可用 AI 提供商表格模型
     * <p>
     * 该类继承自 {@link javax.swing.table.AbstractTableModel}, 用于在 UI 中展示和编辑
     * {@link AIProviderConfig} 对象列表. 表格包含三列: 提供商, 模型名称和备注, 其中
     * 备注列可编辑. 所有数据均采用深拷贝方式存储, 保证外部修改不会影响内部状态.
     * <p>
     * 主要功能包括:
     * <ul>
     *   <li> 设置完整配置列表 ({@link #setData(java.util.List)})</li>
     *   <li> 获取当前配置列表 ({@link #getData()})</li>
     *   <li> 按行索引获取单个配置 ({@link #getProviderConfig(int)})</li>
     *   <li> 支持单元格编辑, 仅允许备注列被修改 </li>
     * </ul>
     * <p>
     * 该模型常用于 AI 配置管理界面, 配合 {@link javax.swing.JTable} 使用, 可实现
     * 动态展示, 编辑和保存 AI 提供商信息.
     *
     * @author dong4j
     * @version 1.0.0
     * @date 2025.10.24
     * @since 1.0.0
     */
    private static class AvailableProvidersTableModel extends AbstractTableModel {
        /**
         * 表格列的名称数组
         * <p>
         * 包含提供者, 模型和备注三列的国际化显示名称
         */
        private final String[] columnNames = {
            AICommonBundle.message("settings.available.providers.column.provider"),
            AICommonBundle.message("settings.available.providers.column.model"),
            AICommonBundle.message("settings.available.providers.column.remark")
        };
        /** AIProviderConfig 列表 */
        private final List<AIProviderConfig> data = new ArrayList<>();

        /**
         * 设置新的配置数据列表
         * <p>
         * 清除当前数据, 并将传入的配置列表复制后添加到当前数据中, 最后通知表格数据已更改.
         *
         * @param configs 要设置的配置数据列表
         * @throws NullPointerException 如果传入的 configs 为 null
         */
        public void setData(List<AIProviderConfig> configs) {
            data.clear();
            configs.forEach(config -> data.add(config.copy()));
            fireTableDataChanged();
        }

        /**
         * 获取 AI 提供者配置数据的副本
         * <p>
         * 创建并返回当前配置数据的深拷贝, 确保原始数据不被修改
         *
         * @return AI 提供者配置数据的副本列表
         */
        public List<AIProviderConfig> getData() {
            List<AIProviderConfig> copy = new ArrayList<>();
            data.forEach(config -> copy.add(config.copy()));
            return copy;
        }

        /**
         * 根据索引获取 AIProviderConfig 配置
         * <p>
         * 该方法根据传入的索引值从内部数据列表中检索对应的 {@link AIProviderConfig} 对象.
         * 若索引在合法范围内 (0 ≤ index < data.size()), 则返回对应的配置; 否则返回 {@code null}.
         *
         * @param index 配置列表中的索引位置
         * @return 指定索引处的 {@link AIProviderConfig} 对象, 若索引无效则返回 {@code null}
         */
        public AIProviderConfig getProviderConfig(int index) {
            if (index >= 0 && index < data.size()) {
                return data.get(index);
            }
            return null;
        }

        /**
         * 获取数据行数
         * <p>
         * 返回数据集合中的元素数量
         *
         * @return 数据行数
         */
        @Override
        public int getRowCount() {
            return data.size();
        }

        /**
         * 获取列的数量
         * <p>
         * 返回当前列名数组中的列数, 即表格的列数.
         *
         * @return 列的数量
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * 根据列索引获取对应的列名称
         * <p>
         * 通过指定的列索引从列名称数组中获取对应的列名称
         *
         * @param column 列索引
         * @return 对应的列名称
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * 根据行索引和列索引获取对应单元格的值
         * <p>
         * 从指定行和列中获取数据, 用于表格展示等场景.
         *
         * @param rowIndex    行索引
         * @param columnIndex 列索引
         * @return 对应单元格的值, 若数据为空则返回空字符串
         */
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

        /**
         * 判断表格中指定单元格是否可编辑
         * <p>
         * 该方法用于确定表格中指定行和列的单元格是否允许编辑. 只有当列索引为 2 时返回 true, 表示可编辑.
         *
         * @param rowIndex    行索引
         * @param columnIndex 列索引
         * @return 如果单元格可编辑, 返回 true; 否则返回 false
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        /**
         * 设置表格单元格的值
         * <p>
         * 当列索引为 2 且行索引在有效范围内时, 将传入的值转换为字符串并存储到对应行的 {@code remark} 字段,
         * 同时触发表格单元格更新事件.
         *
         * @param aValue      要设置的值, 若为 {@code null} 则存储为空字符串
         * @param rowIndex    行索引
         * @param columnIndex 列索引
         */
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 2 && rowIndex >= 0 && rowIndex < data.size()) {
                data.get(rowIndex).remark = aValue != null ? aValue.toString() : "";
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}

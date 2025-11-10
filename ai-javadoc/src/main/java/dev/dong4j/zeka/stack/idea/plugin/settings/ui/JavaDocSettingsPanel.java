package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import lombok.Getter;

/**
 * JavaDoc 设置面板 UI
 *
 * <p>构建设置界面的所有 UI 组件。
 *
 * @author dong4j
 * @version 1.0.0
 */
@SuppressWarnings( {"D", "DuplicatedCode"})
public class JavaDocSettingsPanel {

    /** 主界面主面板，用于承载主要功能组件和布局 */
    private JPanel mainPanel;

    // AI 提供商配置
    /** AI 服务商下拉选择框 */
    private ComboBox<String> providerComboBox;
    /** 模型下拉框组件，用于选择不同的模型 */
    private ComboBox<String> modelComboBox;
    /** 基础 URL 输入框 */
    private JBTextField baseUrlField;
    /** API 密钥输入框 */
    @Getter
    private JBPasswordField apiKeyField;
    /** 测试连接按钮 */
    private JButton testConnectionButton;
    /** 刷新模型按钮 */
    private JButton refreshModelsButton;

    // 可用服务商列表相关组件
    /** 显示可用服务商列表的复选框 */
    private JBCheckBox showAvailableProvidersCheckBox;
    /** 可用服务商列表表格 */
    private JBTable availableProvidersTable;
    /** 可用服务商列表面板（包含表格和工具栏） */
    private JPanel availableProvidersPanel;
    /** 可用服务商列表表格模型 */
    private AvailableProvidersTableModel availableProvidersTableModel;

    // 验证状态标记
    /** 配置是否已验证的标记 */
    private boolean configurationVerified = false;

    // 功能配置
    /** 生成针对类的复选框 */
    private JBCheckBox generateForClassCheckBox;
    /** 方法生成复选框，用于控制是否为方法生成代码 */
    private JBCheckBox generateForMethodCheckBox;
    /** 生成字段的复选框 */
    private JBCheckBox generateForFieldCheckBox;
    /** 覆盖已有注释复选框 */
    private JBCheckBox overrideExistingCheckBox;
    /** 启用代码压缩的复选框 */
    private JBCheckBox enableCodeCompressionCheckBox;
    /** 在中英文间添加空格复选框 */
    private JBCheckBox addSpaceBetweenChineseAndEnglishCheckBox;
    /** 将中文标点符号转为英文标点符号复选框 */
    private JBCheckBox replaceChinesePunctuationCheckBox;
    /** 最大类代码行数设置控件 */
    private JSpinner maxClassCodeLinesSpinner;

    // 语言支持
    /** Java 语言支持选项框 */
    private JBCheckBox javaCheckBox;
    /** Kotlin 语言支持开关控件 */
    private JBCheckBox kotlinCheckBox;

    // JavaDoc 标签配置
    /** 显示自定义 JavaDoc 标签的复选框 */
    private JBCheckBox showCustomJavaDocTagsCheckBox;
    /** 自定义 JavaDoc 标签列表表格 */
    private JBTable customJavaDocTagsTable;
    /** 自定义 JavaDoc 标签列表面板（包含表格和工具栏） */
    private JPanel customJavaDocTagsPanel;
    /** 自定义 JavaDoc 标签列表表格模型 */
    private CustomJavaDocTagsTableModel customJavaDocTagsTableModel;

    // 高级设置
    /** 显示高级设置的复选框 */
    private JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板（用于控制可见性） */
    private JPanel advancedSettingsPanel;

    // 高级配置
    /** 最大重试次数的下拉选择器 */
    private JSpinner maxRetriesSpinner;
    /** 超时时间选择器，用于设置请求超时时间 */
    private JSpinner timeoutSpinner;
    /** 温度选择下拉框 */
    private JSpinner temperatureSpinner;
    /** 最大令牌数输入控件 */
    private JSpinner maxTokensSpinner;
    /** 顶部参数的下拉选择器控件 */
    private JSpinner topPSpinner;
    /** 用于选择 Top K 值的下拉框组件 */
    private JSpinner topKSpinner;
    /** 偏差惩罚系数调节器，用于设置生成文本时的偏差惩罚值 */
    private JSpinner presencePenaltySpinner;
    /** 日志详细模式复选框，用于控制是否输出详细日志信息 */
    private JBCheckBox verboseLoggingCheckBox;
    /** 性能模式复选框，用于启用或禁用性能优化模式 */
    private JBCheckBox performanceModeCheckBox;
    /** 显示提供商统计信息复选框 */
    private JBCheckBox showProviderStatisticsCheckBox;

    /** 系统提示文本区域，用于显示或编辑系统提示内容 */
    public JTextArea systemPromptTextArea;
    /** 类提示文本区域，用于显示或输入类相关的提示信息 */
    public JTextArea classPromptTextArea;
    /** 方法提示文本区域，用于显示方法相关的提示信息 */
    public JTextArea methodPromptTextArea;
    /** 提示信息显示区域，用于展示操作提示或说明文字 */
    public JTextArea fieldPromptTextArea;
    /** 测试提示文本区域 */
    public JTextArea testPromptTextArea;

    /** 存储复选框和提示标签的映射关系，用于更新提示文本颜色 */
    private final java.util.Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new java.util.HashMap<>();

    /**
     * 构造函数，初始化 JavaDoc 设置面板
     * <p>
     * 调用创建用户界面和设置事件监听器的方法，完成面板的初始化
     */
    public JavaDocSettingsPanel() {
        createUI();
        setupListeners();
        // 初始化字段的可用性和可编辑状态
        updateApiKeyFieldEnabled();
        updateBaseUrlFieldEditable();
    }

    /**
     * 初始化用户界面组件，创建并配置所有 UI 元素，包括下拉框、文本字段、按钮、复选框等。
     * <p>
     * 该方法负责构建整个设置界面的主面板，包括 AI 提供商配置、模型选择、基础 URL 和 API 密钥输入、
     * 连接测试按钮、模型刷新按钮、生成选项、语言支持、高级配置参数以及提示模板区域。
     */
    private void createUI() {
        // AI 提供商配置
        providerComboBox = new ComboBox<>(AIProviderType.getAllDisplayNames().toArray(new String[0]));

        // 创建可编辑的模型下拉框，用户可以输入任何模型名称
        modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true);  // 允许用户输入自定义模型名称
        updateModelList();

        baseUrlField = new JBTextField();
        baseUrlField.setToolTipText(JavaDocBundle.message("settings.base.url.tooltip"));

        apiKeyField = new JBPasswordField();
        apiKeyField.setToolTipText(JavaDocBundle.message("settings.api.key.tooltip"));

        testConnectionButton = new JButton(JavaDocBundle.message("settings.test.connection"));
        testConnectionButton.addActionListener(e -> testConnection());

        refreshModelsButton = new JButton(JavaDocBundle.message("settings.refresh.models"));
        refreshModelsButton.addActionListener(e -> refreshAvailableModels());

        // 创建可用服务商列表组件
        showAvailableProvidersCheckBox = new JBCheckBox(JavaDocBundle.message("settings.show.available.providers"));
        availableProvidersTableModel = new AvailableProvidersTableModel();
        availableProvidersTable = new JBTable(availableProvidersTableModel);
        availableProvidersTable.setPreferredScrollableViewportSize(new Dimension(500, 100));

        // 创建带工具栏的面板
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(availableProvidersTable)
            .setRemoveAction(button -> {
                int selectedRow = availableProvidersTable.getSelectedRow();
                if (selectedRow >= 0) {
                    removeAvailableProvider(selectedRow);
                }
            }).addExtraAction(new AnAction("清空全部",
                                           "清空所有可用服务商配置",
                                           com.intellij.icons.AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllAvailableProviders();
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    // 在后台线程中执行 update，避免阻塞 EDT
                    return ActionUpdateThread.BGT;
                }
            });

        availableProvidersPanel = decorator.createPanel();
        availableProvidersPanel.setVisible(false); // 默认隐藏

        // 功能配置
        generateForClassCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.class"));
        generateForMethodCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.method"));
        generateForFieldCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.field"));
        overrideExistingCheckBox = new JBCheckBox(JavaDocBundle.message("settings.override.existing"));
        enableCodeCompressionCheckBox = new JBCheckBox(JavaDocBundle.message("settings.enable.code.compression"));
        maxClassCodeLinesSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 300000, 100));
        addSpaceBetweenChineseAndEnglishCheckBox = new JBCheckBox(JavaDocBundle.message("settings.add.space.between.chinese.and.english"));
        replaceChinesePunctuationCheckBox = new JBCheckBox(JavaDocBundle.message("settings.replace.chinese.punctuation"));

        // 语言支持
        javaCheckBox = new JBCheckBox(JavaDocBundle.message("settings.language.java"));
        javaCheckBox.setEnabled(true);
        kotlinCheckBox = new JBCheckBox(JavaDocBundle.message("settings.language.kotlin"));
        kotlinCheckBox.setEnabled(false);

        // 创建自定义 JavaDoc 标签组件
        showCustomJavaDocTagsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.custom.javadoc.tags"));
        customJavaDocTagsTableModel = new CustomJavaDocTagsTableModel();
        customJavaDocTagsTable = new JBTable(customJavaDocTagsTableModel);
        customJavaDocTagsTable.setPreferredScrollableViewportSize(new Dimension(500, 100));

        // 创建带工具栏的面板
        ToolbarDecorator tagsDecorator = ToolbarDecorator.createDecorator(customJavaDocTagsTable)
            .setAddAction(button -> {
                addCustomJavaDocTag();
            })
            .setRemoveAction(button -> {
                int selectedRow = customJavaDocTagsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    removeCustomJavaDocTag(selectedRow);
                }
            })
            .addExtraAction(new AnAction(JavaDocBundle.message("settings.custom.javadoc.tags.clear.all"),
                                         JavaDocBundle.message("settings.custom.javadoc.tags.clear.all.description"),
                                         com.intellij.icons.AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllCustomJavaDocTags();
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }
            });

        customJavaDocTagsPanel = tagsDecorator.createPanel();
        // 可见性将在 loadSettings 中根据配置设置

        // 高级配置
        maxRetriesSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(30000, 1000, 300000, 1000));
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 2.0, 0.1));
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        topPSpinner = new JSpinner(new SpinnerNumberModel(0.9, 0.0, 1.0, 0.1));
        topKSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100, 1));
        presencePenaltySpinner = new JSpinner(new SpinnerNumberModel(0.1, -2.0, 2.0, 0.1));
        verboseLoggingCheckBox = new JBCheckBox(JavaDocBundle.message("settings.verbose.logging"));
        performanceModeCheckBox = new JBCheckBox(JavaDocBundle.message("settings.performance.mode"));
        showProviderStatisticsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.show.provider.statistics"));

        // Prompt 配置 - 创建文本区域（将在 Tab 页中使用）
        // 增加初始高度：15行（原来10行），宽度保持50列不变
        systemPromptTextArea = new JTextArea(15, 50);
        classPromptTextArea = new JTextArea(15, 50);
        methodPromptTextArea = new JTextArea(15, 50);
        fieldPromptTextArea = new JTextArea(15, 50);
        testPromptTextArea = new JTextArea(15, 50);

        // 创建高级设置复选框
        showAdvancedSettingsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.advanced.settings.show"));

        // 创建高级设置容器面板
        advancedSettingsPanel = new JPanel(new BorderLayout());
        advancedSettingsPanel.setVisible(false); // 默认隐藏

        // 构建高级设置面板内容
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            // 模型参数设置
            .addComponent(createModelParamsPanel())
            .addSeparator(10)
            // Prompt 模板与提示词
            .addComponent(createPromptTemplatesPanel())
            .getPanel();

        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            // 第一组：基础连接配置（API 接入）
            .addComponent(createBasicConnectionConfigPanel())
            .addSeparator(10)

            // 第二组：高级设置（可折叠）
            .addComponent(showAdvancedSettingsCheckBox)
            .addComponent(advancedSettingsPanel)
            .addSeparator(10)

            // 第三组：支持的语言
            .addComponent(createLanguageSupportPanel())
            .addSeparator(10)

            // 第四组：生成规则配置
            .addComponent(createGenerationRulesPanel())
            .addSeparator(10)

            // 第五组：其他设置
            .addComponent(createOtherSettingsPanel())

            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
    }

    /**
     * 创建基础连接配置面板
     *
     * <p>创建一个包含基础连接配置所有组件的面板，并添加边框。
     *
     * @return 基础连接配置面板
     */
    private JPanel createBasicConnectionConfigPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.provider.label")), providerComboBox)
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.base.url.label")), baseUrlField)
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.api.key.label")), createApiKeyPanel())
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.model.label")), createModelPanel())
            .addSeparator(10)
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.max.retries")),
                                 createAdvancedConfigPanel(maxRetriesSpinner,
                                                           "settings.max.retries.hint"))
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.timeout")),
                                 createAdvancedConfigPanel(timeoutSpinner,
                                                           "settings.timeout.hint"))
            .addComponent(createCheckBoxWithHint(verboseLoggingCheckBox, "settings.verbose.logging.hint"))
            .addComponent(createCheckBoxWithHint(performanceModeCheckBox, "settings.performance.mode.hint"))
            .addComponent(createPerformanceModeSubConfigPanel())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.basic.connection.config")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建模型参数设置面板
     *
     * <p>创建一个包含模型参数设置所有组件的面板，并添加边框。
     *
     * @return 模型参数设置面板
     */
    private JPanel createModelParamsPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.max.tokens")),
                                 createAdvancedConfigPanel(maxTokensSpinner,
                                                           "settings.max.tokens.hint"))
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.temperature")),
                                 createAdvancedConfigPanel(temperatureSpinner,
                                                           "settings.temperature.hint"))
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.top.p")),
                                 createAdvancedConfigPanel(topPSpinner,
                                                           "settings.top.p.hint"))
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.top.k")),
                                 createAdvancedConfigPanel(topKSpinner,
                                                           "settings.top.k.hint"))
            .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.presence.penalty")),
                                 createAdvancedConfigPanel(presencePenaltySpinner,
                                                           "settings.presence.penalty.hint"))
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.advanced.settings.model.params")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建 Prompt 模板与提示词面板
     *
     * <p>创建一个包含 Prompt 模板与提示词所有组件的面板，并添加边框。
     *
     * @return Prompt 模板与提示词面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + JavaDocBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.advanced.settings.prompt.templates")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建语言支持面板
     *
     * <p>创建一个包含语言支持复选框的面板，用于选择支持哪些编程语言，并添加边框。
     *
     * @return 语言支持面板
     */
    private JPanel createLanguageSupportPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(javaCheckBox)
            .addComponent(kotlinCheckBox)
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.language.support")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建生成规则配置面板
     *
     * <p>创建一个包含生成规则配置的面板，包括：
     * <ul>
     *   <li>生成选项（类/方法/字段）</li>
     *   <li>覆盖已有注释</li>
     *   <li>代码压缩配置</li>
     * </ul>
     * 并添加边框。
     *
     * @return 生成规则配置面板
     */
    private JPanel createGenerationRulesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createGenerationOptionsPanel())
            .addComponent(createCheckBoxWithHint(overrideExistingCheckBox, "settings.override.existing.hint"))
            .addComponent(createCheckBoxWithHint(enableCodeCompressionCheckBox, "settings.enable.code.compression.hint"))
            .addComponent(createCodeCompressionSubConfigPanel())
            .addComponent(
                createCheckBoxWithHint(addSpaceBetweenChineseAndEnglishCheckBox,
                                       "settings.add.space.between.chinese.and.english.hint"))
            .addComponent(createCheckBoxWithHint(replaceChinesePunctuationCheckBox,
                                                 "settings.replace.chinese.punctuation.hint"))
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.generation.rules.config")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建其他设置面板
     *
     * <p>创建一个包含其他设置所有组件的面板，并添加边框。
     *
     * @return 其他设置面板
     */
    private JPanel createOtherSettingsPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(showCustomJavaDocTagsCheckBox)
            .addComponent(customJavaDocTagsPanel)
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.other.settings")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建生成选项面板
     *
     * <p>创建一个包含生成选项复选框的面板，用于选择要为哪些类型的元素生成文档。
     * 面板包含3个复选框水平排列（类、方法、字段）。
     *
     * @return 生成选项面板
     */
    private JPanel createGenerationOptionsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new java.awt.BorderLayout());

        // 3个复选框水平排列
        JBCheckBox[] checkBoxes = {
            generateForClassCheckBox,
            generateForMethodCheckBox,
            generateForFieldCheckBox
        };

        String[] hintKeys = {
            "settings.generate.for.class.hint",
            "settings.generate.for.method.hint",
            "settings.generate.for.field.hint"
        };

        JPanel checkBoxPanel = createHorizontalCheckBoxPanel(checkBoxes, hintKeys, 3);
        mainPanel.add(checkBoxPanel, java.awt.BorderLayout.NORTH);

        return mainPanel;
    }

    /**
     * 创建模型配置面板
     * <p>
     * 用于构建包含模型选择下拉框和测试连接按钮的面板。
     *
     * @return 模型配置面板
     */
    private JPanel createModelPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(modelComboBox, BorderLayout.CENTER);
        panel.add(testConnectionButton, BorderLayout.EAST);
        return panel;
    }

    /**
     * 创建API密钥输入面板
     * <p>
     * 初始化并返回一个包含API密钥输入字段和"获取最新模型"按钮的面板。
     *
     * @return 包含API密钥输入字段和按钮的面板
     */
    private JPanel createApiKeyPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(apiKeyField, BorderLayout.CENTER);
        panel.add(refreshModelsButton, BorderLayout.EAST);
        return panel;
    }

    /**
     * 创建高级配置面板，包含一个带宽度限制的 JSpinner 和提示标签
     * <p>
     * 该方法用于构建一个布局面板，左侧放置一个设置宽度的 JSpinner 控件，右侧放置一个带有提示信息的标签。
     * 提示标签的字体大小和颜色会根据系统 UI 设置进行调整。
     *
     * @param spinner 用于配置的 JSpinner 控件
     * @param hintKey 提示信息的键，用于从资源文件中获取对应的提示文本
     * @return 包含 JSpinner 和提示标签的面板
     */
    private JPanel createAdvancedConfigPanel(JSpinner spinner, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 固定输入框宽度
        spinner.setPreferredSize(new Dimension(120, spinner.getPreferredSize().height));
        panel.add(spinner, BorderLayout.WEST);

        // 提示文本放在右侧，但限制宽度
        JBLabel hintLabel = new JBLabel(JavaDocBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(300, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建包含复选框和提示文本的面板
     * <p>
     * 该方法用于创建一个包含复选框和提示文本的面板，提示文本通过指定的键从资源文件中获取。
     * 当复选框被勾选时，提示文本会以正常颜色（高亮）显示；未勾选时，提示文本以较暗的颜色显示。
     *
     * <p>特殊处理：
     * <ul>
     *   <li>显示统计信息复选框：不在这里添加监听器，因为它需要依赖性能模式的状态，监听器在 setupListeners 中添加</li>
     *   <li>其他复选框：自动添加监听器来更新提示文本颜色</li>
     * </ul>
     *
     * @param checkBox 要添加到面板中的复选框
     * @param hintKey  用于获取提示文本的资源键
     * @return 包含复选框和提示文本的面板
     */
    private JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 复选框放在左侧
        panel.add(checkBox, BorderLayout.WEST);

        // 提示文本放在右侧
        JBLabel hintLabel = new JBLabel(JavaDocBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));

        // 保存映射关系，用于后续更新颜色
        checkBoxHintLabelMap.put(checkBox, hintLabel);

        // 根据复选框状态设置提示文本颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());

        // 监听复选框状态变化，动态更新提示文本颜色
        // 注意：显示统计信息复选框的监听器在 setupListeners 中添加，因为它需要特殊处理
        if (checkBox != showProviderStatisticsCheckBox) {
            checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));
        }

        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /** 类代码最大行数标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesLabel;

    /** 类代码最大行数提示标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesHintLabel;

    /**
     * 创建代码压缩的子配置面板（类代码最大行数）
     * <p>
     * 该类代码最大行数配置作为代码压缩的子配置，会向右缩进2个空格。
     * 当代码压缩复选框被勾选时，该配置才可用。
     *
     * @return 包含类代码最大行数配置的面板
     */
    private JPanel createCodeCompressionSubConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 添加左侧缩进（2个空格，约20像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(22));

        // 创建标签
        maxClassCodeLinesLabel = new JBLabel(JavaDocBundle.message("settings.max.class.code.lines"));

        // 创建提示标签
        maxClassCodeLinesHintLabel = new JBLabel(JavaDocBundle.message("settings.max.class.code.lines.hint"));
        maxClassCodeLinesHintLabel.setFont(maxClassCodeLinesHintLabel.getFont().deriveFont(maxClassCodeLinesHintLabel.getFont().getSize() - 2.0f));
        maxClassCodeLinesHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        maxClassCodeLinesHintLabel.setPreferredSize(new Dimension(300, maxClassCodeLinesHintLabel.getPreferredSize().height));

        // 创建包含标签、输入框和提示的面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(maxClassCodeLinesLabel, BorderLayout.WEST);

        // 创建输入框和提示的面板
        JPanel spinnerPanel = new JPanel(new BorderLayout(5, 0));
        maxClassCodeLinesSpinner.setPreferredSize(new Dimension(120, maxClassCodeLinesSpinner.getPreferredSize().height));
        spinnerPanel.add(maxClassCodeLinesSpinner, BorderLayout.WEST);
        spinnerPanel.add(maxClassCodeLinesHintLabel, BorderLayout.CENTER);
        contentPanel.add(spinnerPanel, BorderLayout.CENTER);

        indentPanel.add(contentPanel, BorderLayout.CENTER);
        panel.add(indentPanel, BorderLayout.CENTER);

        // 初始状态：根据代码压缩复选框的状态设置可用性
        updateMaxClassCodeLinesEnabled();

        return panel;
    }

    /**
     * 创建性能模式的子配置面板（显示统计信息和可用服务商）
     * <p>
     * 该面板包含性能模式的子配置，会向右缩进2个空格。
     * 当性能模式复选框被勾选时，这些配置才可用。
     *
     * @return 包含性能模式子配置的面板
     */
    private JPanel createPerformanceModeSubConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 添加左侧缩进（2个空格，约22像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(22));

        // 创建垂直布局面板，包含两个复选框
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showProviderStatisticsCheckBox, "settings.show.provider.statistics.hint"))
            .addComponent(createCheckBoxWithHint(showAvailableProvidersCheckBox, "settings.show.available.providers.hint"))
            .addComponent(availableProvidersPanel)
            .getPanel();

        indentPanel.add(contentPanel, BorderLayout.CENTER);
        panel.add(indentPanel, BorderLayout.CENTER);

        // 初始状态：根据性能模式复选框的状态设置可用性
        updateShowProviderStatisticsEnabled();
        updateShowAvailableProvidersEnabled();

        return panel;
    }

    /**
     * 更新提示标签的颜色
     * <p>
     * 根据复选框的选中状态，设置提示标签的前景色。
     * 选中时使用正常颜色（高亮），未选中时使用较暗的颜色。
     *
     * @param hintLabel 提示标签
     * @param selected  是否选中
     */
    private void updateHintLabelColor(JBLabel hintLabel, boolean selected) {
        if (selected) {
            // 选中时使用正常颜色（高亮显示）
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            // 未选中时使用较暗的颜色
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    /**
     * 更新类代码最大行数输入框的可用性
     * <p>
     * 根据代码压缩复选框的状态，设置类代码最大行数输入框、标签和提示的可用性。
     */
    private void updateMaxClassCodeLinesEnabled() {
        boolean enabled = enableCodeCompressionCheckBox.isSelected();
        maxClassCodeLinesSpinner.setEnabled(enabled);
        if (maxClassCodeLinesLabel != null) {
            maxClassCodeLinesLabel.setEnabled(enabled);
        }
        if (maxClassCodeLinesHintLabel != null) {
            maxClassCodeLinesHintLabel.setEnabled(enabled);
            // 根据可用性更新提示文本颜色
            if (enabled) {
                maxClassCodeLinesHintLabel.setForeground(UIManager.getColor("Label.foreground"));
            } else {
                maxClassCodeLinesHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
    }

    /**
     * 更新显示统计信息复选框的可用性
     * <p>
     * 根据性能模式复选框的状态，设置显示统计信息复选框的可用性和提示文本颜色。
     * 当性能模式未启用时，显示统计信息复选框及其提示文本都会显示为禁用状态。
     */
    private void updateShowProviderStatisticsEnabled() {
        boolean enabled = performanceModeCheckBox.isSelected();
        showProviderStatisticsCheckBox.setEnabled(enabled);

        // 更新显示统计信息复选框的提示文本颜色
        // 如果性能模式未启用，提示文本显示为禁用状态
        // 如果性能模式启用，则根据显示统计信息复选框的状态更新颜色
        JBLabel hintLabel = checkBoxHintLabelMap.get(showProviderStatisticsCheckBox);
        if (hintLabel != null) {
            if (enabled) {
                // 性能模式启用时，根据显示统计信息复选框的状态更新颜色
                updateHintLabelColor(hintLabel, showProviderStatisticsCheckBox.isSelected());
            } else {
                // 性能模式未启用时，提示文本显示为禁用状态
                hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
    }

    /**
     * 更新显示可用服务商复选框的可用性
     * <p>
     * 根据性能模式复选框的状态，设置显示可用服务商复选框的可用性和提示文本颜色。
     * 当性能模式未启用时，显示可用服务商复选框及其提示文本都会显示为禁用状态。
     */
    private void updateShowAvailableProvidersEnabled() {
        boolean enabled = performanceModeCheckBox.isSelected();
        showAvailableProvidersCheckBox.setEnabled(enabled);

        // 更新显示可用服务商复选框的提示文本颜色
        // 如果性能模式未启用，提示文本显示为禁用状态
        // 如果性能模式启用，则根据显示可用服务商复选框的状态更新颜色
        JBLabel hintLabel = checkBoxHintLabelMap.get(showAvailableProvidersCheckBox);
        if (hintLabel != null) {
            if (enabled) {
                // 性能模式启用时，根据显示可用服务商复选框的状态更新颜色
                updateHintLabelColor(hintLabel, showAvailableProvidersCheckBox.isSelected());
            } else {
                // 性能模式未启用时，提示文本显示为禁用状态
                hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
    }

    /**
     * 更新所有复选框的提示文本颜色
     * <p>
     * 根据每个复选框的当前选中状态，更新对应的提示文本颜色。
     * 用于在加载设置时初始化提示文本的颜色。
     *
     * <p>特殊处理：
     * <ul>
     *   <li>显示统计信息和显示可用服务商复选框：如果性能模式未启用，提示文本显示为禁用状态</li>
     *   <li>其他复选框：根据复选框的选中状态更新颜色</li>
     * </ul>
     */
    private void updateAllCheckBoxHintColors() {
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) -> {
            // 显示统计信息和显示可用服务商复选框需要特殊处理
            if (checkBox == showProviderStatisticsCheckBox || checkBox == showAvailableProvidersCheckBox) {
                // 如果性能模式未启用，提示文本显示为禁用状态
                if (!performanceModeCheckBox.isSelected()) {
                    hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                } else {
                    // 性能模式启用时，根据复选框状态更新颜色
                    updateHintLabelColor(hintLabel, checkBox.isSelected());
                }
            } else {
                // 其他复选框根据选中状态更新颜色
                updateHintLabelColor(hintLabel, checkBox.isSelected());
            }
        });
    }

    /**
     * 创建水平排列的复选框面板
     *
     * @param checkBoxes  复选框数组
     * @param hintKeys    对应的提示文本键数组
     * @param itemsPerRow 每行显示的复选框数量
     * @return 水平排列的复选框面板
     */
    private JPanel createHorizontalCheckBoxPanel(JBCheckBox[] checkBoxes, String[] hintKeys, int itemsPerRow) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

        // 设置间距 - 减少水平间距
        gbc.insets = JBUI.insets(5, 1);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        for (int i = 0; i < checkBoxes.length; i++) {
            // 计算行和列
            int row = i / itemsPerRow;

            gbc.gridx = i % itemsPerRow;
            gbc.gridy = row;
            gbc.weightx = 1.0 / itemsPerRow; // 平均分配宽度

            // 创建单个复选框的面板
            JPanel checkBoxPanel = new JPanel(new BorderLayout(5, 0));
            checkBoxPanel.add(checkBoxes[i], BorderLayout.WEST);

            // 添加提示文本
            if (i < hintKeys.length && hintKeys[i] != null) {
                JBLabel hintLabel = new JBLabel(JavaDocBundle.message(hintKeys[i]));
                hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
                hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                checkBoxPanel.add(hintLabel, BorderLayout.CENTER);
            }

            mainPanel.add(checkBoxPanel, gbc);
        }

        return mainPanel;
    }

    /**
     * 创建用于显示提示配置的选项卡面板
     * <p>
     * 初始化一个包含多个提示配置选项卡的 JBTabbedPane，每个选项卡对应不同的提示类型，如系统提示、类提示、方法提示等。
     *
     * @return 包含提示配置选项卡的 JBTabbedPane 实例
     */
    private JBTabbedPane createPromptTabbedPane() {
        // Prompt 配置 - Tab 页
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        // 增加 Tab 页的高度：宽度保持600不变，高度从200增加到400
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        // 创建各个 Tab 页
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.system"), createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.class"), createPromptTab(classPromptTextArea, "class"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.method"), createPromptTab(methodPromptTextArea, "method"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.field"), createPromptTab(fieldPromptTextArea, "field"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.test"), createPromptTab(testPromptTextArea, "test"));

        return promptTabbedPane;
    }

    /**
     * 创建提示信息标签页面板
     * <p>
     * 根据给定的文本区域和提示类型，创建一个包含文本区域和重置按钮的标签页面板。
     *
     * @param textArea   文本区域组件
     * @param promptType 提示类型，用于加载对应的提示信息和资源
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        // 创建文本区域
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(JavaDocBundle.message("settings.prompt." + promptType + ".tooltip"));

        // 添加文档监听器，根据内容自动调整大小
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }
        });

        // 创建滚动面板，并添加边框以在四周留出空间
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
        JButton resetButton = new JButton(JavaDocBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        // 初始化时根据内容调整大小
        SwingUtilities.invokeLater(() -> adjustTextAreaSize(textArea));

        return tabPanel;
    }

    /**
     * 根据文本内容自动调整文本区域的大小
     * <p>
     * 该方法会根据文本内容的行数自动调整文本区域的行数，但会设置最小和最大行数限制。
     * 最小行数：15行（初始大小）
     * 最大行数：50行（避免占用过多空间）
     *
     * @param textArea 要调整大小的文本区域
     */
    private void adjustTextAreaSize(JTextArea textArea) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 计算文本的行数
                int lineCount = textArea.getLineCount();

                // 设置最小和最大行数限制
                int minRows = 15;  // 最小行数
                int maxRows = 50;   // 最大行数

                // 计算实际需要的行数（至少显示所有内容，但不超过最大值）
                int rows = Math.max(minRows, Math.min(lineCount, maxRows));

                // 如果行数发生变化，更新文本区域的行数
                if (rows != textArea.getRows()) {
                    textArea.setRows(rows);
                    // 触发父容器重新布局
                    if (textArea.getParent() != null) {
                        textArea.getParent().revalidate();
                    }
                }
            } catch (Exception e) {
                // 静默处理异常，避免影响功能
            }
        });
    }

    /**
     * 将指定类型的提示内容重置为默认模板
     * <p>
     * 根据传入的提示类型，获取对应的默认提示模板，并将其设置到指定的文本区域中。
     *
     * @param promptType 提示类型，如 "system"、"class"、"method" 等
     * @param textArea   要设置默认模板的文本区域组件
     */
    public void resetPromptToDefault(String promptType, JTextArea textArea) {
        String defaultTemplate = switch (promptType) {
            case "system" -> SettingsState.getDefaultSystemPromptTemplate();
            case "class" -> SettingsState.getDefaultClassPromptTemplate();
            case "method" -> SettingsState.getDefaultMethodPromptTemplate();
            case "field" -> SettingsState.getDefaultFieldPromptTemplate();
            case "test" -> SettingsState.getDefaultTestPromptTemplate();
            default -> "";
        };
        textArea.setText(defaultTemplate);
    }

    /**
     * 创建一个带有指定文本区域的滚动面板
     * <p>
     * 该方法用于创建一个 JScrollPane 实例，并设置其首选大小和滚动条策略。
     *
     * @param textArea 要放入滚动面板中的文本区域
     * @return 配置好的滚动面板实例
     */
    private JScrollPane createScrollPane(JTextArea textArea) {
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 150));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    /**
     * 初始化各种监听器，用于响应用户界面组件的变化
     * <p>
     * 该方法为各个输入组件添加动作监听器，当组件内容发生变化时，触发相应的更新或验证状态清除操作。
     * 包括提供商、Base URL、API Key、模型选择以及代码优化配置等变化的监听。
     */
    private void setupListeners() {
        // 提供商变更时更新模型列表和默认值
        providerComboBox.addActionListener(e -> {
            updateModelList();
            updateDefaultValues();
            updateApiKeyFieldEnabled();
            updateBaseUrlFieldEditable();
            // 关键配置修改，清除验证状态
            markConfigurationAsUnverified();
        });

        // Base URL 变更时清除验证状态
        baseUrlField.addActionListener(e -> markConfigurationAsUnverified());

        // API Key 变更时清除验证状态
        apiKeyField.addActionListener(e -> markConfigurationAsUnverified());

        // 模型选择变更时清除验证状态
        modelComboBox.addActionListener(e -> markConfigurationAsUnverified());

        // 监听代码压缩配置变更
        enableCodeCompressionCheckBox.addActionListener(e -> {
            // 当启用/禁用代码压缩时，更新最大行数输入框的可用性
            updateMaxClassCodeLinesEnabled();
        });

        // 监听性能模式配置变更
        performanceModeCheckBox.addActionListener(e -> {
            // 当启用/禁用性能模式时，更新显示统计信息复选框的可用性和提示文本颜色
            updateShowProviderStatisticsEnabled();
            // 更新显示可用服务商复选框的可用性和提示文本颜色
            updateShowAvailableProvidersEnabled();

            // 更新性能模式复选框本身的提示文本颜色
            JBLabel performanceModeHintLabel = checkBoxHintLabelMap.get(performanceModeCheckBox);
            if (performanceModeHintLabel != null) {
                updateHintLabelColor(performanceModeHintLabel, performanceModeCheckBox.isSelected());
            }
        });

        // 监听高级设置复选框状态变化，控制高级设置面板的显示/隐藏
        showAdvancedSettingsCheckBox.addActionListener(e -> {
            boolean selected = showAdvancedSettingsCheckBox.isSelected();
            advancedSettingsPanel.setVisible(selected);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        // 监听显示统计信息复选框状态变化，更新其提示文本颜色
        // 注意：只有当性能模式启用时，提示文本颜色才根据复选框状态更新
        // 需要移除 createCheckBoxWithHint 中添加的默认监听器，然后添加自定义逻辑
        // 但由于 createCheckBoxWithHint 已经添加了监听器，我们在这里再次添加
        // 监听器会按顺序执行，我们需要确保逻辑正确
        showProviderStatisticsCheckBox.addActionListener(e -> {
            // 调用 updateShowProviderStatisticsEnabled 来统一处理
            // 这样可以确保提示文本颜色正确更新
            updateShowProviderStatisticsEnabled();
        });

        // 监听显示可用服务商复选框状态变化，更新其提示文本颜色和面板可见性
        showAvailableProvidersCheckBox.addActionListener(e -> {
            // 调用 updateShowAvailableProvidersEnabled 来统一处理
            // 这样可以确保提示文本颜色正确更新
            updateShowAvailableProvidersEnabled();
            // 控制可用服务商面板的显示/隐藏
            availableProvidersPanel.setVisible(showAvailableProvidersCheckBox.isSelected());
        });

        // 监听显示自定义 JavaDoc 标签复选框状态变化
        showCustomJavaDocTagsCheckBox.addActionListener(e -> {
            customJavaDocTagsPanel.setVisible(showCustomJavaDocTagsCheckBox.isSelected());
        });
    }

    /**
     * 更新模型列表，根据选择的提供商标识符加载对应的模型选项
     * <p>
     * 该方法首先获取用户选择的提供商显示名称，将其转换为对应的提供商标识符。
     * 然后根据该标识符获取对应的提供商类型，并加载该类型支持的所有模型。
     * 最后将用户之前选择的模型恢复到下拉框中，若为空则使用默认模型。
     * 同时设置模型输入框的提示文本。
     */
    private void updateModelList() {
        String displayName = (String) providerComboBox.getSelectedItem();
        if (displayName == null) {
            return;
        }

        // 将显示名称转换为提供商标识符
        String providerId = AIProviderType.getProviderIdByDisplayName(displayName);
        if (providerId == null) {
            return;
        }

        AIProviderType providerType = AIProviderType.fromProviderId(providerId);
        if (providerType == null) {
            return;
        }

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
    }

    /**
     * 刷新可用模型列表（用户手动触发）
     *
     * <p>当用户点击"获取最新模型"按钮时调用此方法。
     * 会显示加载状态，并在完成后恢复按钮状态。
     */
    private void refreshAvailableModels() {
        String displayName = (String) providerComboBox.getSelectedItem();
        String baseUrl = baseUrlField.getText().trim();

        if (displayName == null || baseUrl.isEmpty()) {
            JOptionPane.showMessageDialog(
                getParentWindow(),
                JavaDocBundle.message("error.base.url.missing"),
                JavaDocBundle.message("settings.error.title"),
                JOptionPane.WARNING_MESSAGE
                                         );
            return;
        }

        // 将显示名称转换为提供商标识符
        String providerId = AIProviderType.getProviderIdByDisplayName(displayName);
        if (providerId == null) {
            return;
        }

        // 检查是否需要 API Key
        AIProviderType providerType = AIProviderType.fromProviderId(providerId);
        boolean needsApiKey = providerType != null && providerType.requiresApiKey();
        if (needsApiKey && apiKeyField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(
                getParentWindow(),
                JavaDocBundle.message("error.api.key.missing"),
                JavaDocBundle.message("settings.error.title"),
                JOptionPane.WARNING_MESSAGE
                                         );
            return;
        }

        // 设置按钮状态
        refreshModelsButton.setEnabled(false);
        refreshModelsButton.setText(JavaDocBundle.message("settings.refresh.models.testing"));

        // 在后台线程中获取模型列表
        new Thread(() -> {
            try {
                // 简单测试, 只需要一个默认配置即可
                SettingsState testSettings = new SettingsState();
                // 使用设置面板的当前配置创建一个服务提供商配置
                SettingsState.ProviderConfig snapshotProviderConfig = getProviderConfigSnapshot();
                AIServiceProvider provider = AIServiceFactory.createProvider(testSettings, snapshotProviderConfig);

                // 检查提供商创建是否成功
                if (provider == null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                            getParentWindow(),
                            "创建 AI 服务提供商失败，请检查配置是否正确",
                            JavaDocBundle.message("settings.error.title"),
                            JOptionPane.ERROR_MESSAGE
                                                     );
                        refreshModelsButton.setText(JavaDocBundle.message("settings.refresh.models"));
                        refreshModelsButton.setEnabled(true);
                    });
                    return;
                }

                List<String> availableModels = provider.getAvailableModels(new String(apiKeyField.getPassword()).trim());

                // 按名称排序模型列表
                availableModels.sort(String::compareToIgnoreCase);

                // 在 UI 线程中更新下拉框
                SwingUtilities.invokeLater(() -> {
                    if (!availableModels.isEmpty()) {
                        // 保存当前选择的模型
                        String currentModel = (String) modelComboBox.getSelectedItem();

                        // 清空当前列表
                        modelComboBox.removeAllItems();

                        // 添加可用模型
                        for (String model : availableModels) {
                            modelComboBox.addItem(model);
                        }

                        // 尝试恢复用户之前选择的模型
                        if (currentModel != null && !currentModel.trim().isEmpty()) {
                            // 检查当前模型是否在可用列表中
                            boolean found = false;
                            for (String model : availableModels) {
                                if (model.equals(currentModel)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (found) {
                                modelComboBox.setSelectedItem(currentModel);
                            } else {
                                // 如果当前模型不可用，选择第一个可用模型
                                modelComboBox.setSelectedIndex(0);
                            }
                        } else {
                            // 如果没有当前选择，使用默认模型
                            modelComboBox.setSelectedItem(provider.getDefaultModel());
                        }

                        // 更新提示文本
                        if (modelComboBox.getEditor() != null &&
                            modelComboBox.getEditor().getEditorComponent() instanceof JTextField textField) {
                            textField.setToolTipText("从服务提供商获取的可用模型列表");
                        }

                        JOptionPane.showMessageDialog(
                            getParentWindow(),
                            "成功获取到 " + availableModels.size() + " 个可用模型",
                            JavaDocBundle.message("settings.test.result.title"),
                            JOptionPane.INFORMATION_MESSAGE
                                                     );
                    } else {
                        JOptionPane.showMessageDialog(
                            getParentWindow(),
                            "未获取到可用模型，请检查配置是否正确",
                            JavaDocBundle.message("settings.error.title"),
                            JOptionPane.WARNING_MESSAGE
                                                     );
                    }

                    refreshModelsButton.setText(JavaDocBundle.message("settings.refresh.models"));
                    refreshModelsButton.setEnabled(true);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        getParentWindow(),
                        "获取模型列表失败: " + e.getMessage(),
                        JavaDocBundle.message("settings.error.title"),
                        JOptionPane.ERROR_MESSAGE
                                                 );
                    refreshModelsButton.setText(JavaDocBundle.message("settings.refresh.models"));
                    refreshModelsButton.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * 更新默认值配置，根据选择的提供者设置对应的配置信息
     * <p>
     * 该方法首先获取当前选择的显示名称，将其转换为对应的提供者标识符。如果标识符有效，则获取对应的提供者类型。
     * 接着，尝试从已保存的配置中查找对应的配置信息，如果存在则使用该配置；否则使用提供者类型中的默认配置。
     * 最后，将配置信息填充到对应的 UI 控件中。
     */
    private void updateDefaultValues() {
        String displayName = (String) providerComboBox.getSelectedItem();
        if (displayName == null) {
            return;
        }

        // 将显示名称转换为提供商标识符
        String providerId = AIProviderType.getProviderIdByDisplayName(displayName);
        if (providerId == null) {
            return;
        }

        AIProviderType providerType = AIProviderType.fromProviderId(providerId);
        if (providerType == null) {
            return;
        }

        // 从 defaultProviders 获取该服务商类型的默认配置
        // 这样可以确保切换服务商时不会丢失该服务商的配置信息（包括 API Key）
        SettingsState settings = SettingsState.getInstance();
        SettingsState.ProviderConfig defaultConfig = settings.getDefaultProviderConfig(providerType);

        // 加载配置到 UI
        baseUrlField.setText(defaultConfig.baseUrl);
        modelComboBox.setSelectedItem(defaultConfig.modelName);

        // 从 PasswordSafe 读取 API Key（使用 defaultConfig 的 UUID）
        // 异步加载 PasswordSafe 中的 API Key
        loadApiKeyForProvider(defaultConfig, providerId);

        // 加载验证状态
        this.configurationVerified = defaultConfig.configurationVerified;
    }

    /**
     * 异步加载指定提供商配置的 API Key，并在 UI 中更新显示
     *
     * @param providerConfig     提供商配置
     * @param expectedProviderId 当前期望的提供商标识符
     */
    private void loadApiKeyForProvider(@NotNull SettingsState.ProviderConfig providerConfig,
                                       @NotNull String expectedProviderId) {
        apiKeyField.setText("");

        if (providerConfig.md5 == null || providerConfig.md5.trim().isEmpty()) {
            return;
        }

        SettingsState.loadApiKeyAsync(providerConfig.md5, apiKey -> {
            String currentProviderId = getSelectedProviderId();
            if (!expectedProviderId.equals(currentProviderId)) {
                return;
            }
            apiKeyField.setText(apiKey != null ? apiKey : "");
        });
    }

    @Nullable
    private String getSelectedProviderId() {
        String displayName = (String) providerComboBox.getSelectedItem();
        return displayName == null ? null : AIProviderType.getProviderIdByDisplayName(displayName);
    }

    /**
     * 查找已保存的提供商配置
     *
     * @param providerId 提供商ID
     * @return 已保存的配置，如果没有找到则返回null
     */
    private SettingsState.ProviderConfig findSavedProviderConfig(String providerId) {
        SettingsState settings = SettingsState.getInstance();
        // 优先查找已验证的配置，如果没有则查找所有配置（包括未验证的）
        return settings.getAvailableProviders().stream()
            .filter(config -> config.providerType != null && providerId.equals(config.providerType.getProviderId()))
            .findFirst()
            .orElse(settings.availableProviders.stream()
                        .filter(config -> config.providerType != null && providerId.equals(config.providerType.getProviderId()))
                        .findFirst()
                        .orElse(null));
    }

    /**
     * 更新 API Key 字段的可用状态
     * <p>
     * 根据当前选择的提供商类型，设置 API Key 字段是否可用。
     * 某些本地服务（如 Ollama、LM Studio）不需要 API Key，这些服务的 API Key 字段会被禁用。
     * 其他需要认证的服务（如通义千问、硅基流动、自定义服务）的 API Key 字段会被启用。
     */
    private void updateApiKeyFieldEnabled() {
        String displayName = (String) providerComboBox.getSelectedItem();
        if (displayName == null) {
            apiKeyField.setEnabled(false);
            testConnectionButton.setEnabled(false);
            return;
        }

        // 将显示名称转换为提供商标识符
        String providerId = AIProviderType.getProviderIdByDisplayName(displayName);
        AIProviderType providerType = providerId == null ? null : AIProviderType.fromProviderId(providerId);

        // 根据提供商类型的 requiresApiKey 属性设置可用性
        boolean requiresKey = providerType != null && providerType.requiresApiKey();
        apiKeyField.setEnabled(requiresKey);
        testConnectionButton.setEnabled(true);
    }

    /**
     * 更新 Base URL 字段的可编辑状态
     * <p>
     * 根据当前选择的提供商类型，设置 Base URL 字段是否可编辑。
     * 某些官方服务（如通义千问、硅基流动）的 Base URL 是固定的，不允许修改。
     * 本地服务（如 Ollama、LM Studio）和自定义服务的 Base URL 可以修改。
     */
    private void updateBaseUrlFieldEditable() {
        String displayName = (String) providerComboBox.getSelectedItem();
        if (displayName == null) {
            baseUrlField.setEditable(false);
            return;
        }

        // 将显示名称转换为提供商标识符
        String providerId = AIProviderType.getProviderIdByDisplayName(displayName);
        AIProviderType providerType = providerId == null ? null : AIProviderType.fromProviderId(providerId);

        // 根据提供商类型的 baseUrlEditable 属性设置可编辑性
        boolean editable = providerType != null && providerType.isBaseUrlEditable();
        baseUrlField.setEditable(editable);
    }

    /**
     * 测试与 AI 服务提供商的连接
     * <p>
     * 该方法用于验证当前配置是否能够成功创建 AI 服务提供商，并测试其配置是否有效。
     * 在测试过程中，会临时允许创建未验证的提供商，测试完成后会根据结果更新配置状态。
     */
    private void testConnection() {
        // 简单测试, 只需要一个默认配置即可
        SettingsState testSettings = new SettingsState();
        // 使用设置面板的当前配置创建一个服务提供商配置
        SettingsState.ProviderConfig snapshotProviderConfig = getProviderConfigSnapshot();
        AIServiceProvider provider = AIServiceFactory.createProvider(testSettings, snapshotProviderConfig);

        // 检查提供商创建是否成功
        if (provider == null) {
            JOptionPane.showMessageDialog(
                getParentWindow(),
                "创建 AI 服务提供商失败，请检查配置是否正确（提供商、模型、Base URL 等）",
                JavaDocBundle.message("settings.error.title"),
                JOptionPane.ERROR_MESSAGE
                                         );
            return;
        }

        testConnectionButton.setEnabled(false);
        testConnectionButton.setText(JavaDocBundle.message("settings.test.connection.testing"));

        // 在后台线程测试
        new Thread(() -> {
            try {
                ValidationResult result = provider.validateConfiguration(new String(apiKeyField.getPassword()).trim());

                SwingUtilities.invokeLater(() -> {
                    if (result.isSuccess()) {
                        // 测试成功，标记配置为已验证
                        markConfigurationAsVerified();

                        // 添加到可用提供商列表
                        addToAvailableProviders(snapshotProviderConfig);

                        JOptionPane.showMessageDialog(
                            getParentWindow(),
                            result.getMessage(),
                            JavaDocBundle.message("settings.test.result.title"),
                            JOptionPane.INFORMATION_MESSAGE
                                                     );
                    } else {
                        // 测试失败，清除验证状态
                        markConfigurationAsUnverified();

                        // 从可用提供商列表中移除
                        removeFromAvailableProviders(snapshotProviderConfig);

                        // 构建详细的错误消息
                        String errorMessage = result.getMessage();
                        String errorDetails = result.getErrorDetails();
                        if (errorDetails != null && !errorDetails.isEmpty()) {
                            errorMessage = errorMessage + "\n\n详细信息:\n" + errorDetails;
                        }

                        JOptionPane.showMessageDialog(
                            getParentWindow(),
                            errorMessage,
                            JavaDocBundle.message("settings.test.result.title"),
                            JOptionPane.ERROR_MESSAGE
                                                     );
                    }
                    testConnectionButton.setText(JavaDocBundle.message("settings.test.connection"));
                    testConnectionButton.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    // 测试异常，清除验证状态
                    markConfigurationAsUnverified();

                    // 从可用提供商列表中移除
                    removeFromAvailableProviders(snapshotProviderConfig);

                    String errorMessage = JavaDocBundle.message("settings.test.connection.error", e.getMessage());
                    JOptionPane.showMessageDialog(
                        getParentWindow(),
                        errorMessage,
                        JavaDocBundle.message("settings.test.result.title"),
                        JOptionPane.ERROR_MESSAGE
                                                 );
                    testConnectionButton.setText(JavaDocBundle.message("settings.test.connection"));
                    testConnectionButton.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * 获取主面板组件
     * <p>
     * 返回应用程序中主面板的引用，用于界面展示或操作。
     *
     * @return 主面板组件
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * 获取对话框的父窗口
     * <p>
     * 用于确保 JOptionPane 对话框能够正确居中显示在设置窗口中。
     *
     * @return 包含主面板的顶层窗口，如果无法获取则返回 null
     */
    private Component getParentWindow() {
        return SwingUtilities.getWindowAncestor(mainPanel);
    }

    /**
     * 测试通过后将当前配置添加到可用提供商列表
     * <p>
     * 使用 defaultProviders 的 UUID 创建配置，确保 API Key 正确关联
     * <p>
     * 测试通过后，除了添加到 availableProviders，还要更新 defaultProviders 中当前服务商的配置，
     * 确保即使用户没有点击 Apply，配置也能被保存。
     */
    private void addToAvailableProviders(SettingsState.ProviderConfig snapshotProviderConfig) {
        SettingsState settings = SettingsState.getInstance();

        // 将 API Key 存储到 PasswordSafe（使用默认配置的 UUID）
        String apiKey = new String(apiKeyField.getPassword()).trim();
        if (!apiKey.trim().isEmpty()) {
            SettingsState.setApiKey(snapshotProviderConfig.md5, apiKey);
        }

        // 设置默认备注为当前时间
        if (snapshotProviderConfig.remark == null || snapshotProviderConfig.remark.trim().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            snapshotProviderConfig.remark = dateFormat.format(new Date());
        }

        // 先删除相同的配置
        removeFromAvailableProviders(snapshotProviderConfig);
        settings.availableProviders.add(snapshotProviderConfig);

        // 更新表格显示
        availableProvidersTableModel.setData(settings.availableProviders);

        // 更新 defaultProviders 中当前服务商的配置，确保配置被保存
        // 使用 snapshotProviderConfig 的 providerType，确保配置一致
        if (snapshotProviderConfig.providerType != null) {
            // 使用 snapshotProviderConfig 的副本，但更新验证状态为当前状态
            SettingsState.ProviderConfig defaultConfig = new SettingsState.ProviderConfig(snapshotProviderConfig);
            defaultConfig.configurationVerified = this.configurationVerified;

            // 更新 defaultProviders 中当前服务商的配置
            // 这样确保即使用户没有点击 Apply，配置也能被保存
            settings.updateDefaultProviderConfig(snapshotProviderConfig.providerType, defaultConfig);
        }
    }

    /**
     * 从可用提供者列表中移除指定的配置项
     * <p>
     * 根据提供的配置项的 MD5 值，从全局设置中的可用提供者列表中移除匹配的配置项
     *
     * @param snapshotProviderConfig 要移除的配置项
     */
    private void removeFromAvailableProviders(SettingsState.ProviderConfig snapshotProviderConfig) {
        SettingsState settings = SettingsState.getInstance();
        settings.availableProviders.removeIf(config -> config.md5.equals(snapshotProviderConfig.md5));

        // 同步更新表格显示
        availableProvidersTableModel.setData(settings.availableProviders);
    }

    /**
     * 删除表格中选中的服务商配置
     * <p>
     * 该方法会：
     * 1. 从表格模型中删除选中的行
     * 2. 从全局配置中删除对应的配置
     * 3. 从 PasswordSafe 中删除对应的 API Key
     *
     * @param selectedRow 选中的行索引
     */
    private void removeAvailableProvider(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= availableProvidersTableModel.getRowCount()) {
            return;
        }

        // 获取要删除的配置
        List<SettingsState.ProviderConfig> data = availableProvidersTableModel.getData();
        SettingsState.ProviderConfig configToRemove = data.get(selectedRow);

        // 确认删除
        String providerName = configToRemove.providerType != null ?
                              configToRemove.providerType.getDisplayName() : "未知";
        String modelName = configToRemove.modelName != null ? configToRemove.modelName : "未知";

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            String.format("确定要删除服务商 \"%s - %s\" 吗？", providerName, modelName),
            "确认删除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // 从全局配置中删除
        SettingsState settings = SettingsState.getInstance();
        settings.availableProviders.removeIf(config ->
                                                 config.md5 != null && config.md5.equals(configToRemove.md5));

        // 从表格模型中删除
        availableProvidersTableModel.removeRow(selectedRow);
    }

    /**
     * 清空所有可用服务商配置
     * <p>
     * 该方法会：
     * 1. 显示确认对话框
     * 2. 从表格模型中清空所有数据
     * 3. 从全局配置中清空所有配置
     * 4. 批量从 PasswordSafe 中删除所有 API Key
     */
    private void clearAllAvailableProviders() {
        if (availableProvidersTableModel.getRowCount() == 0) {
            return;
        }

        // 确认清空
        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            String.format("确定要清空所有可用服务商吗(%s 个)？\n此操作将删除所有已保存的配置和 API Key！",
                          availableProvidersTableModel.getRowCount()),
            "确认清空",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // 获取所有配置以删除对应的 API Key
        List<SettingsState.ProviderConfig> allConfigs = availableProvidersTableModel.getData();

        // 批量从 PasswordSafe 中删除 API Key
        for (SettingsState.ProviderConfig config : allConfigs) {
            if (config.md5 != null && !config.md5.trim().isEmpty()) {
                SettingsState.deleteApiKey(config.md5);
            }
        }

        // 从全局配置中清空
        SettingsState settings = SettingsState.getInstance();
        settings.availableProviders.clear();

        // 从表格模型中清空
        availableProvidersTableModel.clearAll();

    }

    /**
     * 标记配置为已验证
     * <p>
     * 测试连接成功后调用，更新内部验证状态并同步到 defaultProviders
     */
    private void markConfigurationAsVerified() {
        this.configurationVerified = true;

        // 更新 defaultProviders 中的验证状态
        SettingsState settings = SettingsState.getInstance();
        String displayName = (String) providerComboBox.getSelectedItem();
        String providerId = displayName != null ? AIProviderType.getProviderIdByDisplayName(displayName) : null;
        AIProviderType providerType = providerId != null ? AIProviderType.fromProviderId(providerId) : null;

        if (providerType != null) {
            // 获取当前的 API Key
            String apiKey = new String(apiKeyField.getPassword()).trim();

            // 使用当前配置创建新的 ProviderConfig，确保 md5 正确生成
            SettingsState.ProviderConfig newConfig = new SettingsState.ProviderConfig(
                apiKey,
                providerType,
                modelComboBox.getEditor().getItem().toString().trim(),
                SettingsState.normalizeBaseUrl(baseUrlField.getText().trim()),
                true
            );

            // 将 API Key 存储到 PasswordSafe（使用新配置的 md5）
            if (!apiKey.isEmpty()) {
                SettingsState.setApiKey(newConfig.md5, apiKey);
            }

            // 更新 defaultProviders
            settings.updateDefaultProviderConfig(providerType, newConfig);
        }
    }

    /**
     * 标记配置为未验证
     */
    private void markConfigurationAsUnverified() {
        this.configurationVerified = false;
    }


    /**
     * 在设置页面中获取未保存的提供商配置(快照)
     * <p>
     * 该方法用于从界面组件中提取当前的 AI 提供商配置信息，包括提供商类型、模型名称、基础 URL 等，并生成一个配置快照对象。
     * 同时，如果 API Key 不为空，会将其存储到 PasswordSafe 中。
     *
     * @return 包含当前 AI 提供商配置信息的 SettingsState.ProviderConfig 对象
     */
    public SettingsState.ProviderConfig getProviderConfigSnapshot() {
        // 将 API Key 存储到 PasswordSafe（使用默认配置的 UUID）
        String apiKey = new String(apiKeyField.getPassword()).trim();

        // AI 提供商配置 - 将显示名称转换为提供商标识符
        String displayName = (String) providerComboBox.getSelectedItem();
        String providerId = displayName != null ? AIProviderType.getProviderIdByDisplayName(displayName) : null;

        // 获取用户输入的模型名称（可能是从列表选择的，也可能是手动输入的）
        Object selectedModel = modelComboBox.getEditor().getItem();

        return new SettingsState.ProviderConfig(
            apiKey,
            providerId != null ? AIProviderType.fromProviderId(providerId) : null,
            selectedModel != null ? selectedModel.toString().trim() : "",
            SettingsState.normalizeBaseUrl(baseUrlField.getText().trim()),
            true
        );
    }

    /**
     * 从 UI 获取配置
     */
    @NotNull
    public SettingsState getSettings() {
        SettingsState settings = new SettingsState();

        // 从全局配置中深拷贝 availableProviders 和 defaultProviders，避免对象引用共享
        SettingsState globalSettings = SettingsState.getInstance();

        // 从表格模型中获取 availableProviders（包含用户在表格中编辑的备注）
        settings.availableProviders.addAll(availableProvidersTableModel.getData());

        // 深拷贝 defaultProviders（避免修改全局配置）
        for (Map.Entry<AIProviderType, SettingsState.ProviderConfig> entry : globalSettings.defaultProviders.entrySet()) {
            settings.defaultProviders.put(entry.getKey(), new SettingsState.ProviderConfig(entry.getValue()));
        }

        // AI 提供商配置 - 将显示名称转换为提供商标识符
        String displayName = (String) providerComboBox.getSelectedItem();
        String providerId = displayName != null ? AIProviderType.getProviderIdByDisplayName(displayName) : null;
        AIProviderType providerType = providerId != null ? AIProviderType.fromProviderId(providerId) : null;
        settings.providerType = providerType != null ? providerType : AIProviderType.QIANWEN;

        // 直接更新 defaultProviders 中当前服务商的配置
        SettingsState.ProviderConfig defaultConfig = settings.getDefaultProviderConfig(settings.providerType);
        
        // 获取用户输入的模型名称（可能是从列表选择的，也可能是手动输入的）
        Object selectedModel = modelComboBox.getEditor().getItem();
        defaultConfig.modelName = selectedModel != null ? selectedModel.toString().trim() : "";

        // 设置 baseUrl（使用标准化方法）
        defaultConfig.baseUrl = SettingsState.normalizeBaseUrl(baseUrlField.getText().trim());

        // 获取 API Key 并更新 md5（因为 md5 是基于 providerType、baseUrl、modelName 和 apiKey 计算的）
        // 注意：这里只计算 md5，不保存 API Key 到 PasswordSafe
        // API Key 的保存应该在 apply() 方法中统一处理，避免在 isModified() 频繁调用时重复保存
        String apiKey = new String(apiKeyField.getPassword()).trim();
        defaultConfig.md5 = defaultConfig.buildMd5(apiKey);
        
        // 设置验证状态
        defaultConfig.configurationVerified = this.configurationVerified;

        // 更新 defaultProviders
        settings.updateDefaultProviderConfig(settings.providerType, defaultConfig);

        // 功能配置
        settings.generateForClass = generateForClassCheckBox.isSelected();
        settings.generateForMethod = generateForMethodCheckBox.isSelected();
        settings.generateForField = generateForFieldCheckBox.isSelected();
        settings.overrideExisting = overrideExistingCheckBox.isSelected();
        settings.enableCodeCompression = enableCodeCompressionCheckBox.isSelected();
        settings.maxClassCodeLines = (Integer) maxClassCodeLinesSpinner.getValue();
        settings.addSpaceBetweenChineseAndEnglish = addSpaceBetweenChineseAndEnglishCheckBox.isSelected();
        settings.replaceChinesePunctuation = replaceChinesePunctuationCheckBox.isSelected();

        // 语言支持
        settings.supportedLanguages = new HashSet<>();
        if (javaCheckBox.isSelected()) {
            settings.supportedLanguages.add("java");
        }
        if (kotlinCheckBox.isSelected()) {
            settings.supportedLanguages.add("kotlin");
        }

        // 高级配置
        settings.maxRetries = (Integer) maxRetriesSpinner.getValue();
        settings.timeout = (Integer) timeoutSpinner.getValue();
        settings.temperature = (Double) temperatureSpinner.getValue();
        settings.maxTokens = (Integer) maxTokensSpinner.getValue();
        settings.topP = (Double) topPSpinner.getValue();
        settings.topK = (Integer) topKSpinner.getValue();
        settings.presencePenalty = (Double) presencePenaltySpinner.getValue();
        settings.verboseLogging = verboseLoggingCheckBox.isSelected();
        settings.performanceMode = performanceModeCheckBox.isSelected();
        settings.showProviderStatistics = showProviderStatisticsCheckBox.isSelected();

        // Prompt 配置 - 从 Tab 页获取
        settings.systemPromptTemplate = systemPromptTextArea.getText().trim();
        settings.classPromptTemplate = classPromptTextArea.getText().trim();
        settings.methodPromptTemplate = methodPromptTextArea.getText().trim();
        settings.fieldPromptTemplate = fieldPromptTextArea.getText().trim();
        settings.testPromptTemplate = testPromptTextArea.getText().trim();

        // 自定义 JavaDoc 标签配置
        settings.customJavaDocTags = customJavaDocTagsTableModel.getData();
        settings.showCustomJavaDocTags = showCustomJavaDocTagsCheckBox.isSelected();

        // 高级设置显示状态
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();

        return settings;
    }

    /**
     * 加载配置到 UI
     */
    @SuppressWarnings("DuplicatedCode")
    public void loadSettings(@NotNull SettingsState settings) {
        // AI 提供商配置 - 将提供商标识符转换为显示名称
        AIProviderType providerType = settings.providerType != null ? settings.providerType : AIProviderType.QIANWEN;
        String displayName = providerType.getDisplayName();
        providerComboBox.setSelectedItem(displayName);
        updateModelList();

        // 从 defaultProviders 获取当前服务商的配置
        SettingsState.ProviderConfig defaultConfig = settings.getDefaultProviderConfig(providerType);
        modelComboBox.setSelectedItem(defaultConfig.modelName);
        baseUrlField.setText(defaultConfig.baseUrl);

        // 异步加载 PasswordSafe 中的 API Key
        loadApiKeyForProvider(defaultConfig, providerType.getProviderId());

        // 加载验证状态
        this.configurationVerified = defaultConfig.configurationVerified;

        // 加载可用服务商列表
        availableProvidersTableModel.setData(settings.availableProviders);

        // 功能配置
        generateForClassCheckBox.setSelected(settings.generateForClass);
        generateForMethodCheckBox.setSelected(settings.generateForMethod);
        generateForFieldCheckBox.setSelected(settings.generateForField);
        overrideExistingCheckBox.setSelected(settings.overrideExisting);
        enableCodeCompressionCheckBox.setSelected(settings.enableCodeCompression);
        maxClassCodeLinesSpinner.setValue(settings.maxClassCodeLines);
        addSpaceBetweenChineseAndEnglishCheckBox.setSelected(settings.addSpaceBetweenChineseAndEnglish);
        replaceChinesePunctuationCheckBox.setSelected(settings.replaceChinesePunctuation);

        // 根据代码压缩设置更新最大行数输入框的可用性
        updateMaxClassCodeLinesEnabled();

        // 根据性能模式设置更新显示统计信息复选框的可用性
        performanceModeCheckBox.setSelected(settings.performanceMode);
        showProviderStatisticsCheckBox.setSelected(settings.showProviderStatistics);
        updateShowProviderStatisticsEnabled();
        updateShowAvailableProvidersEnabled();

        // 语言支持
        javaCheckBox.setSelected(settings.supportedLanguages.contains("java"));
        kotlinCheckBox.setSelected(settings.supportedLanguages.contains("kotlin"));

        // 高级配置
        maxRetriesSpinner.setValue(settings.maxRetries);
        timeoutSpinner.setValue(settings.timeout);
        temperatureSpinner.setValue(settings.temperature);
        maxTokensSpinner.setValue(settings.maxTokens);
        topPSpinner.setValue(settings.topP);
        topKSpinner.setValue(settings.topK);
        presencePenaltySpinner.setValue(settings.presencePenalty);
        verboseLoggingCheckBox.setSelected(settings.verboseLogging);

        // 更新所有复选框的提示文本颜色（必须在所有复选框状态设置完成后调用）
        updateAllCheckBoxHintColors();

        // Prompt 配置 - 加载到 Tab 页
        systemPromptTextArea.setText(settings.systemPromptTemplate);
        classPromptTextArea.setText(settings.classPromptTemplate);
        methodPromptTextArea.setText(settings.methodPromptTemplate);
        fieldPromptTextArea.setText(settings.fieldPromptTemplate);
        testPromptTextArea.setText(settings.testPromptTemplate);

        // 加载自定义 JavaDoc 标签配置
        customJavaDocTagsTableModel.setData(settings.customJavaDocTags);
        showCustomJavaDocTagsCheckBox.setSelected(settings.showCustomJavaDocTags);
        customJavaDocTagsPanel.setVisible(settings.showCustomJavaDocTags);

        // 高级设置显示状态
        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);

        updateApiKeyFieldEnabled();
        updateBaseUrlFieldEditable();
    }

    /**
     * 添加自定义 JavaDoc 标签
     */
    private void addCustomJavaDocTag() {
        String tagName = JOptionPane.showInputDialog(
            getParentWindow(),
            JavaDocBundle.message("settings.custom.javadoc.tags.add.prompt"),
            JavaDocBundle.message("settings.custom.javadoc.tags.add.title"),
            JOptionPane.QUESTION_MESSAGE
                                                    );

        if (tagName != null && !tagName.trim().isEmpty()) {
            tagName = tagName.trim();

            // 验证标签名称
            if (!SettingsState.isValidTagName(tagName)) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavaDocBundle.message("settings.custom.javadoc.tags.invalid.name", tagName),
                    JavaDocBundle.message("settings.error.title"),
                    JOptionPane.ERROR_MESSAGE
                                             );
                return;
            }

            // 检查是否已存在
            List<String> currentTags = customJavaDocTagsTableModel.getData();
            String tagNameLower = tagName.toLowerCase();
            if (currentTags.stream().anyMatch(t -> t.toLowerCase().equals(tagNameLower))) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavaDocBundle.message("settings.custom.javadoc.tags.already.exists", tagName),
                    JavaDocBundle.message("settings.error.title"),
                    JOptionPane.WARNING_MESSAGE
                                             );
                return;
            }

            // 添加到表格
            customJavaDocTagsTableModel.addTag(tagName);
        }
    }

    /**
     * 删除自定义 JavaDoc 标签
     */
    private void removeCustomJavaDocTag(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= customJavaDocTagsTableModel.getRowCount()) {
            return;
        }

        String tagName = customJavaDocTagsTableModel.getData().get(selectedRow);

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            JavaDocBundle.message("settings.custom.javadoc.tags.delete.confirm", tagName),
            JavaDocBundle.message("settings.custom.javadoc.tags.delete.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            customJavaDocTagsTableModel.removeRow(selectedRow);
        }
    }

    /**
     * 清空所有自定义 JavaDoc 标签
     */
    private void clearAllCustomJavaDocTags() {
        if (customJavaDocTagsTableModel.getRowCount() == 0) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            JavaDocBundle.message("settings.custom.javadoc.tags.clear.confirm",
                                  customJavaDocTagsTableModel.getRowCount()),
            JavaDocBundle.message("settings.custom.javadoc.tags.clear.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            customJavaDocTagsTableModel.clearAll();
        }
    }

    /**
     * 可用服务商列表的表格模型
     * <p>
     * 用于在表格中显示已验证的服务商配置信息
     */
    private static class AvailableProvidersTableModel extends AbstractTableModel {
        private final String[] columnNames = {"服务商", "模型", "备注"};
        private final List<SettingsState.ProviderConfig> data;

        public AvailableProvidersTableModel() {
            this.data = new ArrayList<>();
        }

        public void setData(List<SettingsState.ProviderConfig> newData) {
            this.data.clear();
            this.data.addAll(newData);
            fireTableDataChanged();
        }

        public List<SettingsState.ProviderConfig> getData() {
            return new ArrayList<>(data);
        }

        public void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        public void clearAll() {
            int size = data.size();
            if (size > 0) {
                data.clear();
                fireTableRowsDeleted(0, size - 1);
            }
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
            SettingsState.ProviderConfig config = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> config.providerType != null ? config.providerType.getDisplayName() : "";
                case 1 -> config.modelName != null ? config.modelName : "";
                case 2 -> config.remark != null ? config.remark : "";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // 只有备注列可以编辑
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

    /**
     * 自定义 JavaDoc 标签列表的表格模型
     */
    private static class CustomJavaDocTagsTableModel extends AbstractTableModel {
        private final String[] columnNames = {JavaDocBundle.message("settings.custom.javadoc.tags.column.name")};
        private final List<String> data;

        public CustomJavaDocTagsTableModel() {
            this.data = new ArrayList<>();
        }

        public void setData(List<String> newData) {
            this.data.clear();
            if (newData != null) {
                this.data.addAll(newData);
            }
            fireTableDataChanged();
        }

        public List<String> getData() {
            return new ArrayList<>(data);
        }

        public void addTag(String tagName) {
            data.add(tagName);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        public void clearAll() {
            int size = data.size();
            if (size > 0) {
                data.clear();
                fireTableRowsDeleted(0, size - 1);
            }
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
            if (rowIndex >= 0 && rowIndex < data.size()) {
                return data.get(rowIndex);
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // 允许编辑标签名称
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size() && aValue != null) {
                String newTagName = aValue.toString().trim();

                // 验证标签名称
                if (!SettingsState.isValidTagName(newTagName)) {
                    // 可以显示错误提示，这里简单处理为不更新
                    return;
                }

                // 检查是否与其他标签重复
                String newTagNameLower = newTagName.toLowerCase();
                for (int i = 0; i < data.size(); i++) {
                    if (i != rowIndex && data.get(i).toLowerCase().equals(newTagNameLower)) {
                        // 重复标签，不更新
                        return;
                    }
                }

                data.set(rowIndex, newTagName);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}


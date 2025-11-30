package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import dev.dong4j.zeka.stack.idea.plugin.common.icons.AICommonIcons;
import dev.dong4j.zeka.stack.idea.plugin.settings.CustomJavaDocTag;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * JavaDoc 设置面板类
 * <p>
 * 提供 JavaDoc 生成工具的配置界面, 允许用户配置 AI 提供商, 生成规则, 语言支持,
 * 代码压缩, 性能模式等各项设置. 该面板包含多个功能区域, 如 AI 提供商选择,
 * 生成规则配置, 自定义 JavaDoc 标签管理, 高级提示模板设置等.
 * 支持 Java 和 Kotlin 语言, 提供中文英文间距调整, 中文标点替换等本地化功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class JavaDocSettingsPanel {

    /** 主界面主面板，用于承载主要功能组件和布局 */
    private JPanel mainPanel;

    /** AI 提供商选择下拉框 */
    private JComboBox<AIProviderConfig> providerComboBox;

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
    /** 性能模式复选框 */
    private JBCheckBox performanceModeCheckBox;
    /** 显示任务统计复选框 */
    private JBCheckBox showProviderStatisticsCheckBox;

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

    /** 类代码最大行数标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesLabel;
    /** 类代码最大行数提示标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesHintLabel;

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

    /** AI 提供商设置变更监听器 */
    private AIProviderSettingsListener providerSettingsListener;

    /** AI 提供商选择面板（用于动态刷新） */
    private JPanel aiProviderSelectionPanel;


    /**
     * 构造函数，初始化 JavaDoc 设置面板
     * <p>
     * 调用创建用户界面和设置事件监听器的方法，完成面板的初始化
     */
    public JavaDocSettingsPanel() {
        createUI();
        setupListeners();
        registerProviderSettingsListener();
    }

    /**
     * 获取主面板
     * <p>
     * 返回用于显示主要内容的面板组件
     *
     * @return 主面板组件
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * 初始化用户界面组件，创建并配置所有 UI 元素，包括下拉框、文本字段、按钮、复选框等。
     * <p>
     * 该方法负责构建整个设置界面的主面板，包括 AI 提供商配置、模型选择、基础 URL 和 API 密钥输入、
     * 连接测试按钮、模型刷新按钮、生成选项、语言支持、高级配置参数以及提示模板区域。
     */
    private void createUI() {
        // 功能配置
        generateForClassCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.class"));
        generateForMethodCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.method"));
        generateForFieldCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.field"));
        overrideExistingCheckBox = new JBCheckBox(JavaDocBundle.message("settings.override.existing"));
        enableCodeCompressionCheckBox = new JBCheckBox(JavaDocBundle.message("settings.enable.code.compression"));
        maxClassCodeLinesSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 300000, 100));
        addSpaceBetweenChineseAndEnglishCheckBox = new JBCheckBox(JavaDocBundle.message("settings.add.space.between.chinese.and.english"));
        replaceChinesePunctuationCheckBox = new JBCheckBox(JavaDocBundle.message("settings.replace.chinese.punctuation"));
        performanceModeCheckBox = new JBCheckBox(JavaDocBundle.message("settings.performance.mode"));
        showProviderStatisticsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.show.provider.statistics"));

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
            .setAddAction(button -> addCustomJavaDocTag())
            .setRemoveAction(button -> {
                int selectedRow = customJavaDocTagsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    removeCustomJavaDocTag(selectedRow);
                }
            })
            .addExtraAction(new AnAction(JavaDocBundle.message("settings.custom.javadoc.tags.clear.all"),
                                         JavaDocBundle.message("settings.custom.javadoc.tags.clear.all.description"),
                                         com.intellij.icons.AllIcons.Actions.GC) {
                /**
                 * 处理动作事件, 清除所有自定义的 JavaDoc 标签
                 * <p>
                 * 该方法用于响应动作事件, 执行清除自定义 JavaDoc 标签的操作
                 *
                 * @param e 动作事件对象
                 */
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllCustomJavaDocTags();
                }

                /**
                 * 根据表格数据状态更新操作按钮的启用状态
                 * <p>
                 * 检查自定义 JavaDoc 标签表格中是否有数据行, 若有则启用按钮, 否则禁用
                 *
                 * @param e 动作事件对象, 包含操作相关的上下文信息
                 */
                @Override
                public void update(@NotNull AnActionEvent e) {
                    // 根据表格状态启用/禁用按钮
                    boolean hasData = customJavaDocTagsTableModel.getRowCount() > 0;
                    e.getPresentation().setEnabled(hasData);
                }

                /**
                 * 获取动作更新线程
                 * <p>
                 * 返回用于更新动作的线程, 该线程为事件调度线程 (EDT)
                 *
                 * @return 动作更新线程
                 */
                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    // 需要访问 Swing 组件（表格模型），必须在 EDT 中执行
                    return ActionUpdateThread.EDT;
                }
            });

        customJavaDocTagsPanel = tagsDecorator.createPanel();
        // 可见性将在 loadSettings 中根据配置设置

        // Prompt 配置 - 创建文本区域（将在 Tab 页中使用）
        // 增加初始高度：15行（原来10行），宽度保持50列不变
        systemPromptTextArea = new JTextArea(15, 50);
        classPromptTextArea = new JTextArea(15, 50);
        methodPromptTextArea = new JTextArea(15, 50);
        fieldPromptTextArea = new JTextArea(15, 50);
        testPromptTextArea = new JTextArea(15, 50);

        // 创建高级设置复选框
        showAdvancedSettingsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.prompt.settings.show"));

        // 创建高级设置容器面板
        advancedSettingsPanel = new JPanel(new BorderLayout());
        advancedSettingsPanel.setVisible(false); // 默认隐藏

        // 构建高级设置面板内容（只包含 Prompt 模板，AI 配置已在 AIProviderConfigPanel 中）
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            // Prompt 模板与提示词
            .addComponent(createPromptTemplatesPanel())
            .getPanel();

        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            // 第一组：AI 提供商选择
            .addComponent(aiProviderSelectionPanel = createAIProviderSelectionPanel())
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
     * 创建 AI 提供商选择面板
     * <p>
     * 只显示供应商选择下拉框，其他 AI 配置在 Settings → Tools → IntelliAI Engine 中管理。
     *
     * @return AI 提供商选择面板
     */
    private JPanel createAIProviderSelectionPanel() {
        // 从 intelli-ai-engine 获取可用服务商列表
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        JPanel panel;

        // 如果没有可用服务商，显示提示信息和跳转链接
        if (aiProviderTypes.isEmpty()) {
            // 创建提示信息面板
            JBLabel warningLabel = new JBLabel(JavaDocBundle.message("settings.ai.provider.no.available.warning"));
            // 使用警告颜色（如果系统不支持，则使用默认的警告颜色）
            java.awt.Color warningColor = UIManager.getColor("Label.warningForeground");
            if (warningColor == null) {
                warningColor = new JBColor(new Color(255, 140, 0), new Color(255, 140, 0)); // 橙色作为警告颜色
            }
            warningLabel.setForeground(warningColor);

            // 创建跳转链接
            HyperlinkLabel linkLabel = new HyperlinkLabel(JavaDocBundle.message("settings.ai.provider.open.ai.common.settings"));
            linkLabel.addHyperlinkListener(e -> {
                // 打开 IntelliAI Engine 全局设置页面（应用级配置）
                // 使用 null 作为 parent 参数表示打开应用级（全局）配置，而不是项目级配置
                // 直接创建 Configurable 实例，确保能够正确打开配置页面
                ShowSettingsUtil.getInstance().editConfigurable(null, "IntelliAI Engine");
            });

            // 创建空的下拉框（禁用状态）
            providerComboBox = new ComboBox<>(new AIProviderConfig[0]);
            providerComboBox.setEnabled(false);

            panel = FormBuilder.createFormBuilder()
                .addComponent(warningLabel)
                .addComponent(linkLabel)
                .addComponent(new JBLabel()) // 空行
                .addLabeledComponent(new JBLabel(JavaDocBundle.message("settings.ai.provider") + ":"), providerComboBox)
                .getPanel();
        } else {
            // 创建供应商下拉框
            providerComboBox = new ComboBox<>(aiProviderTypes.toArray(new AIProviderConfig[0]));
            providerComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {

                JBLabel label = new JBLabel();
                if (value != null) {
                    Icon icon = AICommonIcons.getProviderIcon(value.providerType);
                    label.setIcon(icon);
                    label.setText(value.providerType.getDisplayName() + ":" + value.modelName);
                }
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                label.setOpaque(true);
                return label;
            });

            JBLabel providerLabel = new JBLabel(JavaDocBundle.message("settings.ai.provider") + ":");
            JBLabel hintLabel = new JBLabel(JavaDocBundle.message("settings.ai.provider.hint"));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1f));

            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(providerLabel, providerComboBox)
                .addComponent(hintLabel)
                .getPanel();
        }

        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.ai.provider.selection")));

        return panel;
    }

    /**
     * 获取已验证的 AI 服务提供商类型列表
     * <p>
     * 从全局设置中获取已验证的 AI 服务提供商配置, 并提取其中唯一的提供商类型.
     *
     * @return 包含已验证 AI 服务提供商类型的列表
     */
    @NotNull
    private static List<AIProviderConfig> getAiProviderTypes() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getVerifiedProviders();
    }

    /**
     * 注册 AI 提供商设置变更监听器
     * <p>
     * 当 IntelliAI Engine 设置中的可用提供商列表发生变化时，自动刷新下拉列表。
     */
    private void registerProviderSettingsListener() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        providerSettingsListener = settings -> refreshProviderComboBox();
        globalSettings.addListener(providerSettingsListener);
    }

    /**
     * 刷新提供商下拉框
     * <p>
     * 从 IntelliAI Engine 设置中重新获取可用提供商列表，并更新下拉框内容。
     * 如果之前没有可用提供商，现在有了，会重新创建面板。
     */
    @SuppressWarnings("D")
    private void refreshProviderComboBox() {
        if (aiProviderSelectionPanel == null) {
            return;
        }

        // 从 intelli-ai-engine 获取可用服务商列表
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        // 判断之前是否有可用提供商（通过下拉框是否启用来判断）
        boolean hadProviders = providerComboBox != null && providerComboBox.isEnabled();
        boolean hasProviders = !aiProviderTypes.isEmpty();

        // 如果状态没有变化，只需要更新下拉框内容
        if (hadProviders && hasProviders) {
            // 保存当前选中的值
            AIProviderConfig selectedValue = (AIProviderConfig) providerComboBox.getSelectedItem();

            // 更新下拉框模型
            providerComboBox.setModel(new DefaultComboBoxModel<>(aiProviderTypes.toArray(new AIProviderConfig[0])));

            // 恢复之前选中的值（如果还存在）
            if (selectedValue != null && aiProviderTypes.contains(selectedValue)) {
                providerComboBox.setSelectedItem(selectedValue);
            } else if (!aiProviderTypes.isEmpty()) {
                // 如果之前选中的值不存在了，选择第一个
                providerComboBox.setSelectedIndex(0);
            }
            return;
        }

        // 如果状态发生变化（从无到有，或从有到无），需要重新创建整个面板
        JPanel parent = (JPanel) aiProviderSelectionPanel.getParent();
        if (parent != null) {
            int index = -1;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                if (parent.getComponent(i) == aiProviderSelectionPanel) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                parent.remove(index);
                JPanel newPanel = createAIProviderSelectionPanel();
                aiProviderSelectionPanel = newPanel;
                parent.add(newPanel, index);
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    /**
     * 释放资源
     * <p>
     * 取消注册监听器，避免内存泄漏。
     * 应该在设置页面关闭时调用。
     */
    public void dispose() {
        if (providerSettingsListener != null) {
            AIProviderSettings globalSettings = AIProviderSettings.getInstance();
            globalSettings.removeListener(providerSettingsListener);
            providerSettingsListener = null;
        }
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
     * 创建带边框的面板
     *
     * <p>通用方法，用于创建包含任意数量组件的带边框面板。
     * 将多个组件使用 FormBuilder 添加到内容面板中，
     * 然后为面板添加带标题的边框。
     *
     * @param borderTitle 边框标题的国际化键
     * @param components  要添加到面板中的组件（可变参数）
     * @return 带边框的面板
     */
    private JPanel createPanelWithBorder(String borderTitle, JComponent... components) {
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        for (JComponent component : components) {
            formBuilder.addComponent(component);
        }
        JPanel contentPanel = formBuilder.getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message(borderTitle)
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
        return createPanelWithBorder("settings.language.support", javaCheckBox, kotlinCheckBox);
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
            .addComponent(createCheckBoxWithHint(performanceModeCheckBox, "settings.performance.mode.hint"))
            .addComponent(createPerformanceModeSubConfigPanel())
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
        // 创建自定义 JavaDoc 标签提示标签
        JBLabel customTagsHintLabel = new JBLabel(JavaDocBundle.message("settings.custom.javadoc.tags.hint"));
        customTagsHintLabel.setFont(customTagsHintLabel.getFont().deriveFont(customTagsHintLabel.getFont().getSize() - 1f));
        customTagsHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        customTagsHintLabel.setBorder(JBUI.Borders.emptyLeft(22)); // 与复选框对齐

        return createPanelWithBorder("settings.other.settings",
                                     showCustomJavaDocTagsCheckBox,
                                     customTagsHintLabel,
                                     customJavaDocTagsPanel);
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
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

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
     * 创建性能模式子配置面板
     * <p>
     * 该方法构建一个用于配置性能模式相关选项的面板，包含显示任务统计复选框。
     * 需要向右缩进2个空格（约22像素）。
     *
     * @return 返回配置好的性能模式子配置面板
     */
    private JPanel createPerformanceModeSubConfigPanel() {
        // 性能模式的子配置面板，包含显示任务统计
        // 需要向右缩进2个空格（约22像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(20));

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showProviderStatisticsCheckBox, "settings.show.provider.statistics.hint"))
            .getPanel();

        indentPanel.add(contentPanel, BorderLayout.CENTER);
        return indentPanel;
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
     * 更新所有复选框的提示文本颜色
     * <p>
     * 根据每个复选框的当前选中状态，更新对应的提示文本颜色。
     * 用于在加载设置时初始化提示文本的颜色。
     */
    private void updateAllCheckBoxHintColors() {
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) ->
                                         updateHintLabelColor(hintLabel, checkBox.isSelected()));
    }

    /**
     * 创建水平排列的复选框面板
     *
     * @param checkBoxes  复选框数组
     * @param hintKeys    对应的提示文本键数组
     * @param itemsPerRow 每行显示的复选框数量
     * @return 水平排列的复选框面板
     */
    @SuppressWarnings("SameParameterValue")
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
            /**
             * 处理文档事件, 调整文本区域大小
             * <p>
             * 当文档事件发生时, 调用 adjustTextAreaSize 方法调整文本区域的大小
             *
             * @param e 文档事件对象
             */
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            /**
             * 文档更新时触发的回调方法.
             * <p>
             * 当文本域的内容发生变化时, 该方法会被调用,
             *
             * @param e 文档事件, 包含更新的相关信息
             */
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            /**
             * 处理文档内容变化事件, 调整文本区域大小
             * <p>
             * 当文本区域内容发生变化时, 调用 adjustTextAreaSize 方法调整其尺寸
             *
             * @param e 文档变化事件对象
             */
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

        // 创建内容面板，包含占位符说明（如果是类提示）和文本区域
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 如果是类提示，添加占位符说明
        if ("class".equals(promptType)) {
            JBLabel placeholderHint = new JBLabel(JavaDocBundle.message("settings.prompt.class.placeholder.hint"));
            placeholderHint.setFont(placeholderHint.getFont().deriveFont(placeholderHint.getFont().getSize() - 1f));
            placeholderHint.setForeground(UIManager.getColor("Label.disabledForeground"));
            placeholderHint.setBorder(JBUI.Borders.empty(5, 10));
            contentPanel.add(placeholderHint, BorderLayout.NORTH);
        }

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        tabPanel.add(contentPanel, BorderLayout.CENTER);

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
     * 初始化各种监听器，用于响应用户界面组件的变化
     * <p>
     * 该方法为各个输入组件添加动作监听器，当组件内容发生变化时，触发相应的更新或验证状态清除操作。
     * 包括提供商、Base URL、API Key、模型选择以及代码优化配置等变化的监听。
     */
    private void setupListeners() {
        enableCodeCompressionCheckBox.addActionListener(e -> updateMaxClassCodeLinesEnabled());

        showAdvancedSettingsCheckBox.addActionListener(e -> {
            boolean selected = showAdvancedSettingsCheckBox.isSelected();
            advancedSettingsPanel.setVisible(selected);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        showCustomJavaDocTagsCheckBox.addActionListener(e ->
                                                            customJavaDocTagsPanel.setVisible(showCustomJavaDocTagsCheckBox.isSelected())
                                                       );

        performanceModeCheckBox.addActionListener(e -> updatePerformanceModeSubConfigEnabled());
    }

    /**
     * 更新性能模式子配置的启用状态
     * <p>
     * 根据性能模式复选框的状态, 启用或禁用相关配置项, 并更新提示标签的显示状态和颜色.
     */
    private void updatePerformanceModeSubConfigEnabled() {
        boolean enabled = performanceModeCheckBox.isSelected();
        showProviderStatisticsCheckBox.setEnabled(enabled);
        if (!enabled) {
            showProviderStatisticsCheckBox.setSelected(false);
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
    }

    /**
     * 获取当前设置状态对象, 用于保存用户配置的各类设置信息.
     * <p>
     * 该方法会从界面组件中读取用户选择的配置项, 并将其封装到 SettingsState 对象中.
     * 包括提供者设置, 生成选项, 语言支持, 提示模板, 自定义 JavaDoc 标签等.
     *
     * @return 当前设置状态对象
     * @since 1.0
     */
    @NotNull
    public SettingsState getSettings() {
        SettingsState settings = new SettingsState();

        // 获取选择的供应商类型
        settings.providerConfig = (AIProviderConfig) providerComboBox.getSelectedItem();

        settings.generateForClass = generateForClassCheckBox.isSelected();
        settings.generateForMethod = generateForMethodCheckBox.isSelected();
        settings.generateForField = generateForFieldCheckBox.isSelected();
        settings.overrideExisting = overrideExistingCheckBox.isSelected();
        settings.enableCodeCompression = enableCodeCompressionCheckBox.isSelected();
        settings.maxClassCodeLines = (Integer) maxClassCodeLinesSpinner.getValue();
        settings.addSpaceBetweenChineseAndEnglish = addSpaceBetweenChineseAndEnglishCheckBox.isSelected();
        settings.replaceChinesePunctuation = replaceChinesePunctuationCheckBox.isSelected();
        settings.performanceMode = performanceModeCheckBox.isSelected();
        settings.showProviderStatistics = showProviderStatisticsCheckBox.isSelected();

        settings.supportedLanguages = new HashSet<>();
        if (javaCheckBox.isSelected()) {
            settings.supportedLanguages.add("java");
        }
        if (kotlinCheckBox.isSelected()) {
            settings.supportedLanguages.add("kotlin");
        }

        settings.systemPromptTemplate = systemPromptTextArea.getText().trim();
        settings.classPromptTemplate = classPromptTextArea.getText().trim();
        settings.methodPromptTemplate = methodPromptTextArea.getText().trim();
        settings.fieldPromptTemplate = fieldPromptTextArea.getText().trim();
        settings.testPromptTemplate = testPromptTextArea.getText().trim();

        // 获取标签列表（已经是 List<CustomJavaDocTag>）
        settings.customJavaDocTags = new ArrayList<>(customJavaDocTagsTableModel.getData());
        settings.showCustomJavaDocTags = showCustomJavaDocTagsCheckBox.isSelected();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();

        return settings;
    }

    /**
     * 加载设置配置到界面组件中
     * <p>
     * 将传入的 SettingsState 对象中的配置信息同步到各个 UI 控件中, 包括生成选项, 代码压缩设置,
     * 提示模板, 自定义 JavaDoc 标签等高级设置.
     *
     * @param settings 包含所有设置信息的 SettingsState 对象
     */
    @SuppressWarnings("DuplicatedCode")
    public void loadSettings(@NotNull SettingsState settings) {
        // 从 intelli-ai-engine 获取可用服务商列表

        if (settings.providerConfig == null) {
            final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();
            if (CollectionUtils.isNotEmpty(aiProviderTypes)) {
                providerComboBox.setSelectedItem(aiProviderTypes.get(0));
            }
        } else {
            providerComboBox.setSelectedItem(settings.providerConfig);
        }

        generateForClassCheckBox.setSelected(settings.generateForClass);
        generateForMethodCheckBox.setSelected(settings.generateForMethod);
        generateForFieldCheckBox.setSelected(settings.generateForField);
        overrideExistingCheckBox.setSelected(settings.overrideExisting);
        enableCodeCompressionCheckBox.setSelected(settings.enableCodeCompression);
        maxClassCodeLinesSpinner.setValue(settings.maxClassCodeLines);
        addSpaceBetweenChineseAndEnglishCheckBox.setSelected(settings.addSpaceBetweenChineseAndEnglish);
        replaceChinesePunctuationCheckBox.setSelected(settings.replaceChinesePunctuation);
        performanceModeCheckBox.setSelected(settings.performanceMode);
        showProviderStatisticsCheckBox.setSelected(settings.showProviderStatistics);

        updateMaxClassCodeLinesEnabled();
        updatePerformanceModeSubConfigEnabled();

        javaCheckBox.setSelected(settings.supportedLanguages.contains("java"));
        kotlinCheckBox.setSelected(settings.supportedLanguages.contains("kotlin"));

        systemPromptTextArea.setText(settings.systemPromptTemplate);
        classPromptTextArea.setText(settings.classPromptTemplate);
        methodPromptTextArea.setText(settings.methodPromptTemplate);
        fieldPromptTextArea.setText(settings.fieldPromptTemplate);
        testPromptTextArea.setText(settings.testPromptTemplate);

        // 设置标签列表（已经是 List<CustomJavaDocTag>）
        if (settings.customJavaDocTags != null) {
            customJavaDocTagsTableModel.setData(new ArrayList<>(settings.customJavaDocTags));
        } else {
            customJavaDocTagsTableModel.setData(new ArrayList<>());
        }
        showCustomJavaDocTagsCheckBox.setSelected(settings.showCustomJavaDocTags);
        customJavaDocTagsPanel.setVisible(settings.showCustomJavaDocTags);

        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);

        updateAllCheckBoxHintColors();
    }

    /**
     * 获取主面板所在的顶级窗口
     * <p>
     * 使用 Swing 工具类查找主面板的顶级窗口祖先
     *
     * @return 主面板所在的顶级窗口, 若未找到则返回 null
     */
    private java.awt.Window getParentWindow() {
        return SwingUtilities.getWindowAncestor(mainPanel);
    }

    /**
     * 添加自定义 JavaDoc 标签
     * <p>
     * 该方法弹出输入框提示用户输入自定义标签名称和默认值, 随后对输入进行合法性校验
     * (使用 {@link SettingsState#isValidTagName(String)}), 并检查当前标签列表中是否已存在相同名称 (不区分大小写).<br>
     * 若输入合法且标签不存在, 则将该标签添加到 {@link CustomJavaDocTagsTableModel} 中;<br>
     * 否则根据不同情况弹出相应的错误或警告对话框.
     */
    private void addCustomJavaDocTag() {
        // 创建输入对话框
        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridLayout(2, 2, 5, 5));

        JBLabel tagNameLabel = new JBLabel(JavaDocBundle.message("settings.custom.javadoc.tags.column.name") + ":");
        JBTextField tagNameField = new JBTextField();
        JBLabel defaultValueLabel = new JBLabel(JavaDocBundle.message("settings.custom.javadoc.tags.column.default.value") + ":");
        JBTextField defaultValueField = new JBTextField();

        panel.add(tagNameLabel);
        panel.add(tagNameField);
        panel.add(defaultValueLabel);
        panel.add(defaultValueField);

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            panel,
            JavaDocBundle.message("settings.custom.javadoc.tags.add.title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
                                                  );

        if (result == JOptionPane.OK_OPTION) {
            String tagName = tagNameField.getText().trim();
            String defaultValue = defaultValueField.getText().trim();

            if (tagName.isEmpty()) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavaDocBundle.message("settings.custom.javadoc.tags.invalid.name", tagName),
                    JavaDocBundle.message("settings.error.title"),
                    JOptionPane.ERROR_MESSAGE
                                             );
                return;
            }

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
            List<CustomJavaDocTag> currentTags = customJavaDocTagsTableModel.getData();
            String tagNameLower = tagName.toLowerCase();
            if (currentTags.stream().anyMatch(t -> t.tagName.toLowerCase().equals(tagNameLower))) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavaDocBundle.message("settings.custom.javadoc.tags.already.exists", tagName),
                    JavaDocBundle.message("settings.error.title"),
                    JOptionPane.WARNING_MESSAGE
                                             );
                return;
            }

            // 添加到表格
            customJavaDocTagsTableModel.addTag(
                new CustomJavaDocTag(tagName, defaultValue)
                                              );
        }
    }

    /**
     * 删除自定义 JavaDoc 标签
     */
    private void removeCustomJavaDocTag(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= customJavaDocTagsTableModel.getRowCount()) {
            return;
        }

        CustomJavaDocTag tag = customJavaDocTagsTableModel.getData().get(selectedRow);
        String tagName = tag.tagName;

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
     * 自定义 JavaDoc 标签表格模型
     * <p>
     * 该模型用于管理自定义 JavaDoc 标签的数据, 继承自 AbstractTableModel,
     * 提供了对自定义标签的增删改查操作, 支持表格界面的数据展示和编辑功能.
     * 主要用于 JavaDoc 设置界面中自定义标签的管理, 包括标签名称和默认值的配置.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private static class CustomJavaDocTagsTableModel extends AbstractTableModel {
        /**
         * 列名数组, 用于显示自定义 JavaDoc 标签的设置界面
         * <p>
         * 数组中的元素通过 JavaDocBundle 获取国际化字符串
         */
        private final String[] columnNames = {
            JavaDocBundle.message("settings.custom.javadoc.tags.column.name"),
            JavaDocBundle.message("settings.custom.javadoc.tags.column.default.value")
        };
        /** 数据列表 */
        private final List<CustomJavaDocTag> data;

        /**
         * 构造函数, 初始化 CustomJavaDocTagsTableModel 实例
         * <p>
         * 创建一个空的表格模型, 用于展示 JavaDoc 标签信息
         *
         * @since 2.0.0
         */
        public CustomJavaDocTagsTableModel() {
            this.data = new ArrayList<>();
        }

        /**
         * 设置新的数据列表并触发表格数据变更事件
         * <p>
         * 清除当前数据列表, 若传入的 newData 不为 null, 则将新数据添加到当前数据列表中, 并触发表格数据变更事件.
         *
         * @param newData 要设置的新数据列表
         * @since 2.0.0
         */
        public void setData(List<CustomJavaDocTag> newData) {
            this.data.clear();
            if (newData != null) {
                this.data.addAll(newData);
            }
            fireTableDataChanged();
        }

        /**
         * 获取数据列表
         * <p>
         * 返回数据的副本列表
         *
         * @return 数据列表
         */
        public List<CustomJavaDocTag> getData() {
            return new ArrayList<>(data);
        }

        /**
         * 添加一个标签到数据集合中, 并通知表格数据已更新
         * <p>
         * 该方法将指定的标签添加到内部数据集合, 并触发表格行插入事件以更新界面.
         *
         * @param tag 要添加的标签
         */
        public void addTag(CustomJavaDocTag tag) {
            data.add(tag);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        /**
         * 删除指定行的数据并通知表格视图更新.
         *
         * <p> 该方法首先检查传入的行索引是否在合法范围内 (0 ≤ row < data.size()). 若合法, 则从内部数据集合中移除对应行, 并通过 {@code fireTableRowsDeleted} 通知表格模型行已被删除, 从而触发视图刷新
         * .</p>
         *
         * @param row 要删除的行索引, 基于 0 的索引
         */
        public void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        /**
         * 清除所有数据并通知表格数据已删除
         * <p>
         * 该方法会清除数据集合中的所有元素, 并触发表格数据删除的事件通知.
         *
         * @since 2.0.0
         */
        public void clearAll() {
            int size = data.size();
            if (size > 0) {
                data.clear();
                fireTableRowsDeleted(0, size - 1);
            }
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
         * 根据列索引获取列名称
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
         * 获取表格中指定行和列的单元格值
         * <p>
         * 根据行索引和列索引返回对应的数据值, 若行索引超出范围或数据为空, 则返回空字符串
         *
         * @param rowIndex    行索引
         * @param columnIndex 列索引
         * @return 表格单元格的值
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size()) {
                CustomJavaDocTag tag = data.get(rowIndex);
                if (columnIndex == 0) {
                    return tag.tagName;
                } else if (columnIndex == 1) {
                    return tag.defaultValue;
                }
            }
            return "";
        }

        /**
         * 判断指定单元格是否可编辑
         * <p>
         * 该方法用于确定表格中指定行和列的单元格是否允许用户进行编辑操作.
         *
         * @param rowIndex    表格中的行索引
         * @param columnIndex 表格中的列索引
         * @return 如果单元格可编辑则返回 true, 否则返回 false
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // 允许编辑标签名称和默认值
        }

        /**
         * 设置表格指定单元格的值.
         * <p>
         * 仅当行索引在有效范围内且传入值不为 {@code null} 时才会进行处理. 
         * 对于标签名称列，会进行合法性校验和重复检查。
         * 对于默认值列，直接更新值。
         *
         * @param aValue      要设置的新值, 通常为 {@link String}, 但方法接受任何对象并调用 {@link Object#toString()}.
         * @param rowIndex    行索引, 必须在 0 与 {@link #data} 的大小之间.
         * @param columnIndex 列索引, 用于通知表格更新.
         */
        @SuppressWarnings("D")
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size() && aValue != null) {
                CustomJavaDocTag tag = data.get(rowIndex);

                if (columnIndex == 0) {
                    // 编辑标签名称
                    String newTagName = aValue.toString().trim();

                    // 验证标签名称
                    if (!SettingsState.isValidTagName(newTagName)) {
                        // 可以显示错误提示，这里简单处理为不更新
                        return;
                    }

                    // 检查是否与其他标签重复
                    String newTagNameLower = newTagName.toLowerCase();
                    for (int i = 0; i < data.size(); i++) {
                        if (i != rowIndex && data.get(i).tagName.toLowerCase().equals(newTagNameLower)) {
                            // 重复标签，不更新
                            return;
                        }
                    }

                    tag.tagName = newTagName;
                    fireTableCellUpdated(rowIndex, columnIndex);
                } else if (columnIndex == 1) {
                    // 编辑默认值
                    tag.defaultValue = aValue.toString();
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            }
        }
    }

}


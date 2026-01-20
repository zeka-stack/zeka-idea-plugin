package dev.dong4j.zeka.stack.idea.plugin.terminal.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.FeedbackPanel;
import dev.dong4j.zeka.stack.idea.plugin.terminal.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import lombok.Getter;

/**
 * 终端设置面板类
 * <p> 该类用于配置终端的相关设置, 包括 AI 提供商选择, 高级设置开关, 触发前缀, 系统提示和终端模板等.
 * 通过提供图形界面组件, 用户可以方便地进行相关设置的修改和应用.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class TerminalSettingsPanel {

    /**
     * 主面板
     * <p> 用于布局和管理设置面板中的各个组件.
     */
    @Getter
    private final JPanel mainPanel;
    /** AI 服务商选择面板 */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    // 高级设置
    /** 用于显示高级设置的复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板 (用于控制可见性) */
    private final JPanel advancedSettingsPanel;
    /** 是否启用 Terminal AI 的复选框 */
    private final JBCheckBox enableTerminalAICheckBox;
    /** 是否启用流式输出 */
    private final JBCheckBox enableStreamResponseCheckBox;
    /** 是否启用上下文检测 */
    private final JBCheckBox enableTerminalContextCheckBox;
    /** 触发前缀输入框, 用于设置终端命令触发前缀 */
    private final JBTextField triggerPrefixField;

    // Prompt 配置
    /** 系统提示文本区域, 用于输入和编辑系统级别的提示词内容 */
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /** 终端模板文本区域 */
    public final JBTextArea terminalTemplateTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数, 初始化设置面板
     * <p> 创建并配置插件设置面板的主界面, 包括 AI 服务商选择, 高级设置选项, 系统提示和终端模板输入区域等
     * <p> 初始化过程中会创建并设置以下组件:
     * <ul>
     * <li>{@link JBCheckBox}: 显示高级设置的复选框 </li>
     * <li>{@link JPanel}: 高级设置内容容器面板 </li>
     * <li>{@link AIProviderSelectionPanel}:AI 服务商选择面板 </li>
     * <li>{@link JBTextArea}: 系统提示和终端模板文本区域 </li>
     * <li>{@link FeedbackPanel}: 反馈面板 </li>
     * </ul>
     * <p> 所有组件通过 FormBuilder 进行布局, 并设置适当的边距和分隔符
     *
     * @since 1.0.0
     */
    public TerminalSettingsPanel() {
        showAdvancedSettingsCheckBox = new JBCheckBox(TerminalBundle.message("settings.prompt.settings.show"));
        enableTerminalAICheckBox = new JBCheckBox(TerminalBundle.message("settings.terminal.enable"));
        enableStreamResponseCheckBox = new JBCheckBox(TerminalBundle.message("settings.terminal.stream.enable"));
        enableTerminalContextCheckBox = new JBCheckBox(TerminalBundle.message("settings.terminal.context.enable"));
        triggerPrefixField = new JBTextField();

        advancedSettingsPanel = new JPanel(new BorderLayout());
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            .addComponent(createPromptTemplatesPanel())
            .getPanel();
        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            TerminalBundle::message,
            () -> {
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        FeedbackPanel feedbackPanel = new FeedbackPanel(
            null,
            PluginContents.PLUGIN_ID,
            PluginContents.PLUGIN_NAME,
            "zeka-stack-terminal-plugin"
        );

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addSeparator(10)
            .addComponent(enableTerminalAICheckBox)
            .addComponent(enableStreamResponseCheckBox)
            .addComponent(enableTerminalContextCheckBox)
            .addLabeledComponent(TerminalBundle.message("settings.terminal.trigger.prefix"), triggerPrefixField)
            .addComponent(showAdvancedSettingsCheckBox)
            .addComponent(advancedSettingsPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .addComponent(feedbackPanel.getContent())
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
        setupListeners();
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     * <p>比较界面上的配置项 (如系统提示词, 终端模板, 高级设置可见性,AI 服务提供商配置等) 与传入的 SettingsState 实例, 以确定是否有变更.
     * <p>如果 AI 服务商配置为空或不匹配, 也认为设置已修改.
     *
     * @param settings         需要比较的设置状态对象, 不能为 null
     * @param providerSettings 当前选中的 AI 提供商配置, 可能为 null
     * @return 如果界面设置与指定的设置状态不同, 则返回 true; 否则返回 false
     * @since 1.0.0
     */
    public boolean isModified(@NotNull SettingsState settings, @Nullable AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !terminalTemplateTextArea.getText().equals(settings.terminalTemplate)
            || showAdvancedSettingsCheckBox.isSelected() != settings.showAdvancedSettings
            || enableTerminalAICheckBox.isSelected() != settings.enableTerminalAI
            || enableStreamResponseCheckBox.isSelected() != settings.enableStreamResponse
            || enableTerminalContextCheckBox.isSelected() != settings.enableTerminalContext
            || !triggerPrefixField.getText().equals(settings.triggerPrefix)) {
            return true;
        }

        AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
        if (selectedConfig == null) {
            return providerSettings != null;
        }
        if (providerSettings == null) {
            return true;
        }
        return !providerSettings.contentEquals(selectedConfig);
    }

    /**
     * 将界面中的设置项应用到指定的 SettingsState 对象中
     * <p> 该方法用于将当前设置面板中配置的系统提示词, 终端模板, 高级设置可见性, 终端 AI 启用状态, 触发前缀以及 AI 提供商配置等应用到给定的 SettingsState 对象中, 实现界面与数据模型的同步
     * <p> 如果当前使用默认提示词模板, 还会自动更新提示词模板版本号
     *
     * @param settings 需要应用设置的 SettingsState 对象, 不能为 null
     */
    public void apply(@NotNull SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.terminalTemplate = terminalTemplateTextArea.getText();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();
        settings.enableTerminalAI = enableTerminalAICheckBox.isSelected();
        settings.enableStreamResponse = enableStreamResponseCheckBox.isSelected();
        settings.enableTerminalContext = enableTerminalContextCheckBox.isSelected();
        settings.triggerPrefix = triggerPrefixField.getText();
        if (settings.isUsingDefaultPrompts()) {
            settings.promptTemplateVersion = SettingsState.PROMPT_TEMPLATE_VERSION;
        }

        AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
        if (selectedConfig != null) {
            settings.providerConfig = selectedConfig.copy();
        }
    }

    /**
     * 将界面设置重置为指定的配置状态
     * <p> 该方法会根据给定的 SettingsState 对象, 更新界面中的各个组件状态, 使其与配置一致
     *
     * @param settings 包含当前设置状态的 SettingsState 对象, 不能为 null
     */
    public void reset(@NotNull SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        terminalTemplateTextArea.setText(settings.terminalTemplate);
        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        enableTerminalAICheckBox.setSelected(settings.enableTerminalAI);
        enableStreamResponseCheckBox.setSelected(settings.enableStreamResponse);
        enableTerminalContextCheckBox.setSelected(settings.enableTerminalContext);
        triggerPrefixField.setText(settings.triggerPrefix);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);
        aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
    }

    /**
     * 为高级设置复选框添加事件监听器,
     * 当复选框状态改变时根据其选中状态更新高级设置面板的可见性.
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e ->
                                                           advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected()));
    }

    /**
     * 创建提示词模板面板
     * <p> 用于生成包含提示词模板内容的面板, 包含标签和提示词模板 Tab 页面板.
     *
     * @return 包含提示词模板内容的面板
     * @since 1.0.0
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + TerminalBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            TerminalBundle.message("settings.advanced.settings.prompt.templates"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提示词模板的 Tab 页面板
     * <p> 用于创建包含系统提示词和终端模板的标签页面板, 每个标签页内包含文本区域和重置按钮.
     *
     * @return 包含提示词内容和重置按钮的标签页面板
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        promptTabbedPane.addTab(TerminalBundle.message("settings.prompt.tab.system"),
                                createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(TerminalBundle.message("settings.prompt.tab.terminal"),
                                createPromptTab(terminalTemplateTextArea, "terminal"));

        return promptTabbedPane;
    }

    /**
     * 创建提示词编辑标签页面板
     * <p> 用于生成包含文本区域和重置按钮的面板, 支持换行和单词换行, 适用于编辑系统提示或终端模板等提示信息.
     * <p> 文本区域设置为垂直滚动, 水平不滚动, 并添加适当边距; 底部放置重置按钮, 点击后可将内容恢复为默认值.
     *
     * @param textArea   用于输入提示信息的文本区域, 不能为 null
     * @param promptType 提示信息的类型, 如 "system" 或 "terminal"
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(TerminalBundle.message("settings.prompt." + promptType + ".tooltip"));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JButton resetButton = new JButton(TerminalBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * 将指定类型的提示词重置为其默认值
     * <p> 根据给定的提示词类型, 将对应的文本区域内容重置为默认的系统提示或终端模板
     *
     * @param promptType 提示词类型, 可以是 "system" 或 "terminal"
     * @param textArea   文本区域对象, 用于显示和编辑提示词内容
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "terminal":
                textArea.setText(SettingsState.getDefaultUserPrompt());
                break;
            default:
                break;
        }
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p> 设置标题的字体为系统中定义的标签字体, 并设置标题颜色为系统中定义的标签前景色
     *
     * @param titledBorder 要配置的 TitledBorder 对象, 不能为 null
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        java.awt.Color titleColor = com.intellij.util.ui.UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 释放资源
     * <p> 该方法用于释放面板中占用的资源, 通常在关闭或销毁设置面板时调用.
     */
    public void dispose() {
        aiProviderSelectionPanel.dispose();
    }
}

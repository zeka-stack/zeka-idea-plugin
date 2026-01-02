package dev.dong4j.zeka.stack.idea.plugin.example.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
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
import dev.dong4j.zeka.stack.idea.plugin.example.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.example.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import lombok.Getter;

/**
 * 示例设置面板类
 * <p> 该类用于创建和管理一个包含 AI 提供商选择, 系统提示文本区域和示例模板文本区域的用户界面面板.
 * <p> 通过复选框可以控制高级设置面板的可见性, 并提供了相应的事件监听器来响应用户交互.
 * <p> 主要功能包括:
 * <ul>
 * <li> 初始化主面板布局, 包含 AI 提供商选择面板, 复选框和高级设置面板 </li>
 * <li> 提供检查设置是否被修改的方法 </li>
 * <li> 应用当前设置到状态对象 </li>
 * <li> 重置面板中的控件以反映给定的状态对象 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class ExampleSettingsPanel {

    /**
     * 主面板
     * <p> 用于布局和管理设置面板中的各个组件.
     */
    @Getter
    private final JPanel mainPanel;
    /** AI 服务商选择面板 */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    // 高级设置
    /** 显示高级设置的复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板 (用于控制可见性) */
    private final JPanel advancedSettingsPanel;

    // Prompt 配置
    /** 系统提示文本区域 */
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /** 示例模板文本区域 */
    public final JBTextArea exampleTemplateTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数, 初始化设置面板
     * <p> 创建并配置插件设置面板的主界面, 包括 AI 服务商选择, 高级设置选项, 系统提示和示例模板输入区域等
     * <p> 初始化过程中会创建并设置以下组件:
     * <ul>
     *   <li>{@link JBCheckBox}: 显示高级设置的复选框 </li>
     *   <li>{@link JPanel}: 高级设置内容容器面板 </li>
     *   <li>{@link AIProviderSelectionPanel}:AI 服务商选择面板 </li>
     *   <li>{@link JBTextArea}: 系统提示和示例模板文本区域 </li>
     *   <li>{@link FeedbackPanel}: 反馈面板 </li>
     * </ul>
     * <p> 所有组件通过 FormBuilder 进行布局, 并设置适当的边距和分隔符
     *
     * @since 1.0.0
     */
    public ExampleSettingsPanel() {
        showAdvancedSettingsCheckBox = new JBCheckBox(ExampleBundle.message("settings.prompt.settings.show"));

        advancedSettingsPanel = new JPanel(new BorderLayout());
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            .addComponent(createPromptTemplatesPanel())
            .getPanel();
        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            ExampleBundle::message,
            () -> {
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        FeedbackPanel feedbackPanel = new FeedbackPanel(
            null,
            PluginContents.PLUGIN_ID,
            PluginContents.PLUGIN_NAME,
            "zeka-stack-example-plugin"
        );

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addSeparator(10)
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
     * <p>比较界面上的配置项 (如系统提示词, 示例模板和高级设置可见性) 与传入的 SettingsState 实例, 以确定是否有变更.
     * <p>如果 AI 服务商配置为空或不匹配, 也认为设置已修改.
     *
     * @param settings         需要比较的设置状态对象
     * @param providerSettings 当前选中的 AI 提供商配置, 可能为 null
     * @return 如果界面设置与指定的设置状态不同, 则返回 true; 否则返回 false
     * @since 1.0.0
     */
    public boolean isModified(@NotNull SettingsState settings, @Nullable AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !exampleTemplateTextArea.getText().equals(settings.exampleTemplate)
            || showAdvancedSettingsCheckBox.isSelected() != settings.showAdvancedSettings) {
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
     * <p> 该方法用于将当前设置面板中配置的系统提示, 示例模板, 高级设置可见性以及 AI 提供商配置等应用到给定的 SettingsState 对象中, 实现界面与数据模型的同步
     *
     * @param settings 需要应用设置的 SettingsState 对象, 不能为 null
     */
    public void apply(@NotNull SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.exampleTemplate = exampleTemplateTextArea.getText();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();

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
        exampleTemplateTextArea.setText(settings.exampleTemplate);
        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);
        aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
    }

    /**
     * 设置监听器
     * <p> 为高级设置复选框添加事件监听器, 当复选框的状态改变时, 更新高级设置面板的可见性.
     *
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e ->
                                                           advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected()));
    }

    /**
     * 创建提示词模板面板
     * <p> 用于生成包含提示词模板内容的面板, 包含标签和提示词模板 Tab 页面板
     *
     * @return 包含提示词模板内容的面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + ExampleBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            ExampleBundle.message("settings.advanced.settings.prompt.templates"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提示词模板的 Tab 页面板
     * <p> 用于创建包含系统提示词和示例模板的标签页面板, 每个标签页内包含文本区域和重置按钮.
     *
     * @return 包含提示词内容和重置按钮的标签页面板
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        promptTabbedPane.addTab(ExampleBundle.message("settings.prompt.tab.system"),
                                createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(ExampleBundle.message("settings.prompt.tab.example"),
                                createPromptTab(exampleTemplateTextArea, "example"));

        return promptTabbedPane;
    }

    /**
     * 创建提示信息标签页面板
     * <p> 创建一个包含文本区域和重置按钮的面板, 用于编辑特定类型的提示信息 (如系统提示或示例提示)
     * <p> 文本区域支持换行和单词换行, 并设置适当的滚动条策略和提示信息
     * <p> 重置按钮点击后会将文本区域恢复为默认值
     *
     * @param textArea   用于输入提示信息的文本区域, 不能为 null
     * @param promptType 提示信息的类型, 如 "system" 或 "example"
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(ExampleBundle.message("settings.prompt." + promptType + ".tooltip"));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JButton resetButton = new JButton(ExampleBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * 将指定类型的提示词重置为其默认值
     * <p> 根据给定的提示词类型, 将对应的文本区域内容重置为默认的系统提示或示例模板
     *
     * @param promptType 提示词类型, 可以是 "system" 或 "example"
     * @param textArea   文本区域对象, 用于显示和编辑提示词内容
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "example":
                textArea.setText(SettingsState.getDefaultUserTemplate());
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

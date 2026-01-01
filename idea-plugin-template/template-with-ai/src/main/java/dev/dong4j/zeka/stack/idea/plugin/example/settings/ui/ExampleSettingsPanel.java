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

/**
 * 插件设置面板 UI
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ExampleSettingsPanel {

    /** 主面板 */
    private final JPanel mainPanel;
    /** AI 服务商选择面板 */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    // 高级设置
    /** 显示高级设置的复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板（用于控制可见性） */
    private final JPanel advancedSettingsPanel;

    // Prompt 配置
    /** 系统提示文本区域 */
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /** 示例模板文本区域 */
    public final JBTextArea exampleTemplateTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数, 初始化设置面板
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

    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
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
     * 将界面中的设置项应用到给定的 SettingsState 对象中
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
     * 重置界面设置为指定的配置状态
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
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e ->
            advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected()));
    }

    /**
     * 创建提示词模板面板
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
     * 创建提示词模板 Tab 页面板
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
     * 重置提示词到默认值
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "example":
                textArea.setText(SettingsState.getDefaultExampleTemplate());
                break;
            default:
                break;
        }
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        java.awt.Color titleColor = com.intellij.util.ui.UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 释放资源
     */
    public void dispose() {
        aiProviderSelectionPanel.dispose();
    }
}

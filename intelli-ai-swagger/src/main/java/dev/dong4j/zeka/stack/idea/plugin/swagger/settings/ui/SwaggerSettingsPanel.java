package dev.dong4j.zeka.stack.idea.plugin.swagger.settings.ui;

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
import dev.dong4j.zeka.stack.idea.plugin.swagger.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;

/**
 * Swagger 设置面板类
 * <p> 用于在 IntelliJ IDEA 插件中提供 Swagger 相关的配置界面, 包括系统提示词和 Swagger 模板的编辑, 高级设置的显示控制以及 AI 提供商的选择.
 * <p> 该面板支持用户修改系统提示,Swagger 模板内容, 并可选择是否显示高级设置. 同时, 支持重置提示内容为默认值.
 * <p> 主要功能包括:
 * <ul>
 *   <li> 提供用户界面用于配置系统提示和 Swagger 模板 </li>
 * <li> 支持切换高级设置的显示与隐藏 </li>
 * <li> 集成 AI 提供商选择功能, 允许用户配置不同的 AI 提供商参数 </li>
 * <li> 支持重置提示内容为默认值 </li>
 * <li> 提供反馈面板以收集用户反馈 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class SwaggerSettingsPanel {

    /**
     * 主面板
     * <p> 用于布局和管理整个插件设置面板的组件.
     */
    private final JPanel mainPanel;
    /** AI 服务商选择面板 */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    // 高级设置
    /** 显示高级设置的复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板 (用于控制可见性) */
    private final JPanel advancedSettingsPanel;

    // Prompt 配置
    /** 系统提示文本区域, 用于输入和显示系统提示内容 */
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /** Swagger 模板文本区域, 用于输入和编辑 Swagger 模板内容 */
    public final JBTextArea swaggerTemplateTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数, 初始化插件设置面板
     * <p> 该构造函数用于初始化插件设置面板的各种组件和布局, 包括主面板,AI 服务商选择面板, 高级设置复选框, 高级设置容器面板, 系统提示文本区域和 Swagger 模板文本区域.
     * <p> 具体步骤如下:
     * <ul>
     * <li> 创建并初始化高级设置复选框 </li>
     * <li> 创建并初始化高级设置容器面板, 并将其内容设置为提示词模板面板 </li>
     * <li> 创建并初始化 AI 服务商选择面板 </li>
     * <li> 创建并初始化反馈面板 </li>
     * <li> 使用 FormBuilder 构建主面板, 并添加 AI 服务商选择面板, 高级设置复选框, 高级设置容器面板和反馈面板 </li>
     * <li> 为主面板添加边框 </li>
     * <li> 设置监听器以控制高级设置容器面板的可见性 </li>
     * </ul>
     *
     * @since 1.0.0
     */
    public SwaggerSettingsPanel() {
        showAdvancedSettingsCheckBox = new JBCheckBox(SwaggerBundle.message("settings.prompt.settings.show"));

        advancedSettingsPanel = new JPanel(new BorderLayout());
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            .addComponent(createPromptTemplatesPanel())
            .getPanel();
        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            SwaggerBundle::message,
            () -> {
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        FeedbackPanel feedbackPanel = new FeedbackPanel(
            null,
            PluginContents.PLUGIN_ID,
            PluginContents.PLUGIN_NAME,
            "zeka-stack-swagger-plugin"
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
     * 获取主设置面板
     * <p> 返回插件设置界面的主面板, 用于显示各种配置选项.
     *
     * @return 主设置面板
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     * <p> 比较界面中的设置项和传入的配置对象, 判断是否有任何值发生变化 </p>
     *
     * @param settings         要比较的设置状态对象, 不能为 null
     * @param providerSettings AI 服务商配置信息, 可以为 null
     * @return 如果界面设置与给定设置不同则返回 true, 否则返回 false
     */
    public boolean isModified(@NotNull SettingsState settings, @Nullable AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !swaggerTemplateTextArea.getText().equals(settings.swaggerTemplate)
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
     * <p> 此方法用于将用户在设置面板中所做的更改保存到 SettingsState 对象中, 包括系统提示词,Swagger 模板内容, 是否显示高级设置以及选中的 AI 服务提供商配置.
     *
     * @param settings 要应用设置的 SettingsState 对象, 不能为 null
     */
    public void apply(@NotNull SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.swaggerTemplate = swaggerTemplateTextArea.getText();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();

        AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
        if (selectedConfig != null) {
            settings.providerConfig = selectedConfig.copy();
        }
    }

    /**
     * 将界面中的设置项重置为指定的配置状态
     * <p> 该方法用于将当前界面中的各项设置还原为给定的 SettingsState 对象中的配置值.
     *
     * @param settings 需要应用的配置状态对象, 不能为 null
     */
    public void reset(@NotNull SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        swaggerTemplateTextArea.setText(settings.swaggerTemplate);
        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);
        aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
    }

    /**
     * 设置监听器
     * <p> 为高级设置复选框添加事件监听器, 当复选框状态改变时, 控制高级设置面板的可见性
     *
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e ->
                                                           advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected()));
    }

    /**
     * 创建提示词模板面板
     * <p> 该方法用于创建一个包含提示词模板的面板, 其中包括一个标签和一个选项卡面板.
     * <p> 面板布局使用 BorderLayout, 其中心位置放置了包含提示词模板的选项卡面板.
     *
     * @return 包含提示词模板的 JPanel 面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + SwaggerBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            SwaggerBundle.message("settings.advanced.settings.prompt.templates"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提示词模板标签页面板
     * <p> 创建一个包含系统提示和 Swagger 提示的标签页面板, 用于用户编辑和管理提示模板
     * <p> 使用示例:
     * <pre>{@code
     * JBTabbedPane tabbedPane = createPromptTabbedPane();
     * }</pre>
     *
     * @return 包含系统提示和 Swagger 提示的标签页面板
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        promptTabbedPane.addTab(SwaggerBundle.message("settings.prompt.tab.system"),
                                createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(SwaggerBundle.message("settings.prompt.tab.swagger"),
                                createPromptTab(swaggerTemplateTextArea, "swagger"));

        return promptTabbedPane;
    }

    /**
     * 创建提示词模板的标签页面板
     * <p> 该方法用于创建一个包含文本区域和重置按钮的标签页面板, 用于展示和编辑系统提示或 Swagger 模板内容.
     *
     * @param textArea   文本区域组件, 用于显示和编辑提示词内容
     * @param promptType 提示词类型, 用于确定加载的默认提示词 ("system" 或 "swagger")
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(SwaggerBundle.message("settings.prompt." + promptType + ".tooltip"));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JButton resetButton = new JButton(SwaggerBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * 将指定类型的提示词重置为默认值
     * <p> 根据传入的提示类型, 将对应的文本区域内容设置为默认值.
     *
     * @param promptType 提示类型, 支持 "system" 和 "swagger"
     * @param textArea   要更新内容的文本区域
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "swagger":
                textArea.setText(SettingsState.getDefaultSwaggerTemplate());
                break;
            default:
                break;
        }
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p> 设置标题边框的字体为系统默认标签字体, 并设置标题颜色为默认标签前景色
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
     * <p> 该方法用于在面板不再需要时释放相关资源, 防止内存泄漏.
     */
    public void dispose() {
        aiProviderSelectionPanel.dispose();
    }
}

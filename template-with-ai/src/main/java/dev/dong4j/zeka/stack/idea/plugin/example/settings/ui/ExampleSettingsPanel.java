package dev.dong4j.zeka.stack.idea.plugin.example.settings.ui;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.example.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import icons.AICommonIcons;
import lombok.Getter;

/**
 * 插件设置面板 UI
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ExampleSettingsPanel {

    /** 主面板 */
    @Getter
    private final JPanel mainPanel;
    /** AI 提供商选择下拉框 */
    private JComboBox<AIProviderConfig> providerComboBox;
    /** AI 提供商设置变更监听器 */
    private AIProviderSettingsListener providerSettingsListener;
    /** AI 提供商选择面板（用于动态刷新） */
    private JPanel aiProviderSelectionPanel;

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
        // 创建高级设置复选框
        showAdvancedSettingsCheckBox = new JBCheckBox(ExampleBundle.message("settings.prompt.settings.show"));

        // 创建高级设置容器面板
        advancedSettingsPanel = new JPanel(new BorderLayout());

        // 构建高级设置面板内容（只包含 Prompt 模板）
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

            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 注册供应商设置变更监听器
        registerProviderSettingsListener();

        // 设置高级设置复选框的监听器
        setupListeners();
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     */
    public boolean isModified(SettingsState settings, AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !exampleTemplateTextArea.getText().equals(settings.exampleTemplate)
            || showAdvancedSettingsCheckBox.isSelected() != settings.showAdvancedSettings) {
            return true;
        }
        if (providerComboBox == null || !providerComboBox.isEnabled()) {
            return false;
        }
        AIProviderConfig selectedConfig = (AIProviderConfig) providerComboBox.getSelectedItem();
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
    public void apply(SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.exampleTemplate = exampleTemplateTextArea.getText();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();
        if (providerComboBox != null && providerComboBox.isEnabled()) {
            AIProviderConfig selectedConfig = (AIProviderConfig) providerComboBox.getSelectedItem();
            if (selectedConfig != null) {
                settings.providerConfig = selectedConfig.copy();
            }
        }
    }

    /**
     * 重置界面设置为指定的配置状态
     */
    public void reset(SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        exampleTemplateTextArea.setText(settings.exampleTemplate);
        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);
        if (providerComboBox != null && providerComboBox.isEnabled()) {
            // 尝试恢复之前选中的供应商
            if (settings.providerConfig != null) {
                // 检查当前列表中是否包含保存的配置
                List<AIProviderConfig> availableProviders = getAiProviderTypes();
                AIProviderConfig matchingConfig = availableProviders.stream()
                    .filter(config -> config.credentialId != null
                                      && config.credentialId.equals(settings.providerConfig.credentialId))
                    .findFirst()
                    .orElse(null);
                if (matchingConfig != null) {
                    providerComboBox.setSelectedItem(matchingConfig);
                } else if (!availableProviders.isEmpty()) {
                    // 如果找不到匹配的，选择第一个
                    providerComboBox.setSelectedIndex(0);
                }
            } else if (providerComboBox.getItemCount() > 0) {
                // 如果没有保存的配置，选择第一个
                providerComboBox.setSelectedIndex(0);
            }
        }
    }

    /**
     * 创建 AI 提供商选择面板
     */
    private JPanel createAIProviderSelectionPanel() {
        // 从 intelli-ai-engine 获取可用服务商列表
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        JPanel panel;

        // 如果没有可用服务商，显示提示信息和跳转链接
        if (aiProviderTypes.isEmpty()) {
            // 创建提示信息面板
            JBLabel warningLabel = new JBLabel(ExampleBundle.message("settings.ai.provider.no.available.warning"));
            // 使用警告颜色
            java.awt.Color warningColor = UIManager.getColor("Label.warningForeground");
            if (warningColor == null) {
                warningColor = new JBColor(new Color(255, 140, 0), new Color(255, 140, 0)); // 橙色作为警告颜色
            }
            warningLabel.setForeground(warningColor);

            // 创建跳转链接
            HyperlinkLabel linkLabel = new HyperlinkLabel(ExampleBundle.message("settings.ai.provider.open.ai.common.settings"));
            linkLabel.addHyperlinkListener(e -> {
                // 打开 IntelliAI Engine 全局设置页面（应用级配置）
                ShowSettingsUtil.getInstance().editConfigurable(null, EngineContents.PLUGIN_NAME);
            });

            // 创建空的下拉框（禁用状态）
            providerComboBox = new ComboBox<>(new AIProviderConfig[0]);
            providerComboBox.setEnabled(false);

            panel = FormBuilder.createFormBuilder()
                .addComponent(warningLabel)
                .addComponent(linkLabel)
                .addComponent(new JBLabel()) // 空行
                .addLabeledComponent(new JBLabel(ExampleBundle.message("settings.ai.provider") + ":"), providerComboBox)
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

            JBLabel providerLabel = new JBLabel(ExampleBundle.message("settings.ai.provider") + ":");
            JBLabel hintLabel = new JBLabel(ExampleBundle.message("settings.ai.provider.hint"));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1f));

            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(providerLabel, providerComboBox)
                .addComponent(hintLabel)
                .getPanel();
        }

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ExampleBundle.message("settings.ai.provider.selection"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 获取已验证的 AI 服务提供商类型列表
     */
    @NotNull
    private static List<AIProviderConfig> getAiProviderTypes() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getVerifiedProviders();
    }

    /**
     * 注册 AI 提供商设置变更监听器
     */
    private void registerProviderSettingsListener() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        providerSettingsListener = settings -> refreshProviderComboBox();
        globalSettings.addListener(providerSettingsListener);
    }

    /**
     * 刷新提供商下拉框
     */
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

                // 恢复选中的供应商
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e -> {
            advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected());
        });
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p>
     * 显式设置字体和颜色，确保在 2025 版本中正常显示。
     * 使用 UIUtil 获取主题感知的文本颜色，自动适配浅色和深色主题。
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 创建提示词模板面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + ExampleBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
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
        // 设置 Tab 页的尺寸
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        // 创建各个 Tab 页
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

        // 创建文本区域
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(ExampleBundle.message("settings.prompt." + promptType + ".tooltip"));

        // 创建滚动面板
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
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
        }
    }

    /**
     * 释放资源
     */
    public void dispose() {
        if (providerSettingsListener != null) {
            AIProviderSettings globalSettings = AIProviderSettings.getInstance();
            globalSettings.removeListener(providerSettingsListener);
            providerSettingsListener = null;
        }
    }
}


package dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui;

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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import dev.dong4j.zeka.stack.idea.plugin.common.icons.AICommonIcons;
import lombok.Getter;

/**
 * 插件设置面板 UI
 */
public class ChangelogSettingsPanel {

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

    // Prompt 配置 - 创建文本区域（将在 Tab 页中使用）
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    public final JBTextArea changelogTemplateTextArea = new JBTextArea(15, 50);
    public final JBTextArea dailyReportTemplateTextArea = new JBTextArea(15, 50);
    public final JBTextArea weeklyReportTemplateTextArea = new JBTextArea(15, 50);

    public ChangelogSettingsPanel() {
        // 创建高级设置复选框
        showAdvancedSettingsCheckBox = new JBCheckBox(ChangelogBundle.message("settings.prompt.settings.show"));

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

    public boolean isModified(SettingsState settings, AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !changelogTemplateTextArea.getText().equals(settings.changelogTemplate)
            || !dailyReportTemplateTextArea.getText().equals(settings.dailyReportTemplate)
            || !weeklyReportTemplateTextArea.getText().equals(settings.weeklyReportTemplate)
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

    public void apply(SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.changelogTemplate = changelogTemplateTextArea.getText();
        settings.dailyReportTemplate = dailyReportTemplateTextArea.getText();
        settings.weeklyReportTemplate = weeklyReportTemplateTextArea.getText();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();
        if (providerComboBox != null && providerComboBox.isEnabled()) {
            AIProviderConfig selectedConfig = (AIProviderConfig) providerComboBox.getSelectedItem();
            if (selectedConfig != null) {
                settings.providerConfig = selectedConfig.copy();
            }
        }
    }

    public void reset(SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        changelogTemplateTextArea.setText(settings.changelogTemplate);
        dailyReportTemplateTextArea.setText(settings.dailyReportTemplate);
        weeklyReportTemplateTextArea.setText(settings.weeklyReportTemplate);
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
     * <p>
     * 只显示供应商选择下拉框，其他 AI 配置在 Settings → Tools → AI Common 中管理。
     *
     * @return AI 提供商选择面板
     */
    private JPanel createAIProviderSelectionPanel() {
        // 从 ai-common 获取可用服务商列表
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        JPanel panel;

        // 如果没有可用服务商，显示提示信息和跳转链接
        if (aiProviderTypes.isEmpty()) {
            // 创建提示信息面板
            JBLabel warningLabel = new JBLabel(ChangelogBundle.message("settings.ai.provider.no.available.warning"));
            // 使用警告颜色
            java.awt.Color warningColor = UIManager.getColor("Label.warningForeground");
            if (warningColor == null) {
                warningColor = new JBColor(new Color(255, 140, 0), new Color(255, 140, 0)); // 橙色作为警告颜色
            }
            warningLabel.setForeground(warningColor);

            // 创建跳转链接
            HyperlinkLabel linkLabel = new HyperlinkLabel(ChangelogBundle.message("settings.ai.provider.open.ai.common.settings"));
            linkLabel.addHyperlinkListener(e -> {
                // 打开 AI Common 全局设置页面（应用级配置）
                ShowSettingsUtil.getInstance().editConfigurable(null, "AI Common");
            });

            // 创建空的下拉框（禁用状态）
            providerComboBox = new ComboBox<>(new AIProviderConfig[0]);
            providerComboBox.setEnabled(false);

            panel = FormBuilder.createFormBuilder()
                .addComponent(warningLabel)
                .addComponent(linkLabel)
                .addComponent(new JBLabel()) // 空行
                .addLabeledComponent(new JBLabel(ChangelogBundle.message("settings.ai.provider") + ":"), providerComboBox)
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

            JBLabel providerLabel = new JBLabel(ChangelogBundle.message("settings.ai.provider") + ":");
            JBLabel hintLabel = new JBLabel(ChangelogBundle.message("settings.ai.provider.hint"));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1f));

            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(providerLabel, providerComboBox)
                .addComponent(hintLabel)
                .getPanel();
        }

        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ChangelogBundle.message("settings.ai.provider.selection")));

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
     * 当 AI Common 设置中的可用提供商列表发生变化时，自动刷新下拉列表。
     */
    private void registerProviderSettingsListener() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        providerSettingsListener = settings -> refreshProviderComboBox();
        globalSettings.addListener(providerSettingsListener);
    }

    /**
     * 刷新提供商下拉框
     * <p>
     * 从 AI Common 设置中重新获取可用提供商列表，并更新下拉框内容。
     * 如果之前没有可用提供商，现在有了，会重新创建面板。
     */
    private void refreshProviderComboBox() {
        if (aiProviderSelectionPanel == null) {
            return;
        }

        // 从 ai-common 获取可用服务商列表
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
     * <p>
     * 为高级设置复选框添加监听器，控制高级设置面板的显示/隐藏。
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e -> {
            advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected());
        });
    }

    /**
     * 创建提示词模板面板
     * <p>
     * 创建一个包含提示词模板 Tab 页的面板，并添加边框。
     *
     * @return 提示词模板面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + ChangelogBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ChangelogBundle.message("settings.advanced.settings.prompt.templates"));
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提示词模板 Tab 页面板
     * <p>
     * 初始化一个包含多个提示配置选项卡的 JBTabbedPane，每个选项卡对应不同的提示类型。
     *
     * @return 包含提示配置选项卡的 JBTabbedPane 实例
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        // 设置 Tab 页的尺寸
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        // 创建各个 Tab 页
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.system"), createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.changelog"), createPromptTab(changelogTemplateTextArea,
                                                                                                          "changelog"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.daily.report"), createPromptTab(dailyReportTemplateTextArea,
                                                                                                             "daily.report"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.weekly.report"),
                                createPromptTab(weeklyReportTemplateTextArea, "weekly.report"));

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
    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        // 创建文本区域
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(ChangelogBundle.message("settings.prompt." + promptType + ".tooltip"));

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
        JButton resetButton = new JButton(ChangelogBundle.message("settings.prompt.reset"));
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
    private void adjustTextAreaSize(JBTextArea textArea) {
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
                        textArea.getParent().repaint();
                    }
                }
            } catch (Exception e) {
                // 忽略异常，避免影响 UI
            }
        });
    }

    /**
     * 重置提示词到默认值
     * <p>
     * 根据提示类型，将文本区域重置为对应的默认值。
     *
     * @param promptType 提示类型
     * @param textArea   文本区域
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "changelog":
                textArea.setText(SettingsState.getDefaultChangelogTemplate());
                break;
            case "daily.report":
                textArea.setText(SettingsState.getDefaultDailyReportTemplate());
                break;
            case "weekly.report":
                textArea.setText(SettingsState.getDefaultWeeklyReportTemplate());
                break;
        }
        adjustTextAreaSize(textArea);
    }

    /**
     * 释放资源
     * <p>
     * 移除注册的监听器，避免内存泄漏。
     */
    public void dispose() {
        if (providerSettingsListener != null) {
            AIProviderSettings globalSettings = AIProviderSettings.getInstance();
            globalSettings.removeListener(providerSettingsListener);
            providerSettingsListener = null;
        }
    }
}

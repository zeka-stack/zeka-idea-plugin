package dev.dong4j.zeka.stack.idea.plugin.workflow.settings.ui;

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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel;
import dev.dong4j.zeka.stack.idea.plugin.workflow.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;
import lombok.Getter;

/**
 * IntelliAI Tracer 设置面板。
 *
 * @author dong4j
 * @version 1.0.1
 * @since 1.0.1
 */
public class TracerSettingsPanel {

    @Getter
    private final JPanel mainPanel;

    /** AI 提供商选择面板（使用 engine 插件中的通用类） */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    private final JBCheckBox showPromptSettingsCheckBox;
    private final JPanel promptSettingsPanel;

    private final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    private final JBTextArea workflowTemplateTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数。
     */
    public TracerSettingsPanel() {
        showPromptSettingsCheckBox = new JBCheckBox(WorkflowBundle.message("settings.prompt.settings.show"));
        promptSettingsPanel = new JPanel(new BorderLayout());
        promptSettingsPanel.add(createPromptTemplatesPanel(), BorderLayout.NORTH);
        promptSettingsPanel.setVisible(false);

        // 初始化 AI 提供商选择面板（使用 engine 插件中的通用类）
        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            WorkflowBundle::message,
            () -> {
                // 面板刷新后的回调：恢复选中的供应商
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addSeparator(10)
            .addComponent(showPromptSettingsCheckBox)
            .addComponent(promptSettingsPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));

        setupListeners();
    }

    /**
     * 判断是否修改。
     */
    public boolean isModified(SettingsState settings, AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !workflowTemplateTextArea.getText().equals(settings.workflowTemplate)
            || showPromptSettingsCheckBox.isSelected() != settings.showPromptSettings) {
            return true;
        }
        AIProviderConfig selected = aiProviderSelectionPanel != null ? aiProviderSelectionPanel.getSelectedProvider() : null;
        if (selected == null) {
            return providerSettings != null;
        }
        if (providerSettings == null) {
            return true;
        }
        return !providerSettings.contentEquals(selected);
    }

    /**
     * 应用设置。
     */
    public void apply(SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.workflowTemplate = workflowTemplateTextArea.getText();
        settings.showPromptSettings = showPromptSettingsCheckBox.isSelected();
        if (aiProviderSelectionPanel != null) {
            AIProviderConfig selected = aiProviderSelectionPanel.getSelectedProvider();
            if (selected != null) {
                settings.providerConfig = selected.copy();
            }
        }
    }

    /**
     * 重置 UI。
     */
    public void reset(SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        workflowTemplateTextArea.setText(settings.workflowTemplate);
        showPromptSettingsCheckBox.setSelected(settings.showPromptSettings);
        promptSettingsPanel.setVisible(settings.showPromptSettings);
        if (aiProviderSelectionPanel != null) {
            // 设置选中的提供商配置
            aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
        }
    }

    /**
     * 释放资源。
     */
    public void dispose() {
        if (aiProviderSelectionPanel != null) {
            aiProviderSelectionPanel.dispose();
        }
    }

    private void setupListeners() {
        showPromptSettingsCheckBox.addActionListener(e -> promptSettingsPanel.setVisible(showPromptSettingsCheckBox.isSelected()));
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

    private JPanel createPromptTemplatesPanel() {
        JPanel content = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + WorkflowBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.CENTER);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            WorkflowBundle.message("settings.prompt.section.title"));
        configureTitledBorder(border);
        panel.setBorder(border);
        return panel;
    }

    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(600, 300));
        tabbedPane.addTab(WorkflowBundle.message("settings.prompt.tab.system"),
                          createPromptTab(systemPromptTextArea, "system"));
        tabbedPane.addTab(WorkflowBundle.message("settings.prompt.tab.workflow"),
                          createPromptTab(workflowTemplateTextArea, "workflow"));
        return tabbedPane;
    }

    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tab = new JPanel(new BorderLayout());

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(WorkflowBundle.message("settings.prompt." + promptType + ".tooltip"));

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

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));
        tab.add(scrollPane, BorderLayout.CENTER);

        JButton resetButton = new JButton(WorkflowBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tab.add(resetButton, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> adjustTextAreaSize(textArea));
        return tab;
    }

    private void adjustTextAreaSize(JBTextArea textArea) {
        SwingUtilities.invokeLater(() -> {
            int lineCount = textArea.getLineCount();
            int minRows = 15;
            int maxRows = 50;
            int rows = Math.max(minRows, Math.min(lineCount, maxRows));
            if (rows != textArea.getRows()) {
                textArea.setRows(rows);
                if (textArea.getParent() != null) {
                    textArea.getParent().revalidate();
                    textArea.getParent().repaint();
                }
            }
        });
    }

    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "workflow":
            default:
                textArea.setText(SettingsState.getDefaultWorkflowTemplate());
                break;
        }
        adjustTextAreaSize(textArea);
    }
}


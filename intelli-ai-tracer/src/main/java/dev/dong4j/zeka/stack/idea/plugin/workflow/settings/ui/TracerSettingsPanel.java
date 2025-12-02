package dev.dong4j.zeka.stack.idea.plugin.workflow.settings.ui;

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

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import dev.dong4j.zeka.stack.idea.plugin.workflow.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;
import icons.AICommonIcons;
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

    private JComboBox<AIProviderConfig> providerComboBox;
    private JPanel aiProviderSelectionPanel;
    private AIProviderSettingsListener providerSettingsListener;

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

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(aiProviderSelectionPanel = createAIProviderSelectionPanel())
            .addSeparator(10)
            .addComponent(showPromptSettingsCheckBox)
            .addComponent(promptSettingsPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
        mainPanel.setMinimumSize(new Dimension(550, 760));

        registerProviderSettingsListener();
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
        if (providerComboBox == null || !providerComboBox.isEnabled()) {
            return providerSettings != null;
        }
        AIProviderConfig selected = (AIProviderConfig) providerComboBox.getSelectedItem();
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
        if (providerComboBox != null && providerComboBox.isEnabled()) {
            AIProviderConfig selected = (AIProviderConfig) providerComboBox.getSelectedItem();
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

        if (providerComboBox != null && providerComboBox.isEnabled()) {
            if (settings.providerConfig != null) {
                List<AIProviderConfig> providers = getVerifiedProviders();
                AIProviderConfig matching = providers.stream()
                    .filter(config -> config.contentEquals(settings.providerConfig))
                    .findFirst()
                    .orElse(null);
                if (matching != null) {
                    providerComboBox.setSelectedItem(matching);
                } else if (!providers.isEmpty()) {
                    providerComboBox.setSelectedIndex(0);
                }
            } else if (providerComboBox.getItemCount() > 0) {
                providerComboBox.setSelectedIndex(0);
            }
        }
    }

    /**
     * 释放资源。
     */
    public void dispose() {
        if (providerSettingsListener != null) {
            AIProviderSettings.getInstance().removeListener(providerSettingsListener);
            providerSettingsListener = null;
        }
    }

    private JPanel createAIProviderSelectionPanel() {
        final List<AIProviderConfig> providers = getVerifiedProviders();
        JPanel panel;

        if (providers.isEmpty()) {
            JBLabel warningLabel = new JBLabel(WorkflowBundle.message("settings.ai.provider.no.available.warning"));
            Color warningColor = UIManager.getColor("Label.warningForeground");
            if (warningColor == null) {
                warningColor = new JBColor(new Color(255, 140, 0), new Color(255, 140, 0));
            }
            warningLabel.setForeground(warningColor);

            HyperlinkLabel linkLabel = new HyperlinkLabel(WorkflowBundle.message("settings.ai.provider.open.engine.settings"));
            linkLabel.addHyperlinkListener(e -> ShowSettingsUtil.getInstance().editConfigurable(null, "IntelliAI Engine"));

            providerComboBox = new ComboBox<>(new AIProviderConfig[0]);
            providerComboBox.setEnabled(false);

            panel = FormBuilder.createFormBuilder()
                .addComponent(warningLabel)
                .addComponent(linkLabel)
                .addComponent(new JBLabel())
                .addLabeledComponent(new JBLabel(WorkflowBundle.message("settings.ai.provider") + ":"), providerComboBox)
                .getPanel();
        } else {
            providerComboBox = new ComboBox<>(providers.toArray(new AIProviderConfig[0]));
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

            JBLabel providerLabel = new JBLabel(WorkflowBundle.message("settings.ai.provider") + ":");
            JBLabel hintLabel = new JBLabel(WorkflowBundle.message("settings.ai.provider.hint"));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1f));

            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(providerLabel, providerComboBox)
                .addComponent(hintLabel)
                .getPanel();
        }

        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            WorkflowBundle.message("settings.ai.provider.selection")));

        return panel;
    }

    private static List<AIProviderConfig> getVerifiedProviders() {
        return AIProviderSettings.getInstance().getVerifiedProviders();
    }

    private void registerProviderSettingsListener() {
        providerSettingsListener = settings -> refreshProviderComboBox();
        AIProviderSettings.getInstance().addListener(providerSettingsListener);
    }

    @SuppressWarnings("D")
    private void refreshProviderComboBox() {
        if (aiProviderSelectionPanel == null) {
            return;
        }
        List<AIProviderConfig> providers = getVerifiedProviders();
        boolean hadProviders = providerComboBox != null && providerComboBox.isEnabled();
        boolean hasProviders = !providers.isEmpty();

        if (hadProviders && hasProviders) {
            AIProviderConfig selected = (AIProviderConfig) providerComboBox.getSelectedItem();
            providerComboBox.setModel(new DefaultComboBoxModel<>(providers.toArray(new AIProviderConfig[0])));
            if (selected != null) {
                providers.stream()
                    .filter(config -> config.contentEquals(selected))
                    .findFirst()
                    .ifPresentOrElse(
                        match -> providerComboBox.setSelectedItem(match),
                        () -> {
                            if (!providers.isEmpty()) {
                                providerComboBox.setSelectedIndex(0);
                            }
                        }
                                    );
            } else if (!providers.isEmpty()) {
                providerComboBox.setSelectedIndex(0);
            }
            return;
        }

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

                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        }
    }

    private void setupListeners() {
        showPromptSettingsCheckBox.addActionListener(e -> promptSettingsPanel.setVisible(showPromptSettingsCheckBox.isSelected()));
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


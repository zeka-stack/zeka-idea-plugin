package dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderConfigPanel;
import lombok.Getter;

/**
 * 插件设置面板 UI
 */
public class ChangelogSettingsPanel {

    @Getter
    private final JPanel mainPanel;
    private final AICredentialManager credentialManager;
    private final AIProviderConfigPanel providerConfigPanel;
    private final JBTextArea systemPromptTextArea = new JBTextArea(5, 60);
    private final JBTextArea changelogTemplateTextArea = new JBTextArea(10, 60);

    public ChangelogSettingsPanel() {
        this.credentialManager = new AICredentialManager("AI Changelog", "AI_CHANGELOG_API_KEY_");
        this.providerConfigPanel = new AIProviderConfigPanel(credentialManager);

        // 配置文本区域
        systemPromptTextArea.setLineWrap(true);
        systemPromptTextArea.setWrapStyleWord(true);
        changelogTemplateTextArea.setLineWrap(true);
        changelogTemplateTextArea.setWrapStyleWord(true);

        JScrollPane systemPromptScrollPane = new JScrollPane(systemPromptTextArea);
        systemPromptScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        systemPromptScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollPane templateScrollPane = new JScrollPane(changelogTemplateTextArea);
        templateScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        templateScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(providerConfigPanel.getPanel())
            .addSeparator()
            .addLabeledComponent(new JBLabel("系统提示词:"), systemPromptScrollPane, 1, false)
            .addSeparator()
            .addLabeledComponent(new JBLabel("变更日志模板:"), templateScrollPane, 1, false)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));
    }

    public boolean isModified(SettingsState settings, AIProviderSettings providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !changelogTemplateTextArea.getText().equals(settings.changelogTemplate)) {
            return true;
        }
        return !providerSettings.contentEquals(providerConfigPanel.getSettings());
    }

    public void apply(SettingsState settings, AIProviderSettings providerSettings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.changelogTemplate = changelogTemplateTextArea.getText();

        AIProviderSettings updated = providerConfigPanel.getSettings();
        providerSettings.applyFrom(updated);
    }

    public void reset(SettingsState settings, AIProviderSettings providerSettings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        changelogTemplateTextArea.setText(settings.changelogTemplate);

        providerConfigPanel.loadSettings(providerSettings);
    }
}

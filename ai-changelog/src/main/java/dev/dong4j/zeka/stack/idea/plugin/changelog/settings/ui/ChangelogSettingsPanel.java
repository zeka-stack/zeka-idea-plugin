package dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderConfigPanel;

/**
 * 插件设置面板 UI
 */
public class ChangelogSettingsPanel {

    private final JPanel mainPanel;
    private final AICredentialManager credentialManager;
    private final AIProviderConfigPanel providerConfigPanel;
    private final JBTextField exampleTextTextField = new JBTextField();
    private final JBCheckBox enableFeatureCheckBox = new JBCheckBox("Enable Feature");
    private final JBTextField maxItemsTextField = new JBTextField();
    private final TextFieldWithBrowseButton customPathField = new TextFieldWithBrowseButton();
    private final JBCheckBox debugModeCheckBox = new JBCheckBox("Debug Mode");
    private final JBTextField timeoutTextField = new JBTextField();

    public ChangelogSettingsPanel() {
        this.credentialManager = new AICredentialManager("AI Changelog", "AI_CHANGELOG_API_KEY_");
        this.providerConfigPanel = new AIProviderConfigPanel(credentialManager);

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(providerConfigPanel.getPanel())
            .addSeparator()
                .addLabeledComponent(new JBLabel("Example Text:"), exampleTextTextField, 1, false)
                .addComponent(enableFeatureCheckBox, 1)
                .addLabeledComponent(new JBLabel("Max Items:"), maxItemsTextField, 1, false)
                .addSeparator()
                .addLabeledComponent(new JBLabel("Custom Path:"), customPathField, 1, false)
                .addComponent(debugModeCheckBox, 1)
                .addLabeledComponent(new JBLabel("Timeout (ms):"), timeoutTextField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public boolean isModified(SettingsState settings, AIProviderSettings providerSettings) {
        if (!exampleTextTextField.getText().equals(settings.getExampleText())
                || enableFeatureCheckBox.isSelected() != settings.isEnableFeature()
                || !maxItemsTextField.getText().equals(String.valueOf(settings.getMaxItems()))
                || !customPathField.getText().equals(settings.getCustomPath())
                || debugModeCheckBox.isSelected() != settings.isDebugMode()
            || !timeoutTextField.getText().equals(String.valueOf(settings.getTimeout()))) {
            return true;
        }
        return providerConfigPanel.isModified(providerSettings);
    }

    public void apply(SettingsState settings, AIProviderSettings providerSettings) {
        settings.setExampleText(exampleTextTextField.getText());
        settings.setEnableFeature(enableFeatureCheckBox.isSelected());
        
        try {
            settings.setMaxItems(Integer.parseInt(maxItemsTextField.getText()));
        } catch (NumberFormatException e) {
            settings.setMaxItems(100);
        }
        
        settings.setCustomPath(customPathField.getText());
        settings.setDebugMode(debugModeCheckBox.isSelected());
        
        try {
            settings.setTimeout(Integer.parseInt(timeoutTextField.getText()));
        } catch (NumberFormatException e) {
            settings.setTimeout(5000);
        }

        AIProviderSettings updated = providerConfigPanel.getSettings();
        providerSettings.applyFrom(updated);

        AIProviderConfig selectedConfig = providerSettings.defaultProviders.get(providerSettings.providerType);
        if (selectedConfig != null && selectedConfig.credentialId != null) {
            String apiKey = providerConfigPanel.getCurrentApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                credentialManager.setApiKey(selectedConfig.credentialId, apiKey);
            } else {
                credentialManager.deleteApiKey(selectedConfig.credentialId);
            }
        }
    }

    public void reset(SettingsState settings, AIProviderSettings providerSettings) {
        exampleTextTextField.setText(settings.getExampleText());
        enableFeatureCheckBox.setSelected(settings.isEnableFeature());
        maxItemsTextField.setText(String.valueOf(settings.getMaxItems()));
        customPathField.setText(settings.getCustomPath());
        debugModeCheckBox.setSelected(settings.isDebugMode());
        timeoutTextField.setText(String.valueOf(settings.getTimeout()));

        providerConfigPanel.loadSettings(providerSettings);
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * IntelliAI Engine 全局设置配置页面
 * <p>
 * 提供全局 AI 提供商配置界面，位于 Settings → Tools → IntelliAI Engine
 *
 * @author dong4j
 * @version 1.0.0
 */
public class AICommonSettingsConfigurable implements SearchableConfigurable {

    private AICommonSettingsPanel settingsPanel;

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return "dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable";
    }

    @Override
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return "IntelliAI Engine";
    }

    @Override
    @Nullable
    public String getHelpTopic() {
        return "settings.ai.common";
    }

    @Override
    @Nullable
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new AICommonSettingsPanel();
        }
        return settingsPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        AIProviderSettings currentSettings = AIProviderSettings.getInstance();
        return settingsPanel.isModified(currentSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }
        AIProviderSettings currentSettings = AIProviderSettings.getInstance();
        AIProviderSettings panelSettings = settingsPanel.getSettings();
        currentSettings.applyFrom(panelSettings);
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.loadSettings(AIProviderSettings.getInstance());
        }
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}


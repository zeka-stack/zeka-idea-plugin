package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui.ChangelogSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * 插件设置配置界面
 */
public class ChangelogSettingsConfigurable implements Configurable {

    private ChangelogSettingsPanel settingsPanel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "AI Changelog";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsPanel = new ChangelogSettingsPanel();
        return settingsPanel.getMainPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        AIProviderSettings providerSettings = settings.providerSettings;
        return settingsPanel.isModified(settings, providerSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings, settings.providerSettings);
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.reset(settings, settings.providerSettings);
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}

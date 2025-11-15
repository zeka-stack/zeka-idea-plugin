package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui.ChangelogSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 插件设置配置界面
 */
public class ChangelogSettingsConfigurable implements Configurable {

    private ChangelogSettingsPanel settingsPanel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return ChangelogBundle.message("settings.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new ChangelogSettingsPanel();
        }
        // 初始化 UI 数据
        reset();
        return settingsPanel.getMainPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerSettings = settings.providerConfig;
        return settingsPanel.isModified(settings, providerSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings);
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            SettingsState settings = SettingsState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    @Override
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }
}

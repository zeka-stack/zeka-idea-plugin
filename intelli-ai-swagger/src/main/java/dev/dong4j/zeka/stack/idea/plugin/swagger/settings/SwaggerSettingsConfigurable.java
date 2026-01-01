package dev.dong4j.zeka.stack.idea.plugin.swagger.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.ui.SwaggerSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;

/**
 * 插件设置配置界面
 *
 * @author dong4j
 * @since 1.0.0
 */
public class SwaggerSettingsConfigurable implements Configurable {

    private SwaggerSettingsPanel settingsPanel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return SwaggerBundle.message("settings.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new SwaggerSettingsPanel();
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


package dev.dong4j.zeka.stack.idea.plugin.workflow.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.workflow.settings.ui.TracerSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;

/**
 * IntelliAI Tracer 设置页 Configurable。
 */
public class TracerSettingsConfigurable implements SearchableConfigurable {

    private TracerSettingsPanel settingsPanel;

    @Override
    public @NotNull String getId() {
        return "dev.dong4j.zeka.stack.idea.plugin.workflow.settings";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return WorkflowBundle.message("settings.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new TracerSettingsPanel();
        }
        reset();
        return settingsPanel.getMainPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        SettingsState settings = SettingsState.getInstance();
        return settingsPanel.isModified(settings, settings.providerConfig);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }
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


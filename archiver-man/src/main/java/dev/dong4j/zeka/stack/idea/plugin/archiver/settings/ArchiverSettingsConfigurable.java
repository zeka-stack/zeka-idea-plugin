package dev.dong4j.zeka.stack.idea.plugin.archiver.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;

/**
 * 设置页面实现。
 */
public final class ArchiverSettingsConfigurable implements SearchableConfigurable {
    private ArchiverSettingsPanel panel;

    @NotNull
    @Override
    public String getId() {
        return "dev.dong4j.zeka.stack.idea.plugin.archiver.settings";
    }

    @Override
    public String getDisplayName() {
        return ArchiverBundle.message("settings.title");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (panel == null) {
            panel = new ArchiverSettingsPanel();
        }
        panel.reset(ArchiverSettingsState.getInstance());
        return panel.getComponent();
    }

    @Override
    public boolean isModified() {
        if (panel == null) {
            return false;
        }
        ArchiverSettingsState current = ArchiverSettingsState.getInstance();
        ArchiverSettingsState snapshot = panel.collectState();
        return snapshot.enableArchiveBrowser != current.enableArchiveBrowser
               || snapshot.enableEditableMode != current.enableEditableMode
               || snapshot.enableAutoBackup != current.enableAutoBackup
               || snapshot.showEditableBadge != current.showEditableBadge
               || snapshot.maxEditableFileSizeMb != current.maxEditableFileSizeMb
               || snapshot.batchSaveDelayMillis != current.batchSaveDelayMillis;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (panel == null) {
            return;
        }
        panel.apply(ArchiverSettingsState.getInstance());
    }

    @Override
    public void reset() {
        if (panel != null) {
            panel.reset(ArchiverSettingsState.getInstance());
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}


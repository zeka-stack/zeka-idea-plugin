package dev.dong4j.zeka.stack.idea.plugin.archiver.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Archiver Man 设置持久化。
 *
 * @author dong4j
 * @since 0.5.0
 */
@Service(Service.Level.APP)
@State(name = "ArchiverSettingsState", storages = @Storage("ArchiverSettings.xml"))
public final class ArchiverSettingsState implements PersistentStateComponent<ArchiverSettingsState> {

    public boolean enableArchiveBrowser = true;
    public boolean enableEditableMode = true;
    public boolean enableAutoBackup = true;
    public int maxEditableFileSizeMb = 2;
    public int batchSaveDelayMillis = 500;
    public boolean showEditableBadge = true;

    public static ArchiverSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(ArchiverSettingsState.class);
    }

    @Nullable
    @Override
    public ArchiverSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ArchiverSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public long maxEditableBytes() {
        int size = Math.max(1, maxEditableFileSizeMb);
        return size * 1024L * 1024L;
    }
}


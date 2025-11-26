package dev.dong4j.zeka.stack.idea.plugin.archiver.util;

/**
 * 功能开关集中处，后续会迁移到正式的设置页面。
 *
 * @author dong4j
 * @since 0.3.0
 */

import com.intellij.openapi.application.ApplicationManager;

import dev.dong4j.zeka.stack.idea.plugin.archiver.settings.ArchiverSettingsState;

/**
 * 功能开关。
 */
public final class ArchiverFeatureToggles {
    private static final long DEFAULT_MAX_EDITABLE_BYTES = 2 * 1024 * 1024;
    private static final ArchiverSettingsState DEFAULT_STATE = new ArchiverSettingsState();

    private ArchiverFeatureToggles() {
    }

    private static ArchiverSettingsState state() {
        if (ApplicationManager.getApplication() == null) {
            return DEFAULT_STATE;
        }
        ArchiverSettingsState service = ApplicationManager.getApplication().getService(ArchiverSettingsState.class);
        return service != null ? service : DEFAULT_STATE;
    }

    public static boolean isArchiveBrowserEnabled() {
        return state().enableArchiveBrowser;
    }

    public static boolean isEditableModeEnabled() {
        return state().enableEditableMode;
    }

    public static boolean isAutoBackupEnabled() {
        return state().enableAutoBackup;
    }

    public static boolean showEditableBadge() {
        return state().showEditableBadge;
    }

    public static long maxEditableBytes() {
        ArchiverSettingsState s = state();
        return s == null ? DEFAULT_MAX_EDITABLE_BYTES : s.maxEditableBytes();
    }

    public static int batchSaveDelayMillis() {
        return Math.max(0, state().batchSaveDelayMillis);
    }
}


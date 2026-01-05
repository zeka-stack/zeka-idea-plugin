package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Autocomplete 全局设置
 */
@State(
    name = "AutocompleteSettings",
    storages = @Storage("zeka.stack.intelliai.engine.xml")
)
public class AutocompleteSettings implements PersistentStateComponent<AutocompleteSettings> {
    public boolean enabled = true;
    public boolean autoTrigger = true;
    public boolean lookupTrigger = true;
    public long debounceMs = 300L;
    public long timeoutMs = 8000L;
    public int maxFileLength = 2_000_000;

    /** 指定 ProviderId（为空则走 AIProviderSettings 的默认选择） */
    public String providerId;

    /** 故障切换 ProviderId（为空则不切换） */
    public String fallbackProviderId;

    public static AutocompleteSettings getInstance() {
        return ApplicationManager.getApplication().getService(AutocompleteSettings.class);
    }

    @Override
    public @NotNull AutocompleteSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AutocompleteSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}

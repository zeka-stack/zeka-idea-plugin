package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * 插件设置状态管理
 * 使用 @State 注解自动持久化配置
 */
@State(
        name = "ChangelogPluginSettings",
        storages = @Storage("changelog-plugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    // 基础设置
    public String exampleText = "Hello World";
    public boolean enableFeature = true;
    public int maxItems = 100;

    // 高级设置
    public String customPath = "";
    public boolean debugMode = false;
    public int timeout = 5000;

    // AI 提供商配置
    public AIProviderSettings providerSettings = new AIProviderSettings();

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getter 和 Setter 方法
    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    public boolean isEnableFeature() {
        return enableFeature;
    }

    public void setEnableFeature(boolean enableFeature) {
        this.enableFeature = enableFeature;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public String getCustomPath() {
        return customPath;
    }

    public void setCustomPath(String customPath) {
        this.customPath = customPath;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

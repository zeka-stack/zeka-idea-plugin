package dev.dong4j.zeka.stack.idea.plugin.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Data;

/**
 * Proxyer Settings State
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 03:01:31
 * @since hello.world
 */
@Data
@State(
    name = "ZksDevHelperProxyerSettingsState",
    storages = @Storage("zeka.stack.dev.helper.proxyer.xml")
)
public class ProxyerSettingsState implements PersistentStateComponent<ProxyerSettingsState> {

    // ========== Proxyer 模块配置 ==========
    /** 是否启用 Proxyer 功能，默认启用 */
    private boolean enableProxyer = true;

    // 后续可以在此添加更多 Proxyer 相关配置选项，例如：
    // private boolean enableInterfaceAutoRecognition = true;
    // private String proxyerScanPath = "";

    /**
     * 获取当前对象的 Proxyer 设置状态
     * <p>
     * 返回当前对象的 Proxyer 设置状态，该方法用于获取配置信息。
     *
     * @return 当前对象的 Proxyer 设置状态，可能为 null
     */
    @Nullable
    @Override
    public ProxyerSettingsState getState() {
        return this;
    }

    /**
     * 加载状态信息到当前对象
     * <p>
     * 通过复制传入的 ProxyerSettingsState 对象的状态到当前对象中，实现状态的加载。
     *
     * @param state 要加载状态的 ProxyerSettingsState 对象
     */
    @Override
    public void loadState(@NotNull ProxyerSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 获取 ProxyerSettingsState 的单例实例
     * <p>
     * 通过 ApplicationManager 获取应用实例，并从中获取 ProxyerSettingsState 服务对象。
     *
     * @return ProxyerSettingsState 的单例实例
     */
    public static ProxyerSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(ProxyerSettingsState.class);
    }
}


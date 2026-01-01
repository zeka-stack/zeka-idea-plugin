package dev.dong4j.zeka.stack.idea.plugin.settings.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.ProxyerSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.settings.ui.ProxyerSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;

/**
 * Proxyer Settings Configurable
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 03:02:16
 * @since hello.world
 */
public class ProxyerSettingsConfigurable implements Configurable {

    /**
     * 设置面板，用于展示和配置 Proxyer 设置
     *
     * @see ProxyerSettingsPanel
     */
    private ProxyerSettingsPanel settingsPanel;

    /**
     * 获取显示名称
     * <p>
     * 返回 Proxyer 设置的显示名称（使用国际化）
     *
     * @return 显示名称
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    @NotNull
    public String getDisplayName() {
        return HelperBundle.message("settings.proxyer.display.name");
    }

    /**
     * 创建组件
     * <p>
     * 用于创建并返回配置面板的主面板组件。初始化设置面板并调用其获取主面板的方法。
     *
     * @return 配置面板的主面板组件，可能为 null
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new ProxyerSettingsPanel();
        }
        reset();
        return settingsPanel.getMainPanel();
    }

    /**
     * 判断当前设置是否被修改过
     * <p>
     * 通过获取 Proxyer 设置状态实例，检查设置面板是否被修改
     *
     * @return 如果设置被修改返回 true，否则返回 false
     */
    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        ProxyerSettingsState settings = ProxyerSettingsState.getInstance();
        return settingsPanel.isModified(settings);
    }

    /**
     * 应用设置面板中的配置
     * <p>
     * 如果设置面板不为空，则获取 Proxyer 设置状态实例，并将设置应用到设置状态中。
     */
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel != null) {
            ProxyerSettingsState settings = ProxyerSettingsState.getInstance();
            settingsPanel.apply(settings);
        }
    }

    /**
     * 重置设置面板的状态
     * <p>
     * 如果设置面板不为空，则获取 Proxyer 设置状态实例，并调用设置面板的重置方法，将其状态恢复为初始值。
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            ProxyerSettingsState settings = ProxyerSettingsState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    /**
     * 释放 UI 资源
     * <p>
     * 将设置面板引用置为 null，以释放相关 UI 资源。
     */
    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}


package dev.dong4j.zeka.stack.idea.plugin.settings.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.ZksDevHelperMainState;
import dev.dong4j.zeka.stack.idea.plugin.settings.ui.ZksDevHelperMainPanel;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;

/**
 * ZKS Dev Helper 主设置配置类
 * <p>
 * 实现了 Configurable 接口，用于配置和管理 ZKS Dev Helper 插件的主设置界面。
 * 该类位于 {@code settings.configurable} 包中，作为一级菜单的配置类。
 * 通过 {@link dev.dong4j.zeka.stack.idea.plugin.settings.ui.ZksDevHelperMainPanel} 提供用户界面组件，
 * 并处理设置的修改、应用和重置。
 * <p>
 * 目录结构说明：
 * <ul>
 *   <li>{@code settings.configurable} - 配置类（Configurable 接口实现）</li>
 *   <li>{@code settings.ui} - UI 面板类（Panel 类）</li>
 *   <li>{@code settings.state} - 状态类（PersistentStateComponent 实现）</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.02
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.ui.ZksDevHelperMainPanel
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.state.ZksDevHelperMainState
 * @since 1.0.0
 */
public class ZksDevHelperMainConfigurable implements Configurable {

    /**
     * 设置面板，用于展示和配置主设置
     *
     * @see ZksDevHelperMainPanel
     */
    private ZksDevHelperMainPanel settingsPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    @NotNull
    public String getDisplayName() {
        return HelperBundle.message("settings.main.display.name");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new ZksDevHelperMainPanel();
        }
        reset();
        return settingsPanel.getMainPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        ZksDevHelperMainState settings = ZksDevHelperMainState.getInstance();
        return settingsPanel.isModified(settings);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel != null) {
            ZksDevHelperMainState settings = ZksDevHelperMainState.getInstance();
            settingsPanel.apply(settings);
        }
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            ZksDevHelperMainState settings = ZksDevHelperMainState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}


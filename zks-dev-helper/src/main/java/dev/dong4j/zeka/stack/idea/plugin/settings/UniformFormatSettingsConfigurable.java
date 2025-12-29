package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.openapi.options.Configurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.util.ZKSDevHelperBundle;

/**
 * 统一格式设置配置类
 * <p> 实现了 Configurable 接口, 用于配置和管理统一格式设置. 该类通过设置面板提供用户界面组件, 并处理设置的修改, 应用和重置.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.29
 * @since 1.0.0
 */
public class UniformFormatSettingsConfigurable implements Configurable {

    /**
     * 设置面板, 用于展示和配置统一格式设置
     *
     * @see UniformFormatSettingsPanel
     */
    private UniformFormatSettingsPanel settingsPanel;

    /**
     * 获取显示名称
     * <p> 返回 ZKS Dev Helper 插件的显示名称（使用国际化）
     *
     * @return 显示名称
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    @NotNull
    public String getDisplayName() {
        return ZKSDevHelperBundle.message("settings.display.name");
    }

    /**
     * 创建组件
     * <p> 用于创建并返回配置面板的主面板组件. 初始化设置面板并调用其获取主面板的方法.
     *
     * @return 配置面板的主面板组件, 可能为 null
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        settingsPanel = new UniformFormatSettingsPanel();
        return settingsPanel.getMainPanel();
    }

    /**
     * 判断当前设置是否被修改过
     * <p> 通过获取统一格式设置状态实例, 检查设置面板是否被修改
     *
     * @return 如果设置被修改返回 true, 否则返回 false
     */
    @Override
    public boolean isModified() {
        UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
        return settingsPanel != null && settingsPanel.isModified(settings);
    }

    /**
     * 应用设置面板中的配置
     * <p> 如果设置面板不为空, 则获取统一格式设置状态实例, 并将设置应用到设置面板上.
     *
     * @since 1.0
     */
    @Override
    public void apply() {
        if (settingsPanel != null) {
            UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
            settingsPanel.apply(settings);
        }
    }

    /**
     * 重置设置面板的状态
     * <p> 如果设置面板不为空, 则获取统一格式设置状态实例, 并调用设置面板的重置方法, 将其状态恢复为初始值.
     *
     * @since 1.0
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    /**
     * 释放 UI 资源
     * <p> 将设置面板引用置为 null, 以释放相关 UI 资源.
     */
    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}

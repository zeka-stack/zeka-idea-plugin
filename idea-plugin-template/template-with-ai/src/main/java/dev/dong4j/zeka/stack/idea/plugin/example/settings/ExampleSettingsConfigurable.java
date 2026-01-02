package dev.dong4j.zeka.stack.idea.plugin.example.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.example.settings.ui.ExampleSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;

/**
 * 设置配置类
 * <p> 用于管理应用程序的设置配置, 提供配置面板的创建, 修改状态判断, 应用和重置功能.
 * <p> 该类实现了 Configurable 接口, 通常用于 IDE 插件或工具中, 支持用户自定义设置的保存与恢复.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class ExampleSettingsConfigurable implements Configurable {

    /** 设置面板, 用于显示和编辑插件配置界面 */
    private ExampleSettingsPanel settingsPanel;

    /**
     * 获取显示名称
     * <p> 返回此配置界面在 IDE 中的显示名称
     * <p> 该名称将用于设置对话框的标题栏
     *
     * @return 配置界面的显示名称, 通常为本地化字符串
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return ExampleBundle.message("settings.display.name");
    }

    /**
     * 创建并返回设置面板组件
     * <p> 如果 settingsPanel 为空, 则创建一个新的 ExampleSettingsPanel 实例, 并调用 reset 方法进行初始化.
     * <p> 最后返回设置面板的主要面板组件.
     *
     * @return 设置面板的主要面板组件, 如果未创建则返回 null
     */
    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new ExampleSettingsPanel();
        }
        // 初始化 UI 数据
        reset();
        return settingsPanel.getMainPanel();
    }

    /**
     * 检查设置是否被修改
     * <p> 比较当前设置面板中的配置与全局设置状态, 判断是否有未保存的修改
     *
     * @return 如果设置已被修改则返回 true, 否则返回 false
     * @since 1.0.0
     */
    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerSettings = settings.providerConfig;
        return settingsPanel.isModified(settings, providerSettings);
    }

    /**
     * 应用当前配置设置
     * <p> 调用设置面板的 apply 方法将当前界面中的配置应用到全局设置中
     *
     * @throws ConfigurationException 当配置应用失败时抛出
     */
    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings);
    }

    /**
     * 重置设置面板的状态
     * <p> 从配置状态中加载数据并更新面板内容, 用于在界面重新显示时恢复当前设置
     *
     * @since 1.0.0
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            SettingsState settings = SettingsState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    /**
     * 释放 UI 资源
     * <p> 用于释放与设置面板相关的 UI 资源, 防止内存泄漏.
     *
     */
    @Override
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }
}


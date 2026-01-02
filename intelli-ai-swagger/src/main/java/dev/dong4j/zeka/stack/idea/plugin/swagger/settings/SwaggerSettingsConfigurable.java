package dev.dong4j.zeka.stack.idea.plugin.swagger.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.ui.SwaggerSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;

/**
 * Swagger 设置配置类
 * <p> 实现 Configurable 接口, 用于在 IDE 设置界面中提供 Swagger 相关配置功能.
 * <p> 该类负责创建和管理 Swagger 设置面板, 支持用户修改和保存 Swagger 相关的配置选项.
 * <p> 主要功能包括:
 * <ul>
 *   <li> 提供用户友好的配置界面 </li>
 *   <li> 支持配置项的修改检测 </li>
 *   <li> 支持配置的加载, 应用和重置 </li>
 *   <li> 管理 UI 资源的生命周期 </li>
 * </ul>
 * <p> 使用示例:
 * <pre>{@code
 * SwaggerSettingsConfigurable configurable = new SwaggerSettingsConfigurable();
 * JComponent component = configurable.createComponent();
 * configurable.apply();
 * configurable.reset();
 * configurable.disposeUIResources();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class SwaggerSettingsConfigurable implements Configurable {

    /** 存储 Swagger 设置面板实例, 用于配置界面交互 */
    private SwaggerSettingsPanel settingsPanel;

    /**
     * 获取插件设置配置界面的显示名称
     * <p> 返回插件设置配置界面的标题名称
     *
     * @return 插件设置配置界面的显示名称
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return SwaggerBundle.message("settings.display.name");
    }

    /**
     * 创建配置界面组件
     * <p> 初始化并返回插件的设置配置界面组件, 如果尚未创建则先创建面板实例
     *
     * @return 配置界面组件, 可能为 null
     */
    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new SwaggerSettingsPanel();
        }
        // 初始化 UI 数据
        reset();
        return settingsPanel.getMainPanel();
    }

    /**
     * 检查当前设置是否被修改
     * <p> 通过比较设置面板中的当前状态与存储的状态, 判断设置是否已被修改
     *
     * @return 如果设置被修改则返回 true, 否则返回 false
     */
    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerSettings = settings.providerConfig;
        return settingsPanel.isModified(settings, providerSettings);
    }

    /**
     * 应用配置更改
     * <p> 将设置面板中的配置应用到全局设置状态中
     *
     * @throws ConfigurationException 如果应用配置过程中发生错误
     */
    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings);
    }

    /**
     * 重置设置面板的状态
     * <p> 使用当前的设置状态重置设置面板, 确保界面与最新的设置保持一致
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
     * <p> 用于释放与用户界面相关的资源, 如面板等, 防止内存泄漏
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


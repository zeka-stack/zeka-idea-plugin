package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui.ChangelogSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 插件设置配置界面
 */
public class ChangelogSettingsConfigurable implements Configurable {

    /**
     * 设置面板, 用于管理变更日志相关设置
     *
     * @see ChangelogSettingsPanel
     */
    private ChangelogSettingsPanel settingsPanel;

    /**
     * 获取显示名称
     * <p>
     * 返回用于显示的名称, 通常用于界面展示或日志记录
     *
     * @return 显示名称
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return ChangelogBundle.message("settings.display.name");
    }

    /**
     * 创建设置组件
     * <p>
     * 初始化并返回设置面板的主组件. 如果设置面板尚未创建, 则先创建一个新的设置面板.
     *
     * @return 设置面板的主组件, 可能为 null
     */
    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new ChangelogSettingsPanel();
        }
        // 初始化 UI 数据
        reset();
        return settingsPanel.getMainPanel();
    }

    /**
     * 检查设置面板中的配置是否已被修改
     * <p>
     * 通过获取当前设置状态和提供商配置, 判断设置面板中的配置是否发生了变化.
     *
     * @return 如果配置已被修改则返回 true, 否则返回 false
     */
    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerSettings = settings.providerConfig;
        return settingsPanel.isModified(settings, providerSettings);
    }

    /**
     * 应用配置设置
     * <p>
     * 获取配置状态实例, 并将配置应用到设置面板上
     *
     * @throws ConfigurationException 如果应用配置过程中发生异常
     * @since 1.0
     */
    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings);
    }

    /**
     * 重置设置面板
     * <p>
     * 如果 {@code settingsPanel} 不为 {@code null}, 则获取 {@link SettingsState} 单例实例, 并
     * <p>
     * 该方法覆盖了父类 / 接口中的 {@code reset} 方法.
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            SettingsState settings = SettingsState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    /**
     * 释放与用户界面相关的资源
     * <p>
     * 该方法用于清理与设置面板相关的 UI 资源, 防止内存泄漏.
     */
    @Override
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }
}

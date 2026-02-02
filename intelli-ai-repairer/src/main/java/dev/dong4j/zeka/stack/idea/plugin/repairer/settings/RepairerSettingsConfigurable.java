package dev.dong4j.zeka.stack.idea.plugin.repairer.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.repairer.settings.ui.RepairerSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;

/**
 * Repairer 设置配置类
 * <p>
 * 实现 Configurable 接口，在 IDE 设置中提供 Repairer 的 AI 服务商配置。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class RepairerSettingsConfigurable implements Configurable {

    /**
     * 用于存储和管理设置面板的实例
     * <p> 该面板包含了 Repairer 设置的相关 UI 组件
     *
     * @see RepairerSettingsPanel
     */
    private RepairerSettingsPanel settingsPanel;

    /**
     * 获取 Repairer 设置页面的标题.
     * <p> 该方法返回在 IDE 设置面板中显示的标题字符串, 使用 {@link RepairerBundle} 进行国际化.
     *
     * @return 在设置页面中显示的标题
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return RepairerBundle.message("settings.display.name");
    }

    /**
     * 创建配置界面组件
     * <p> 如果设置面板尚未初始化, 则创建新的 RepairerSettingsPanel 实例, 然后重置面板状态并返回主面板组件.
     *
     * @return 配置界面的主面板组件, 如果未初始化或无可用组件则返回 null
     */
    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new RepairerSettingsPanel();
        }
        reset();
        return settingsPanel.getMainPanel();
    }

    /**
     * 检查设置界面中的内容是否已被修改
     * <p> 通过比较当前界面状态与持久化的配置状态, 判断是否有未保存的更改
     *
     * @return 如果设置已被修改返回 {@code true}, 否则返回 {@code false}
     */
    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerSettings = settings.providerConfig;
        return settingsPanel.isModified(settings, providerSettings);
    }

    /**
     * 应用当前设置到配置状态中
     * <p> 获取当前的配置状态对象, 并将设置面板中的配置应用到该对象中.
     *
     * @throws ConfigurationException 如果在应用配置过程中发生错误
     */
    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings);
    }

    /**
     * 重置设置面板状态
     * <p> 当设置面板不为 null 时, 获取当前全局设置状态并调用面板的重置方法, 恢复默认配置
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
     * <p> 当配置对话框关闭时调用此方法, 用于清理和释放设置面板占用的 UI 资源
     * <p> 如果设置面板不为 null, 则调用其 dispose 方法并设置为 null
     */
    @Override
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }
}

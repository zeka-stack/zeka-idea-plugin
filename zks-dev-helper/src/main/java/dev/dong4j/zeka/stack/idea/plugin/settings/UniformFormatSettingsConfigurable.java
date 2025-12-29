package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.openapi.options.Configurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * 统一格式插件设置配置类
 * <p>
 * 该类用于实现插件的统一格式设置功能，提供配置界面、状态同步、修改检测和应用重置等操作。
 * 主要用于插件配置界面的构建和配置状态的管理。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
public class UniformFormatSettingsConfigurable implements Configurable {

    /** 设置面板，用于展示和配置统一格式设置 */
    private UniformFormatSettingsPanel settingsPanel;

    /**
     * 获取显示名称
     * <p>
     * 返回统一格式的显示名称
     *
     * @return 显示名称
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Uniform Format";
    }

    /**
     * 创建组件
     * <p>
     * 用于创建并返回配置面板的主面板组件。
     *
     * @return 配置面板的主面板组件，可能为 null
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        settingsPanel = new UniformFormatSettingsPanel();
        return settingsPanel.getMainPanel();
    }

    /**
     * 判断当前设置是否被修改过
     * <p>
     * 通过获取统一格式设置状态实例，检查设置面板是否被修改
     *
     * @return 如果设置被修改返回 true，否则返回 false
     */
    @Override
    public boolean isModified() {
        UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
        return settingsPanel != null && settingsPanel.isModified(settings);
    }

    /**
     * 应用设置面板中的配置
     * <p>
     * 如果设置面板不为空，则获取统一格式设置状态实例，并将设置应用到设置面板上。
     *
     * @param 无 参数
     * @return 无 返回值
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
     * <p>
     * 如果设置面板不为空，则获取统一格式设置状态实例，并调用设置面板的重置方法，将其状态恢复为初始值。
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
            settingsPanel.reset(settings);
        }
    }

    /**
     * 释放UI资源
     * <p>
     * 将设置面板引用置为null，以释放相关UI资源。
     *
     * @author Java开发工程师
     * @since 1.0
     */
    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}

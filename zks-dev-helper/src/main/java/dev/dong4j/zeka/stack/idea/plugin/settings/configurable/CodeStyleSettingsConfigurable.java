package dev.dong4j.zeka.stack.idea.plugin.settings.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.settings.ui.CodeStyleSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;

/**
 * 代码样式设置配置类
 * <p>
 * 实现了 Configurable 接口，作为二级菜单，用于配置代码样式相关设置，
 * 包括文件模板、Live Template、代码风格配置和在线更新功能。
 * 该类位于 {@code settings.configurable} 包中。
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
 * @date 2026.01.01
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.ui.CodeStyleSettingsPanel
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState
 * @since 1.0.0
 */
public class CodeStyleSettingsConfigurable implements Configurable {

    /**
     * 设置面板，用于展示和配置代码样式设置
     *
     * @see CodeStyleSettingsPanel
     */
    private CodeStyleSettingsPanel settingsPanel;

    /**
     * 获取显示名称
     * <p>
     * 返回代码样式设置的显示名称（使用国际化）
     *
     * @return 显示名称
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    @NotNull
    public String getDisplayName() {
        return HelperBundle.message("settings.codestyle.display.name");
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
            settingsPanel = new CodeStyleSettingsPanel();
        }
        return settingsPanel.getMainPanel();
    }

    /**
     * 判断当前设置是否被修改过
     * <p>
     * 通过获取代码样式设置状态实例，检查设置面板是否被修改
     *
     * @return 如果设置被修改返回 true，否则返回 false
     */
    @Override
    public boolean isModified() {
        CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
        return settingsPanel != null && settingsPanel.isModified(settings);
    }

    /**
     * 应用设置面板中的配置
     * <p>
     * 如果设置面板不为空，则获取代码样式设置状态实例，并将设置应用到设置状态中。
     *
     * @since 1.0
     */
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel != null) {
            CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
            settingsPanel.apply(settings);
        }
    }

    /**
     * 重置设置面板的状态
     * <p>
     * 如果设置面板不为空，则获取代码样式设置状态实例，并调用设置面板的重置方法，将其状态恢复为初始值。
     *
     * @since 1.0
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
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


package dev.dong4j.zeka.stack.idea.plugin.terminal.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.ui.TerminalSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;

/**
 * TerminalSettingsConfigurable 类
 * <p> 用于配置终端设置的可配置组件, 提供用户界面面板的创建, 修改状态检测, 应用更改和资源释放等功能.
 * 主要用于在 IDE 中管理终端相关的配置选项, 如终端类型, 环境变量, 路径设置等.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class TerminalSettingsConfigurable implements Configurable {

    /** 设置面板, 用于显示和编辑插件配置界面 */
    private TerminalSettingsPanel settingsPanel;

    /**
     * 获取配置界面的显示名称
     * <p> 返回此配置界面在 IDE 设置对话框中显示的名称, 该名称将用于设置窗口的标题栏 </p>
     *
     * @return 配置界面的显示名称, 为本地化字符串
     * @since 1.0.0
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return TerminalBundle.message("settings.display.name");
    }

    /**
     * 创建并返回设置面板组件
     * <p> 若 {@code settingsPanel} 为空, 将实例化 {@link TerminalSettingsPanel} 并调用 {@link #reset()} 进行初始化;
     * <p> 随后返回 {@code settingsPanel} 的主面板 {@link JComponent}.
     *
     * @return 主面板组件; 若未创建则返回 {@code null}
     */
    @Override
    public @Nullable JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new TerminalSettingsPanel();
        }
        // 初始化 UI 数据
        reset();
        return settingsPanel.getMainPanel();
    }

    /**
     * 检查设置是否被修改
     * <p> 比较当前设置面板中的配置与全局设置状态, 判断是否有未保存的修改.
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
     * @since 1.0.0
     */
    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        settingsPanel.apply(settings);
    }

    /**
     * 重置设置面板的状态
     * <p> 从配置状态中加载数据并更新面板内容, 用于在界面重新显示时恢复当前设置.
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
     * <p> 用于释放与设置面板相关的 UI 资源, 防止内存泄漏
     */
    @Override
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }
}


package dev.dong4j.zeka.stack.idea.plugin.common.settings;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderConfigPanel;

/**
 * AI 通用设置面板类
 * <p> 用于在 IDE 插件中展示和管理 AI 服务提供商的通用配置, 包括凭据管理, 设置加载与保存, 修改状态检测等功能.
 * 该面板封装了 {@link AIProviderConfigPanel}, 并集成到插件的设置界面中, 支持与 {@link AIProviderSettings} 数据模型联动.
 * 适用于需要统一管理多个 AI 服务提供商配置的插件场景.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22
 * @since 1.0.0
 */
public class AICommonSettingsPanel {

    /** AI 提供者配置面板实例, 用于管理 AI 服务的通用设置界面. */
    private final AIProviderConfigPanel configPanel;

    /**
     * 初始化 AI 通用设置面板
     * <p> 创建 AICredentialManager 实例并用于初始化配置面板, 随后加载默认设置
     *
     * @since 1.0.0
     */
    public AICommonSettingsPanel() {
        AICredentialManager credentialManager = new AICredentialManager(EngineContents.PLUGIN_NAME, "AI_COMMON_");
        this.configPanel = new AIProviderConfigPanel(credentialManager);
        // 创建 UI 后立即加载配置，确保复选框等组件显示正确的初始状态
        this.configPanel.loadSettings(AIProviderSettings.getInstance());
    }

    /**
     * 获取主面板组件
     * <p> 返回封装了 AI 提供者配置面板的 JPanel 实例, 用于在 UI 中展示和操作配置内容
     *
     * @return 非空的 JPanel 实例, 表示主配置面板
     */
    @NotNull
    public JPanel getPanel() {
        return configPanel.getPanel();
    }

    /**
     * 加载配置设置
     * <p> 将指定的 AI 提供者设置加载到配置面板中, 用于初始化面板显示内容
     *
     * @param settings 需要加载的 AI 提供者设置对象, 不能为空
     */
    public void loadSettings(@NotNull AIProviderSettings settings) {
        configPanel.loadSettings(settings);
    }

    /**
     * 获取当前配置设置对象
     * <p> 返回封装在配置面板中的 AI 提供者设置实例, 用于获取或操作当前的 AI 服务配置.
     *
     * @return 当前的 AI 提供者设置对象, 非空 (@NotNull)
     */
    @NotNull
    public AIProviderSettings getSettings() {
        return configPanel.getSettings();
    }

    /**
     * 检查当前设置是否已修改
     * <p> 通过委托调用配置面板的 isModified 方法, 判断当前设置与传入的设置对象是否存在差异
     *
     * @param currentSettings 当前的 AI 提供者设置对象, 不能为空
     * @return 如果设置已修改则返回 true, 否则返回 false
     */
    public boolean isModified(@NotNull AIProviderSettings currentSettings) {
        return configPanel.isModified(currentSettings);
    }

    /**
     * 检查是否允许进行下一次编辑
     * <p> 该方法委托给内部配置面板, 用于判断当前配置状态是否允许用户继续编辑下一个字段或设置项.
     *
     * @return 如果允许进行下一次编辑则返回 true, 否则返回 false
     */
    public boolean isNextEditEnabled() {
        return configPanel.isNextEditEnabled();
    }

    /**
     * 释放资源并清理配置面板
     * <p> 调用内部配置面板的 dispose 方法, 释放相关资源, 确保面板在使用后被正确销毁
     */
    public void dispose() {
        configPanel.dispose();
    }
}

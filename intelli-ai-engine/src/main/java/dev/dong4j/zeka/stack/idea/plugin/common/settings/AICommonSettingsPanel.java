package dev.dong4j.zeka.stack.idea.plugin.common.settings;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderConfigPanel;

/**
 * AI 通用设置面板类
 * <p>
 * 该类提供 AI 服务的通用配置界面, 封装了 AI 提供者的配置面板功能,
 * 包括设置的加载, 保存, 修改状态检测等操作, 用于在 IDE 中配置 AI 相关参数
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AICommonSettingsPanel {

    private final AIProviderConfigPanel configPanel;

    public AICommonSettingsPanel() {
        AICredentialManager credentialManager = new AICredentialManager("IntelliAI Engine", "AI_COMMON_");
        this.configPanel = new AIProviderConfigPanel(credentialManager, null);
    }

    /**
     * 获取主面板
     */
    @NotNull
    public JPanel getPanel() {
        return configPanel.getPanel();
    }

    /**
     * 加载设置
     */
    public void loadSettings(@NotNull AIProviderSettings settings) {
        configPanel.loadSettings(settings);
    }

    /**
     * 获取设置
     */
    @NotNull
    public AIProviderSettings getSettings() {
        return configPanel.getSettings();
    }

    /**
     * 检查是否已修改
     */
    public boolean isModified(@NotNull AIProviderSettings currentSettings) {
        return configPanel.isModified(currentSettings);
    }
}


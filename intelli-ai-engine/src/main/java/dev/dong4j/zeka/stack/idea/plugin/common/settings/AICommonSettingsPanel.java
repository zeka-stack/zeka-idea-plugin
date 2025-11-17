package dev.dong4j.zeka.stack.idea.plugin.common.settings;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderConfigPanel;

/**
 * IntelliAI Engine 全局设置面板
 * <p>
 * 用于配置全局 AI 提供商设置，包括可用供应商列表、模型参数、运行时设置等。
 *
 * @author dong4j
 * @version 1.0.0
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


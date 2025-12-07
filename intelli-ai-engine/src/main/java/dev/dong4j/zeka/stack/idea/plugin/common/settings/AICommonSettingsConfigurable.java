package dev.dong4j.zeka.stack.idea.plugin.common.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * AI 通用设置可配置类
 * <p>
 * 实现了 SearchableConfigurable 接口, 用于在 IDE 中提供 AI 通用设置的配置界面.
 * 该类负责创建和管理 AI 设置面板, 处理设置的保存, 重置和应用等操作.
 * 主要功能包括显示 IntelliAI 引擎的通用配置选项, 管理 AI 提供者的设置参数.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AICommonSettingsConfigurable implements SearchableConfigurable {

    private AICommonSettingsPanel settingsPanel;

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return "dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable";
    }

    @Override
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return EngineContents.PLUGIN_NAME;
    }

    @Override
    @Nullable
    public String getHelpTopic() {
        return "settings.ai.common";
    }

    @Override
    @Nullable
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new AICommonSettingsPanel();
        }
        return settingsPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        AIProviderSettings currentSettings = AIProviderSettings.getInstance();
        return settingsPanel.isModified(currentSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }
        AIProviderSettings currentSettings = AIProviderSettings.getInstance();
        AIProviderSettings panelSettings = settingsPanel.getSettings();
        currentSettings.applyFrom(panelSettings);
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.loadSettings(AIProviderSettings.getInstance());
        }
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}


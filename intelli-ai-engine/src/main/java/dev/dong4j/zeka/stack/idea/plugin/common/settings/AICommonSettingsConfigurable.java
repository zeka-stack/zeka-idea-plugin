package dev.dong4j.zeka.stack.idea.plugin.common.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.agent.IntelliAgentUpdateChecker;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.nextedit.NextEditSettings;

/**
 * AI 通用设置配置类
 * <p> 实现了 SearchableConfigurable 接口, 用于在 IntelliJ IDEA 插件中管理 AI 相关的通用设置.
 * 包括设置面板的创建, 设置的保存, 重置和界面资源的释放等功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.01
 * @since 1.0.0
 */
public class AICommonSettingsConfigurable implements SearchableConfigurable {

    /**
     * AI 通用设置面板实例
     * <p> 用于在 IDE 中显示和管理 AI 设置选项
     *
     * @see AICommonSettingsPanel
     */
    private AICommonSettingsPanel settingsPanel;

    /**
     * 获取配置项的唯一标识符
     * <p> 返回一个字符串, 表示当前配置项的唯一标识符. 此标识符用于在 IDE 中标识和区分不同的配置项.
     *
     * @return 唯一标识符字符串
     */
    @Override
    @NotNull
    @NonNls
    public String getId() {
        return "dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable";
    }

    /**
     * 获取插件的显示名称
     * <p> 返回插件在 IDE 中的显示名称, 通常用于菜单项或配置界面的标题
     *
     * @return 插件的显示名称
     */
    @Override
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return EngineContents.PLUGIN_NAME;
    }

    /**
     * 获取帮助主题的标识符
     * <p> 返回与 AI 通用设置相关的帮助主题标识符
     *
     * @return 帮助主题标识符, 如果不需要帮助主题则返回 null
     */
    @Override
    @Nullable
    public String getHelpTopic() {
        return "settings.ai.common";
    }

    /**
     * 创建并返回 AI 通用设置面板组件
     * <p> 如果 settingsPanel 为空, 则创建一个新的 AICommonSettingsPanel 实例. 然后返回该面板的根组件.
     *
     * @return 包含 AI 通用设置的 JComponent 组件
     */
    @Override
    @Nullable
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new AICommonSettingsPanel();
        }
        return settingsPanel.getPanel();
    }

    /**
     * 检查当前设置是否被修改
     * <p> 通过比较当前的 AIProviderSettings 实例与设置面板中的设置来判断是否被修改
     *
     * @return 如果设置被修改则返回 true, 否则返回 false
     */
    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        AIProviderSettings currentSettings = AIProviderSettings.getInstance();
        return settingsPanel.isModified(currentSettings);
    }

    /**
     * 应用当前设置面板中的更改
     * <p> 此方法将设置面板中的配置应用到全局设置中, 并启动智能代理更新检查器.
     * 如果设置面板为空, 则直接返回.
     *
     * @throws ConfigurationException 当应用配置时发生错误
     */
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }
        AIProviderSettings currentSettings = AIProviderSettings.getInstance();
        AIProviderSettings panelSettings = settingsPanel.getSettings();
        currentSettings.applyFrom(panelSettings);
        NextEditSettings.getInstance().enabled = settingsPanel.isNextEditEnabled();

        // 重新启动更新检查器（根据新的配置）
        IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
        updateChecker.start();
    }

    /**
     * 重置设置面板
     * <p> 将当前的 AI 提供者设置加载到设置面板中, 使面板反映最新的设置状态.
     *
     * @since 1.0.0
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.loadSettings(AIProviderSettings.getInstance());
        }
    }

    /**
     * 释放 UI 资源
     * <p> 将 settingsPanel 设置为 null, 以便在不再需要时释放相关资源, 防止内存泄漏.
     */
    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}

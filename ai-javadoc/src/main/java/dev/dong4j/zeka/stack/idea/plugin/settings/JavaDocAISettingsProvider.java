package dev.dong4j.zeka.stack.idea.plugin.settings;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.PluginAISettingsProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * AI Javadoc 插件的 AI 设置提供者
 * <p>
 * 实现 PluginAISettingsProvider 接口，从 SettingsState 读取配置。
 * 作为项目服务注册，供 AIService 使用。
 *
 * @author dong4j
 * @version 1.0.0
 */
public final class JavaDocAISettingsProvider implements PluginAISettingsProvider {

    @Override
    @NotNull
    public AIProviderType getDefaultProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerType != null ? settings.providerType : AIProviderType.QIANWEN;
    }

    @Override
    @NotNull
    public AIModelParameters getModelParameters() {
        SettingsState settings = SettingsState.getInstance();
        // 如果插件配置了模型参数，使用插件的；否则使用全局默认值
        if (settings.modelParameters != null) {
            return settings.modelParameters;
        }
        return AIProviderSettings.getInstance().modelParameters;
    }

    @Override
    @NotNull
    public AIRuntimeSettings getRuntimeSettings() {
        SettingsState settings = SettingsState.getInstance();
        // 如果插件配置了运行时设置，使用插件的；否则使用全局默认值
        if (settings.runtimeSettings != null) {
            return settings.runtimeSettings;
        }
        return AIProviderSettings.getInstance().runtimeSettings;
    }

    @Override
    public boolean isPerformanceMode() {
        SettingsState settings = SettingsState.getInstance();
        return settings.performanceMode;
    }

}


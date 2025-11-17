package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;

/**
 * AI 提供商设置变更监听器
 * <p>
 * 用于监听 AIProviderSettings 中可用提供商列表的变化，以便在设置页面中动态刷新 UI。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AIProviderSettingsListener {
    /**
     * 当可用提供商列表发生变化时调用
     * <p>
     * 该方法在以下情况下会被调用：
     * <ul>
     *   <li>添加新的可用提供商</li>
     *   <li>移除可用提供商</li>
     *   <li>清空所有可用提供商</li>
     *   <li>通过 applyFrom 方法更新配置</li>
     * </ul>
     *
     * @param settings 更新后的 AIProviderSettings 实例
     */
    void onAvailableProvidersChanged(@NotNull AIProviderSettings settings);
}


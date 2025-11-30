package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;

/**
 * AI 提供者设置监听器接口
 * <p>
 * 用于监听 AI 提供者设置的变化, 当可用的 AI 提供者发生改变时触发相应的回调方法.
 * 实现该接口的类可以接收 AI 提供者设置变更的通知, 以便及时更新相关的配置或状态.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
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


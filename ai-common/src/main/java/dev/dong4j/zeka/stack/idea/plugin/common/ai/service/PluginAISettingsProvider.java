package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 插件 AI 设置提供者接口
 * <p>
 * 外部插件需要实现此接口，提供自己的默认供应商选择和模型参数等配置。
 *
 * @author dong4j
 * @version 1.0.0
 */
public interface PluginAISettingsProvider {

    /**
     * 获取插件使用的默认供应商类型
     * <p>
     * 从全局可用供应商列表中选取。
     *
     * @return 默认供应商类型
     */
    @NotNull
    AIProviderType getDefaultProviderType();

    /**
     * 获取模型参数（可选）
     * <p>
     * 如果返回 null，则使用全局默认值。
     *
     * @return 模型参数，可以为 null
     */
    @NotNull
    AIModelParameters getModelParameters();

    /**
     * 获取运行时设置（可选）
     * <p>
     * 如果返回 null，则使用全局默认值。
     *
     * @return 运行时设置，可以为 null
     */
    @NotNull
    AIRuntimeSettings getRuntimeSettings();

    /**
     * 是否启用性能模式
     *
     * @return 是否启用性能模式
     */
    boolean isPerformanceMode();
}


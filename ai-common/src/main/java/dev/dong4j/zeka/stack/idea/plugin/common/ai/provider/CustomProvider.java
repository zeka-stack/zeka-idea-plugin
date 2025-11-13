package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 自定义 OpenAI 兼容服务提供商。
 */
public class CustomProvider extends AICompatibleProvider {

    /**
     * 构造一个 CustomProvider 实例
     * <p>
     * 使用提供的 AI 配置, 模型参数和运行时设置初始化 CustomProvider 对象
     *
     * @param config          AIProviderConfig 对象, 用于配置 AI 提供者
     * @param modelParameters AIModelParameters 对象, 用于指定模型参数
     * @param runtimeSettings AIRuntimeSettings 对象, 用于设置运行时参数
     */
    public CustomProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }

    /**
     * 构造一个自定义的 AI 提供者实例
     * <p>
     * 使用指定的配置, 模型参数, 运行时设置和控制台日志器初始化 AI 提供者.
     *
     * @param config          配置信息, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     * @param consoleLogger   控制台日志器, 可以为 null
     */
    public CustomProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings,
                          @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    /**
     * 创建一个 {@code CustomProvider} 实例.
     * <p>
     * 该构造函数使用指定的 {@link AIProviderConfig},{@link AIModelParameters},{@link AIRuntimeSettings},{@link AIConsoleLogger}
     * 以及性能模式标志来初始化 {@code CustomProvider}.
     *
     * @param config          AIProvider 的配置信息, 不能为空
     * @param modelParameters AI 模型参数, 不能为空
     * @param runtimeSettings AI 运行时设置, 不能为空
     * @param consoleLogger   用于日志输出的控制台日志器, 允许为 {@code null}
     * @param performanceMode 是否启用性能模式
     */
    public CustomProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings,
                          @Nullable AIConsoleLogger consoleLogger,
                          boolean performanceMode) {
        super(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
    }
}

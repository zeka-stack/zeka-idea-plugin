package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * LM Studio 服务提供商。
 */
public class LMStudioProvider extends AICompatibleProvider {

    /**
     * 初始化 LMStudioProvider 实例
     * <p>
     * 使用提供的 AIProviderConfig,AIModelParameters 和 AIRuntimeSettings 对象进行初始化
     *
     * @param config          AIProvider 配置信息
     * @param modelParameters AI 模型参数
     * @param runtimeSettings AI 运行时设置
     * @since 1.0
     */
    public LMStudioProvider(@NotNull AIProviderConfig config,
                            @NotNull AIModelParameters modelParameters,
                            @NotNull AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }

    /**
     * 构造一个 LMStudioProvider 实例
     * <p>
     * 使用指定的配置, 模型参数, 运行时设置和控制台日志记录器初始化 LMStudio 提供者.
     *
     * @param config          配置信息, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     * @param consoleLogger   控制台日志记录器, 可以为 null
     */
    public LMStudioProvider(@NotNull AIProviderConfig config,
                            @NotNull AIModelParameters modelParameters,
                            @NotNull AIRuntimeSettings runtimeSettings,
                            @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    /**
     * 创建一个 {@code LMStudioProvider} 实例.
     * <p>
     * 该构造函数将 {@link AIProviderConfig},{@link AIModelParameters},{@link AIRuntimeSettings},
     * {@link AIConsoleLogger}(可选) 以及性能模式标志传递给父类构造函数, 以完成 {@code LMStudioProvider}
     * 的初始化.
     *
     * @param config          AI 提供者的配置信息, 不能为空
     * @param modelParameters AI 模型参数, 不能为空
     * @param runtimeSettings AI 运行时设置, 不能为空
     * @param consoleLogger   控制台日志记录器, 可为 {@code null}
     * @param performanceMode 是否开启性能模式
     */
    public LMStudioProvider(@NotNull AIProviderConfig config,
                            @NotNull AIModelParameters modelParameters,
                            @NotNull AIRuntimeSettings runtimeSettings,
                            @Nullable AIConsoleLogger consoleLogger,
                            boolean performanceMode) {
        super(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
    }
}

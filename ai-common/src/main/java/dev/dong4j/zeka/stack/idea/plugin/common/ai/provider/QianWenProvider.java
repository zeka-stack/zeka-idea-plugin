package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 通义千问服务提供商。
 */
public class QianWenProvider extends AICompatibleProvider {

    /**
     * 构造一个 QianWenProvider 实例
     * <p>
     * 使用提供的配置, 模型参数和运行时设置初始化 QianWenProvider 对象
     *
     * @param config          AI 服务的配置信息
     * @param modelParameters 模型相关的参数配置
     * @param runtimeSettings 运行时环境设置
     */
    public QianWenProvider(@NotNull AIProviderConfig config,
                           @NotNull AIModelParameters modelParameters,
                           @NotNull AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }

    /**
     * 构造一个 {@code QianWenProvider} 实例.
     * <p>
     * 该构造函数使用指定的 {@link AIProviderConfig},{@link AIModelParameters},{@link AIRuntimeSettings} 以及可选的 {@link AIConsoleLogger} 初始化 {@code
     * QianWenProvider}, 并将 {@code false} 作为最后一个参数传递给父类构造函数.
     *
     * @param config          提供者配置, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     * @param consoleLogger   控制台日志记录器, 可为空
     */
    public QianWenProvider(@NotNull AIProviderConfig config,
                           @NotNull AIModelParameters modelParameters,
                           @NotNull AIRuntimeSettings runtimeSettings,
                           @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    /**
     * 构造一个 QianWenProvider 实例
     * <p>
     * 初始化 QianWenProvider, 传入配置信息, 模型参数, 运行时设置, 控制台日志记录器以及性能模式标志
     *
     * @param config          AI 提供商配置信息
     * @param modelParameters 模型参数配置
     * @param runtimeSettings 运行时设置
     * @param consoleLogger   控制台日志记录器, 可为空
     * @param performanceMode 是否启用性能模式
     */
    public QianWenProvider(@NotNull AIProviderConfig config,
                           @NotNull AIModelParameters modelParameters,
                           @NotNull AIRuntimeSettings runtimeSettings,
                           @Nullable AIConsoleLogger consoleLogger,
                           boolean performanceMode) {
        super(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
    }
}

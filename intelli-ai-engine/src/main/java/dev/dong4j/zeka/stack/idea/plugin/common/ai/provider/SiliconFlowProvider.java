package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 提供硅基流 (SiliconFlow)AI 服务的实现类
 * <p>
 * 该类继承自 AICompatibleProvider, 用于集成和管理硅基流 AI 模型的配置, 参数及运行时设置, 支持多种构造方式以适应不同的使用场景.
 * 主要职责包括初始化 AI 提供者配置, 模型参数及运行时环境, 并支持日志记录和性能模式的配置.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public class SiliconFlowProvider extends AICompatibleProvider {

    /**
     * 初始化 SiliconFlowProvider 实例
     * <p>
     * 使用指定的配置, 模型参数, 运行时设置和控制台日志记录器创建 SiliconFlowProvider 对象.
     *
     * @param config          配置信息, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     * @param consoleLogger   控制台日志记录器, 可以为 null
     */
    public SiliconFlowProvider(@NotNull AIProviderConfig config,
                               @NotNull AIModelParameters modelParameters,
                               @NotNull AIRuntimeSettings runtimeSettings,
                               @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger);
    }

}

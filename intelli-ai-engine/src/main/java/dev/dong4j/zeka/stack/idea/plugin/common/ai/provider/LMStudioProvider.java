package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * LMStudio 服务提供者类
 * <p>
 * 该类用于封装 LMStudio 模型的调用逻辑, 实现与 AI 兼容接口的对接, 支持模型参数配置, 运行时设置以及日志输出等功能.
 * 提供了多种构造方法以适应不同的使用场景, 包括是否启用性能模式等高级配置.
 *
 * @author 作者名
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public class LMStudioProvider extends AICompatibleProvider {

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
        super(config, modelParameters, runtimeSettings, consoleLogger);
    }

}

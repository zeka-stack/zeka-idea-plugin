package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 自定义 AI 服务提供者类
 * <p>
 * 该类继承自 AICompatibleProvider, 用于实现特定的 AI 服务提供逻辑. 支持多种构造方式, 可灵活配置 AI 服务参数, 运行时设置以及日志记录功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public class CustomProvider extends AICompatibleProvider {

    /**
     * 创建一个 {@code CustomProvider} 实例.
     * <p>
     * 该构造函数使用指定的 {@link AIProviderConfig},{@link AIModelParameters},{@link AIRuntimeSettings},{@link AIConsoleLogger}
     * 来初始化 {@code CustomProvider}.
     *
     * @param config          AIProvider 的配置信息, 不能为空
     * @param modelParameters AI 模型参数, 不能为空
     * @param runtimeSettings AI 运行时设置, 不能为空
     * @param consoleLogger   用于日志输出的控制台日志器, 允许为 {@code null}
     */
    public CustomProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings,
                          @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger);
    }
}

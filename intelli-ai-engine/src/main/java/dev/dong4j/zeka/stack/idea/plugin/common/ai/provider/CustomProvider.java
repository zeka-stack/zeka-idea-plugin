package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 自定义 AI 提供者类
 * <p>
 * 继承自 AICompatibleProvider, 用于实现自定义的 AI 服务提供者功能,
 * 通过配置参数, 模型参数, 运行时设置和控制台日志器来初始化自定义 AI 提供者实例
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
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

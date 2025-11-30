package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 通义千问 AI 服务提供者
 * <p>
 * 继承自 AI 兼容提供者, 专门用于处理通义千问 AI 模型的服务调用和配置管理,
 * 负责初始化通义千问相关的配置参数, 模型参数和运行时设置
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class QianWenProvider extends AICompatibleProvider {

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
        super(config, modelParameters, runtimeSettings, consoleLogger);
    }

}

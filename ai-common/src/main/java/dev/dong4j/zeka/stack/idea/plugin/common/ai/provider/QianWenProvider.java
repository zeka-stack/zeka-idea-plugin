package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * QianWenProvider
 * <p>
 * 该类实现了对千问 (QianWen)AI 模型的兼容性支持, 继承自 {@link AICompatibleProvider}.
 * 通过多种构造函数, 用户可以灵活配置 AIProviderConfig,AIModelParameters,AIRuntimeSettings
 * 以及可选的 AIConsoleLogger 与性能模式开关, 以满足不同场景下的日志记录与性能调优需求.
 * <p>
 * 主要职责:
 * <ul>
 *   <li> 封装千问模型的调用细节, 提供统一的接口给业务层使用.</li>
 *   <li> 支持自定义日志输出, 方便调试与监控.</li>
 *   <li> 可开启性能模式, 优化模型调用的延迟与吞吐量.</li>
 * </ul>
 * <p>
 * 使用场景:
 * <ul>
 *   <li> 需要在项目中集成千问 AI 服务时, 直接使用本类实例化并调用.</li>
 *   <li> 在需要对日志进行自定义处理或开启性能优化时, 选择相应的构造函数.</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.11.14
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

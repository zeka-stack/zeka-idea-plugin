package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.IflowProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.LMStudioProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.ModelScopeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.OllamaProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.QianWenProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.SiliconFlowProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * AIServiceFactory
 * <p>
 * 用于创建不同 AI 服务提供者的工厂类, 根据配置信息动态生成对应的 AI 服务实现.
 * 支持多种 AI 服务类型, 如 QianWen,SiliconFlow,Ollama 等, 并提供日志记录和性能模式配置选项.
 *
 * @author 作者名
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public final class AIServiceFactory {

    /**
     * 私有构造函数, 用于防止外部实例化
     * <p>
     * 该构造函数仅在内部使用, 确保 AIServiceFactory 类只能通过静态方法创建实例
     */
    private AIServiceFactory() {
    }

    /**
     * 创建 AI 服务提供者实例
     * <p>
     * 根据提供的 {@link AIProviderConfig},{@link AIModelParameters} 与 {@link AIRuntimeSettings} 创建
     * {@link AIServiceProvider} 对象. 该方法内部调用带有更多参数的重载方法, 传入 {@code null}
     * 的扩展配置以及 {@code false} 的兼容性标志.
     *
     * @param config          AI 提供者配置, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     * @return 创建的 {@link AIServiceProvider} 实例; 若创建失败则返回 {@code null}
     */
    @Nullable
    public static AIServiceProvider createProvider(@NotNull AIProviderConfig config) {
        return createProvider(config, null);
    }

    /**
     * 根据配置信息创建对应的 AI 服务提供者实例
     * <p>
     * 根据传入的配置, 模型参数, 运行时设置以及日志记录器创建相应的 AI 服务提供者.
     * 如果未指定提供者类型, 则默认使用 QIANWEN 类型.
     *
     * @param config          提供者配置信息, 不能为空
     * @param modelParameters 模型参数信息, 不能为空
     * @param runtimeSettings 运行时设置信息, 不能为空
     * @param consoleLogger   控制台日志记录器, 可以为 null
     * @return 创建的 AI 服务提供者实例, 可能为 null
     */
    @Nullable
    public static AIServiceProvider createProvider(@NotNull AIProviderConfig config,
                                                   @Nullable AIConsoleLogger consoleLogger) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        AIModelParameters modelParameters = config.modelParameters != null ? config.modelParameters : new AIModelParameters();
        AIRuntimeSettings runtimeSettings = config.runtimeSettings != null ? config.runtimeSettings : new AIRuntimeSettings();
        return switch (providerType) {
            case CUSTOM -> new CustomProvider(config, modelParameters, runtimeSettings, consoleLogger);
            case QIANWEN -> new QianWenProvider(config, modelParameters, runtimeSettings, consoleLogger);
            case SILICONFLOW -> new SiliconFlowProvider(config, modelParameters, runtimeSettings, consoleLogger);
            case OLLAMA -> new OllamaProvider(config, modelParameters, runtimeSettings, consoleLogger);
            case LM_STUDIO -> new LMStudioProvider(config, modelParameters, runtimeSettings, consoleLogger);
            case MODELSCOPE -> new ModelScopeProvider(config, modelParameters, runtimeSettings, consoleLogger);
            case IFLOW -> new IflowProvider(config, modelParameters, runtimeSettings, consoleLogger);
        };
    }
}

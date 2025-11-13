package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.LMStudioProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.OllamaProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.QianWenProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.SiliconFlowProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * AI 服务工厂，负责根据配置创建对应的服务提供商实例。
 */
public final class AIServiceFactory {

    private AIServiceFactory() {
    }

    @Nullable
    public static AIServiceProvider createProvider(@NotNull AIProviderConfig config,
                                                   @NotNull AIModelParameters modelParameters,
                                                   @NotNull AIRuntimeSettings runtimeSettings) {
        return createProvider(config, modelParameters, runtimeSettings, null, false);
    }

    @Nullable
    public static AIServiceProvider createProvider(@NotNull AIProviderConfig config,
                                                   @NotNull AIModelParameters modelParameters,
                                                   @NotNull AIRuntimeSettings runtimeSettings,
                                                   @Nullable AIConsoleLogger consoleLogger) {
        return createProvider(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    @Nullable
    public static AIServiceProvider createProvider(@NotNull AIProviderConfig config,
                                                   @NotNull AIModelParameters modelParameters,
                                                   @NotNull AIRuntimeSettings runtimeSettings,
                                                   @Nullable AIConsoleLogger consoleLogger,
                                                   boolean performanceMode) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        return switch (providerType) {
            case CUSTOM -> new CustomProvider(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
            case QIANWEN -> new QianWenProvider(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
            case SILICONFLOW -> new SiliconFlowProvider(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
            case OLLAMA -> new OllamaProvider(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
            case LM_STUDIO -> new LMStudioProvider(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
        };
    }
}

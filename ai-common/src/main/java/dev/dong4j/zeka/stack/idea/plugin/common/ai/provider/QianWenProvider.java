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

    public QianWenProvider(@NotNull AIProviderConfig config,
                           @NotNull AIModelParameters modelParameters,
                           @NotNull AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }

    public QianWenProvider(@NotNull AIProviderConfig config,
                           @NotNull AIModelParameters modelParameters,
                           @NotNull AIRuntimeSettings runtimeSettings,
                           @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    public QianWenProvider(@NotNull AIProviderConfig config,
                           @NotNull AIModelParameters modelParameters,
                           @NotNull AIRuntimeSettings runtimeSettings,
                           @Nullable AIConsoleLogger consoleLogger,
                           boolean performanceMode) {
        super(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
    }
}

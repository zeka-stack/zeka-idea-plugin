package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 通义千问服务提供商。
 */
public class QianWenProvider extends AICompatibleProvider {

    public QianWenProvider(AIProviderConfig config,
                           AIModelParameters modelParameters,
                           AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }
}

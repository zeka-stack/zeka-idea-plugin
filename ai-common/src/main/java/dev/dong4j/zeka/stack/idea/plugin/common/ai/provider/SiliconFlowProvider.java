package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 硅基流动服务提供商。
 */
public class SiliconFlowProvider extends AICompatibleProvider {

    public SiliconFlowProvider(AIProviderConfig config,
                               AIModelParameters modelParameters,
                               AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }
}

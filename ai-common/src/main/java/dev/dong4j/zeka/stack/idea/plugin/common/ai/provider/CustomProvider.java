package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * 自定义 OpenAI 兼容服务提供商。
 */
public class CustomProvider extends AICompatibleProvider {

    public CustomProvider(AIProviderConfig config,
                          AIModelParameters modelParameters,
                          AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }
}

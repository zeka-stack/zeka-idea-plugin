package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Cloudflare Workers AI OpenAI 兼容提供商实现类
 */
public class CloudflareOpenAIProvider extends OpenAILikeProvider {

    /**
     * 初始化 Cloudflare Workers AI OpenAI 兼容提供者实例
     *
     * @param project         项目上下文, 不能为空
     * @param config          提供者配置信息, 不能为空
     * @param modelParameters 模型参数配置, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     */
    public CloudflareOpenAIProvider(@NotNull Project project,
                                    @NotNull AIProviderConfig config,
                                    @NotNull AIModelParameters modelParameters,
                                    @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}

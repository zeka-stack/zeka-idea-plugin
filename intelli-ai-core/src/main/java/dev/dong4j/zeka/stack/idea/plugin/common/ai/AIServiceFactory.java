package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.AnthropicLikeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.ModelScopeAnthropicProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.ZaiAnthropicProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.ZhipuAnthropicProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.ollama.OllamaLikeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.AzureOpenAIProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.GitHubModelsProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.IflowProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.ModelScopeOpenAIProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.OpenAILikeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.OrcaRouterProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.ZaiOpenAIProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.ZhipuOpenAIProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * AI 服务工厂类
 * <p>
 * 用于创建和管理不同类型的 AI 服务提供者实例, 根据配置参数动态创建相应的 AI 服务提供者
 * 支持多种 AI 服务提供商, 包括千问, 硅基流动,Ollama,LM Studio, 魔搭,iFlow, 智谱等
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
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
     * 根据配置信息创建对应的 AI 服务提供者实例
     * <p>
     * 根据传入的配置, 模型参数, 运行时设置以及日志记录器创建相应的 AI 服务提供者.
     * 如果未指定提供者类型, 则默认使用 QIANWEN 类型.
     *
     * @param config 提供者配置信息, 不能为空
     * @return 创建的 AI 服务提供者实例, 可能为 null
     */
    public static AIServiceProvider createProvider(@NotNull AIConsoleLogger logger, @NotNull AIProviderConfig config) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        AIModelParameters modelParameters = config.modelParameters != null ? config.modelParameters : new AIModelParameters();
        AIRuntimeSettings runtimeSettings = config.runtimeSettings != null ? config.runtimeSettings : new AIRuntimeSettings();
        return switch (providerType) {
            case FREEAI, OPENAI, DEEPSEEK, DOUBAO, GROK, HUNYUAN, QIANWEN, SILICONFLOW, OPENROUTER, BEDROCK, CLOUDFLARE, HUGGINGFACE,
                 NVIDIA,
                 MISTRAL, LM_STUDIO, MOONSHOT, QUOTIO -> new OpenAILikeProvider(logger, config, modelParameters, runtimeSettings);
            case ORCAROUTER -> new OrcaRouterProvider(logger, config, modelParameters, runtimeSettings);

            case AZURE -> new AzureOpenAIProvider(logger, config, modelParameters, runtimeSettings);
            case GITHUB_MODELS -> new GitHubModelsProvider(logger, config, modelParameters, runtimeSettings);
            case MODELSCOPE -> new ModelScopeOpenAIProvider(logger, config, modelParameters, runtimeSettings);
            case IFLOW -> new IflowProvider(logger, config, modelParameters, runtimeSettings);
            case ZHIPU -> new ZhipuOpenAIProvider(logger, config, modelParameters, runtimeSettings);
            case ZAI -> new ZaiOpenAIProvider(logger, config, modelParameters, runtimeSettings);

            case ANTHROPIC, MOONSHOT_ANTHROPIC,
                 DEEPSEEK_ANTHROPIC, DOUBAO_ANTHROPIC,
                 HUNYUAN_ANTHROPIC -> new AnthropicLikeProvider(logger, config, modelParameters, runtimeSettings);
            case ZHIPU_ANTHROPIC -> new ZhipuAnthropicProvider(logger, config, modelParameters, runtimeSettings);
            case MODELSCOPE_ANTHROPIC -> new ModelScopeAnthropicProvider(logger, config, modelParameters, runtimeSettings);
            case ZAI_ANTHROPIC -> new ZaiAnthropicProvider(logger, config, modelParameters, runtimeSettings);

            case OLLAMA -> new OllamaLikeProvider(logger, config, modelParameters, runtimeSettings);
        };
    }

}

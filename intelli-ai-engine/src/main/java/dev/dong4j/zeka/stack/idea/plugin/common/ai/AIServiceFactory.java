package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.AnthropicLikeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.ModelScopeAnthropicProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.ZaiAnthropicProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic.ZhipuAnthropicProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.codex.CodexLikeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.gemini.GeminLikeiProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.ollama.OllamaLikeProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.AzureOpenAIProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.GitHubModelsProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.IflowProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.ModelScopeOpenAIProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai.OpenAILikeProvider;
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
     * 根据提供的配置创建 AI 服务提供商实例
     * <p>
     * 使用当前项目和指定的 AIProviderConfig 配置来创建并返回一个 AIServiceProvider 实例.
     *
     * @param config AI 服务提供商的配置信息, 不可为 null
     * @return 创建的 AIServiceProvider 实例
     * @throws NullPointerException 如果 config 参数为 null 时抛出
     * @since 1.0.0
     */
    public static AIServiceProvider createProvider(@NotNull AIProviderConfig config) {
        return createProvider(getProject(), config);
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
    public static AIServiceProvider createProvider(@NotNull Project project, @NotNull AIProviderConfig config) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        AIModelParameters modelParameters = config.modelParameters != null ? config.modelParameters : new AIModelParameters();
        AIRuntimeSettings runtimeSettings = config.runtimeSettings != null ? config.runtimeSettings : new AIRuntimeSettings();
        return switch (providerType) {
            case OPENAI, DEEPSEEK, DOUBAO, GROK, HUNYUAN, QIANWEN, SILICONFLOW, OPENROUTER, BEDROCK, CLOUDFLARE, HUGGINGFACE, NVIDIA,
                 MISTRAL, LM_STUDIO, MOONSHOT -> new OpenAILikeProvider(project, config, modelParameters, runtimeSettings);

            case AZURE -> new AzureOpenAIProvider(project, config, modelParameters, runtimeSettings);
            case GITHUB_MODELS -> new GitHubModelsProvider(project, config, modelParameters, runtimeSettings);
            case MODELSCOPE -> new ModelScopeOpenAIProvider(project, config, modelParameters, runtimeSettings);
            case IFLOW -> new IflowProvider(project, config, modelParameters, runtimeSettings);
            case ZHIPU -> new ZhipuOpenAIProvider(project, config, modelParameters, runtimeSettings);
            case ZAI -> new ZaiOpenAIProvider(project, config, modelParameters, runtimeSettings);

            case ANTHROPIC, MOONSHOT_ANTHROPIC,
                 DEEPSEEK_ANTHROPIC, DOUBAO_ANTHROPIC,
                 HUNYUAN_ANTHROPIC -> new AnthropicLikeProvider(project, config, modelParameters, runtimeSettings);
            case ZHIPU_ANTHROPIC -> new ZhipuAnthropicProvider(project, config, modelParameters, runtimeSettings);
            case MODELSCOPE_ANTHROPIC -> new ModelScopeAnthropicProvider(project, config, modelParameters, runtimeSettings);
            case ZAI_ANTHROPIC -> new ZaiAnthropicProvider(project, config, modelParameters, runtimeSettings);

            case OLLAMA -> new OllamaLikeProvider(project, config, modelParameters, runtimeSettings);
            case GEMINI -> new GeminLikeiProvider(project, config, modelParameters, runtimeSettings);
            case CODEX -> new CodexLikeProvider(project, config, modelParameters, runtimeSettings);
        };
    }

    /**
     * 获取项目实例
     * <p>
     * 在应用级设置中，优先使用打开的项目，如果没有打开的项目则使用默认项目。
     * 这确保了在设置页面中也能正常使用需要 project 的功能。
     *
     * @return 项目实例，不会为 null
     */
    @NotNull
    private static Project getProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return openProjects[0];
        }
        // 如果没有打开的项目，使用默认项目
        return ProjectManager.getInstance().getDefaultProject();
    }
}

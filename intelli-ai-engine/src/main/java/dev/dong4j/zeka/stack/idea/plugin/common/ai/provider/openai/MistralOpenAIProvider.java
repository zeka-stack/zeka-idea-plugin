package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Mistral OpenAI 提供者类
 * <p> 继承自 OpenAILikeProvider, 用于封装 Mistral 系列模型的 OpenAI 兼容接口调用逻辑, 主要职责是为项目提供与 Mistral 模型交互的基础设施支持, 不负责具体的请求处理, 仅作为底层服务提供者.</p>
 * <p> 该类设计遵循面向对象原则, 避免将基础设施细节暴露给上层业务逻辑, 确保职责分离.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.18
 * @since 1.0.0
 */
public class MistralOpenAIProvider extends OpenAILikeProvider {

    /**
     * 初始化 Mistral AI OpenAI 兼容提供者实例
     * <p> 调用父类构造函数, 传入项目, 配置, 模型参数和运行时设置
     *
     * @param project         项目上下文
     * @param config          AI 提供者配置
     * @param modelParameters 模型参数配置
     * @param runtimeSettings 运行时设置
     */
    public MistralOpenAIProvider(@NotNull Project project,
                                 @NotNull AIProviderConfig config,
                                 @NotNull AIModelParameters modelParameters,
                                 @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}

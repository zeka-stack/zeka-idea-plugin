package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.codex;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Codex Provider（OpenAI）
 * <p>
 * 当前实现基于 OpenAI Chat Completions 兼容协议。
 */
public class CodexLikeProvider extends AICompatibleProvider {

    /**
     * 初始化 Codex 兼容提供者实例
     * <p> 调用父类构造函数, 传入项目, 配置, 模型参数和运行时设置
     *
     * @param project         项目上下文
     * @param config          AI 提供者配置
     * @param modelParameters 模型参数配置
     * @param runtimeSettings 运行时设置
     */
    public CodexLikeProvider(@NotNull Project project,
                             @NotNull AIProviderConfig config,
                             @NotNull AIModelParameters modelParameters,
                             @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}


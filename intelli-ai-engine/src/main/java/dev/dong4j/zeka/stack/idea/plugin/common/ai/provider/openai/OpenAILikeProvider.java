package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * OpenAI 兼容提供者抽象基类
 * <p>
 * 用于统一 OpenAI 兼容接口的提供者实现, 继承自 {@link AICompatibleProvider}.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class OpenAILikeProvider extends AICompatibleProvider {

    /**
     * 初始化 OpenAI 兼容提供者
     *
     * @param project         当前项目
     * @param config          提供者配置
     * @param modelParameters 模型参数
     * @param runtimeSettings 运行时设置
     */
    public OpenAILikeProvider(@NotNull Project project,
                              @NotNull AIProviderConfig config,
                              @NotNull AIModelParameters modelParameters,
                              @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * ModelScope OpenAI 兼容提供商实现类
 * <p>
 * 该类通过 ModelScope 的模型列表接口获取可用模型.
 */
@Slf4j
public class ModelScopeOpenAIProvider extends OpenAILikeProvider {

    /**
     * 初始化 ModelScope OpenAI 兼容提供者实例
     * <p>
     * 该构造函数用于创建 ModelScope 提供者的实例, 继承自 OpenAILikeProvider, 初始化时传入项目, 配置, 模型参数和运行时设置.
     *
     * @param project         项目上下文对象, 非空
     * @param config          提供者配置对象, 非空
     * @param modelParameters 模型参数对象, 非空
     * @param runtimeSettings 运行时设置对象, 非空
     */
    public ModelScopeOpenAIProvider(@NotNull Project project,
                                    @NotNull AIProviderConfig config,
                                    @NotNull AIModelParameters modelParameters,
                                    @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * 智谱 AI OpenAI 兼容提供商实现类
 */
public class ZhipuOpenAIProvider extends OpenAILikeProvider {

    /**
     * 初始化智谱 AI OpenAI 兼容提供者实例
     * <p> 调用父类构造函数, 传入项目, 配置, 模型参数和运行时设置
     *
     * @param project         项目上下文, 非空
     * @param config          提供者配置, 非空
     * @param modelParameters 模型参数, 非空
     * @param runtimeSettings 运行时设置, 非空
     */
    public ZhipuOpenAIProvider(@NotNull Project project,
                               @NotNull AIProviderConfig config,
                               @NotNull AIModelParameters modelParameters,
                               @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

}

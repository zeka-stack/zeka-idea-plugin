package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic;

import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * ModelScope Anthropic 兼容提供商实现类
 * <p>
 * 该类通过 ModelScope 的模型列表接口获取可用模型.
 */
@Slf4j
public class ModelScopeAnthropicProvider extends AnthropicLikeProvider {

    /**
     * 初始化 ModelScope Anthropic 兼容提供者实例
     * <p>
     * 该构造函数用于创建 ModelScope 提供者的实例, 继承自 AnthropicLikeProvider,
     * 并传递必要的配置参数以支持后续模型列表获取功能.
     *
     * @param project         项目上下文对象, 用于日志输出和控制台打印
     * @param config          提供者配置信息, 包含基础设置和认证信息
     * @param modelParameters 模型参数配置, 用于指定模型相关参数
     * @param runtimeSettings 运行时设置, 包括超时等配置
     */
    public ModelScopeAnthropicProvider(@NotNull Project project,
                                       @NotNull AIProviderConfig config,
                                       @NotNull AIModelParameters modelParameters,
                                       @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}

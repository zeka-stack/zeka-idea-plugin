package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic;

import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * 智谱 AI 模型提供者实现类
 * <p> 继承自 AnthropicLikeProvider, 实现 ZhipudModelListProvider 接口, 用于查询和检索智谱 AI 平台支持的模型列表.
 * 该类专注于模型可用性查询, 不负责实际的请求处理, 仅作为基础设施层的适配器, 避免将基础设施关注点侵入业务逻辑.
 * 适用于需要集成智谱 AI 模型服务的系统模块, 通过注入项目, 配置, 模型参数和运行时设置完成初始化.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.18
 * @since 1.0.0
 */
public class ZhipuAnthropicProvider extends AnthropicLikeProvider {

    /**
     * 初始化智谱 AI Anthropic 兼容提供者实例
     * <p> 调用父类构造函数, 传入项目, 配置, 模型参数和运行时设置
     *
     * @param project         项目对象, 非空
     * @param config          配置对象, 非空
     * @param modelParameters 模型参数对象, 非空
     * @param runtimeSettings 运行时设置对象, 非空
     */
    public ZhipuAnthropicProvider(@NotNull Project project,
                                  @NotNull AIProviderConfig config,
                                  @NotNull AIModelParameters modelParameters,
                                  @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

}

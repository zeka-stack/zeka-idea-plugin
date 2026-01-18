package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Cloudflare OpenAI 提供者类
 * <p> 继承自 OpenAILikeProvider, 用于封装与 Cloudflare 平台集成的 OpenAI 兼容服务逻辑. 该类专注于提供与 Cloudflare API 交互的适配能力, 不负责具体的 HTTP 请求处理,
 * 而是作为业务层与基础设施层之间的桥梁, 确保服务调用符合 OpenAI 协议规范.</p>
 * <p> 设计目标: 避免基础设施关注, 采用面向对象设计原则, 将请求处理逻辑委托给父类或外部组件, 本类仅负责配置注入与上下文初始化.</p>
 * <p> 适用场景: 在需要对接 Cloudflare OpenAI 兼容服务的项目中, 作为统一入口提供标准化的 AI 服务接入能力.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.18
 * @since 1.0.0
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

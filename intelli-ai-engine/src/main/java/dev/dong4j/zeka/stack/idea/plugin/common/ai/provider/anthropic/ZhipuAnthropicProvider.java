package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.model.ZhipudModelListProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

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
public class ZhipuAnthropicProvider extends AnthropicLikeProvider implements ZhipudModelListProvider {

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

    /**
     * 获取可用模型列表
     * <p> 调用父类接口获取当前提供商支持的模型列表, 支持传入 API 密钥进行动态获取
     *
     * @param apiKey API 密钥, 可为空
     * @return 可用模型名称列表, 非空
     * @see ZhipudModelListProvider#getAvailableModels(String)
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        return ZhipudModelListProvider.super.getAvailableModels(apiKey);
    }

    /**
     * 获取当前实例所属的项目对象
     * <p> 返回该提供者实例所关联的 IntelliJ IDEA 项目对象, 用于访问项目级配置, 文件系统等资源
     *
     * @return 项目对象, 非空
     */
    @Override
    @NotNull
    public Project getProject() {
        return project;
    }

    /**
     * 获取模型列表提供器的名称
     * <p> 返回当前提供器的名称, 固定为“智谱 AI”
     *
     * @return 模型列表提供器的名称, 值为 "智谱 AI"
     */
    @Override
    @NotNull
    public String getModelListProviderName() {
        return "智谱AI";
    }
}

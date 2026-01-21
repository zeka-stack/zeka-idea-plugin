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
 * Z.AI Anthropic 兼容提供商实现类
 */
public class ZaiAnthropicProvider extends AnthropicLikeProvider implements ZhipudModelListProvider {

    /**
     * 初始化 Z.AI Anthropic 兼容提供者实例
     * <p> 调用父类构造函数, 传入项目, 配置, 模型参数和运行时设置
     *
     * @param project         项目实例, 非空
     * @param config          配置信息, 非空
     * @param modelParameters 模型参数, 非空
     * @param runtimeSettings 运行时设置, 非空
     */
    public ZaiAnthropicProvider(@NotNull Project project,
                                @NotNull AIProviderConfig config,
                                @NotNull AIModelParameters modelParameters,
                                @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 获取可用模型列表
     * <p> 调用父类 {@link ZhipudModelListProvider} 的 {@code getAvailableModels} 方法, 根据传入的 API 密钥获取当前提供商支持的模型列表
     *
     * @param apiKey API 密钥, 可为空
     * @return 可用模型列表, 始终返回非空列表
     * @see ZhipudModelListProvider#getAvailableModels(String)
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        return ZhipudModelListProvider.super.getAvailableModels(apiKey);
    }

    /**
     * 获取当前项目实例
     * <p> 返回该提供者关联的项目对象, 用于访问项目级配置和资源
     *
     * @return 项目实例, 非空
     */
    @Override
    @NotNull
    public Project getProject() {
        return project;
    }

    /**
     * 获取模型列表提供者名称
     * <p> 返回当前提供者名称为 "Z.AI"
     *
     * @return 模型列表提供者名称, 固定返回 "Z.AI"
     */
    @Override
    @NotNull
    public String getModelListProviderName() {
        return "Z.AI";
    }
}

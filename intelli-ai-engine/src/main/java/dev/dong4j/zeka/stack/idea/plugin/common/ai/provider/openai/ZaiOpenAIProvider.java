package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.ZhipudModelListProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Z.AI OpenAI 兼容提供商实现类
 */
public class ZaiOpenAIProvider extends OpenAILikeProvider implements ZhipudModelListProvider {

    /**
     * 初始化 Z.AI OpenAI 兼容提供者实例
     * <p> 调用父类构造函数, 传入项目, 配置, 模型参数和运行时设置
     *
     * @param project         项目上下文, 不能为空
     * @param config          提供者配置信息, 不能为空
     * @param modelParameters 模型参数配置, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     */
    public ZaiOpenAIProvider(@NotNull Project project,
                             @NotNull AIProviderConfig config,
                             @NotNull AIModelParameters modelParameters,
                             @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 获取可用模型列表
     * <p> 调用父类 ZhipudModelListProvider 的 getAvailableModels 方法, 根据传入的 API 密钥获取支持的模型列表
     *
     * @param apiKey API 密钥, 可为空
     * @return 可用模型列表, 始终返回非空列表
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
     * @return 非空的 Project 对象, 表示当前实例所属的项目上下文
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

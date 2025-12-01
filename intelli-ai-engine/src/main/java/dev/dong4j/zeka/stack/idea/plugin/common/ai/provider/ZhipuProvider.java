package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * 智谱 AI 服务提供商实现类
 * <p>
 * 该类继承自 AICompatibleProvider, 专门用于集成智谱 AI 服务, 提供智谱 AI 模型的管理和调用功能.
 * 实现了获取可用模型列表的功能, 支持多种智谱 AI 模型, 包括 glm-4 系列的不同版本和变体.
 * 该类负责与智谱 AI API 进行交互, 处理模型列表的获取和日志记录等操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ZhipuProvider extends AICompatibleProvider {

    /**
     * 构造一个 {@code ZhipuProvider} 实例.
     * <p>
     * 该构造函数使用指定的 {@link AIProviderConfig},{@link AIModelParameters},{@link AIRuntimeSettings} 以及可选的 {@link AIConsoleLogger} 初始化 {@code
     * ZhipuProvider}, 并将 {@code false} 作为最后一个参数传递给父类构造函数.
     *
     * @param config          提供者配置, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     */
    public ZhipuProvider(@NotNull Project project,
                         @NotNull AIProviderConfig config,
                         @NotNull AIModelParameters modelParameters,
                         @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 获取可用的模型列表
     * <p>
     * 智谱AI 返回固定的模型列表, 包括所有支持的 GLM 模型.
     * <a href="https://docs.bigmodel.cn/api-reference/%E6%A8%A1%E5%9E%8B-api/%E5%AF%B9%E8%AF%9D%E8%A1%A5%E5%85%A8#%E6%96%87%E6%9C%AC%E6%A8%A1%E5%9E%8B">...</a>
     *
     * @param apiKey API Key, 可以为 null 或空字符串
     * @return 可用的模型列表
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 智谱AI 获取模型列表 ===");
        AIConsoleLoggerUtil.print(project, "返回固定模型列表");

        // 返回固定的模型列表
        List<String> models = new ArrayList<>();
        models.add("glm-4.6");
        models.add("glm-4.5");
        models.add("glm-4.5-air");
        models.add("glm-4.5-x");
        models.add("glm-4.5-airx");
        models.add("glm-4.5-flash");
        models.add("glm-4-plus");
        models.add("glm-4-air-250414");
        models.add("glm-4-airx");
        models.add("glm-4-flashx");
        models.add("glm-4-flashx-250414");

        AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
        models.forEach(model -> AIConsoleLoggerUtil.print(project, "  - " + model));
        return models;
    }
}


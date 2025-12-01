package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * LMStudio AI 提供商实现类
 * <p>
 * 该类继承自 AICompatibleProvider, 专门用于集成 LMStudio AI 模型服务,
 * 提供与 LMStudio 兼容的 AI 模型调用功能, 支持模型参数配置和运行时设置
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class LMStudioProvider extends AICompatibleProvider {

    /**
     * 构造一个 LMStudioProvider 实例
     * <p>
     * 使用指定的配置, 模型参数, 运行时设置和控制台日志记录器初始化 LMStudio 提供者.
     *
     * @param config          配置信息, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     */
    public LMStudioProvider(@NotNull Project project,
                            @NotNull AIProviderConfig config,
                            @NotNull AIModelParameters modelParameters,
                            @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

}

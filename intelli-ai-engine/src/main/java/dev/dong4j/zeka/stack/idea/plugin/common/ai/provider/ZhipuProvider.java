package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * ZhipuProvider
 * <p>
 * 该类实现了对智谱AI (ChatGLM) AI 模型的兼容性支持, 继承自 {@link AICompatibleProvider}.
 * 通过多种构造函数, 用户可以灵活配置 AIProviderConfig,AIModelParameters,AIRuntimeSettings
 * 以及可选的 AIConsoleLogger 与性能模式开关, 以满足不同场景下的日志记录与性能调优需求.
 * <p>
 * 主要职责:
 * <ul>
 *   <li> 封装智谱AI模型的调用细节, 提供统一的接口给业务层使用.</li>
 *   <li> 支持自定义日志输出, 方便调试与监控.</li>
 *   <li> 可开启性能模式, 优化模型调用的延迟与吞吐量.</li>
 * </ul>
 * <p>
 * 使用场景:
 * <ul>
 *   <li> 需要在项目中集成智谱AI服务时, 直接使用本类实例化并调用.</li>
 *   <li> 在需要对日志进行自定义处理或开启性能优化时, 选择相应的构造函数.</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.01.XX
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
     * @param consoleLogger   控制台日志记录器, 可为空
     */
    public ZhipuProvider(@NotNull AIProviderConfig config,
                         @NotNull AIModelParameters modelParameters,
                         @NotNull AIRuntimeSettings runtimeSettings,
                         @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger);
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
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printWithTimestamp("=== 智谱AI 获取模型列表 ===");
            consoleLogger.print("返回固定模型列表");
        }

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

        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printSuccess("成功获取 " + models.size() + " 个模型");
            models.forEach(model -> consoleLogger.print("  - " + model));
        }

        return models;
    }
}


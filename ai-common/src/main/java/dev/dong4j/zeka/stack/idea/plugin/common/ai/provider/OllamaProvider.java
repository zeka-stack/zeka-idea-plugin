package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Ollama 服务提供商。
 */
public class OllamaProvider extends AICompatibleProvider {

    /**
     * 构造函数, 用于初始化 OllamaProvider 实例
     * <p>
     * 通过传入的配置, 模型参数和运行时设置来初始化 OllamaProvider 对象
     *
     * @param config          AI 提供商的配置信息
     * @param modelParameters 模型相关参数
     * @param runtimeSettings 运行时设置信息
     */
    public OllamaProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }

    /**
     * 构造函数, 用于初始化 OllamaProvider 实例
     * <p>
     * 通过传入的配置, 模型参数, 运行时设置和控制台日志记录器来初始化 OllamaProvider.
     *
     * @param config          AI 提供商的配置信息
     * @param modelParameters 模型相关参数
     * @param runtimeSettings 运行时设置
     * @param consoleLogger   可选的控制台日志记录器
     */
    public OllamaProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings,
                          @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    /**
     * 构造一个 OllamaProvider 实例
     * <p>
     * 初始化 OllamaProvider, 传入配置信息, 模型参数, 运行时设置, 控制台日志记录器以及性能模式标志.
     *
     * @param config          AI 提供商配置信息, 不能为空
     * @param modelParameters 模型参数, 不能为空
     * @param runtimeSettings 运行时设置, 不能为空
     * @param consoleLogger   控制台日志记录器, 可为空
     * @param performanceMode 是否启用性能模式, 影响运行时行为
     */
    public OllamaProvider(@NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings,
                          @Nullable AIConsoleLogger consoleLogger,
                          boolean performanceMode) {
        super(config, modelParameters, runtimeSettings, consoleLogger, performanceMode);
    }

    /**
     * 获取可用的模型列表
     * <p>
     * 首先从父类获取可用模型列表, 如果列表为空, 则返回指定 AI 提供商支持的模型列表.
     *
     * @param apiKey 用于认证的 API 密钥
     * @return 可用的模型名称列表
     */
    @NotNull
    @Override
    public List<String> getAvailableModels(String apiKey) {
        List<String> models = super.getAvailableModels(apiKey);
        if (models.isEmpty()) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
        return models;
    }

    /**
     * 解析模型响应内容, 提取模型名称列表
     * <p>
     * 从给定的 JSON 响应体中解析出模型名称, 并返回一个字符串列表. 如果响应中包含 "models" 字段且为数组类型, 则遍历该数组提取每个模型的 "name" 或 "model" 字段值. 若解析失败或结果为空, 则返回默认支持的模型列表.
     *
     * @param responseBody 需要解析的 JSON 响应体字符串
     * @return 解析得到的模型名称列表, 若解析失败或为空则返回默认模型列表
     * @throws Exception 如果解析过程中发生异常, 则捕获并返回默认模型列表
     */
    @SuppressWarnings("D")
    @Override
    protected List<String> parseModelsResponse(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("models") && json.get("models").isJsonArray()) {
                JsonArray modelsArray = json.getAsJsonArray("models");
                for (JsonElement element : modelsArray) {
                    JsonObject modelObj = element.getAsJsonObject();
                    String modelName = null;
                    if (modelObj.has("name")) {
                        modelName = modelObj.get("name").getAsString();
                    } else if (modelObj.has("model")) {
                        modelName = modelObj.get("model").getAsString();
                    }
                    if (modelName != null && !modelName.trim().isEmpty()) {
                        models.add(modelName.trim());
                    }
                }
            } else {
                models = super.parseModelsResponse(responseBody);
            }
        } catch (Exception ignored) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
        if (models.isEmpty()) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
        return models;
    }
}

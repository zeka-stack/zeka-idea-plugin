/*
 * OrcaRouter OpenAI 兼容接入模块：复用公共聊天协议，仅收敛服务商特有的模型目录语义。
 */
package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * OrcaRouter OpenAI 兼容提供商
 * <p>
 * 聊天请求沿用 {@link OpenAILikeProvider}，模型目录则按 OrcaRouter 返回的 endpoint 与输出模态过滤，
 * 避免把仅支持 Anthropic 协议或纯图片输出的模型展示在当前 OpenAI Chat 配置中。
 * 对缺少能力元数据的条目保持兼容，防止服务端分批补字段时误删仍可用的模型。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://docs.orcarouter.ai/getting-started/models">OrcaRouter Models</a>
 */
public final class OrcaRouterProvider extends OpenAILikeProvider {

    /**
     * 初始化 OrcaRouter 提供者
     *
     * @param project         项目上下文
     * @param config          提供者配置
     * @param modelParameters 模型参数
     * @param runtimeSettings 运行时设置
     */
    public OrcaRouterProvider(@NotNull Project project,
                              @NotNull AIProviderConfig config,
                              @NotNull AIModelParameters modelParameters,
                              @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 解析 OrcaRouter 模型目录，只返回当前 OpenAI Chat 入口可使用的文本模型
     *
     * @param responseBody OrcaRouter 模型目录响应
     * @return 按服务端顺序排列的模型 ID
     */
    @Override
    protected List<String> parseModelsResponse(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonArray()) {
                return models;
            }

            for (JsonElement element : root.getAsJsonArray("data")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject model = element.getAsJsonObject();
                if (!supportsOpenAIEndpoint(model) || !supportsTextOutput(model)) {
                    continue;
                }
                if (model.has("id") && model.get("id").isJsonPrimitive()) {
                    String modelId = model.get("id").getAsString().trim();
                    if (!modelId.isEmpty()) {
                        models.add(modelId);
                    }
                }
            }
        } catch (Exception ignored) {
            // 与公共解析器保持一致：目录格式异常时返回空列表，由调用方记录获取失败结果。
        }
        return models;
    }

    /**
     * 判断模型是否支持 OrcaRouter 的 OpenAI 兼容 endpoint
     * <p>
     * 字段缺失时保守放行，因为早期或第三方模型记录可能尚未补齐能力元数据。
     */
    private static boolean supportsOpenAIEndpoint(@NotNull JsonObject model) {
        if (!model.has("supported_endpoint_types") || !model.get("supported_endpoint_types").isJsonArray()) {
            return true;
        }
        return containsIgnoreCase(model.getAsJsonArray("supported_endpoint_types"), "openai");
    }

    /**
     * 判断模型是否能够输出文本
     * <p>
     * 仅当服务端明确声明输出模态且不包含 text 时排除，未知元数据继续保留。
     */
    private static boolean supportsTextOutput(@NotNull JsonObject model) {
        if (!model.has("architecture") || !model.get("architecture").isJsonObject()) {
            return true;
        }
        JsonObject architecture = model.getAsJsonObject("architecture");
        if (!architecture.has("output_modalities") || !architecture.get("output_modalities").isJsonArray()) {
            return true;
        }
        return containsIgnoreCase(architecture.getAsJsonArray("output_modalities"), "text");
    }

    /**
     * 在 JSON 字符串数组中执行不区分大小写的精确匹配
     */
    private static boolean containsIgnoreCase(@NotNull JsonArray values, @NotNull String expected) {
        for (JsonElement value : values) {
            if (value.isJsonPrimitive() && expected.equalsIgnoreCase(value.getAsString())) {
                return true;
            }
        }
        return false;
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;

/**
 * 共享模型列表获取能力
 * <p>
 * 用于封装多个提供商共用的模型列表获取逻辑.
 */
public interface ZhipudModelListProvider {

    /** 模型列表 API 基础 URL */
    String MODEL_API_BASE_URL = SiteContents.MODEL_API_BASE_URL;
    // String MODEL_API_BASE_URL = "http://localhost:8080/api/plugin/v1/models";

    /** 请求超时时间（毫秒） */
    int REQUEST_TIMEOUT_MS = 5000;

    /**
     * 获取当前项目
     *
     * @return 项目实例
     */
    @NotNull
    Project getProject();

    /**
     * 获取模型列表所属的服务商名称
     *
     * @return 服务商名称
     */
    @NotNull
    String getModelListProviderName();

    /**
     * 获取可用模型列表
     * <p>
     * 优先从 API 接口获取模型列表，如果请求失败则回退到固定列表。
     *
     * @param apiKey API Key（可选，用于后续扩展）
     * @return 可用模型列表
     */
    @NotNull
    default List<String> getAvailableModels(@Nullable String apiKey) {
        Project project = getProject();
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== " + getModelListProviderName() + " 获取模型列表 ===");

        // 尝试从 API 获取模型列表
        List<String> models = fetchModelsFromApi(project, apiKey);

        // 如果 API 请求失败，使用固定列表作为后备
        if (models == null || models.isEmpty()) {
            AIConsoleLoggerUtil.print(project, "API 请求失败，使用固定模型列表");
            models = getDefaultModels();
        } else {
            AIConsoleLoggerUtil.printSuccess(project, "成功从 API 获取 " + models.size() + " 个模型");
        }

        models.forEach(model -> AIConsoleLoggerUtil.print(project, "  - " + model));
        return models;
    }

    /**
     * 从 API 获取模型列表
     *
     * @param project 项目实例
     * @param apiKey  API Key（可选，用于后续扩展）
     * @return 模型列表，如果请求失败返回 null
     */
    @Nullable
    default List<String> fetchModelsFromApi(@NotNull Project project, @Nullable String apiKey) {
        try {
            String url = MODEL_API_BASE_URL + "/zhipu";
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                url += "?apiKey=" + java.net.URLEncoder.encode(apiKey, java.nio.charset.StandardCharsets.UTF_8);
            }

            AIConsoleLoggerUtil.print(project, "请求 API: " + url);

            String responseBody = HttpRequests.request(url)
                .productNameAsUserAgent()
                .readTimeout(REQUEST_TIMEOUT_MS)
                .readString();

            // 解析响应
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

            // 检查外层 success
            if (!responseJson.has("success") || !responseJson.get("success").getAsBoolean()) {
                return null;
            }

            // 获取 data 对象
            if (!responseJson.has("data") || !responseJson.get("data").isJsonObject()) {
                return null;
            }

            JsonObject data = responseJson.getAsJsonObject("data");

            // 提取 models 列表
            if (!data.has("models") || !data.get("models").isJsonArray()) {
                return null;
            }

            JsonArray modelsArray = data.getAsJsonArray("models");
            if (modelsArray.isEmpty()) {
                return null;
            }

            List<String> models = new ArrayList<>();
            for (JsonElement element : modelsArray) {
                if (element.isJsonPrimitive()) {
                    String model = element.getAsString();
                    if (model != null && !model.trim().isEmpty()) {
                        models.add(model.trim());
                    }
                }
            }

            return models.isEmpty() ? null : models;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取默认的固定模型列表
     *
     * @return 默认模型列表
     */
    @NotNull
    default List<String> getDefaultModels() {
        List<String> models = new ArrayList<>();
        models.add("glm-4.7");
        return models;
    }
}

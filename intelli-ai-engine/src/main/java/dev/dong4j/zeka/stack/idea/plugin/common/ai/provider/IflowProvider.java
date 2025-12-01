package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * Iflow AI 服务提供商实现类
 * <p>
 * 该类继承自 AICompatibleProvider, 专门用于与 Iflow AI 服务进行交互,
 * 提供模型列表获取, 模型响应解析等核心功能. 支持通过 HTTP 请求获取可用的 AI 模型列表,
 * 并能够解析 Iflow API 返回的模型数据格式. 该类具备完善的错误处理机制和日志记录功能,
 * 确保在各种网络环境下都能稳定运行, 并在出现问题时返回默认的模型列表作为备选方案.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class IflowProvider extends AICompatibleProvider {
    private static final Logger LOG = Logger.getInstance(IflowProvider.class);
    /** IFlow 模型列表接口地址 */
    private static final String MODELS_LIST_URL = "https://iflow.cn/api/platform/models/list";

    /**
     * 构造函数, 用于初始化 IflowProvider 实例
     * <p>
     * 通过传入的配置, 模型参数, 运行时设置和控制台日志记录器来初始化 IflowProvider.
     *
     * @param config          AI 提供商的配置信息
     * @param modelParameters 模型相关参数
     * @param runtimeSettings 运行时设置
     */
    public IflowProvider(@NotNull Project project,
                         @NotNull AIProviderConfig config,
                         @NotNull AIModelParameters modelParameters,
                         @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 获取可用的模型列表
     * <p>
     * 使用 POST 请求调用 IFlow 模型列表接口, 从响应中提取所有厂商分类下的 modelName.
     * 如果请求失败或列表为空, 则返回默认支持的模型列表.
     *
     * @param apiKey 用于认证的 API 密钥
     * @return 可用的模型名称列表
     */
    @NotNull
    @Override
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== IFlow 获取模型列表 ===");
        AIConsoleLoggerUtil.print(project, "接口地址: " + MODELS_LIST_URL);

        try {
            // 构建请求体（空对象）
            String requestBody = "{}";
            byte[] requestBodyBytes = requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;

            String responseBody = HttpRequests.post(MODELS_LIST_URL, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    conn.setConnectTimeout(runtimeSettings.getTimeoutInMillis());
                    conn.setReadTimeout(runtimeSettings.getTimeoutInMillis() * 2);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(request -> {
                    request.write(requestBody);
                    return request.readString();
                });

            if (!responseBody.trim().isEmpty()) {
                List<String> models = parseModelsResponse(responseBody);
                AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
                if (!models.isEmpty() && models.size() <= 10) {
                    models.forEach(model -> AIConsoleLoggerUtil.print(project, "  - " + model));
                }
                // 如果获取的模型列表为空，返回默认模型列表
                if (models.isEmpty()) {
                    return new ArrayList<>(AIProviderType.IFLOW.getSupportedModels());
                }
                return models;
            }

            AIConsoleLoggerUtil.printWarning(project, "服务返回空响应，返回默认模型列表");
            return new ArrayList<>(AIProviderType.IFLOW.getSupportedModels());
        } catch (IOException e) {
            LOG.info("IFlow 获取模型列表网络错误", e);
            AIConsoleLoggerUtil.printError(project, "网络错误: " + e.getMessage());
            return new ArrayList<>(AIProviderType.IFLOW.getSupportedModels());
        } catch (Exception e) {
            LOG.info("IFlow 获取模型列表失败", e);
            AIConsoleLoggerUtil.printError(project, "获取模型列表失败: " + e.getMessage());

            return new ArrayList<>(AIProviderType.IFLOW.getSupportedModels());
        }
    }

    /**
     * 解析模型响应内容, 提取模型名称列表
     * <p>
     * 从给定的 JSON 响应体中解析出模型名称. IFlow 的响应格式中, data 对象包含多个厂商分类,
     * 每个分类下是一个数组, 数组中包含模型对象, 每个模型对象包含 modelName 字段.
     * 该方法会遍历所有厂商分类, 提取所有 modelName 字段.
     *
     * @param responseBody 需要解析的 JSON 响应体字符串
     * @return 解析得到的模型名称列表, 若解析失败或为空则返回空列表
     */
    @SuppressWarnings("D")
    @Override
    protected List<String> parseModelsResponse(String responseBody) {
        Set<String> models = new LinkedHashSet<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // 检查响应是否成功
            if (json.has("success") && !json.get("success").getAsBoolean()) {
                String errorMessage = json.has("message") ? json.get("message").getAsString() : "未知错误";
                LOG.info("IFlow API 返回失败: " + errorMessage);
                return new ArrayList<>();
            }

            // 获取 data 对象
            if (!json.has("data") || !json.get("data").isJsonObject()) {
                LOG.info("IFlow 响应中没有 data 对象");
                return new ArrayList<>();
            }

            JsonObject dataObj = json.getAsJsonObject("data");

            // 遍历 data 对象中的所有厂商分类
            for (String vendorKey : dataObj.keySet()) {
                JsonElement vendorElement = dataObj.get(vendorKey);
                if (vendorElement.isJsonArray()) {
                    JsonArray vendorArray = vendorElement.getAsJsonArray();
                    // 遍历该厂商下的所有模型
                    for (JsonElement modelElement : vendorArray) {
                        if (modelElement.isJsonObject()) {
                            JsonObject modelObj = modelElement.getAsJsonObject();
                            // 提取 modelName 字段
                            if (modelObj.has("modelName")) {
                                String modelName = modelObj.get("modelName").getAsString();
                                if (modelName != null && !modelName.trim().isEmpty()) {
                                    models.add(modelName.trim());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("IFlow 解析模型响应失败", e);
        }
        return new ArrayList<>(models);
    }
}

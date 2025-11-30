package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * ModelScope 提供商实现类
 * <p>
 * 该类继承自 AICompatibleProvider, 专门用于与 ModelScope 平台进行集成,
 * 提供模型列表获取功能. 通过并发请求多个页面数据来获取可用的 AI 模型列表,
 * 支持分页查询和结果合并, 确保能够获取到完整的模型信息.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ModelScopeProvider extends AICompatibleProvider {
    private static final Logger LOG = Logger.getInstance(ModelScopeProvider.class);
    /** ModelScope 模型列表接口地址（固定） */
    private static final String MODELS_LIST_URL = "https://modelscope.cn/api/v1/dolphin/models";
    /** 最大页数，每页最多 30 条 */
    private static final int MAX_PAGES = 3;

    /**
     * 构造函数
     *
     * @param config          提供者配置
     * @param modelParameters 模型参数
     * @param runtimeSettings 运行时配置
     * @param consoleLogger   日志记录器
     */
    public ModelScopeProvider(@NotNull AIProviderConfig config,
                              @NotNull AIModelParameters modelParameters,
                              @NotNull AIRuntimeSettings runtimeSettings,
                              @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger);
    }

    /**
     * 获取 ModelScope 可用模型列表, 不校验 API Key.
     * <p>
     * 并发发送 5 个请求（PageNumber 1-5），每页最多 50 条数据，合并所有结果后返回.
     *
     * @param apiKey API Key, 可为 null
     * @return 模型 id 列表
     */
    @SuppressWarnings("D")
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printWithTimestamp("=== ModelScope 获取模型列表 ===");
            consoleLogger.print("接口地址: " + MODELS_LIST_URL);
            consoleLogger.print("并发请求 " + MAX_PAGES + " 页数据");
        }

        ExecutorService executor = Executors.newFixedThreadPool(MAX_PAGES);
        try {
            // 创建 5 个并发任务，分别获取 1-5 页数据
            List<CompletableFuture<List<String>>> futures = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= MAX_PAGES; pageNumber++) {
                final int page = pageNumber;
                CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return fetchModelsPage(page, apiKey);
                    } catch (Exception e) {
                        LOG.info("ModelScope 获取第 " + page + " 页失败", e);
                        if (consoleLogger != null && runtimeSettings.verboseLogging) {
                            consoleLogger.printWarning("第 " + page + " 页请求失败: " + e.getMessage());
                        }
                        return new ArrayList<String>();
                    }
                }, executor);
                futures.add(future);
            }

            // 等待所有请求完成并合并结果
            Set<String> allModels = new LinkedHashSet<>();
            for (CompletableFuture<List<String>> future : futures) {
                try {
                    List<String> pageModels = future.get();
                    allModels.addAll(pageModels);
                } catch (Exception e) {
                    LOG.info("ModelScope 合并结果失败", e);
                    if (consoleLogger != null && runtimeSettings.verboseLogging) {
                        consoleLogger.printWarning("合并结果时出错: " + e.getMessage());
                    }
                }
            }

            List<String> result = new ArrayList<>(allModels);
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printSuccess("ModelScope 共获取 " + result.size() + " 个模型");
            }
            return result;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 获取指定页的模型列表
     *
     * @param pageNumber 页码（1-5）
     * @param apiKey     API Key, 可为 null
     * @return 该页的模型 id 列表
     */
    @NotNull
    private List<String> fetchModelsPage(int pageNumber, @Nullable String apiKey) throws IOException {
        String requestBody = buildRequestBody(pageNumber);
        // 计算请求体长度，用于禁用分块传输编码
        byte[] requestBodyBytes = requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final int contentLength = requestBodyBytes.length;
        
        String response = HttpRequests.post(MODELS_LIST_URL, "application/json")
            .tuner(connection -> {
                HttpURLConnection conn = (HttpURLConnection) connection;
                conn.setRequestMethod("PUT");
                conn.setConnectTimeout(runtimeSettings.getTimeoutInMillis());
                conn.setReadTimeout(runtimeSettings.getTimeoutInMillis() * 2);
                conn.setRequestProperty("Content-Type", "application/json");
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                }
                // 在连接建立之前设置固定长度流模式，禁用分块传输编码
                // 这样在身份验证失败时可以重试
                conn.setFixedLengthStreamingMode(contentLength);
                conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
            })
            .connect(request -> {
                request.write(requestBody);
                return request.readString();
            });

        return parseModelScopeResponse(response);
    }

    /**
     * 构建请求体 JSON 字符串
     *
     * @param pageNumber 页码（1-5）
     * @return JSON 字符串
     */
    @NotNull
    private static String buildRequestBody(int pageNumber) {
        JsonObject root = new JsonObject();
        root.addProperty("PageSize", 50); // 每页最多 50 条
        root.addProperty("PageNumber", pageNumber);
        root.addProperty("SortBy", "Default");
        root.addProperty("Target", "");

        JsonArray criterion = new JsonArray();
        JsonObject tasks = new JsonObject();
        tasks.addProperty("category", "tasks");
        tasks.addProperty("predicate", "contains");
        JsonArray values = new JsonArray();
        values.add("text-generation");
        tasks.add("values", values);
        tasks.add("sub_values", new JsonArray());
        criterion.add(tasks);
        root.add("Criterion", criterion);

        JsonArray singleCriterion = new JsonArray();
        JsonObject inferenceType = new JsonObject();
        inferenceType.addProperty("category", "inference_type");
        inferenceType.addProperty("DateType", "int");
        inferenceType.addProperty("predicate", "equal");
        inferenceType.addProperty("IntValue", 1);
        singleCriterion.add(inferenceType);
        root.add("SingleCriterion", singleCriterion);

        return root.toString();
    }

    /**
     * 解析 ModelScope 响应
     *
     * @param responseBody 响应体
     * @return 模型 id 列表
     */
    @NotNull
    private List<String> parseModelScopeResponse(@NotNull String responseBody) {
        Set<String> models = new LinkedHashSet<>();
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!json.has("Data")) {
            return new ArrayList<>();
        }
        JsonObject data = json.getAsJsonObject("Data");
        if (!data.has("Model")) {
            return new ArrayList<>();
        }
        JsonObject model = data.getAsJsonObject("Model");
        if (!model.has("Models")) {
            return new ArrayList<>();
        }
        JsonArray modelsArray = model.getAsJsonArray("Models");
        for (JsonElement element : modelsArray) {
            JsonObject item = element.getAsJsonObject();
            if (item.has("BackendSupport")) {
                JsonObject backend = item.getAsJsonObject("BackendSupport");
                if (backend.has("model_id")) {
                    String modelId = backend.get("model_id").getAsString();
                    if (modelId != null && !modelId.trim().isEmpty()) {
                        models.add(modelId.trim());
                    }
                }
            }
        }
        return new ArrayList<>(models);
    }
}


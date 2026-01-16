package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ModelScope Anthropic 兼容提供商实现类
 * <p>
 * 该类通过 ModelScope 的模型列表接口获取可用模型.
 */
@Slf4j
public class ModelScopeAnthropicProvider extends AnthropicLikeProvider {
    /** ModelScope 模型列表接口地址（固定） */
    private static final String MODELS_LIST_URL = "https://modelscope.cn/api/v1/dolphin/models";
    /** 最大页数，每页最多 30 条 */
    private static final int MAX_PAGES = 3;

    /**
     * 初始化 ModelScope Anthropic 兼容提供者实例
     * <p>
     * 该构造函数用于创建 ModelScope 提供者的实例, 继承自 AnthropicLikeProvider,
     * 并传递必要的配置参数以支持后续模型列表获取功能.
     *
     * @param project         项目上下文对象, 用于日志输出和控制台打印
     * @param config          提供者配置信息, 包含基础设置和认证信息
     * @param modelParameters 模型参数配置, 用于指定模型相关参数
     * @param runtimeSettings 运行时设置, 包括超时等配置
     */
    public ModelScopeAnthropicProvider(@NotNull Project project,
                                       @NotNull AIProviderConfig config,
                                       @NotNull AIModelParameters modelParameters,
                                       @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 获取 ModelScope 平台支持的可用模型列表
     * <p> 通过并发请求最多 <pre>{@code MAX_PAGES}</pre> 页数据, 每页最多 30 条模型, 合并去重后返回所有可用模型名称.
     * <p> 请求过程中会打印日志, 包括接口地址, 并发页数, 每页请求状态及最终模型总数.
     *
     * @param apiKey 可选的 API 密钥, 用于认证请求. 若为空或无效, 则使用匿名访问.
     * @return 包含所有可用模型名称的非空列表, 若无模型或请求失败则返回空列表.
     * @since 1.0.0
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== ModelScope 获取模型列表 ===");
        AIConsoleLoggerUtil.print(project, "接口地址: " + MODELS_LIST_URL);
        AIConsoleLoggerUtil.print(project, "并发请求 " + MAX_PAGES + " 页数据");

        ExecutorService executor = Executors.newFixedThreadPool(MAX_PAGES);
        try {
            List<CompletableFuture<List<String>>> futures = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= MAX_PAGES; pageNumber++) {
                final int page = pageNumber;
                CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return fetchModelsPage(page, apiKey);
                    } catch (Exception e) {
                        log.debug("ModelScope 获取第 " + page + " 页失败", e);
                        AIConsoleLoggerUtil.printWarning(project, "第 " + page + " 页请求失败: " + e.getMessage());
                        return new ArrayList<>();
                    }
                }, executor);
                futures.add(future);
            }

            Set<String> allModels = new LinkedHashSet<>();
            for (CompletableFuture<List<String>> future : futures) {
                try {
                    List<String> pageModels = future.get();
                    allModels.addAll(pageModels);
                } catch (Exception e) {
                    log.debug("ModelScope 合并结果失败", e);
                    AIConsoleLoggerUtil.printWarning(project, "合并结果时出错: " + e.getMessage());
                }
            }

            List<String> result = new ArrayList<>(allModels);
            AIConsoleLoggerUtil.printSuccess(project, "ModelScope 共获取 " + result.size() + " 个模型");
            return result;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 获取指定页码的模型列表
     * <p>通过 ModelScope 模型列表接口, 按页码和每页大小 (30) 请求模型数据, 并解析返回结果.
     * 支持传入 API 密钥以进行身份验证.
     *
     * @param pageNumber 页码, 从 1 开始
     * @param apiKey     可选的 API 密钥, 用于身份验证, 若为空则不携带授权头
     * @return 解析后的模型名称列表, 若请求失败或数据格式异常则返回空列表
     * @throws IOException 当网络请求或响应读取失败时抛出
     */
    @NotNull
    private List<String> fetchModelsPage(int pageNumber, @Nullable String apiKey) throws IOException {
        String url = MODELS_LIST_URL + "?PageNumber=" + pageNumber + "&PageSize=30";
        String responseBody = HttpRequests.request(url)
            .tuner(connection -> tuneModelScopeConnection((HttpURLConnection) connection, apiKey))
            .connect(HttpRequests.Request::readString);

        return parseModelsFromResponse(responseBody);
    }

    /**
     * 配置 ModelScope API 请求连接的超时和认证信息
     * <p> 该方法用于设置 HTTP 连接的超时时间及授权头, 适用于 ModelScope 模型服务接口调用.
     * <pre>{@code
     * connection.setConnectTimeout(runtimeSettings.getTimeoutInMillis());
     * connection.setReadTimeout(runtimeSettings.getTimeoutInMillis() * 2);
     * connection.setRequestProperty("Content-Type", "application/json");
     * if (apiKey != null && !apiKey.trim().isEmpty()) {*     connection.setRequestProperty("Authorization", "Bearer" + apiKey.trim());
     * }
     * }</pre>
     *
     * @param connection 需要配置的 HTTP 连接对象, 必须非空
     * @param apiKey     可选的访问令牌, 用于认证, 若为空或为空字符串则不设置授权头
     */
    private void tuneModelScopeConnection(@NotNull HttpURLConnection connection, @Nullable String apiKey) {
        connection.setConnectTimeout(runtimeSettings.getTimeoutInMillis());
        connection.setReadTimeout(runtimeSettings.getTimeoutInMillis() * 2);
        connection.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        }
    }

    /**
     * 解析 ModelScope 模型列表响应数据
     * <p> 从 JSON 响应体中提取模型名称列表, 过滤无效或空名称, 去重后返回
     * <pre>{@code
     * // 示例响应结构
     * {
     *   "Data": [*     {"Name": "model1"},
     *     {"Name": "model2"},
     *     {"Name": " "}
     *   ]
     * }
     * }</pre>
     * <p> 若响应结构不符合预期或解析失败, 将返回空列表
     *
     * @param responseBody 原始 JSON 响应体字符串
     * @return 解析后的模型名称列表, 无重复项, 不包含空或空白名称
     */
    @NotNull
    private List<String> parseModelsFromResponse(@NotNull String responseBody) {
        Set<String> models = new LinkedHashSet<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!json.has("Data") || !json.get("Data").isJsonArray()) {
                return new ArrayList<>();
            }
            JsonArray dataArray = json.getAsJsonArray("Data");
            for (JsonElement element : dataArray) {
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("Name")) {
                        String modelName = obj.get("Name").getAsString();
                        if (modelName != null && !modelName.trim().isEmpty()) {
                            models.add(modelName.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("ModelScope 解析模型列表失败", e);
        }
        return new ArrayList<>(models);
    }
}

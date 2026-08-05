package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.BlockingRequestExecutor;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.StreamRequestExecutor;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub 模型提供者类
 * <p>继承自 OpenAILikeProvider, 用于封装与 GitHub API 交互的模型查询与内容生成逻辑, 支持同步和流式响应. 该类不负责请求处理, 仅专注于模型列表获取, 请求构建与响应处理, 符合面向对象设计原则, 避免基础设施关注, 封装业务规则
 * .</p>
 * <p>主要功能包括:</p>
 * <ul>
 *   <li>通过指定的 API Key 查询 GitHub 提供的可用模型列表</li>
 *   <li>支持同步内容生成 (<pre>{@code generateContent}</pre>)和流式内容生成(<pre>{@code generateContentStream}</pre>)</li>
 *   <li>提供配置验证功能, 通过发送测试请求验证连接与权限</li>
 *   <li>自动处理连接超时, 请求头设置 (如认证, 版本号) 等底层细节</li>
 * </ul>
 * <p>该类内部使用 <a href="https://github.com/github/rest-api-description">GitHub REST API</a>, 接口地址为 <pre>{@code https://api.github.com}</pre>, 并使用版本 <pre>{@code 2022-11-28}</pre>.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.18
 * @since 1.0.0
 */
@Slf4j
public class GitHubModelsProvider extends OpenAILikeProvider {
    /** GitHub Models 接口地址, 用于获取可用模型列表 <a href="https://models.github.ai/catalog/models">https://models.github.ai/catalog/models</a> */
    private static final String MODELS_LIST_URL = "https://models.github.ai/catalog/models";
    /**
     * GitHub API 版本号, 用于指定请求的接口版本
     * <a href="https://docs.github.com/en/rest/overview/api-versioning">https://docs.github.com/en/rest/overview/api-versioning</a>
     */
    private static final String API_VERSION = "2022-11-28";

    /**
     * 初始化 GitHub Models 提供商实例
     * <p> 用于创建与 GitHub Models API 交互的客户端, 支持配置项目, 提供者配置, 模型参数和运行时设置
     *
     * @param project         项目上下文, 用于日志输出和配置管理
     * @param config          提供者配置, 包含 API 地址, 认证信息等
     * @param modelParameters 模型参数, 如模型名称, 温度等
     * @param runtimeSettings 运行时设置, 如超时时间, 重试策略等
     */
    public GitHubModelsProvider(@NotNull Project project,
                                @NotNull AIProviderConfig config,
                                @NotNull AIModelParameters modelParameters,
                                @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 生成内容响应
     * <p> 通过阻塞式请求执行器发送请求并获取响应内容, 支持传入 API 密钥和响应监听器
     *
     * @param request  AI 聊天请求对象, 包含对话内容和参数
     * @param apiKey   可选的 API 密钥, 用于身份验证
     * @param listener 可选的响应监听器, 用于接收响应回调
     * @return 服务端返回的响应内容字符串
     * @throws AIServiceException 当请求失败或服务端返回错误时抛出
     */
    @Override
    @NotNull
    public String generateContent(@NotNull AIChatRequest request,
                                  @Nullable String apiKey,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        BlockingRequestExecutor executor = new BlockingRequestExecutor(project, config, this::tuneConnection);
        return executor.sendRequest(buildRequestBody(request), apiKey, listener, false);
    }

    /**
     * 以流式方式生成内容
     * <p> 通过流式请求执行器发送流式请求, 并将响应数据逐块推送至监听器
     *
     * @param request  AI 聊天请求对象, 包含对话内容和参数
     * @param apiKey   可选的 API 密钥, 用于身份验证
     * @param listener 流式响应监听器, 用于接收逐块响应数据
     * @throws AIServiceException 当服务调用失败时抛出异常
     */
    @Override
    public void generateContentStream(@NotNull AIChatRequest request,
                                      @Nullable String apiKey,
                                      @NotNull AIStreamResponseListener listener) throws AIServiceException {
        StreamRequestExecutor executor = new StreamRequestExecutor(project, config, this::tuneConnection);
        executor.sendStreamRequest(buildRequestBody(request, true), apiKey, listener);
    }

    /**
     * 验证当前配置是否有效
     * <p> 通过发送一个简单的 ping-pong 请求测试与服务端的连接状态, 验证配置是否正确.
     * 若连接成功并收到非空响应, 则返回成功结果; 否则根据异常类型返回失败结果.
     *
     * @param apiKey 可选的 API 密钥, 用于认证请求. 若为空或无效, 则可能返回默认模型列表.
     * @return 配置验证结果, 包含成功或失败信息及可选异常详情
     */
    @Override
    @NotNull
    public ValidationResult validateConfiguration(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始验证配置 ===");
        AIConsoleLoggerUtil.print(project, "提供商: " + getProviderType().getDisplayName());
        AIConsoleLoggerUtil.print(project, "模型: " + getModelName());
        AIConsoleLoggerUtil.print(project, "Base URL: " + getBaseUrl());
        try {
            AIChatRequest request = new AIChatRequest("i say ping, you say pong", "ping", 0);
            BlockingRequestExecutor executor = new BlockingRequestExecutor(project, config, this::tuneConnection);
            String response = executor.sendRequest(buildRequestBody(request), apiKey, null, true);
            if (!response.isEmpty()) {
                AIConsoleLoggerUtil.printSuccess(project, "=== 配置验证成功 ===");
                return ValidationResult.success("连接成功！提供商: " + getProviderType().getDisplayName() +
                                                ", 模型: " + getModelName());
            }
            AIConsoleLoggerUtil.printError(project, "配置验证失败：服务返回空响应");
            return ValidationResult.failure("连接失败：服务返回空响应");
        } catch (AIServiceException e) {
            AIConsoleLoggerUtil.printError(project, "配置验证失败: " + e.getMessage());
            return ValidationResult.failure("配置验证失败", AIServiceException.build(e));
        } catch (Exception e) {
            String details = e.getMessage();
            if (details == null || details.isEmpty()) {
                details = e.getClass().getSimpleName();
            }
            AIConsoleLoggerUtil.printError(project, "配置验证异常: " + details);
            return ValidationResult.failure("配置验证异常", details, e);
        }
    }

    /**
     * 获取可用模型列表
     * <p> 通过调用 GitHub Models API 接口获取当前支持的模型列表. 若未提供 API Key, 则返回默认模型列表.
     * <p> 请求地址: <a href="https://models.github.ai/catalog/models">https://models.github.ai/catalog/models</a>
     * <p> 接口版本:2022-11-28
     *
     * @param apiKey 可选的 API 密钥, 用于认证访问 GitHub Models API. 若为空或无效, 则返回默认模型列表.
     * @return 可用模型名称列表. 若请求失败或响应为空, 则返回提供者默认支持的模型列表.
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== GitHub Models 获取模型列表 ===");
        AIConsoleLoggerUtil.print(project, "接口地址: " + MODELS_LIST_URL);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            AIConsoleLoggerUtil.printWarning(project, "需要 API Key 但未提供，返回默认模型列表");
            return new ArrayList<>(getProviderType().getSupportedModels());
        }
        try {
            String responseBody = HttpRequests.request(MODELS_LIST_URL)
                .tuner(connection -> tuneConnection((HttpURLConnection) connection, apiKey))
                .connect(HttpRequests.Request::readString);

            if (!responseBody.trim().isEmpty()) {
                List<String> models = parseModelsResponse(responseBody);
                AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
                if (!models.isEmpty() && models.size() <= 10) {
                    models.forEach(model -> AIConsoleLoggerUtil.print(project, "  - " + model));
                }
                if (models.isEmpty()) {
                    return new ArrayList<>(getProviderType().getSupportedModels());
                }
                return models;
            }

            AIConsoleLoggerUtil.printWarning(project, "服务返回空响应，返回默认模型列表");
            return new ArrayList<>(getProviderType().getSupportedModels());
        } catch (IOException e) {
            log.debug("GitHub Models 获取模型列表网络错误", e);
            AIConsoleLoggerUtil.printError(project, "网络错误: " + e.getMessage());
            return new ArrayList<>(getProviderType().getSupportedModels());
        } catch (Exception e) {
            log.debug("GitHub Models 获取模型列表失败", e);
            AIConsoleLoggerUtil.printError(project, "获取模型列表失败: " + e.getMessage());
            return new ArrayList<>(getProviderType().getSupportedModels());
        }
    }

    /**
     * 解析 GitHub Models API 返回的模型列表响应
     * <p> 从 JSON 响应体中提取模型 ID 并去重, 返回模型名称列表
     * <p> 如果解析失败或响应体为空, 则返回空列表
     *
     * @param responseBody 从 GitHub Models API 获取的原始 JSON 响应体
     * @return 解析出的模型 ID 列表, 去重后以 List 形式返回, 若解析失败则返回空列表
     */
    protected List<String> parseModelsResponse(String responseBody) {
        Set<String> models = new LinkedHashSet<>();
        try {
            JsonArray array = JsonParser.parseString(responseBody).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                String id = element.getAsJsonObject().get("id").getAsString();
                if (id != null && !id.trim().isEmpty()) {
                    models.add(id.trim());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse GitHub Models response", e);
        }
        return new ArrayList<>(models);
    }

    /**
     * 配置 HTTP 连接参数并设置请求头
     * <p> 该方法用于配置 HttpURLConnection 的超时时间, 请求头信息, 以及可选的认证令牌.
     * 适用于 GitHub Models API 的连接初始化.
     *
     * @param connection HTTP 连接对象, 用于设置超时和请求头
     * @param apiKey     可选的 API 密钥, 若提供则设置 Authorization 请求头
     * @since 1.0
     */
    private void tuneConnection(HttpURLConnection connection, @Nullable String apiKey) {
        int timeoutMillis = runtimeSettings.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", API_VERSION);
        if (apiKey != null && !apiKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        AIConsoleLoggerUtil.print(project, String.format("连接超时: [%ss] 读取超时: [%ss]\n", runtimeSettings.timeout,
                                                         (runtimeSettings.timeout * 2)));
    }
}

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
import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
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
 * AI 兼容性提供者抽象类
 * <p>
 * 该类实现了 AIServiceProvider 接口, 为各种 AI 服务提供商提供统一的兼容性实现.
 * 包含了 AI 服务调用的核心逻辑, 如内容生成, 配置验证, 模型列表获取等功能,
 * 并提供了重试机制, 日志记录, 请求构建等通用功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public abstract class AICompatibleProvider implements AIServiceProvider {

    /**
     * 日志记录器, 用于记录 AI 兼容提供者相关的日志信息
     */
    private static final Logger LOG = Logger.getInstance(AICompatibleProvider.class);

    /**
     * 当前操作的项目对象
     * <p>
     * 用于访问和操作与该项目相关的数据和设置
     */
    protected final Project project;
    /** AI 服务提供商配置信息 */
    protected final AIProviderConfig config;
    /**
     * 模型参数配置
     * <p>
     * 用于定义和传递 AI 模型运行时所需的各项参数
     */
    protected final AIModelParameters modelParameters;
    /** AI 运行时设置 */
    protected final AIRuntimeSettings runtimeSettings;

    /**
     * /**
     * 初始化 AI 兼容性提供者
     * <p>
     * 使用提供的配置, 模型参数, 运行时设置和控制台日志记录器初始化 AI 兼容性提供者.
     *
     * @param config          AI 提供者配置对象, 不可为 null
     * @param modelParameters 模型参数对象, 不可为 null
     * @param runtimeSettings 运行时设置对象, 不可为 null
     */
    protected AICompatibleProvider(@NotNull Project project, @NotNull AIProviderConfig config,
                                   @NotNull AIModelParameters modelParameters,
                                   @NotNull AIRuntimeSettings runtimeSettings) {
        this.project = project;
        this.config = config.copy();
        this.config.baseUrl = normalizeBaseUrl(this.config.baseUrl);
        this.modelParameters = modelParameters.copy();
        this.runtimeSettings = runtimeSettings.copy();
    }

    /**
     * 对基础 URL 进行标准化处理
     * <p>
     * 如果传入的 URL 为 null 或为空字符串, 则返回空字符串. 否则, 若 URL 以斜杠结尾, 则移除末尾的斜杠, 返回处理后的 URL.
     *
     * @param baseUrl 需要标准化的基础 URL 字符串
     * @return 标准化后的 URL 字符串, 若输入为 null 或空则返回空字符串
     */
    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 获取 AI 服务提供商类型
     * <p>
     * 返回当前配置中的 AI 服务提供商类型
     *
     * @return AI 服务提供商类型
     */
    @Override
    @NotNull
    public AIProviderType getProviderType() {
        return config.providerType;
    }

    /**
     * 获取模型名称
     * <p>
     * 返回配置中指定的模型名称
     *
     * @return 配置中的模型名称
     */
    @Override
    @NotNull
    public String getModelName() {
        return config.modelName;
    }

    /**
     * 获取基础 URL 配置值
     * <p>
     * 返回系统配置中的基础 URL 值, 用于构建请求地址等用途
     *
     * @return 基础 URL 配置值
     */
    @Override
    @NotNull
    public String getBaseUrl() {
        return config.baseUrl;
    }

    /**
     * 生成 AI 聊天内容
     * <p>
     * 根据提供的请求参数和配置, 调用 AI 服务生成内容, 并在失败时进行重试处理.
     *
     * @param request  AI 聊天请求对象, 包含生成内容所需的信息
     * @param apiKey   用于认证的 API 密钥, 可为 null
     * @param listener 响应监听器, 用于接收生成过程中的事件通知, 可为 null
     * @return 生成的 AI 响应内容
     * @throws AIServiceException 当 AI 服务调用失败且无法重试时抛出
     */
    @Override
    @NotNull
    public String generateContent(@NotNull AIChatRequest request,
                                  @Nullable String apiKey,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始生成内容 ===");
        // 模型相关信息一行输出
        AIConsoleLoggerUtil.print(project, String.format("模型信息: 供应商=%s | 模型=%s | Base URL=%s",
                                                         getProviderType().getDisplayName(), getModelName(), getBaseUrl()));
        // 当前开启的配置
        StringBuilder configInfo = new StringBuilder("当前配置: ");
        if (configInfo.length() > "当前配置: ".length()) {
            configInfo.append(" | ");
        }
        configInfo.append("详细日志✓");
        if (configInfo.length() == "当前配置: ".length()) {
            configInfo.append("默认配置");
        }
        AIConsoleLoggerUtil.print(project, configInfo.toString());

        // 使用全局的 verboseLogging 设置
        AIRuntimeSettings runtime = runtimeSettings;
        int attempts = 0;
        BlockingRequestExecutor executor = new BlockingRequestExecutor(project, config, this::tuneConnection);

        while (attempts < Math.max(1, runtime.maxRetries)) {
            try {
                String result = executor.sendRequest(buildRequestBody(request), apiKey, listener, false);
                AIConsoleLoggerUtil.printSuccess(project, "=== 内容生成成功 ===");
                AIConsoleLoggerUtil.print(project, "响应长度: " + result.length() + " 字符");
                return result;
            } catch (AIServiceException e) {
                attempts++;
                AIConsoleLoggerUtil.printWarning(project,

                                                 "请求失败 (尝试 " + attempts + "/" + Math.max(1, runtime.maxRetries) + "): " + e.getMessage());
                if (!e.isRetryable() || attempts >= Math.max(1, runtime.maxRetries)) {
                    break;
                }
                long waitTime = (long) (runtime.waitDuration * Math.pow(2, attempts - 1));
                LOG.info("AI request failed, retry in " + waitTime + "ms: " + e.getMessage());
                AIConsoleLoggerUtil.print(project, "等待 " + waitTime + "ms 后重试...");
                try {
                    TimeUnit.MILLISECONDS.sleep(waitTime);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AIServiceException("Interrupted during retry",
                                                 AIServiceException.ErrorCode.UNKNOWN_ERROR,
                                                 interruptedException);
                }
            }
        }
        AIConsoleLoggerUtil.print(project, "=== 内容生成失败 ===");
        throw new AIServiceException("AI 服务调用失败", AIServiceException.ErrorCode.UNKNOWN_ERROR);

    }

    /**
     * 验证当前配置是否可用.
     * <p>
     * 该方法会向 AI 服务发送一个简单的“ping”请求, 并根据返回结果判断配置是否正确. 若
     * 运行时开启了详细日志, 则会在控制台打印验证过程中的关键信息. 方法会捕获
     * {@link AIServiceException} 以及其他异常, 并将错误信息封装到 {@link ValidationResult}
     * 对象中返回.
     *
     * @param apiKey 可选的 API Key, 若为 {@code null} 则使用默认配置
     * @return {@link ValidationResult}, 若验证成功返回 {@link ValidationResult#success(String)},
     *     否则返回 {@link ValidationResult#failure(String)} 或 {@link ValidationResult#failure(String, Throwable)}
     */
    @Override
    @NotNull
    public ValidationResult validateConfiguration(@Nullable String apiKey) {
        // 使用全局的 verboseLogging 设置
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始验证配置 ===");
        AIConsoleLoggerUtil.print(project, "提供商: " + getProviderType().getDisplayName());
        AIConsoleLoggerUtil.print(project, "模型: " + getModelName());
        AIConsoleLoggerUtil.print(project, "Base URL: " + getBaseUrl());
        try {
            AIChatRequest request = new AIChatRequest("i say ping, you say pong",
                                                      "ping", 0);
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
     * 获取可用的模型列表
     * <p>
     * 根据提供的 API Key 获取可用模型列表, 若未提供 API Key 或需要 API Key 但未提供, 则返回空列表.
     * 若请求过程中发生网络错误或其他异常, 也返回空列表.
     *
     * @param apiKey API Key, 可以为 null 或空字符串
     * @return 可用的模型列表
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始获取可用模型列表 ===");
        AIConsoleLoggerUtil.print(project, "提供商: " + getProviderType().getDisplayName());
        AIConsoleLoggerUtil.print(project, "Base URL: " + config.baseUrl);
        try {
            if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
                AIConsoleLoggerUtil.printWarning(project, "需要 API Key 但未提供，返回空列表");
                return new ArrayList<>();
            }

            String url = config.baseUrl + "/models";
            AIConsoleLoggerUtil.print(project, "请求 URL: " + url);
            String responseBody = HttpRequests.request(url)
                .tuner(connection -> {
                    tuneConnection((HttpURLConnection) connection, apiKey);
                })
                .connect(HttpRequests.Request::readString);

            if (!responseBody.trim().isEmpty()) {
                List<String> models = parseModelsResponse(responseBody);
                AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
                if (!models.isEmpty() && models.size() <= 10) {
                    models.forEach(model -> AIConsoleLoggerUtil.print(project, "  - " + model));
                }
                return models;
            }

            AIConsoleLoggerUtil.printWarning(project, "服务返回空响应");
            return new ArrayList<>();
        } catch (IOException e) {
            LOG.info("Network error while fetching models", e);
            AIConsoleLoggerUtil.printError(project, "网络错误: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.info("Unexpected error while fetching models", e);
            AIConsoleLoggerUtil.printError(project, "获取模型列表失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 判断是否需要 API 密钥
     * <p>
     * 根据配置中的提供者类型判断当前是否需要使用 API 密钥
     *
     * @return 如果需要 API 密钥则返回 true, 否则返回 false
     */
    private boolean requiresApiKey() {
        return config.providerType.requiresApiKey();
    }

    /**
     * 构建发送给 AI 聊天服务的请求体
     * <p>
     * 根据传入的 AI 聊天请求对象, 构建包含系统消息和用户消息的 JSON 请求体, 同时设置模型参数.
     * 默认实现构建标准的 OpenAI 兼容格式请求体.
     * <p>
     * 如果服务商的 API 格式与 OpenAI 不兼容, 子类可以重写此方法以构建自定义格式的请求体.
     * <p>
     * 示例 - 标准 OpenAI 兼容格式:
     * <pre>{@code
     * {
     *   "model": "gpt-3.5-turbo",
     *   "messages": [
     *     {"role": "system", "content": "..."},
     *     {"role": "user", "content": "..."}
     *   ],
     *   "temperature": 0.7,
     *   "max_tokens": 2048
     * }
     * }</pre>
     *
     * @param request AI 聊天请求对象, 包含系统提示和用户提示信息
     * @return 构建完成的 JSON 对象, 包含消息内容和模型参数
     * @since 1.0.0
     */
    protected JsonObject buildRequestBody(AIChatRequest request) {
        return buildRequestBody(request, false, false);
    }

    /**
     * 构建发送给 AI 聊天服务的请求体
     * <p>
     * 根据传入的 AI 聊天请求对象, 构建包含系统消息和用户消息的 JSON 请求体, 并设置模型参数.
     * 默认实现构建标准的 OpenAI 兼容格式请求体.
     * <p>
     * 如果服务商的 API 格式与 OpenAI 不兼容, 子类可以重写此方法以构建自定义格式的请求体.
     * <p>
     * 示例 - 标准 OpenAI 兼容格式:
     * <pre>{@code
     * {
     *  "model": "gpt-3.5-turbo",
     *  "messages": [*   {"role": "system", "content": "..."},
     *   {"role": "user", "content": "..."}
     *  ],
     *  "temperature": 0.7,
     *  "max_tokens": 2048
     * }
     * }</pre>
     *
     * @param request        AI 聊天请求对象, 包含系统提示和用户提示信息
     * @param stream         是否启用流式传输
     * @param enableThinking 是否启用思考模式
     * @return 构建完成的 JSON 对象, 包含消息内容和模型参数
     * @since 1.0.0
     */
    protected JsonObject buildRequestBody(AIChatRequest request, boolean stream, boolean enableThinking) {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", request.systemPrompt());

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", request.userPrompt());

        JsonArray messagesArray = new JsonArray();
        messagesArray.add(systemMessage);
        messagesArray.add(userMessage);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.modelName);

        if (!enableThinking) {
            // 如果设置为关闭思考, 有些思考模型会报错, 所以需要强制设置为开启思考模式
            if (config.modelName.contains("think")) {
                enableThinking = true;
            }
            body.addProperty("think", enableThinking);
            body.addProperty("enable_thinking", enableThinking);
        }
        body.addProperty("stream", stream);
        body.add("messages", messagesArray);

        AIModelParameters params = modelParameters;
        // 只有当值不是 "auto" 时才添加到请求体中
        if (params.temperature != null && !"auto".equalsIgnoreCase(params.temperature.trim())) {
            try {
                body.addProperty("temperature", Double.parseDouble(params.temperature.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略无效的数字格式
            }
        }
        if (params.maxTokens != null && !"auto".equalsIgnoreCase(params.maxTokens.trim())) {
            try {
                // 如果包含 "K" 或 "k"，则转换为数字
                String maxTokensStr = params.maxTokens.trim();
                if (maxTokensStr.endsWith("K") || maxTokensStr.endsWith("k")) {
                    double maxTokensInK = Double.parseDouble(maxTokensStr.substring(0, maxTokensStr.length() - 1));
                    body.addProperty("max_tokens", (int) Math.max(100, Math.round(maxTokensInK * 1000)));
                } else {
                    body.addProperty("max_tokens", Integer.parseInt(maxTokensStr));
                }
            } catch (NumberFormatException ignored) {
                // 忽略无效的数字格式
            }
        }
        if (params.topP != null && !"auto".equalsIgnoreCase(params.topP.trim())) {
            try {
                body.addProperty("top_p", Double.parseDouble(params.topP.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略无效的数字格式
            }
        }
        if (params.topK != null && !"auto".equalsIgnoreCase(params.topK.trim())) {
            try {
                body.addProperty("top_k", Integer.parseInt(params.topK.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略无效的数字格式
            }
        }
        if (params.presencePenalty != null && !"auto".equalsIgnoreCase(params.presencePenalty.trim())) {
            try {
                body.addProperty("presence_penalty", Double.parseDouble(params.presencePenalty.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略无效的数字格式
            }
        }

        return body;
    }

    /**
     * 生成 AI 聊天内容的流式响应
     * <p> 根据提供的请求参数和配置, 调用 AI 服务生成内容, 并通过流式响应的方式逐步返回结果.
     * 在生成过程中, 会将每次接收到的内容传递给响应监听器. 如果请求失败, 则抛出异常.
     *
     * @param request  AI 聊天请求对象, 包含生成内容所需的信息
     * @param apiKey   用于认证的 API 密钥, 可为 null
     * @param listener 响应监听器, 用于接收生成过程中的事件通知, 不可为 null
     * @throws AIServiceException 当 AI 服务调用失败时抛出
     */
    @Override
    public void generateContentStream(@NotNull AIChatRequest request,
                                      @Nullable String apiKey,
                                      @NotNull AIStreamResponseListener listener) throws AIServiceException {
        StreamRequestExecutor executor = new StreamRequestExecutor(project, config, this::tuneConnection);
        executor.sendStreamRequest(buildRequestBody(request, true, true), apiKey, listener);
    }

    /**
     * 配置 HTTP 连接的超时时间和授权头信息
     * <p>
     * 根据运行时设置为给定的 HttpURLConnection 对象设置连接和读取超时时间, 并在需要时添加 API 密钥到请求头中.
     *
     * @param connection 要配置的 HTTP 连接对象
     * @param apiKey     可选的 API 密钥, 用于设置 Authorization 请求头
     */
    private void tuneConnection(HttpURLConnection connection, @Nullable String apiKey) {
        AIRuntimeSettings runtime = runtimeSettings;
        int timeoutMillis = runtime.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        if (apiKey != null && !apiKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        AIConsoleLoggerUtil.print(project, String.format("连接超时: [%ss] 读取超时: [%ss]", runtime.timeout,
                                                         (runtime.timeout * 2)));
    }

    /**
     * 解析模型响应
     * <p>
     * 该方法接收一个 JSON 格式的响应字符串, 尝试解析其中的 {@code "data"} 数组, 并从每个对象中提取 {@code "id"} 字段.
     * 解析成功后将所有非空, 非空白的 id 字符串按原顺序添加到返回列表中; 若解析过程中出现异常, 将记录日志并返回空列表.
     *
     * @param responseBody 包含模型信息的 JSON 响应体
     * @return 包含所有有效模型 id 的列表; 若解析失败则返回空列表
     */
    protected List<String> parseModelsResponse(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("data") && json.get("data").isJsonArray()) {
                JsonArray dataArray = json.getAsJsonArray("data");
                for (JsonElement element : dataArray) {
                    JsonObject modelObj = element.getAsJsonObject();
                    if (modelObj.has("id")) {
                        String modelId = modelObj.get("id").getAsString();
                        if (modelId != null && !modelId.trim().isEmpty()) {
                            models.add(modelId.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("Failed to parse models response", e);
        }
        return models;
    }
}

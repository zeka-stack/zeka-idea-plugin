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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
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
        while (attempts < Math.max(1, runtime.maxRetries)) {
            try {
                String result = sendRequest(buildRequestBody(request), apiKey, listener,
                                            request.promptTokenEstimate(), false);
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
            String response = sendRequest(buildRequestBody(request), apiKey, null, 0, true);
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
        sendStreamRequest(buildRequestBody(request, true, true), apiKey, listener);
    }

    /**
     * 发送流式请求到 AI 服务并处理响应
     * <p> 构造请求体并发送 POST 请求到指定的 AI 服务端点, 处理响应内容并触发流式响应监听器.
     * 如果未配置 API 密钥且需要, 则抛出配置错误异常.
     *
     * @param body     请求体, 以 JsonObject 格式提供
     * @param apiKey   可选的 API 密钥, 用于认证
     * @param listener 必须的响应监听器, 用于记录请求和响应日志, 并处理流式响应
     * @throws AIServiceException 当请求失败, 响应无效, 网络错误或 API 密钥配置错误时抛出
     */
    private void sendStreamRequest(JsonObject body,
                                   @Nullable String apiKey,
                                   @NotNull AIStreamResponseListener listener) throws AIServiceException {
        if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置",
                                         AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String url = config.baseUrl + "/chat/completions";
        String requestBody = body.toString();

        try {
            listener.onStart();
            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;

            HttpRequests.post(url, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    tuneConnection(conn, apiKey);
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(request -> {
                    request.write(requestBody);
                    HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                    readStreamResponse(connection, listener);
                    return null;
                });
        } catch (HttpRequests.HttpStatusException e) {
            AIServiceException.ErrorCode code = switch (e.getStatusCode()) {
                case 401 -> AIServiceException.ErrorCode.INVALID_API_KEY;
                case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
                case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
                default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
            };
            listener.onError("HTTP error: " + e.getMessage(), e);
            throw new AIServiceException("HTTP error: " + e.getMessage(), code, e);
        } catch (IOException e) {
            listener.onError("网络错误: " + e.getMessage(), e);
            throw new AIServiceException("网络错误: " + e.getMessage(),
                                         AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 读取 HTTP 连接的响应流并处理分块数据
     * <p>
     * 从 HTTP 连接的输入流中逐行读取数据, 解析每个数据块, 并调用监听器处理每个数据块的内容.
     * 如果数据块包含思考内容, 则打印思考内容. 当遇到 "[DONE]" 表示数据流结束.
     *
     * @param connection HTTP 连接对象
     * @param listener   数据流响应监听器, 用于处理每个数据块
     * @throws IOException 当读取或处理数据流时发生 I/O 错误
     */
    private void readStreamResponse(HttpURLConnection connection,
                                    @NotNull AIStreamResponseListener listener) throws IOException {
        StringBuilder fullText = new StringBuilder();
        StreamChunkParser parser = selectStreamChunkParser();
        boolean[] inThinking = {false};
        boolean[] thinkPrefixPrinted = {false};
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (line.isBlank()) {
                    continue;
                }

                // 输出原始内容
                // AIConsoleLoggerUtil.printStreamPlain(project, line);

                log.debug("{}", line);

                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    if (!data.startsWith("{")) {
                        continue;
                    }
                    JsonObject json = parseSseJson(data);
                    if (json == null) {
                        continue;
                    }
                    boolean done = parser.isDone(json);
                    StreamChunk chunk = parser.parse(json);
                    if (chunk == null) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    if (chunk.thinking != null && !chunk.thinking.isEmpty()) {
                        printThinking(chunk.thinking, inThinking, thinkPrefixPrinted);
                    }
                    if (chunk.content != null && !chunk.content.isEmpty()) {
                        if (inThinking[0]) {
                            // 思考结束后补空行，保持输出分段
                            AIConsoleLoggerUtil.printStreamPlain(
                                project,
                                "\n\n══════════════════════════════ 正文内容 ══════════════════════════════\n\n");
                            inThinking[0] = false;
                            thinkPrefixPrinted[0] = false;
                        }
                        fullText.append(chunk.content);
                        AIConsoleLoggerUtil.printStreamPlain(project, chunk.content);
                        listener.onChunk(chunk.content);
                    }
                    if (done) {
                        break;
                    }
                }
            }
        }
        AIConsoleLoggerUtil.completeStreamPlain(project);
        listener.onComplete(fullText.toString());
    }

    @Nullable
    private static JsonObject parseSseJson(@NotNull String jsonData) {
        try {
            return JsonParser.parseString(jsonData).getAsJsonObject();
        } catch (Exception e) {
            LOG.warn("Failed to parse stream chunk JSON", e);
            return null;
        }
    }

    /**
     * 打印思考内容
     * <p>
     * 根据传入的思考内容和标志位数组, 决定是否在控制台上打印 "[think]" 前缀.
     * 如果是第一次打印思考内容, 则会加上 "[think]" 前缀, 并将相应的标志位置为已打印.
     *
     * @param thinking           思考内容字符串
     * @param inThinking         标志位数组, 用于标识是否已经开始打印思考内容
     * @param thinkPrefixPrinted 标志位数组, 用于标识是否已经打印过 "[think]" 前缀
     */
    private void printThinking(@NotNull String thinking,
                               boolean @NotNull [] inThinking,
                               boolean @NotNull [] thinkPrefixPrinted) {
        if (!inThinking[0]) {
            inThinking[0] = true;
        }
        if (!thinkPrefixPrinted[0]) {
            AIConsoleLoggerUtil.printStreamPlain(project, "[think] " + thinking);
            thinkPrefixPrinted[0] = true;
            return;
        }
        AIConsoleLoggerUtil.printStreamPlain(project, thinking);
    }

    /**
     * 从给定的 JsonObject 中读取指定键的字符串值
     * <p> 检查 JsonObject 是否包含指定的键, 并返回对应的字符串值. 如果键不存在或值为 null, 则返回 null.
     *
     * @param delta 包含要读取的键的 JsonObject
     * @param key   要读取的键名
     * @return 对应键的字符串值, 如果键不存在或值为 null, 则返回 null
     * @since 1.0.0
     */
    @Nullable
    private static String readStringValue(@NotNull JsonObject delta, @NotNull String key) {
        if (!delta.has(key)) {
            return null;
        }
        JsonElement element = delta.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }

    /**
     * 流块记录类
     * <p> 用于表示流中的一个块, 包含内容和思考两个字段. 该记录类不可变, 并提供了对这两个字段的安全访问.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.31
     * @since 1.0.0
     */
    private record StreamChunk(@Nullable String content, @Nullable String thinking) {
    }

    private interface StreamChunkParser {
        @Nullable
        StreamChunk parse(@NotNull JsonObject json);

        default boolean isDone(@NotNull JsonObject json) {
            JsonObject choice = readFirstChoice(json);
            if (choice == null) {
                return false;
            }
            String finishReason = readStringValue(choice, "finish_reason");
            return "stop".equalsIgnoreCase(finishReason);
        }
    }

    private StreamChunkParser selectStreamChunkParser() {
        List<StreamParserRule> rules = new ArrayList<>();
        rules.add(new StreamParserRule("https://dashscope.aliyuncs.com", AIProviderType.QIANWEN, "qwen",
                                       new DashscopeStreamChunkParser()));
        rules.add(new StreamParserRule("http://localhost:11434", AIProviderType.OLLAMA, null,
                                       new OllamaStreamChunkParser()));
        rules.add(new StreamParserRule("minimax", null, "minimax", new MiniMaxStreamChunkParser()));

        String baseUrl = config.baseUrl == null ? "" : config.baseUrl;
        String modelName = config.modelName == null ? "" : config.modelName;
        AIProviderType providerType = config.providerType;
        for (StreamParserRule rule : rules) {
            if (rule.matches(baseUrl, providerType, modelName)) {
                return rule.parser;
            }
        }
        return new OpenAiStreamChunkParser();
    }

    private record StreamParserRule(String urlPrefix, AIProviderType providerType, String modelPrefix, StreamChunkParser parser) {
            private StreamParserRule(@Nullable String urlPrefix,
                                     @Nullable AIProviderType providerType,
                                     @Nullable String modelPrefix,
                                     @NotNull StreamChunkParser parser) {
                this.urlPrefix = urlPrefix;
                this.providerType = providerType;
                this.modelPrefix = modelPrefix;
                this.parser = parser;
            }

            private boolean matches(@NotNull String baseUrl,
                                    @NotNull AIProviderType provider,
                                    @NotNull String modelName) {
                return matchesUrl(baseUrl, urlPrefix)
                       && matchesProvider(provider, providerType)
                       && matchesModel(modelName, modelPrefix);
            }

            private static boolean matchesUrl(@NotNull String baseUrl, @Nullable String prefix) {
                if (prefix == null || prefix.isBlank()) {
                    return true;
                }
                String normalizedBase = baseUrl.toLowerCase(Locale.ROOT);
                String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
                if (normalizedPrefix.contains("://")) {
                    return normalizedBase.startsWith(normalizedPrefix);
                }
                return normalizedBase.contains(normalizedPrefix);
            }

            private static boolean matchesProvider(@NotNull AIProviderType actual, @Nullable AIProviderType expected) {
                return expected == null || expected == actual;
            }

            private static boolean matchesModel(@NotNull String modelName, @Nullable String prefix) {
                if (prefix == null || prefix.isBlank()) {
                    return true;
                }
                return modelName.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
            }
        }

    private static final class OpenAiStreamChunkParser implements StreamChunkParser {
        @Override
        public StreamChunk parse(@NotNull JsonObject json) {
            JsonObject delta = readFirstDelta(json);
            if (delta == null) {
                return null;
            }
            String content = readStringValue(delta, "content");
            if (content == null || content.isEmpty()) {
                return null;
            }
            return new StreamChunk(content, null);
        }
    }

    private static final class DashscopeStreamChunkParser implements StreamChunkParser {
        @Override
        public StreamChunk parse(@NotNull JsonObject json) {
            JsonObject delta = readFirstDelta(json);
            if (delta == null) {
                return null;
            }
            String content = readStringValue(delta, "content");
            String thinking = readStringValue(delta, "reasoning_content");
            if ((content == null || content.isEmpty()) && (thinking == null || thinking.isEmpty())) {
                return null;
            }
            return new StreamChunk(content, thinking);
        }
    }

    private static final class OllamaStreamChunkParser implements StreamChunkParser {
        @Override
        public StreamChunk parse(@NotNull JsonObject json) {
            JsonObject delta = readFirstDelta(json);
            if (delta == null) {
                return null;
            }
            String content = readStringValue(delta, "content");
            String thinking = readStringValue(delta, "reasoning");
            if ((content == null || content.isEmpty()) && (thinking == null || thinking.isEmpty())) {
                return null;
            }
            return new StreamChunk(content, thinking);
        }
    }

    private static final class MiniMaxStreamChunkParser implements StreamChunkParser {
        private static final String THINK_START = "<think>";
        private static final String THINK_END = "</think>";
        private boolean inThinking;

        @Override
        public StreamChunk parse(@NotNull JsonObject json) {
            JsonObject delta = readFirstDelta(json);
            if (delta == null) {
                return null;
            }
            String content = readStringValue(delta, "content");
            if (content == null || content.isEmpty()) {
                return null;
            }
            StringBuilder thinking = new StringBuilder();
            StringBuilder answer = new StringBuilder();
            int index = 0;
            while (index < content.length()) {
                if (inThinking) {
                    int endIndex = content.indexOf(THINK_END, index);
                    if (endIndex == -1) {
                        thinking.append(content.substring(index));
                        index = content.length();
                        continue;
                    }
                    thinking.append(content, index, endIndex);
                    index = endIndex + THINK_END.length();
                    inThinking = false;
                    continue;
                }
                int startIndex = content.indexOf(THINK_START, index);
                if (startIndex == -1) {
                    answer.append(content.substring(index));
                    index = content.length();
                    continue;
                }
                answer.append(content, index, startIndex);
                index = startIndex + THINK_START.length();
                inThinking = true;
            }
            String thinkingText = !thinking.isEmpty() ? thinking.toString() : null;
            String answerText = !answer.isEmpty() ? answer.toString() : null;
            if (thinkingText == null && answerText == null) {
                return null;
            }
            return new StreamChunk(answerText, thinkingText);
        }
    }

    @Nullable
    private static JsonObject readFirstChoice(@NotNull JsonObject json) {
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).getAsJsonObject();
    }

    @Nullable
    private static JsonObject readFirstDelta(@NotNull JsonObject json) {
        JsonObject choice = readFirstChoice(json);
        if (choice == null) {
            return null;
        }
        return choice.getAsJsonObject("delta");
    }

    /**
     * 发送请求到 AI 服务并获取响应结果
     * <p>
     * 构造请求体并发送 POST 请求到指定的 AI 服务端点, 处理响应内容并返回解析后的结果.
     * 如果未配置 API 密钥且需要, 则抛出配置错误异常.
     *
     * @param body                 请求体, 以 JsonObject 格式提供
     * @param apiKey               可选的 API 密钥, 用于认证
     * @param listener             可选的响应监听器, 用于记录请求和响应日志
     * @param promptLengthEstimate 提示长度估计值 (可能用于内部逻辑)
     * @param validation           是否启用响应验证模式
     * @return AI 服务返回的解析结果字符串
     * @throws AIServiceException 当请求失败, 响应无效, 网络错误或 API 密钥配置错误时抛出
     */
    private String sendRequest(JsonObject body,
                               @Nullable String apiKey,
                               @Nullable AIResponseListener listener,
                               int promptLengthEstimate,
                               boolean validation) throws AIServiceException {
        if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置",
                                         AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String url = config.baseUrl + "/chat/completions";
        String requestBody = body.toString();

        logRequest(listener, requestBody, validation);

        try {
            // 计算请求体长度，用于禁用分块传输编码
            byte[] requestBodyBytes = requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;

            String responseBody = HttpRequests.post(url, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    tuneConnection(conn, apiKey);
                    // 在连接建立之前设置固定长度流模式，禁用分块传输编码
                    // 这样在身份验证失败时可以重试
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(request -> {
                    request.write(requestBody);
                    return request.readString();
                });

            logResponse(listener, responseBody, validation);

            if (!responseBody.trim().isEmpty()) {
                String result = validation ? parseValidationResponse(responseBody) : parseResponse(responseBody, listener);
                LOG.debug("AI response length: " + result.length());
                return result;
            }

            throw new AIServiceException("Invalid response from AI service",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE);
        } catch (HttpRequests.HttpStatusException e) {
            AIServiceException.ErrorCode code = switch (e.getStatusCode()) {
                case 401 -> AIServiceException.ErrorCode.INVALID_API_KEY;
                case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
                case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
                default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
            };
            throw new AIServiceException("HTTP error: " + e.getMessage(), code, e);
        } catch (IOException e) {
            // 检查 IOException 消息中是否包含 HTTP 状态码信息
            // 某些情况下，HTTP 错误可能被包装在 IOException 中
            final String message = getErrorString(e);
            throw new AIServiceException("未知错误: " + message,
                                         AIServiceException.ErrorCode.UNKNOWN_ERROR, e);
        }
    }

    /**
     * 从 IOException 中提取错误信息并根据 HTTP 状态码抛出相应的 AI 服务异常
     * <p>
     * 该方法解析 IOException 的消息内容, 识别特定的 HTTP 状态码错误,
     * 并抛出对应的 AIServiceException 异常, 如果未识别到特定错误则返回原始消息
     *
     * @param e 包含 HTTP 错误信息的 IOException
     * @return 如果没有匹配到特定错误码, 则返回原始错误消息; 否则抛出相应异常
     * @throws AIServiceException 当识别到 429,401 或 5xx HTTP 状态码时抛出相应错误码的异常
     */
    @Nullable
    private static String getErrorString(IOException e) throws AIServiceException {
        String message = e.getMessage();
        if (message != null) {
            // 检查是否包含 HTTP 429 错误（限流）
            if (message.contains("HTTP response code: 429") || message.contains("429")) {
                throw new AIServiceException("请求频率过高，请稍后重试: " + message,
                                             AIServiceException.ErrorCode.RATE_LIMIT, e);
            }
            // 检查是否包含 HTTP 401 错误（认证失败）
            if (message.contains("HTTP response code: 401") || message.contains("401")) {
                throw new AIServiceException("API 密钥无效: " + message,
                                             AIServiceException.ErrorCode.INVALID_API_KEY, e);
            }
            // 检查是否包含其他 HTTP 错误状态码
            if (message.contains("HTTP response code: 5")) {
                throw new AIServiceException("服务暂时不可用，请稍后重试: " + message,
                                             AIServiceException.ErrorCode.SERVICE_UNAVAILABLE, e);
            }
        }
        return message;
    }

    /**
     * 记录 AI 请求信息
     * <p>
     * 根据传入的 {@code listener} 与 {@code validation} 标志决定是否将请求信息回调给 {@link AIResponseListener},
     * 并在开启详细日志时将请求体打印到日志和控制台.
     *
     * @param listener    监听器, 用于回调请求信息; 若为 {@code null} 则不执行回调
     * @param requestBody 请求体内容
     * @param validation  是否已通过验证; 若为 {@code true} 则不回调监听器
     */
    private void logRequest(@Nullable AIResponseListener listener, String requestBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onRequest(getProviderType().getDisplayName(), getModelName(), requestBody, false);
        }
    }

    /**
     * 记录 AI 响应信息, 包括回调通知和详细日志
     * <p>
     * 根据是否启用验证和监听器是否存在, 调用监听器的 onResponse 方法.
     * 如果启用了详细日志模式, 则记录响应内容到日志和控制台.
     *
     * @param listener     AI 响应监听器, 可能为 null
     * @param responseBody AI 响应内容
     * @param validation   是否启用验证模式, 影响是否调用监听器
     */
    private void logResponse(@Nullable AIResponseListener listener, String responseBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onResponse(getProviderType().getDisplayName(), getModelName(), responseBody, false);
        }
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
     * 解析 AI 服务响应内容并返回处理后的结果
     * <p>
     * 从响应体中提取内容, 并处理与令牌使用相关的日志记录和回调通知
     *
     * @param responseBody AI 服务返回的原始响应内容
     * @param listener     用于接收令牌使用情况的监听器, 可以为 null
     * @return 解析并过滤后的响应内容
     * @throws AIServiceException 当响应内容无法解析时抛出
     */
    private String parseResponse(String responseBody, @Nullable AIResponseListener listener) throws AIServiceException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
                int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
                int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : 0;

                // 输出 token 消耗情况，使用 | 分隔，一行输出
                AIConsoleLoggerUtil.print(project, String.format("Token 消耗: Prompt=%d | Completion=%d | " +
                                                                 "Total=%d",
                                                                 promptTokens, completionTokens,
                                                                 totalTokens));

                if (listener != null) {
                    listener.onUsage(getProviderType().getDisplayName(), getModelName(), promptTokens, completionTokens, totalTokens);
                }
            }

            return filterThinkingContent(content);
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

    /**
     * 解析验证响应内容并判断是否为有效响应
     * <p>
     * 该方法用于解析服务返回的响应体, 检查其中的使用情况和选择内容, 以判断响应是否有效.
     * 如果响应有效, 返回 "OK"; 否则抛出异常.
     *
     * @param responseBody 响应体内容字符串
     * @return 如果响应有效, 返回 "OK"
     * @throws AIServiceException 如果解析失败或响应无效时抛出
     */
    private String parseValidationResponse(String responseBody) throws AIServiceException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                if (usage.has("completion_tokens") && usage.get("completion_tokens").getAsInt() > 0) {
                    return "OK";
                }
            }

            if (json.has("choices") && !json.getAsJsonArray("choices").isEmpty()) {
                return "OK";
            }

            throw new AIServiceException("No completion tokens found in response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE);
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse validation response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
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

    /**
     * ", 则返回该标记之后的内容并去除前后空格. 如果内容中包含"</think>
     *
     * @param content 需要过滤的原始内容
     * @return 过滤后的内容
     */
    private String filterThinkingContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String endTag = "</think>";
        int endTagIndex = content.indexOf(endTag);
        if (endTagIndex != -1) {
            return content.substring(endTagIndex + endTag.length()).trim();
        }
        if (content.contains("<think>")) {
            return "";
        }
        return content;
    }
}

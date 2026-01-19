package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.anthropic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Claude（Anthropic）Provider
 * <p>
 * 使用 Anthropic Messages API：/v1/messages
 */
@Slf4j
public class AnthropicLikeProvider extends AICompatibleProvider {

    /** Anthropic API 的版本号, 固定为 2023-06-01 */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * 初始化 Anthropic 提供商实例
     * <p> 用于创建与 Anthropic API 交互的客户端, 配置项目, 提供者配置, 模型参数和运行时设置
     *
     * @param project         项目上下文, 用于日志输出和配置管理
     * @param config          提供者配置, 包含基础 URL, 模型名称等信息
     * @param modelParameters 模型参数, 如温度, 最大令牌数等
     * @param runtimeSettings 运行时设置, 如重试次数, 超时时间等
     */
    public AnthropicLikeProvider(@NotNull Project project,
                                 @NotNull AIProviderConfig config,
                                 @NotNull AIModelParameters modelParameters,
                                 @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 根据聊天请求生成内容
     * <p> 通过 Anthropic Messages API 发送消息请求, 支持重试机制, 若请求失败则按指数退避策略重试, 最多重试次数由运行时设置决定.
     * 若所有重试均失败, 则抛出 AIServiceException.
     *
     * @param request  聊天请求对象, 包含用户提示, 系统提示等信息
     * @param apiKey   API 密钥, 若提供者类型要求密钥且未提供或为空, 则抛出异常
     * @param listener 响应监听器, 用于接收请求, 响应, 使用情况等回调, 可为 null
     * @return 生成的文本内容
     * @throws AIServiceException 当配置错误, 网络错误, 服务不可用或响应解析失败时抛出
     */
    @Override
    @NotNull
    public String generateContent(@NotNull AIChatRequest request,
                                  @Nullable String apiKey,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始生成内容 ===");
        AIConsoleLoggerUtil.print(project, String.format("模型信息: 供应商=%s | 模型=%s | Base URL=%s",
                                                         getProviderType().getDisplayName(), getModelName(), getBaseUrl()));

        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置", AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        AIRuntimeSettings runtime = runtimeSettings;
        int attempts = 0;
        int maxRetries = Math.max(1, runtime.maxRetries);
        while (attempts < maxRetries) {
            try {
                return sendMessagesRequest(buildMessagesRequestBody(request, false), apiKey, listener, false);
            } catch (AIServiceException e) {
                attempts++;
                if (!e.isRetryable() || attempts >= maxRetries) {
                    throw e;
                }
                long waitTime = (long) (runtime.waitDuration * Math.pow(2, attempts - 1));
                AIConsoleLoggerUtil.printWarning(project, "请求失败 (尝试 " + attempts + "/" + maxRetries + "): " + e.getMessage());
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
        throw new AIServiceException("AI 服务调用失败", AIServiceException.ErrorCode.UNKNOWN_ERROR);
    }

    /**
     * 以流式方式生成内容
     * <p> 通过 Anthropic Messages API 的流式接口发送消息请求, 并逐块接收响应内容, 适用于实时输出场景.
     * <p> 请求体使用 {@code buildMessagesRequestBody} 构建, 内容类型为 {@code application/json}, 并设置流式响应头 {@code Accept: text/event-stream}.
     *
     * @param request  AI 聊天请求对象, 包含用户提示, 系统提示等信息
     * @param apiKey   API 密钥, 可为空, 若提供商要求密钥且未提供则抛出异常
     * @param listener 流式响应监听器, 用于接收请求开始, 内容块, 错误及完成事件
     * @throws AIServiceException 当 HTTP 错误, 网络错误或响应解析失败时抛出, 错误码根据具体异常类型映射
     */
    @Override
    public void generateContentStream(@NotNull AIChatRequest request,
                                      @Nullable String apiKey,
                                      @NotNull AIStreamResponseListener listener) throws AIServiceException {
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置", AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String url = config.baseUrl + "/v1/messages";
        JsonObject body = buildMessagesRequestBody(request, true);
        String requestBody = body.toString();

        try {
            listener.onStart();
            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;
            HttpRequests.post(url, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    tuneAnthropicConnection(conn, apiKey);
                    conn.setRequestProperty("Accept", "text/event-stream");
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(requestAdapter -> {
                    requestAdapter.write(requestBody);
                    HttpURLConnection connection = (HttpURLConnection) requestAdapter.getConnection();
                    readClaudeSse(connection, listener);
                    return null;
                });
        } catch (HttpRequests.HttpStatusException e) {
            AIServiceException.ErrorCode code = AICompatibleProvider.mapHttpError(e.getStatusCode());
            String msg = "Claude HTTP 错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, code, e);
        } catch (IOException e) {
            String msg = "Claude 网络错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 验证当前配置是否有效
     * <p>通过发送一个简单的测试请求 ("i say ping, you say pong") 到 AI 服务, 检查连接是否正常.
     * 如果响应非空, 则认为配置有效; 否则返回失败信息.
     *
     * @param apiKey API 密钥, 可为空. 若提供商要求密钥且未提供, 则会抛出异常.
     * @return 验证结果对象, 包含成功或失败的状态, 消息及可选的异常信息
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
            String response = sendMessagesRequest(buildMessagesRequestBody(request, false), apiKey, null, true);
            if (!response.isEmpty()) {
                AIConsoleLoggerUtil.printSuccess(project, "=== 配置验证成功 ===");
                return ValidationResult.success("连接成功！提供商: " + getProviderType().getDisplayName() + ", 模型: " + getModelName());
            }
            return ValidationResult.failure("连接失败：服务返回空响应");
        } catch (AIServiceException e) {
            return ValidationResult.failure("配置验证失败", AIServiceException.build(e));
        } catch (Exception e) {
            return ValidationResult.failure("配置验证异常", e.getMessage(), e);
        }
    }

    /**
     * 获取可用的模型列表
     * <p> 通过调用 Anthropic API 的 /v1/models 接口获取当前提供商支持的模型列表.
     * 如果未提供 API 密钥但提供商要求密钥, 则返回空列表.
     * 若接口调用失败或解析失败, 将返回当前提供商默认支持的模型列表.
     *
     * @param apiKey 可选的 API 密钥, 用于认证请求. 若为 null 或空字符串, 则在提供商要求密钥时返回空列表.
     * @return 可用模型名称列表. 若获取失败, 则返回提供商默认支持的模型列表.
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== Claude 获取模型列表 ===");
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            AIConsoleLoggerUtil.printWarning(project, "需要 API Key 但未提供，返回空列表");
            return new ArrayList<>();
        }

        String url = config.baseUrl + "/v1/models";
        try {
            String responseBody = HttpRequests.request(url)
                .tuner(connection -> tuneAnthropicConnection((HttpURLConnection) connection, apiKey))
                .connect(HttpRequests.Request::readString);
            List<String> models = parseClaudeModelsResponse(responseBody);
            if (!models.isEmpty()) {
                AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
                return models;
            }
        } catch (Exception e) {
            log.debug("Failed to fetch Claude models", e);
        }

        // Anthropic 的模型列表接口可能因权限/版本差异不可用，兜底使用枚举内置列表
        return new ArrayList<>(getProviderType().getSupportedModels());
    }

    /**
     * 配置 Anthropic API 连接参数
     * <p> 设置 HTTP 连接超时时间, 协议版本和 API 密钥头信息, 用于与 Anthropic 服务建立安全连接
     *
     * @param connection 需要配置的 HttpURLConnection 实例
     * @param apiKey     可选的 API 密钥, 用于身份验证, 若为空或空白则不设置密钥头
     */
    private void tuneAnthropicConnection(@NotNull HttpURLConnection connection, @Nullable String apiKey) {
        int timeoutMillis = runtimeSettings.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
        if (apiKey != null && !apiKey.isBlank()) {
            connection.setRequestProperty("x-api-key", apiKey.trim());
        }
    }

    /**
     * 构建用于 Anthropic Messages API 的请求体
     * <p> 根据传入的聊天请求和流式标志, 生成符合 Anthropic API 格式的 JSON 请求体, 包含模型, 流式设置, 系统提示, 用户消息及模型参数.
     *
     * @param request 聊天请求对象, 包含用户提示, 系统提示等信息
     * @param stream  是否启用流式响应模式
     * @return 构建完成的 JSON 请求体, 包含所有必要字段
     */
    private JsonObject buildMessagesRequestBody(@NotNull AIChatRequest request, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.modelName);
        body.addProperty("stream", stream);

        String systemPrompt = request.systemPrompt();
        if (!systemPrompt.isBlank()) {
            body.addProperty("system", systemPrompt);
        }

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", request.userPrompt());
        messages.add(userMsg);
        body.add("messages", messages);

        // 参数映射：Anthropic 主要使用 max_tokens / temperature / top_p / top_k
        AIModelParameters params = modelParameters;
        Integer maxTokens = parseMaxTokens(params.maxTokens);
        if (maxTokens != null) {
            body.addProperty("max_tokens", maxTokens);
        }
        Double temperature = parseDouble(params.temperature);
        if (temperature != null) {
            body.addProperty("temperature", temperature);
        }
        Double topP = parseDouble(params.topP);
        if (topP != null) {
            body.addProperty("top_p", topP);
        }
        Integer topK = parseInt(params.topK);
        if (topK != null) {
            body.addProperty("top_k", topK);
        }
        return body;
    }

    /**
     * 向 Anthropic API 发送消息请求并获取响应
     * <p> 通过 POST 请求向 接口发送消息内容, 根据是否为验证模式决定返回内容类型 </p>
     * <p> 请求体格式为 JSON, 包含模型, 消息内容, 温度, 最大令牌数等参数, 支持监听器回调请求与响应事件 </p>
     *
     * @param body       请求体 JSON 对象, 包含模型名称, 消息内容, 流式标志等配置
     * @param apiKey     API 密钥, 可为空, 若为空且服务需要密钥则抛出异常
     * @param listener   响应监听器, 用于在请求前后或响应时回调事件, 可为空
     * @param validation 是否为验证模式, 若为 true 则仅返回 "OK" 表示连接正常, 否则解析并返回实际响应内容
     * @return 非验证模式下返回解析后的文本响应内容; 验证模式下返回 "OK"
     * @throws AIServiceException 当 HTTP 错误, 网络异常或响应解析失败时抛出, 包含错误码和原始异常
     */
    private String sendMessagesRequest(@NotNull JsonObject body,
                                       @Nullable String apiKey,
                                       @Nullable AIResponseListener listener,
                                       boolean validation) throws AIServiceException {
        String url = config.baseUrl + "/v1/messages";
        String requestBody = body.toString();
        if (listener != null) {
            listener.onRequest(getProviderType().getDisplayName(), config.modelName, requestBody, validation);
        }
        try {
            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;
            String responseBody = HttpRequests.post(url, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    tuneAnthropicConnection(conn, apiKey);
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(request -> {
                    request.write(requestBody);
                    return request.readString();
                });
            if (listener != null) {
                listener.onResponse(getProviderType().getDisplayName(), config.modelName, responseBody, validation);
            }
            if (validation) {
                return "OK";
            }
            return parseClaudeMessageResponse(responseBody, listener);
        } catch (HttpRequests.HttpStatusException e) {
            throw new AIServiceException("Claude HTTP 错误: " + e.getMessage(), AICompatibleProvider.mapHttpError(e.getStatusCode()), e);
        } catch (IOException e) {
            throw new AIServiceException("Claude 网络错误: " + e.getMessage(), AIServiceException.ErrorCode.NETWORK_ERROR, e);
        } catch (Exception e) {
            throw new AIServiceException("Claude 响应解析失败", AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

    /**
     * 解析 Claude API 返回的响应体并提取生成的文本内容
     * <p> 从 JSON 响应中提取文本内容块, 若包含使用统计信息则调用监听器报告 token 数量, 若响应为空则抛出异常
     *
     * @param responseBody 服务器返回的 JSON 响应体字符串
     * @param listener     响应监听器, 用于报告 token 使用情况 (可为空)
     * @return 解析出的文本内容, 若内容为空则抛出异常
     * @throws AIServiceException 当响应包含错误信息或解析失败时抛出
     */
    private String parseClaudeMessageResponse(@NotNull String responseBody, @Nullable AIResponseListener listener) throws AIServiceException {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (json.has("error")) {
            throw new AIServiceException(json.getAsJsonObject("error").toString(), AIServiceException.ErrorCode.INVALID_RESPONSE);
        }

        StringBuilder text = new StringBuilder();
        if (json.has("content") && json.get("content").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("content")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject block = el.getAsJsonObject();
                String type = block.has("type") ? block.get("type").getAsString() : "";
                if ("text".equals(type) && block.has("text")) {
                    text.append(block.get("text").getAsString());
                }
            }
        }

        // usage: input_tokens / output_tokens
        if (json.has("usage") && json.get("usage").isJsonObject() && listener != null) {
            JsonObject usage = json.getAsJsonObject("usage");
            int promptTokens = usage.has("input_tokens") ? usage.get("input_tokens").getAsInt() : 0;
            int completionTokens = usage.has("output_tokens") ? usage.get("output_tokens").getAsInt() : 0;
            listener.onUsage(getProviderType().getDisplayName(), config.modelName, promptTokens, completionTokens, promptTokens + completionTokens);
        }

        String result = text.toString().trim();
        if (result.isEmpty()) {
            throw new AIServiceException("Claude 返回空响应", AIServiceException.ErrorCode.INVALID_RESPONSE);
        }
        return result;
    }

    /**
     * 从 Claude SSE 流式响应中读取并处理内容块
     * <p>该方法用于处理来自 Claude API 的服务器发送事件 (SSE) 流, 逐行解析响应数据, 提取文本块或思考块, 并通过监听器回调通知上层.
     * 支持在流式传输过程中实时推送内容块 (<code>onChunk</code>) 或思考过程(<code>onThinkingChunk</code>), 并在流结束或发生错误时调用相应回调.
     *
     * @param connection 用于读取 SSE 流的 HTTP 连接, 必须已配置为接收文本 /event-stream 类型响应
     * @param listener   流式响应监听器, 用于接收内容块, 思考块, 错误或完成事件
     * @throws IOException 当读取连接输入流时发生 I/O 错误
     *
     *                     <pre>{@code
     *                                         // 示例: 监听器回调处理
     *                                         listener.onChunk("Hello"); // 接收文本块
     *                                         listener.onThinkingChunk("正在思考..."); // 接收思考块
     *                                         listener.onError("错误信息", null); // 发生错误时调用
     *                                         listener.onComplete("完整响应内容"); // 流结束时调用
     *                                         }</pre>
     *
     *                     <a href="https://docs.anthropic.com/claude/reference/messages">Claude Messages API 文档</a>
     */
    private void readClaudeSse(@NotNull HttpURLConnection connection, @NotNull AIStreamResponseListener listener) throws IOException {
        StringBuilder fullText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    break;
                }
                if (!data.startsWith("{")) {
                    continue;
                }
                JsonObject json = safeParseJson(data);
                if (json == null) {
                    continue;
                }
                if (json.has("type")) {
                    String type = json.get("type").getAsString();
                    if ("message_stop".equals(type)) {
                        break;
                    }
                    if ("error".equals(type) && json.has("error")) {
                        String msg = json.get("error").toString();
                        listener.onError(msg, null);
                        break;
                    }
                    if ("content_block_delta".equals(type) && json.has("delta")) {
                        JsonObject delta = json.getAsJsonObject("delta");
                        if (delta.has("text")) {
                            String chunk = delta.get("text").getAsString();
                            if (!chunk.isEmpty()) {
                                fullText.append(chunk);
                                listener.onChunk(chunk);
                            }
                        } else if (delta.has("thinking")) {
                            String chunk = delta.get("thinking").getAsString();
                            if (!chunk.isEmpty()) {
                                listener.onThinkingChunk(chunk);
                            }
                        }
                    }
                }
            }
        }
        listener.onComplete(fullText.toString());
    }

    /**
     * 安全解析 JSON 字符串为对象
     * <p> 尝试将输入的 JSON 字符串解析为 {@link JsonElement}, 若成功且为对象类型则返回其 {@link JsonObject}, 否则返回 null
     *
     * @param data 待解析的 JSON 字符串, 不能为空
     * @return 解析后的 {@link JsonObject}, 若解析失败或非对象类型则返回 null
     */
    private static @Nullable JsonObject safeParseJson(@NotNull String data) {
        try {
            JsonElement el = JsonParser.parseString(data);
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析 Claude 模型列表响应
     * <p> 从 API 返回的 JSON 响应中提取模型 ID 列表, 用于展示可用模型
     *
     * @param responseBody API 返回的原始响应体, 格式为 JSON
     * @return 解析出的模型 ID 列表, 若解析失败或无数据则返回空列表
     */
    private List<String> parseClaudeModelsResponse(@NotNull String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("data") && json.get("data").isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray("data")) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("id")) {
                        String id = obj.get("id").getAsString();
                        if (id != null && !id.isBlank()) {
                            models.add(id.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse Claude models response", e);
        }
        return models;
    }

    /**
     * 将字符串值解析为 Double 类型, 若解析失败或值为 null, 空字符串或 "auto", 则返回 null
     * <p> 该方法用于安全地将字符串转换为浮点数, 避免抛出 NumberFormatException 异常.
     * 若输入值为 "auto"(不区分大小写), 也视为无效值并返回 null.
     *
     * @param value 待解析的字符串值, 可能为 null, 空字符串或 "auto"
     * @return 解析成功的 Double 值, 若失败或值为 "auto" 则返回 null
     */
    private static @Nullable Double parseDouble(@Nullable String value) {
        if (value == null || value.isBlank() || "auto".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 将字符串转换为整数, 若转换失败或值为 "auto" 则返回 null
     * <p> 该方法用于安全地解析字符串为整数, 忽略空值, 空白字符串或 "auto" 字符串, 避免抛出 NumberFormatException
     *
     * @param value 待转换的字符串, 可能为 null 或空白字符串
     * @return 解析成功的整数值, 若失败或值为 "auto" 则返回 null
     */
    private static @Nullable Integer parseInt(@Nullable String value) {
        if (value == null || value.isBlank() || "auto".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 解析最大令牌数字符串
     * <p> 将传入的字符串格式的最大令牌数转换为整数, 支持带 'K' 或 'k' 后缀的千单位格式 (如 "2K","3k"), 并自动转换为对应数值. 若字符串为 null, 空或 "auto", 则返回 null.
     * <p> 示例: 输入 "2K" → 返回 2000, 输入 "500" → 返回 500, 输入 "auto" → 返回 null.
     *
     * @param maxTokens 最大令牌数字符串, 可能为 null, 空字符串或 "auto", 或包含数字及单位后缀的字符串
     * @return 解析后的整数最大令牌数, 若解析失败或输入无效则返回 null
     */
    private static @Nullable Integer parseMaxTokens(@Nullable String maxTokens) {
        if (maxTokens == null || maxTokens.isBlank() || "auto".equalsIgnoreCase(maxTokens.trim())) {
            return null;
        }
        String value = maxTokens.trim();
        try {
            if (value.endsWith("K") || value.endsWith("k")) {
                double inK = Double.parseDouble(value.substring(0, value.length() - 1));
                return (int) Math.max(100, Math.round(inK * 1000));
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.gemini;

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
 * Gemini Provider（Google Generative Language API）
 * <p>
 * 默认使用 AI Studio Key（x-goog-api-key）认证。
 */
@Slf4j
public class GeminLikeiProvider extends AICompatibleProvider {

    /**
     * 初始化 Gemini 兼容提供者实例
     * <p> 用于创建一个支持 Gemini API 的 AI 服务提供者, 继承自 AICompatibleProvider 类.
     * 该构造函数接收项目, 配置, 模型参数和运行时设置等必要参数, 以初始化底层服务连接.
     *
     * @param project         项目上下文, 用于日志输出和调试
     * @param config          服务配置信息, 包括提供商类型, 基础 URL, 模型名称等
     * @param modelParameters 模型参数, 如温度,Top-P,Top-K, 最大令牌数等
     * @param runtimeSettings 运行时设置, 包括重试次数, 超时时间等
     */
    public GeminLikeiProvider(@NotNull Project project,
                              @NotNull AIProviderConfig config,
                              @NotNull AIModelParameters modelParameters,
                              @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 根据聊天请求生成内容
     * <p> 通过调用 Gemini API 服务, 根据指定的聊天请求生成响应内容. 支持重试机制, 若请求失败则按指数退避策略重试, 最多重试次数由运行时配置决定.
     * <p> 在调用前会打印模型信息 (供应商, 模型名称,Base URL), 并验证 API 密钥是否配置.
     *
     * @param request  聊天请求对象, 包含用户提示, 系统提示等信息
     * @param apiKey   API 密钥, 可为空, 若服务需要密钥且未提供则抛出异常
     * @param listener 响应监听器, 用于接收请求, 响应, 使用情况等回调, 可为空
     * @return 生成的文本内容
     * @throws AIServiceException 当配置错误, 网络错误, 服务不可用或响应解析失败时抛出
     * @since 1.0.0
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
                return sendGenerateContent(buildGenerateContentBody(request), apiKey, listener, false);
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
     * <p> 通过 Gemini API 的 SSE(Server-Sent Events) 协议, 逐块接收并传递生成内容, 适用于大模型响应流式输出场景.
     * <p> 请求前会验证是否需要 API 密钥, 若未提供则抛出配置异常.
     *
     * @param request  AI 聊天请求对象, 包含用户提示, 系统提示等信息
     * @param apiKey   API 密钥, 可为空, 若提供商要求密钥且未提供则抛出异常
     * @param listener 流式响应监听器, 用于接收响应开始, 分块内容, 完成及错误事件
     * @throws AIServiceException 当发生 HTTP 错误, 网络错误或响应解析失败时抛出, 错误码根据具体异常类型映射
     */
    @Override
    public void generateContentStream(@NotNull AIChatRequest request,
                                      @Nullable String apiKey,
                                      @NotNull AIStreamResponseListener listener) throws AIServiceException {
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置", AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String url = buildModelUrl(":streamGenerateContent?alt=sse");
        JsonObject body = buildGenerateContentBody(request);
        String requestBody = body.toString();

        try {
            listener.onStart();
            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;
            HttpRequests.post(url, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    tuneGeminiConnection(conn, apiKey);
                    conn.setRequestProperty("Accept", "text/event-stream");
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(requestAdapter -> {
                    requestAdapter.write(requestBody);
                    HttpURLConnection connection = (HttpURLConnection) requestAdapter.getConnection();
                    readGeminiSse(connection, listener);
                    return null;
                });
        } catch (HttpRequests.HttpStatusException e) {
            AIServiceException.ErrorCode code = AICompatibleProvider.mapHttpError(e.getStatusCode());
            String msg = "Gemini HTTP 错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, code, e);
        } catch (IOException e) {
            String msg = "Gemini 网络错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 验证当前 AI 服务配置是否有效
     * <p> 通过发送一个简单的 ping-pong 请求测试与 AI 服务的连接状态, 验证基础配置是否正确.
     * 若连接成功且服务返回非空响应, 则视为配置有效; 否则返回失败结果.
     *
     * @param apiKey 可选的 API 密钥, 用于认证请求. 若服务提供商需要密钥且未提供, 则会抛出配置异常.
     * @return 验证结果对象, 包含成功或失败状态, 消息及可选异常信息
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
            String response = sendGenerateContent(buildGenerateContentBody(request), apiKey, null, true);
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
     * <p> 通过向 Gemini 服务的模型列表接口发起请求, 获取当前配置下支持的模型名称列表.
     * 若请求失败或返回空列表, 则返回当前提供者默认支持的模型列表.
     *
     * @param apiKey API 密钥, 若为 null 或空字符串, 则提示需要密钥并返回空列表
     * @return 可用模型名称列表, 若请求失败则返回默认支持的模型列表
     * @since 1.0
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== Gemini 获取模型列表 ===");
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            AIConsoleLoggerUtil.printWarning(project, "需要 API Key 但未提供，返回空列表");
            return new ArrayList<>();
        }

        String url = config.baseUrl + "/models";
        try {
            String responseBody = HttpRequests.request(url)
                .tuner(connection -> tuneGeminiConnection((HttpURLConnection) connection, apiKey))
                .connect(HttpRequests.Request::readString);
            List<String> models = parseGeminiModelsResponse(responseBody);
            if (!models.isEmpty()) {
                AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
                return models;
            }
        } catch (Exception e) {
            log.debug("Failed to fetch Gemini models", e);
        }

        return new ArrayList<>(getProviderType().getSupportedModels());
    }

    /**
     * 构建模型请求的完整 URL
     * <p> 根据配置的模型名称和后缀拼接成完整的模型请求地址, 自动去除模型名称前缀 "models/"</p>
     * <p> 示例: 若 config.modelName 为 "models/gemini-pro",suffix 为 "/generateContent",
     * 则返回 "<a href="https://api.example.com/models/gemini-pro/generateContent">...</a>"</p>
     *
     * @param suffix 请求路径后缀, 例如 ":generateContent" 或 ":streamGenerateContent?alt=sse"
     * @return 完整的模型请求 URL 字符串
     */
    private String buildModelUrl(@NotNull String suffix) {
        // Gemini: POST {baseUrl}/models/{model}:{method}
        String modelName = config.modelName != null ? config.modelName.trim() : "";
        if (modelName.startsWith("models/")) {
            modelName = modelName.substring("models/".length());
        }
        return config.baseUrl + "/models/" + modelName + suffix;
    }

    /**
     * 配置 Gemini API 连接参数
     * <p> 设置 HTTP 连接的超时时间, 并添加 API 密钥认证头 (如提供).
     * <p> 连接超时时间由运行时设置中的超时毫秒数决定, 读取超时为连接超时的两倍.
     * <p> 若提供 API 密钥, 则将其作为请求头 <code>x-goog-api-key</code> 发送.
     *
     * @param connection 需要配置的 HttpURLConnection 实例
     * @param apiKey     可选的 API 密钥, 用于认证, 若为空或空白则不设置请求头
     */
    private void tuneGeminiConnection(@NotNull HttpURLConnection connection, @Nullable String apiKey) {
        int timeoutMillis = runtimeSettings.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        if (apiKey != null && !apiKey.isBlank()) {
            connection.setRequestProperty("x-goog-api-key", apiKey.trim());
        }
    }

    /**
     * 构建用于生成内容的请求体 JSON 对象
     * <p> 根据传入的 AI 聊天请求, 构造符合 Gemini API 格式的请求体, 包含系统指令, 用户输入内容及生成配置参数.
     * <pre>{@code
     * {*   "systemInstruction": { "parts": [{ "text": "系统提示内容"}] },
     *   "contents": [
     *     {
     *       "role": "user",
     *       "parts": [{"text": "用户输入内容"}]
     *     }
     *   ],
     *   "generationConfig": {
     *     "temperature": 0.7,
     *     "topP": 0.9,
     *     "topK": 40,
     *     "maxOutputTokens": 500
     *   }
     * }
     * }</pre>
     *
     * @param request AI 聊天请求对象, 包含系统提示和用户输入内容
     * @return 构造完成的 JSON 请求体对象
     */
    private JsonObject buildGenerateContentBody(@NotNull AIChatRequest request) {
        JsonObject body = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", request.systemPrompt());
        sysParts.add(sysPart);
        systemInstruction.add("parts", sysParts);
        body.add("systemInstruction", systemInstruction);

        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", request.userPrompt());
        parts.add(textPart);
        userContent.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(userContent);
        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        AIModelParameters params = modelParameters;
        Double temperature = parseDouble(params.temperature);
        if (temperature != null) {
            generationConfig.addProperty("temperature", temperature);
        }
        Double topP = parseDouble(params.topP);
        if (topP != null) {
            generationConfig.addProperty("topP", topP);
        }
        Integer topK = parseInt(params.topK);
        if (topK != null) {
            generationConfig.addProperty("topK", topK);
        }
        Integer maxTokens = parseMaxTokens(params.maxTokens);
        if (maxTokens != null) {
            generationConfig.addProperty("maxOutputTokens", maxTokens);
        }
        if (!generationConfig.isEmpty()) {
            body.add("generationConfig", generationConfig);
        }
        return body;
    }

    /**
     * 发送生成内容请求并获取响应
     * <p> 通过 HTTP POST 请求调用 Gemini 模型生成内容接口, 支持同步和验证模式.
     * 在验证模式下, 仅返回 "OK" 表示连接成功; 在非验证模式下, 解析并返回模型生成的文本内容.
     * 请求过程中会调用监听器记录请求和响应信息, 若发生异常则抛出 AIServiceException.
     *
     * @param body       请求体内容, 包含模型输入参数 (如系统指令, 用户提示等)
     * @param apiKey     API 密钥, 用于认证 (可为空, 但若提供商要求则必须提供)
     * @param listener   响应监听器, 用于记录请求 / 响应内容或使用情况 (可为空)
     * @param validation 是否为验证模式, 若为 true 则仅返回 "OK", 不解析响应内容
     * @return 非验证模式下返回模型生成的文本内容; 验证模式下返回 "OK"
     * @throws AIServiceException 当 HTTP 错误, 网络异常或响应解析失败时抛出
     */
    private String sendGenerateContent(@NotNull JsonObject body,
                                       @Nullable String apiKey,
                                       @Nullable AIResponseListener listener,
                                       boolean validation) throws AIServiceException {
        String url = buildModelUrl(":generateContent");
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
                    tuneGeminiConnection(conn, apiKey);
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
            return parseGenerateContentResponse(responseBody, listener);
        } catch (HttpRequests.HttpStatusException e) {
            throw new AIServiceException("Gemini HTTP 错误: " + e.getMessage(), AICompatibleProvider.mapHttpError(e.getStatusCode()), e);
        } catch (IOException e) {
            throw new AIServiceException("Gemini 网络错误: " + e.getMessage(), AIServiceException.ErrorCode.NETWORK_ERROR, e);
        } catch (Exception e) {
            throw new AIServiceException("Gemini 响应解析失败", AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

    /**
     * 解析 Gemini 模型生成内容的响应体
     * <p> 从 Gemini API 返回的 JSON 响应中提取文本内容, 并可选地记录 token 使用情况
     *
     * @param responseBody Gemini API 返回的原始响应体 (JSON 格式字符串)
     * @param listener     可选的响应监听器, 用于在解析过程中回调 token 使用信息
     * @return 解析出的生成文本内容, 去除首尾空白字符
     * @throws AIServiceException 当响应中包含错误, 无 candidates,candidates 为空或响应内容为空时抛出
     */
    private String parseGenerateContentResponse(@NotNull String responseBody, @Nullable AIResponseListener listener) throws AIServiceException {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (json.has("error")) {
            throw new AIServiceException(json.getAsJsonObject("error").toString(), AIServiceException.ErrorCode.INVALID_RESPONSE);
        }
        if (!json.has("candidates") || !json.get("candidates").isJsonArray()) {
            throw new AIServiceException("Gemini 返回无 candidates", AIServiceException.ErrorCode.INVALID_RESPONSE);
        }
        JsonArray candidates = json.getAsJsonArray("candidates");
        if (candidates.isEmpty()) {
            throw new AIServiceException("Gemini 返回空 candidates", AIServiceException.ErrorCode.INVALID_RESPONSE);
        }

        String text = extractTextFromCandidate(candidates.get(0).getAsJsonObject());
        if (text == null || text.isBlank()) {
            throw new AIServiceException("Gemini 返回空响应", AIServiceException.ErrorCode.INVALID_RESPONSE);
        }

        if (json.has("usageMetadata") && json.get("usageMetadata").isJsonObject() && listener != null) {
            JsonObject usage = json.getAsJsonObject("usageMetadata");
            int promptTokens = usage.has("promptTokenCount") ? usage.get("promptTokenCount").getAsInt() : 0;
            int completionTokens = usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").getAsInt() : 0;
            int totalTokens = usage.has("totalTokenCount") ? usage.get("totalTokenCount").getAsInt() : (promptTokens + completionTokens);
            listener.onUsage(getProviderType().getDisplayName(), config.modelName, promptTokens, completionTokens, totalTokens);
        }
        return text.trim();
    }

    /**
     * 从 Gemini 服务器的 SSE 流中读取并解析响应内容
     * <p> 该方法用于处理流式响应, 逐行读取服务器发送的事件数据 (Event Stream), 并提取其中的文本片段, 逐步拼接完整响应内容.
     * 当收到 "[DONE]" 标记或解析失败时, 停止读取并调用监听器的 {@code onComplete} 方法.
     *
     * @param connection 与 Gemini 服务建立的 HTTP 连接, 必须为 SSE 流式响应
     * @param listener   用于接收流式响应事件的监听器, 包括 {@code onChunk} 和 {@code onComplete} 回调
     * @throws IOException 当网络读取或解析过程中发生 I/O 错误时抛出
     *
     *                     <p> 示例响应格式 (SSE):
     *                     <pre>{@code
     *                                         data: {"candidates": [{"content": {"parts": [{"text": "Hello"}]}}]}
     *                                         data: {"candidates": [{"content": {"parts": [{"text": "World"}]}}]}
     *                                         data: [DONE]
     *                                         }</pre>
     */
    private void readGeminiSse(@NotNull HttpURLConnection connection, @NotNull AIStreamResponseListener listener) throws IOException {
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
                if (json.has("error")) {
                    listener.onError(json.get("error").toString(), null);
                    break;
                }
                if (json.has("candidates") && json.get("candidates").isJsonArray()) {
                    JsonArray candidates = json.getAsJsonArray("candidates");
                    if (!candidates.isEmpty() && candidates.get(0).isJsonObject()) {
                        String chunk = extractTextFromCandidate(candidates.get(0).getAsJsonObject());
                        if (chunk != null && !chunk.isEmpty()) {
                            fullText.append(chunk);
                            listener.onChunk(chunk);
                        }
                    }
                }
            }
        }
        listener.onComplete(fullText.toString());
    }

    /**
     * 从候选内容中提取纯文本内容
     * <p> 遍历候选内容中的各个部分, 提取所有包含 "text" 字段的文本内容并拼接成字符串.
     * 如果候选内容结构不符合预期 (如缺少 content 或 parts 字段), 则返回 null.
     *
     * @param candidate 非空的 JSON 对象, 表示 AI 响应中的一个候选内容
     * @return 提取的文本内容字符串, 如果无有效文本或结构异常则返回 null
     */
    private static @Nullable String extractTextFromCandidate(@NotNull JsonObject candidate) {
        if (!candidate.has("content") || !candidate.get("content").isJsonObject()) {
            return null;
        }
        JsonObject content = candidate.getAsJsonObject("content");
        if (!content.has("parts") || !content.get("parts").isJsonArray()) {
            return null;
        }
        JsonArray parts = content.getAsJsonArray("parts");
        StringBuilder sb = new StringBuilder();
        for (JsonElement partEl : parts) {
            if (!partEl.isJsonObject()) {
                continue;
            }
            JsonObject part = partEl.getAsJsonObject();
            if (part.has("text")) {
                sb.append(part.get("text").getAsString());
            }
        }
        return sb.toString();
    }

    /**
     * 安全解析 JSON 字符串为对象
     * <p> 尝试将传入的 JSON 字符串解析为 {@link JsonObject}, 若解析失败或非对象类型则返回 null
     *
     * @param data 待解析的 JSON 字符串
     * @return 解析成功时返回 {@link JsonObject}, 否则返回 null
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
     * 解析 Gemini 模型列表响应
     * <p> 从 JSON 响应体中提取可用模型名称列表, 过滤无效或空名称, 并去除前缀 "models/".
     * <p> 若解析失败, 将在日志中记录调试信息, 但不抛出异常, 返回空列表.
     *
     * @param responseBody 服务器返回的 JSON 响应体
     * @return 解析出的模型名称列表, 若解析失败或无模型则返回空列表
     */
    private List<String> parseGeminiModelsResponse(@NotNull String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("models") && json.get("models").isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray("models")) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("name")) {
                        continue;
                    }
                    String name = obj.get("name").getAsString();
                    if (name == null || name.isBlank()) {
                        continue;
                    }
                    // name 示例：models/gemini-1.5-pro
                    if (name.startsWith("models/")) {
                        name = name.substring("models/".length());
                    }
                    models.add(name.trim());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse Gemini models response", e);
        }
        return models;
    }

    /**
     * 将字符串值解析为双精度浮点数
     * <p> 如果输入值为 null, 空白字符串或等于 "auto"(不区分大小写), 则返回 null; 否则尝试解析为 Double 类型, 解析失败时也返回 null.
     *
     * @param value 待解析的字符串值, 可能为 null 或空白
     * @return 解析后的 Double 值, 若解析失败或输入无效则返回 null
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
     * 将字符串值转换为整数, 若转换失败或值为 null, 空字符串或 "auto", 则返回 null
     * <p> 该方法用于安全地解析字符串为整数, 忽略格式异常并返回 null 以表示无效输入.
     *
     * @param value 待转换的字符串值, 可能为 null, 空字符串或 "auto"
     * @return 解析后的整数值, 若解析失败或输入无效则返回 null
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
     * 解析最大令牌数参数
     * <p> 将传入的字符串参数转换为整数形式的最大令牌数, 支持带单位 "K" 或 "k" 的格式 (如 "2K" 表示 2000 令牌), 若参数为 null, 空字符串或 "auto", 则返回 null.
     * <p> 若解析失败或格式不合法, 也返回 null.
     *
     * @param maxTokens 最大令牌数的字符串表示, 可为 null, 空字符串,"auto" 或数字字符串, 如 "2K","1000"
     * @return 解析后的整数最大令牌数, 若解析失败或参数无效则返回 null
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

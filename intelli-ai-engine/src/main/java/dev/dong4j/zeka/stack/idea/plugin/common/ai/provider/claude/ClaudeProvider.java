package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.claude;

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
public class ClaudeProvider extends AICompatibleProvider {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public ClaudeProvider(@NotNull Project project,
                          @NotNull AIProviderConfig config,
                          @NotNull AIModelParameters modelParameters,
                          @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

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
            AIServiceException.ErrorCode code = mapHttpError(e.getStatusCode());
            String msg = "Claude HTTP 错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, code, e);
        } catch (IOException e) {
            String msg = "Claude 网络错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

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
            List<String> models = parseModelsResponse(responseBody);
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

    private void tuneAnthropicConnection(@NotNull HttpURLConnection connection, @Nullable String apiKey) {
        int timeoutMillis = runtimeSettings.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
        if (apiKey != null && !apiKey.isBlank()) {
            connection.setRequestProperty("x-api-key", apiKey.trim());
        }
    }

    private static AIServiceException.ErrorCode mapHttpError(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> AIServiceException.ErrorCode.INVALID_API_KEY;
            case 408 -> AIServiceException.ErrorCode.TIMEOUT;
            case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
            case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
            default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
        };
    }

    private JsonObject buildMessagesRequestBody(@NotNull AIChatRequest request, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.modelName);
        body.addProperty("stream", stream);

        String systemPrompt = request.systemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
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
            throw new AIServiceException("Claude HTTP 错误: " + e.getMessage(), mapHttpError(e.getStatusCode()), e);
        } catch (IOException e) {
            throw new AIServiceException("Claude 网络错误: " + e.getMessage(), AIServiceException.ErrorCode.NETWORK_ERROR, e);
        } catch (Exception e) {
            throw new AIServiceException("Claude 响应解析失败", AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

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

    private static @Nullable JsonObject safeParseJson(@NotNull String data) {
        try {
            JsonElement el = JsonParser.parseString(data);
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> parseModelsResponse(@NotNull String responseBody) {
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


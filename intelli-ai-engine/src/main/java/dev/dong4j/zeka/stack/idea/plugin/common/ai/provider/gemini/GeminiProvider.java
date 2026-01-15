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
public class GeminiProvider extends AICompatibleProvider {

    public GeminiProvider(@NotNull Project project,
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
            AIServiceException.ErrorCode code = mapHttpError(e.getStatusCode());
            String msg = "Gemini HTTP 错误: " + e.getMessage();
            listener.onError(msg, e);
            throw new AIServiceException(msg, code, e);
        } catch (IOException e) {
            String msg = "Gemini 网络错误: " + e.getMessage();
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
            List<String> models = parseModelsResponse(responseBody);
            if (!models.isEmpty()) {
                AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
                return models;
            }
        } catch (Exception e) {
            log.debug("Failed to fetch Gemini models", e);
        }

        return new ArrayList<>(getProviderType().getSupportedModels());
    }

    private String buildModelUrl(@NotNull String suffix) {
        // Gemini: POST {baseUrl}/models/{model}:{method}
        String modelName = config.modelName != null ? config.modelName.trim() : "";
        if (modelName.startsWith("models/")) {
            modelName = modelName.substring("models/".length());
        }
        return config.baseUrl + "/models/" + modelName + suffix;
    }

    private void tuneGeminiConnection(@NotNull HttpURLConnection connection, @Nullable String apiKey) {
        int timeoutMillis = runtimeSettings.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        if (apiKey != null && !apiKey.isBlank()) {
            connection.setRequestProperty("x-goog-api-key", apiKey.trim());
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
        if (generationConfig.size() > 0) {
            body.add("generationConfig", generationConfig);
        }
        return body;
    }

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
            throw new AIServiceException("Gemini HTTP 错误: " + e.getMessage(), mapHttpError(e.getStatusCode()), e);
        } catch (IOException e) {
            throw new AIServiceException("Gemini 网络错误: " + e.getMessage(), AIServiceException.ErrorCode.NETWORK_ERROR, e);
        } catch (Exception e) {
            throw new AIServiceException("Gemini 响应解析失败", AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

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

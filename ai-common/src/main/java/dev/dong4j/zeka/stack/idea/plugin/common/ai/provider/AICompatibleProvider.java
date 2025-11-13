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
import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * OpenAI 兼容的服务提供商抽象类。
 */
public abstract class AICompatibleProvider implements AIServiceProvider {

    private static final Logger LOG = Logger.getInstance(AICompatibleProvider.class);

    protected final AIProviderConfig config;
    protected final AIModelParameters modelParameters;
    protected final AIRuntimeSettings runtimeSettings;

    protected AICompatibleProvider(@NotNull AIProviderConfig config,
                                   @NotNull AIModelParameters modelParameters,
                                   @NotNull AIRuntimeSettings runtimeSettings) {
        this.config = config.copy();
        this.config.baseUrl = normalizeBaseUrl(this.config.baseUrl);
        this.modelParameters = modelParameters.copy();
        this.runtimeSettings = runtimeSettings.copy();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    @NotNull
    public AIProviderType getProviderType() {
        return config.providerType;
    }

    @Override
    @NotNull
    public String getModelName() {
        return config.modelName;
    }

    @Override
    @NotNull
    public String getBaseUrl() {
        return config.baseUrl;
    }

    @Override
    @NotNull
    public String generateContent(@NotNull AIChatRequest request,
                                  @Nullable String apiKey,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        AIRuntimeSettings runtime = runtimeSettings;
        int attempts = 0;
        AIServiceException lastException = null;
        while (attempts < Math.max(1, runtime.maxRetries)) {
            try {
                return sendRequest(buildRequestBody(request), apiKey, listener, request.promptTokenEstimate(), false);
            } catch (AIServiceException e) {
                lastException = e;
                attempts++;
                if (!e.isRetryable() || attempts >= Math.max(1, runtime.maxRetries)) {
                    break;
                }
                long waitTime = (long) (runtime.waitDuration * Math.pow(2, attempts - 1));
                LOG.info("AI request failed, retry in " + waitTime + "ms: " + e.getMessage());
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
        throw lastException != null ? lastException :
              new AIServiceException("AI 服务调用失败", AIServiceException.ErrorCode.UNKNOWN_ERROR);
    }

    @Override
    @NotNull
    public ValidationResult validateConfiguration(@Nullable String apiKey) {
        try {
            String response = sendRequest(buildValidationRequestBody(), apiKey, null, 0, true);
            if (!response.isEmpty()) {
                return ValidationResult.success("连接成功！提供商: " + getProviderType().getDisplayName() +
                                                ", 模型: " + getModelName());
            }
            return ValidationResult.failure("连接失败：服务返回空响应");
        } catch (AIServiceException e) {
            return ValidationResult.failure("配置验证失败", AIServiceException.build(e));
        } catch (Exception e) {
            String details = e.getMessage();
            if (details == null || details.isEmpty()) {
                details = e.getClass().getSimpleName();
            }
            return ValidationResult.failure("配置验证异常", details, e);
        }
    }

    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        try {
            if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
                return new ArrayList<>();
            }

            String url = config.baseUrl + "/models";
            String responseBody = HttpRequests.request(url)
                .connect(request -> {
                    HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                    tuneConnection(connection, apiKey);
                    return request.readString();
                });

            if (!responseBody.trim().isEmpty()) {
                return parseModelsResponse(responseBody);
            }

            return new ArrayList<>();
        } catch (IOException e) {
            LOG.info("Network error while fetching models", e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.info("Unexpected error while fetching models", e);
            return new ArrayList<>();
        }
    }

    private boolean requiresApiKey() {
        return config.providerType.requiresApiKey();
    }

    private JsonObject buildRequestBody(AIChatRequest request) {
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
        body.addProperty("think", false);
        body.addProperty("enable_thinking", false);
        body.addProperty("stream", false);
        body.add("messages", messagesArray);

        AIModelParameters params = modelParameters;
        body.addProperty("temperature", params.temperature);
        body.addProperty("max_tokens", params.maxTokens);
        body.addProperty("top_p", params.topP);
        body.addProperty("top_k", params.topK);
        body.addProperty("presence_penalty", params.presencePenalty);

        return body;
    }

    private JsonObject buildValidationRequestBody() {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "i say ping, you say pong");

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "ping");

        JsonArray messagesArray = new JsonArray();
        messagesArray.add(systemMessage);
        messagesArray.add(userMessage);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.modelName);
        body.addProperty("think", false);
        body.addProperty("enable_thinking", false);
        body.addProperty("stream", false);
        body.add("messages", messagesArray);
        body.addProperty("temperature", 0.1);
        body.addProperty("max_tokens", 32);
        body.addProperty("top_p", 0.9);
        return body;
    }

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
            String responseBody = HttpRequests.post(url, "application/json")
                .connect(request -> {
                    HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                    tuneConnection(connection, apiKey);
                    request.write(requestBody);
                    return request.readString();
                });

            logResponse(listener, responseBody, validation);

            if (!responseBody.trim().isEmpty()) {
                String result = validation ? parseValidationResponse(responseBody) : parseResponse(responseBody, listener);
                if (runtimeSettings.verboseLogging) {
                    LOG.debug("AI response length: " + result.length());
                }
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
            throw new AIServiceException("网络错误: " + e.getMessage(),
                                         AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private void logRequest(@Nullable AIResponseListener listener, String requestBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onRequest(getProviderType().getDisplayName(), getModelName(), requestBody, false);
        }

        if (runtimeSettings.verboseLogging) {
            LOG.trace("=== AI Request ===");
            LOG.trace(requestBody);
        }
    }

    private void logResponse(@Nullable AIResponseListener listener, String responseBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onResponse(getProviderType().getDisplayName(), getModelName(), responseBody, false);
        }

        if (runtimeSettings.verboseLogging) {
            LOG.trace("=== AI Response ===");
            LOG.trace(responseBody);
        }
    }

    private void tuneConnection(HttpURLConnection connection, @Nullable String apiKey) {
        AIRuntimeSettings runtime = runtimeSettings;
        connection.setConnectTimeout(runtime.timeout);
        connection.setReadTimeout(runtime.timeout * 2);
        if (requiresApiKey() && apiKey != null) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
    }

    private String parseResponse(String responseBody, @Nullable AIResponseListener listener) throws AIServiceException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

            if (listener != null && json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
                int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
                int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : 0;
                if (runtimeSettings.verboseLogging) {
                    listener.onUsage(getProviderType().getDisplayName(), getModelName(), promptTokens, completionTokens, totalTokens);
                }
            }

            return filterThinkingContent(content);
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

    private String parseValidationResponse(String responseBody) throws AIServiceException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                if (usage.has("completion_tokens") && usage.get("completion_tokens").getAsInt() > 0) {
                    if (runtimeSettings.verboseLogging) {
                        LOG.debug("Validation successful: response has choices");
                    }
                    return "OK";
                }
            }

            if (json.has("choices") && !json.getAsJsonArray("choices").isEmpty()) {
                if (runtimeSettings.verboseLogging) {
                    LOG.debug("Validation successful: response has choices");
                }
                return "OK";
            }

            throw new AIServiceException("No completion tokens found in response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE);
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse validation response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

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

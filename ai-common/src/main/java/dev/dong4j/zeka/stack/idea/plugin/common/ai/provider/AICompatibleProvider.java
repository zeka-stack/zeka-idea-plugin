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
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
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
    @Nullable
    protected final AIConsoleLogger consoleLogger;
    protected final boolean performanceMode;

    protected AICompatibleProvider(@NotNull AIProviderConfig config,
                                   @NotNull AIModelParameters modelParameters,
                                   @NotNull AIRuntimeSettings runtimeSettings) {
        this(config, modelParameters, runtimeSettings, null, false);
    }

    protected AICompatibleProvider(@NotNull AIProviderConfig config,
                                   @NotNull AIModelParameters modelParameters,
                                   @NotNull AIRuntimeSettings runtimeSettings,
                                   @Nullable AIConsoleLogger consoleLogger) {
        this(config, modelParameters, runtimeSettings, consoleLogger, false);
    }

    protected AICompatibleProvider(@NotNull AIProviderConfig config,
                                   @NotNull AIModelParameters modelParameters,
                                   @NotNull AIRuntimeSettings runtimeSettings,
                                   @Nullable AIConsoleLogger consoleLogger,
                                   boolean performanceMode) {
        this.config = config.copy();
        this.config.baseUrl = normalizeBaseUrl(this.config.baseUrl);
        this.modelParameters = modelParameters.copy();
        this.runtimeSettings = runtimeSettings.copy();
        this.consoleLogger = consoleLogger;
        this.performanceMode = performanceMode;
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
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printWithTimestamp("=== 开始生成内容 ===");
            // 模型相关信息一行输出
            consoleLogger.print(String.format("模型信息: 供应商=%s | 模型=%s | Base URL=%s",
                                              getProviderType().getDisplayName(), getModelName(), getBaseUrl()));
            // 当前开启的配置
            StringBuilder configInfo = new StringBuilder("当前配置: ");
            if (performanceMode) {
                configInfo.append("性能模式✓");
            }
            if (runtimeSettings.verboseLogging) {
                if (configInfo.length() > "当前配置: ".length()) {
                    configInfo.append(" | ");
                }
                configInfo.append("详细日志✓");
            }
            if (configInfo.length() == "当前配置: ".length()) {
                configInfo.append("默认配置");
            }
            consoleLogger.print(configInfo.toString());
        }

        AIRuntimeSettings runtime = runtimeSettings;
        int attempts = 0;
        while (attempts < Math.max(1, runtime.maxRetries)) {
            try {
                String result = sendRequest(buildRequestBody(request), apiKey, listener, request.promptTokenEstimate(), false);
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.printSuccess("=== 内容生成成功 ===");
                    consoleLogger.print("响应长度: " + result.length() + " 字符");
                }
                return result;
            } catch (AIServiceException e) {
                attempts++;
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.printWarning("请求失败 (尝试 " + attempts + "/" + Math.max(1, runtime.maxRetries) + "): " + e.getMessage());
                }
                if (!e.isRetryable() || attempts >= Math.max(1, runtime.maxRetries)) {
                    break;
                }
                long waitTime = (long) (runtime.waitDuration * Math.pow(2, attempts - 1));
                LOG.info("AI request failed, retry in " + waitTime + "ms: " + e.getMessage());
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.print("等待 " + waitTime + "ms 后重试...");
                }
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
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printError("=== 内容生成失败 ===");
        }
        throw new AIServiceException("AI 服务调用失败", AIServiceException.ErrorCode.UNKNOWN_ERROR);

    }

    @Override
    @NotNull
    public ValidationResult validateConfiguration(@Nullable String apiKey) {
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printWithTimestamp("=== 开始验证配置 ===");
            consoleLogger.print("提供商: " + getProviderType().getDisplayName());
            consoleLogger.print("模型: " + getModelName());
            consoleLogger.print("Base URL: " + getBaseUrl());
        }
        try {
            AIChatRequest request = new AIChatRequest("i say ping, you say pong",
                                                      "ping", 0);
            String response = sendRequest(buildRequestBody(request), apiKey, null, 0, true);
            if (!response.isEmpty()) {
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.printSuccess("=== 配置验证成功 ===");
                }
                return ValidationResult.success("连接成功！提供商: " + getProviderType().getDisplayName() +
                                                ", 模型: " + getModelName());
            }
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printError("配置验证失败：服务返回空响应");
            }
            return ValidationResult.failure("连接失败：服务返回空响应");
        } catch (AIServiceException e) {
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printError("配置验证失败: " + e.getMessage());
            }
            return ValidationResult.failure("配置验证失败", AIServiceException.build(e));
        } catch (Exception e) {
            String details = e.getMessage();
            if (details == null || details.isEmpty()) {
                details = e.getClass().getSimpleName();
            }
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printError("配置验证异常: " + details);
            }
            return ValidationResult.failure("配置验证异常", details, e);
        }
    }

    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printWithTimestamp("=== 开始获取可用模型列表 ===");
            consoleLogger.print("提供商: " + getProviderType().getDisplayName());
            consoleLogger.print("Base URL: " + config.baseUrl);
        }
        try {
            if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.printWarning("需要 API Key 但未提供，返回空列表");
                }
                return new ArrayList<>();
            }

            String url = config.baseUrl + "/models";
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.print("请求 URL: " + url);
            }
            String responseBody = HttpRequests.request(url)
                .tuner(connection -> {
                    tuneConnection((HttpURLConnection) connection, apiKey);
                })
                .connect(HttpRequests.Request::readString);

            if (!responseBody.trim().isEmpty()) {
                List<String> models = parseModelsResponse(responseBody);
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.printSuccess("成功获取 " + models.size() + " 个模型");
                    if (models.size() > 0 && models.size() <= 10) {
                        models.forEach(model -> consoleLogger.print("  - " + model));
                    }
                }
                return models;
            }

            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printWarning("服务返回空响应");
            }
            return new ArrayList<>();
        } catch (IOException e) {
            LOG.info("Network error while fetching models", e);
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printError("网络错误: " + e.getMessage());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.info("Unexpected error while fetching models", e);
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.printError("获取模型列表失败: " + e.getMessage());
            }
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
            if (consoleLogger != null) {
                consoleLogger.print("=== AI 请求 ===");
                consoleLogger.print(requestBody);
            }
        }
    }

    private void logResponse(@Nullable AIResponseListener listener, String responseBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onResponse(getProviderType().getDisplayName(), getModelName(), responseBody, false);
        }

        if (runtimeSettings.verboseLogging) {
            LOG.trace("=== AI Response ===");
            LOG.trace(responseBody);
            if (consoleLogger != null) {
                consoleLogger.print("=== AI 响应 ===");
                consoleLogger.print(responseBody);
            }
        }
    }

    private void tuneConnection(HttpURLConnection connection, @Nullable String apiKey) {
        AIRuntimeSettings runtime = runtimeSettings;
        connection.setConnectTimeout(runtime.timeout);
        connection.setReadTimeout(runtime.timeout * 2);
        if (requiresApiKey() && apiKey != null) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            if (consoleLogger != null && runtimeSettings.verboseLogging) {
                consoleLogger.print("已设置 Authorization 头");
            }
        }
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.print("连接超时: " + runtime.timeout + "ms");
            consoleLogger.print("读取超时: " + (runtime.timeout * 2) + "ms");
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

            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
                int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
                int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : 0;

                // 输出 token 消耗情况，使用 | 分隔，一行输出
                if (consoleLogger != null && runtimeSettings.verboseLogging) {
                    consoleLogger.print(String.format("Token 消耗: Prompt=%d | Completion=%d | Total=%d",
                                                      promptTokens, completionTokens, totalTokens));
                }

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

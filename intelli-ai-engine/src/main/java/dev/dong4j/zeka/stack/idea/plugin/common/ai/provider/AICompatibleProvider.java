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
 * AI 兼容提供者抽象类
 * <p>
 * 该类是所有 AI 服务提供者的基类, 用于封装通用的 AI 服务调用逻辑, 包括内容生成, 配置验证, 模型获取等功能. 它支持多种 AI 服务的统一接入, 并提供了日志记录, 重试机制, 请求构建和响应解析等核心能力.
 * <p>
 * 该类实现了 {@link AIServiceProvider} 接口, 为具体的 AI 服务提供者 (如 OpenAI, 通义千问等) 提供统一的抽象接口, 便于扩展和维护.
 *
 * @author 作者名
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@SuppressWarnings("D")
public abstract class AICompatibleProvider implements AIServiceProvider {

    /**
     * 日志记录器, 用于记录 AI 兼容提供者相关的日志信息
     */
    private static final Logger LOG = Logger.getInstance(AICompatibleProvider.class);

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
     * 控制台日志记录器
     * <p>
     * 用于输出 AI 相关的日志信息, 可能为 null
     *
     * @see AIConsoleLogger
     */
    @Nullable
    protected final AIConsoleLogger consoleLogger;

    /**
     * 初始化 AI 兼容性提供者
     * <p>
     * 使用提供的配置, 模型参数, 运行时设置和控制台日志记录器初始化 AI 兼容性提供者.
     *
     * @param config          AI 提供者配置对象, 不可为 null
     * @param modelParameters 模型参数对象, 不可为 null
     * @param runtimeSettings 运行时设置对象, 不可为 null
     * @param consoleLogger   控制台日志记录器, 可为 null
     */
    protected AICompatibleProvider(@NotNull AIProviderConfig config,
                                   @NotNull AIModelParameters modelParameters,
                                   @NotNull AIRuntimeSettings runtimeSettings,
                                   @Nullable AIConsoleLogger consoleLogger) {
        this.config = config.copy();
        this.config.baseUrl = normalizeBaseUrl(this.config.baseUrl);
        this.modelParameters = modelParameters.copy();
        this.runtimeSettings = runtimeSettings.copy();
        this.consoleLogger = consoleLogger;
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
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.printWithTimestamp("=== 开始生成内容 ===");
            // 模型相关信息一行输出
            consoleLogger.print(String.format("模型信息: 供应商=%s | 模型=%s | Base URL=%s",
                                              getProviderType().getDisplayName(), getModelName(), getBaseUrl()));
            // 当前开启的配置
            StringBuilder configInfo = new StringBuilder("当前配置: ");
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
                    if (!models.isEmpty() && models.size() <= 10) {
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
     *
     * @param request AI 聊天请求对象, 包含系统提示和用户提示信息
     * @return 构建完成的 JSON 对象, 包含消息内容和模型参数
     */
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
        if (consoleLogger != null && runtimeSettings.verboseLogging) {
            consoleLogger.print(String.format("连接超时: [%ss] 读取超时: [%ss]", runtime.timeout, (runtime.timeout * 2)));
        }
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

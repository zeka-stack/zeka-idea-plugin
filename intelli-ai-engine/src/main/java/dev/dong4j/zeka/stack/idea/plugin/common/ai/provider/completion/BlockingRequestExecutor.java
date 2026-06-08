package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 请求阻塞执行器类
 * <p>负责向 AI 服务发送 HTTP 阻塞式请求, 并处理响应结果, 包括日志记录, 错误转换和响应内容解析.
 * <p>主要功能包括:
 * <ul>
 *   <li>构建并发送 JSON 格式的 POST 请求</li>
 *   <li>根据配置验证 API 密钥</li>
 *   <li>调用连接调整器 (connection tuner) 自定义连接行为</li>
 *   <li>通过监听器回调记录请求与响应信息</li>
 *   <li>解析 AI 返回的 JSON 响应, 提取关键字段如 content,usage 等</li>
 *   <li>处理异常情况, 将 IOException 转换为对应的业务异常</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
@Slf4j
public class BlockingRequestExecutor {

    /**
     * 当前项目实例
     * <p> 表示与 AI 请求执行相关的项目信息
     *
     * @see Project
     */
    private final Project project;
    /** AI 提供商配置信息, 用于指定服务类型, 基础 URL, 模型等参数 */
    private final AIProviderConfig config;
    /** 连接配置器, 用于自定义 HTTP 连接参数, 如设置 API 密钥或请求头 */
    private final BiConsumer<HttpURLConnection, String> connectionTuner;

    /**
     * 构造 BlockingRequestExecutor 实例
     * <p> 初始化 AI 阻塞请求执行器, 用于发送 AI 服务请求
     *
     * @param project         项目上下文, 用于日志记录和状态管理
     * @param config          AI 提供商配置, 包含基础 URL, 模型名称等配置信息
     * @param connectionTuner HTTP 连接调谐器, 用于自定义请求连接行为 (如设置 API 密钥等)
     */
    public BlockingRequestExecutor(Project project,
                                   AIProviderConfig config,
                                   BiConsumer<HttpURLConnection, String> connectionTuner) {
        this.project = project;
        this.config = config;
        this.connectionTuner = connectionTuner;
    }

    /**
     * 向 AI 服务发送请求并获取响应
     * <p> 该方法负责构建并发送 HTTP POST 请求到 AI 服务的 /chat/completions 端点,
     * 支持可选的 API 密钥认证和响应监听功能. 根据 validation 参数决定响应解析模式,
     * 验证模式下仅检查响应是否有效, 普通模式下则解析并返回 AI 生成的内容.</p>
     * <p> 方法会自动处理常见的 HTTP 错误状态码 (如 401,429,5xx 等),
     * 并将其转换为相应的 AIServiceException 错误类型.</p>
     *
     * @param body       请求体, 包含发送给 AI 模型的 JSON 数据, 不能为 null
     * @param apiKey     API 密钥, 如果服务提供商需要认证则必须提供, 支持可为空
     * @param listener   响应监听器, 用于接收请求和响应的回调通知, 支持可为空
     * @param validation 是否为验证模式, 验证模式下仅验证响应有效性, 不解析内容
     * @return AI 服务的响应内容, 验证模式下返回 "OK" 或解析后的内容字符串
     * @throws AIServiceException 当发生配置错误, 无效 API 密钥, 请求频率限制, 服务不可用, 无效响应或未知错误时抛出
     */
    public String sendRequest(JsonObject body,
                              @Nullable String apiKey,
                              @Nullable AIResponseListener listener,
                              boolean validation) throws AIServiceException {
        String url = config.baseUrl + "/chat/completions";
        return sendRequest(body, apiKey, listener, validation, url);
    }

    /**
     * 向 AI 服务发送请求并获取响应（支持自定义 URL）
     *
     * @param body       请求体, 包含发送给 AI 模型的 JSON 数据, 不能为 null
     * @param apiKey     API 密钥, 如果服务提供商需要认证则必须提供, 支持可为空
     * @param listener   响应监听器, 用于接收请求和响应的回调通知, 支持可为空
     * @param validation 是否为验证模式, 验证模式下仅验证响应有效性, 不解析内容
     * @param url        请求地址
     * @return AI 服务的响应内容, 验证模式下返回 "OK" 或解析后的内容字符串
     * @throws AIServiceException 当发生配置错误, 无效 API 密钥, 请求频率限制, 服务不可用, 无效响应或未知错误时抛出
     */
    public String sendRequest(JsonObject body,
                              @Nullable String apiKey,
                              @Nullable AIResponseListener listener,
                              boolean validation,
                              @NotNull String url) throws AIServiceException {
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置",
                                         AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String requestBody = body.toString();

        logRequest(listener, requestBody, validation);

        try {
            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;

            // [HOM-194] 关闭 IDEA 自带的 4xx/5xx 自动抛异常逻辑, 由我们手动在回调内读取
            // error stream 的原始 body 并写入 HttpStatusException.message,
            // 否则上层只能看到 "Request failed with status code 400" 这类无信息的
            // 默认描述, API 真正的错误体 (例如百炼的 InvalidParameter 提示) 会被吞掉.
            String responseBody = HttpRequests.post(url, "application/json")
                .throwStatusCodeException(false)
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    connectionTuner.accept(conn, apiKey);
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(request -> {
                    request.write(requestBody);
                    HttpURLConnection conn = (HttpURLConnection) request.getConnection();
                    int statusCode = conn.getResponseCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        return request.readString();
                    }
                    // 非 2xx 时务必使用 getErrorStream(): getInputStream() 在 4xx 下会直接抛 IOException
                    // 导致拿不到 body. 读到的 body 做截断 + 简单脱敏后塞进 HttpStatusException.message,
                    // 供上层 (AIServiceException.build / UI 弹窗 / idea.log) 透出.
                    String errorBody = readErrorBody(conn);
                    String detail = sanitizeForUser(errorBody);
                    throw new HttpRequests.HttpStatusException(
                        "HTTP " + statusCode + ": " + (detail.isEmpty() ? "(empty response body)" : detail),
                        statusCode, url);
                });

            logResponse(listener, responseBody, validation);

            if (!responseBody.trim().isEmpty()) {
                String result = validation ? parseValidationResponse(responseBody) : parseResponse(responseBody, listener);
                log.debug("AI response length: {}", result.length());
                return result;
            }

            throw new AIServiceException("Invalid response from AI service",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE);
        } catch (HttpRequests.HttpStatusException e) {
            // [HOM-194] 显式区分 400 / 404 (配置类错误, 应当带 detail 给用户)
            // 与 401 / 403 (鉴权类) / 429 (限流) / 5xx (服务端). 之前所有非显式分支被
            // 错误地折叠到 INVALID_RESPONSE, 进而被 AIServiceException.build() 翻译成
            // "服务返回的数据格式错误", 把真正的 API 错误体彻底丢失.
            AIServiceException.ErrorCode code = switch (e.getStatusCode()) {
                case 400, 404 -> AIServiceException.ErrorCode.CONFIGURATION_ERROR;
                case 401, 403 -> AIServiceException.ErrorCode.INVALID_API_KEY;
                case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
                case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
                default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
            };
            // e.getMessage() 已经包含 "HTTP <code>: <body>", 直接透传, 不再额外包一层 "HTTP error:"
            throw new AIServiceException(e.getMessage(), code, e);
        } catch (IOException e) {
            final String message = getErrorString(e);
            throw new AIServiceException("未知错误: " + message,
                                         AIServiceException.ErrorCode.UNKNOWN_ERROR, e);
        }
    }

    /** error body 最大保留长度, 避免 4MB 的 HTML 错误页撑爆日志和弹窗. */
    private static final int MAX_ERROR_BODY_LEN = 4096;

    /** 用于脱敏 Authorization / API Key 出现在错误体 (或上游回显请求) 中的极小概率情况. */
    private static final Pattern AUTH_HEADER_PATTERN =
        Pattern.compile("(?i)(authorization\\s*[:=]\\s*['\"]?)(bearer\\s+)?[A-Za-z0-9._\\-]+");
    private static final Pattern API_KEY_FIELD_PATTERN =
        Pattern.compile("(?i)(\"?api[_-]?key\"?\\s*[:=]\\s*['\"])([^'\"]+)(['\"])");
    private static final Pattern SK_TOKEN_PATTERN =
        Pattern.compile("sk-[A-Za-z0-9]{6,}");

    /**
     * 读取 HttpURLConnection 的 error stream
     * <p>
     * 必须在 4xx/5xx 状态下调用. getErrorStream() 在没有 body 时返回 null, 这里做防御.
     * 任何 IO 异常都被吞掉返回空串, 因为我们已经处于错误处理路径, 不想把读 body 失败
     * 再当成另一个错误向上抛.
     *
     * @param conn 已经发起请求, 且 responseCode 处于 4xx/5xx 的 HttpURLConnection
     * @return error body 字符串, 失败或无 body 时返回空串
     */
    @NotNull
    private static String readErrorBody(@NotNull HttpURLConnection conn) {
        try (InputStream stream = conn.getErrorStream()) {
            if (stream == null) {
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    /**
     * 对透出给用户的错误内容做截断 + 凭据脱敏
     * <p>
     * 截断到 {@value #MAX_ERROR_BODY_LEN} 字符, 同时屏蔽可能出现的 Authorization 头,
     * api_key 字段和 OpenAI 风格的 sk-xxxx token, 避免敏感信息进入 idea.log / 弹窗.
     *
     * @param raw 原始 body, 允许为 null
     * @return 清洗后的字符串, 总长度不超过 MAX_ERROR_BODY_LEN + 截断提示
     */
    @NotNull
    private static String sanitizeForUser(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String redacted = AUTH_HEADER_PATTERN.matcher(raw).replaceAll("$1***");
        redacted = API_KEY_FIELD_PATTERN.matcher(redacted).replaceAll("$1***$3");
        redacted = SK_TOKEN_PATTERN.matcher(redacted).replaceAll("sk-***");
        if (redacted.length() <= MAX_ERROR_BODY_LEN) {
            return redacted;
        }
        return redacted.substring(0, MAX_ERROR_BODY_LEN)
               + "...[truncated " + (redacted.length() - MAX_ERROR_BODY_LEN) + " chars]";
    }

    /**
     * 记录请求信息
     * <p> 在请求发送前记录请求详情, 若存在监听器且不进行验证, 则调用监听器的 onRequest 方法
     *
     * @param listener    请求监听器, 可以为 null
     * @param requestBody 请求体内容
     * @param validation  是否为验证请求
     */
    private void logRequest(@Nullable AIResponseListener listener, String requestBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onRequest(config.providerType.getDisplayName(), config.modelName, requestBody, false);
        }
    }

    /**
     * 记录 AI 服务的响应信息
     * <p> 如果提供了监听器且当前不是验证模式, 则调用监听器的 onResponse 方法记录响应内容
     *
     * @param listener     响应监听器, 用于接收响应事件通知, 可以为 null
     * @param responseBody 从 AI 服务返回的响应体内容
     * @param validation   是否是验证模式 (true 表示仅验证不执行完整流程)
     */
    private void logResponse(@Nullable AIResponseListener listener, String responseBody, boolean validation) {
        if (listener != null && !validation) {
            listener.onResponse(config.providerType.getDisplayName(), config.modelName, responseBody, false);
        }
    }

    /**
     * 解析 AI 服务响应并返回处理后的响应内容
     * <p> 该方法解析 JSON 响应体, 提取消息内容, 并处理 token 使用情况.
     * <p> 如果解析过程中出现异常, 则抛出 AIServiceException 异常.
     *
     * @param responseBody 响应体字符串
     * @param listener     可选的 AI 响应监听器, 用于接收 token 使用情况的通知
     * @return 处理后的响应内容字符串
     * @throws AIServiceException 如果响应解析失败或数据无效
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

                AIConsoleLoggerUtil.print(project, String.format("Token 消耗: Prompt=%d | Completion=%d | " +
                                                                 "Total=%d",
                                                                 promptTokens, completionTokens,
                                                                 totalTokens));

                if (listener != null) {
                    listener.onUsage(config.providerType.getDisplayName(), config.modelName, promptTokens, completionTokens, totalTokens);
                }
            }

            return filterThinkingContent(content);
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse response",
                                         AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

    /**
     * 解析用于验证的响应内容
     * <p> 解析 AI 服务返回的验证响应, 检查是否包含有效的 completion tokens 或 choices 数据.
     * <p> 如果响应中包含有效的 completion tokens 或非空的 choices 列表, 则返回 "OK", 否则抛出异常.
     *
     * @param responseBody 响应体内容
     * @return 如果响应有效, 返回 "OK"
     * @throws AIServiceException 如果未找到有效的 completion tokens 或 choices 数据
     */
    @SuppressWarnings("SameReturnValue")
    private String parseValidationResponse(String responseBody) throws AIServiceException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            AIConsoleLoggerUtil.print(project, "Response JSON:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(json));
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
     * 过滤掉内容中指定思考标记后的部分
     * <p> 该方法用于从 AI 返回的响应内容中移除思考过程的标记部分, 仅保留最终输出内容.
     * <p> 具体逻辑:
     * <ul>
     *   <li> 若内容为空或 null, 则直接返回原内容 </li>
     *   <li> 查找固定结束标记 "<tool_call>", 若存在则返回该标记之后的内容 (去除前后空格)</li>
     *   <li> 若内容中包含特殊标记 "ㅎㅎ", 则返回空字符串 </li>
     *   <li> 否则直接返回原始内容 </li>
     * </ul>
     *
     * @param content 输入的 AI 响应内容, 可能包含思考过程或标记
     * @return 过滤后的最终输出内容, 若匹配到结束标记则返回其后部分, 若包含特殊标记则返回空字符串, 否则原样返回
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

    /**
     * 根据 IOException 获取错误信息并可能抛出特定的 AIServiceException
     * <p> 该方法用于从 IOException 中提取错误消息, 并根据 HTTP 状态码判断是否抛出对应的 AIServiceException 异常.
     * <p> 支持的错误类型包括:
     * <ul>
     *   <li>429: 请求频率过高, 抛出 {@link AIServiceException.ErrorCode#RATE_LIMIT}</li>
     *   <li>401:API 密钥无效, 抛出 {@link AIServiceException.ErrorCode#INVALID_API_KEY}</li>
     *   <li>5xx: 服务暂时不可用, 抛出 {@link AIServiceException.ErrorCode#SERVICE_UNAVAILABLE}</li>
     * </ul>
     * <p> 若未匹配到特定错误码, 则直接返回原始错误消息.
     *
     * @param e 抛出的 IOException 异常
     * @return 原始错误消息, 或在匹配特定错误码时抛出对应的 AIServiceException
     * @throws AIServiceException 当错误消息包含特定 HTTP 状态码时, 抛出对应类型的 AIServiceException
     */
    @Nullable
    private static String getErrorString(IOException e) throws AIServiceException {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("HTTP response code: 429") || message.contains("429")) {
                throw new AIServiceException("请求频率过高，请稍后重试: " + message,
                                             AIServiceException.ErrorCode.RATE_LIMIT, e);
            }
            if (message.contains("HTTP response code: 401") || message.contains("401")) {
                throw new AIServiceException("API 密钥无效: " + message,
                                             AIServiceException.ErrorCode.INVALID_API_KEY, e);
            }
            if (message.contains("HTTP response code: 5")) {
                throw new AIServiceException("服务暂时不可用，请稍后重试: " + message,
                                             AIServiceException.ErrorCode.SERVICE_UNAVAILABLE, e);
            }
        }
        return message;
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.ParseContext;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.RawStreamChunk;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.StreamChunkType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.StreamParseEngine;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * AI 流式请求执行器类
 * <p>用于向 AI 服务发送流式请求并处理服务器发送的流式响应数据, 支持多种 AI 提供商 (如 OpenAI,Dashscope,Ollama,MiniMax 等) 的适配解析.
 * <p>该类通过配置的 AI 服务提供者配置 (AIProviderConfig) 和项目上下文 (Project) 来构建 HTTP 请求, 使用责任链策略解析流式响应.
 * <p>支持在流式响应中识别并输出“思考内容”(thinking)和“正文内容”, 并提供回调监听器 (AIStreamResponseListener) 以处理每一块数据, 错误和完整响应.
 * <p>使用示例:
 * <pre>{@code
 * StreamRequestExecutor executor = new StreamRequestExecutor(project, config, connectionTuner);
 * executor.sendStreamRequest(requestBody, apiKey, listener);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
@Slf4j
public class StreamRequestExecutor {

    /** 当前项目上下文, 用于日志记录和控制台输出 */
    private final Project project;
    /** AI 服务提供商配置信息, 用于指定使用的 AI 模型,API 密钥等关键参数 */
    private final AIProviderConfig config;
    /** HTTP 连接调优器, 用于设置连接参数, 如 API 密钥等 */
    private final BiConsumer<HttpURLConnection, String> connectionTuner;

    /**
     * 构造函数, 初始化 StreamRequestExecutor 对象
     * <p> 设置项目,AI 提供商配置和 HTTP 连接调优器
     *
     * @param project         项目对象
     * @param config          AI 提供商配置对象
     * @param connectionTuner HTTP 连接调优器, 用于自定义 HTTP 连接设置
     */
    public StreamRequestExecutor(Project project,
                                 AIProviderConfig config,
                                 BiConsumer<HttpURLConnection, String> connectionTuner) {
        this.project = project;
        this.config = config;
        this.connectionTuner = connectionTuner;
    }

    /**
     * 发送流请求以获取 AI 服务响应
     * <p> 该方法向指定的 URL 发送 HTTP POST 请求, 并根据响应流解析数据, 调用监听器回调.
     * <p> 在发送请求之前, 会检查是否需要 API 密钥并且已正确配置.
     * <p> 请求完成后, 根据不同的 HTTP 响应状态码抛出相应的异常.
     *
     * @param body     请求体,JSON 格式的请求数据
     * @param apiKey   可选的 API 密钥, 当提供商需要 API 密钥时必须提供
     * @param listener 监听器, 用于接收请求过程中的各种事件回调
     * @throws AIServiceException 当发生网络错误, 无效响应或 API 键无效时抛出
     */
    public void sendStreamRequest(JsonObject body,
                                  @Nullable String apiKey,
                                  @NotNull AIStreamResponseListener listener) throws AIServiceException {
        String url = config.baseUrl + "/chat/completions";
        sendStreamRequest(body, apiKey, listener, url);
    }

    /**
     * 发送流请求以获取 AI 服务响应（支持自定义 URL）
     *
     * @param body     请求体,JSON 格式的请求数据
     * @param apiKey   可选的 API 密钥, 当提供商需要 API 密钥时必须提供
     * @param listener 监听器, 用于接收请求过程中的各种事件回调
     * @param url      请求地址
     * @throws AIServiceException 当发生网络错误, 无效响应或 API 键无效时抛出
     */
    public void sendStreamRequest(JsonObject body,
                                  @Nullable String apiKey,
                                  @NotNull AIStreamResponseListener listener,
                                  @NotNull String url) throws AIServiceException {
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException(AICommonBundle.message("error.ai.service.api.key.required"),
                                         AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String requestBody = body.toString();
        logRequest(listener, requestBody);
        StreamCancellationToken cancellationToken = listener.cancellationToken();

        try {
            listener.onStart();
            byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            final int contentLength = requestBodyBytes.length;

            HttpRequests.post(url, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    connectionTuner.accept(conn, apiKey);
                    conn.setFixedLengthStreamingMode(contentLength);
                    conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                })
                .connect(request -> {
                    request.write(requestBody);
                    HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                    if (cancellationToken != null) {
                        cancellationToken.bindConnection(connection);
                        if (cancellationToken.isCancelled()) {
                            connection.disconnect();
                            return null;
                        }
                    }
                    readStreamResponse(connection, listener, cancellationToken);
                    return null;
                });
        } catch (HttpRequests.HttpStatusException e) {
            AIServiceException.ErrorCode code = switch (e.getStatusCode()) {
                case 401 -> AIServiceException.ErrorCode.INVALID_API_KEY;
                case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
                case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
                default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
            };
            String errorMessage = AICommonBundle.message("error.ai.service.http.error", e.getMessage());
            listener.onError(errorMessage, e);
            throw new AIServiceException(errorMessage, code, e);
        } catch (IOException e) {
            if (cancellationToken != null && cancellationToken.isCancelled()) {
                listener.onComplete("");
                return;
            }
            String errorMessage = AICommonBundle.message("error.ai.service.network.error");
            listener.onError(errorMessage, e);
            throw new AIServiceException(errorMessage, AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 读取流式响应并解析内容
     * <p>从 HTTP 连接中逐行读取 SSE(Server-Sent Events)格式的流式响应数据, 解析为流式块并分段输出给监听器.
     * <p>支持处理包含思考内容 (thinking) 和正文内容 (content) 的流式响应, 根据内容类型切换输出样式.
     * <p>示例处理流程:
     * <pre>{@code
     * // 读取流式响应时, 遇到 "data: " 开头的行, 解析 JSON 数据块
     * // 若包含 "thinking" 字段, 则打印思考过程
     * // 若包含 "content" 字段, 则打印正文内容, 并调用 listener.onChunk
     * // 遇到 "[DONE]" 标记时结束读取
     * }
     * </pre>
     *
     * @param connection HTTP 连接对象, 用于读取响应流
     * @param listener   流式响应监听器, 用于接收分块内容和完成通知
     * @throws IOException 当读取连接或解析流式数据时发生 I/O 错误
     */
    private void readStreamResponse(HttpURLConnection connection,
                                    @NotNull AIStreamResponseListener listener,
                                    @Nullable StreamCancellationToken cancellationToken) throws IOException {
        StringBuilder fullText = new StringBuilder();
        StreamParseEngine parseEngine = StreamParseEngine.createDefault();
        ParseContext parseContext = new ParseContext();
        boolean[] inThinking = {false};
        boolean[] thinkPrefixPrinted = {false};
        boolean[] contentStarted = {false};
        int[] contentNewlineStreak = {0};
        int[] thinkingNewlineStreak = {0};
        UsageStats usageStats = null;
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted() || isCancelled(cancellationToken)) {
                    connection.disconnect();
                    break;
                }
                if (line.isBlank()) {
                    continue;
                }

                log.debug(line);

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
                    UsageStats currentUsage = parseUsage(json);
                    if (currentUsage != null) {
                        usageStats = currentUsage;
                    }
                    RawStreamChunk rawChunk = RawStreamChunk.fromJson(json);
                    boolean done = rawChunk.isDone();
                    if (isCancelled(cancellationToken)) {
                        connection.disconnect();
                        break;
                    }
                    parseEngine.parse(parseContext, rawChunk, chunk -> {
                        if (chunk.text().isEmpty()) {
                            return;
                        }
                        if (chunk.type() == StreamChunkType.THINKING) {
                            String thinking = normalizeStreamNewlines(chunk.text(), thinkingNewlineStreak);
                            if (thinking.isEmpty()) {
                                return;
                            }
                            printThinking(thinking, inThinking, thinkPrefixPrinted);
                            listener.onThinkingChunk(thinking);
                            return;
                        }
                        if (chunk.type() == StreamChunkType.NOTICE) {
                            listener.onNotice(chunk.text());
                            return;
                        }
                        if (chunk.type() == StreamChunkType.CONTENT) {
                            if (inThinking[0]) {
                                AIConsoleLoggerUtil.printStreamPlain(
                                    project,
                                    AICommonBundle.message("stream.response.content.body.divider"));
                                inThinking[0] = false;
                                thinkPrefixPrinted[0] = false;
                            }
                            String content = trimLeadingNewlines(chunk.text(), contentStarted);
                            if (content.isEmpty()) {
                                return;
                            }
                            String normalizedContent = normalizeStreamNewlines(content, contentNewlineStreak);
                            if (normalizedContent.isEmpty()) {
                                return;
                            }
                            fullText.append(normalizedContent);
                            AIConsoleLoggerUtil.printStreamPlain(project, normalizedContent);
                            listener.onChunk(normalizedContent);
                        }
                    });
                    if (done) {
                        break;
                    }
                }
            }
        }
        AIConsoleLoggerUtil.completeStreamPlain(project);
        if (usageStats != null) {
            logUsage(usageStats, listener);
        }
        listener.onComplete(fullText.toString());
    }

    /**
     * 判断是否已取消流式请求
     * <p> 检查传入的取消令牌是否不为 null 且已被标记为取消状态
     *
     * @param cancellationToken 可选的取消令牌对象, 用于跟踪请求是否被取消
     * @return 如果令牌不为 null 且已被取消, 则返回 true; 否则返回 false
     */
    private boolean isCancelled(@Nullable StreamCancellationToken cancellationToken) {
        return cancellationToken != null && cancellationToken.isCancelled();
    }

    /**
     * 解析服务器发送事件 (SSE) 流中的 JSON 数据块
     * <p>尝试将传入的 JSON 字符串解析为 JsonObject, 若解析失败则记录警告日志并返回 null
     * <p>示例:
     * <pre>{@code
     * JsonObject json = parseSseJson("data: {\"content\": \" 你好 \"}");
     * }</pre>
     *
     * @param jsonData 服务器发送事件中的 JSON 数据字符串, 不能为空
     * @return 解析后的 JsonObject, 若解析失败则返回 null
     */
    @Nullable
    private static JsonObject parseSseJson(@NotNull String jsonData) {
        try {
            return JsonParser.parseString(jsonData).getAsJsonObject();
        } catch (Exception e) {
            log.debug("Failed to parse stream chunk JSON", e);
            return null;
        }
    }

    /**
     * 打印思考内容到控制台.
     * <p>
     * 该方法用于输出流式响应中的思考过程文本, 支持自动添加 "[🤔 thinking]" 前缀标记.
     * 首次调用时会打印带前缀的思考内容, 后续调用仅打印纯文本内容.
     *
     * @param thinking           待打印的思考内容, 不能为 null
     * @param inThinking         布尔数组, 用于跟踪是否已开始处理思考内容, 第一个元素表示状态
     * @param thinkPrefixPrinted 布尔数组, 用于跟踪是否已打印过带前缀的思考内容, 第一个元素表示状态
     */
    private void printThinking(@NotNull String thinking,
                               boolean @NotNull [] inThinking,
                               boolean @NotNull [] thinkPrefixPrinted) {
        if (!inThinking[0]) {
            inThinking[0] = true;
        }
        if (!thinkPrefixPrinted[0]) {
            AIConsoleLoggerUtil.printStreamPlain(project, AICommonBundle.message("stream.response.thinking.prefix") + " " + thinking);
            thinkPrefixPrinted[0] = true;
            return;
        }
        AIConsoleLoggerUtil.printStreamPlain(project, thinking);
    }

    /**
     * 去除正文开头多余的换行, 仅在首次输出正文时生效.
     *
     * @param text           原始正文片段
     * @param contentStarted 是否已开始输出正文
     * @return 处理后的正文片段
     */
    @NotNull
    private String trimLeadingNewlines(@NotNull String text, boolean @NotNull [] contentStarted) {
        if (contentStarted[0]) {
            return text;
        }
        int index = 0;
        int length = text.length();
        while (index < length) {
            char ch = text.charAt(index);
            if (ch != '\n' && ch != '\r') {
                break;
            }
            index++;
        }
        if (index >= length) {
            return "";
        }
        contentStarted[0] = true;
        return text.substring(index);
    }

    /**
     * 归一化流式文本中的换行, 将连续空行压缩为单个换行.
     *
     * @param text           原始文本片段
     * @param newlineStreak  换行连续计数, 用于跨 chunk 处理
     * @return 处理后的文本片段
     */
    @NotNull
    private String normalizeStreamNewlines(@NotNull String text, int @NotNull [] newlineStreak) {
        if (text.isEmpty()) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text.length());
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char ch = text.charAt(i);
            if (ch == '\r') {
                if (i + 1 < length && text.charAt(i + 1) == '\n') {
                    i++;
                }
                ch = '\n';
            }
            if (ch == '\n') {
                if (newlineStreak[0] >= 1) {
                    continue;
                }
                newlineStreak[0] = 1;
                builder.append('\n');
                continue;
            }
            newlineStreak[0] = 0;
            builder.append(ch);
        }
        return builder.toString();
    }

    /**
     * 记录请求信息
     * <p> 在请求发送前记录请求详情, 若存在监听器且不进行验证, 则调用监听器的 onRequest 方法
     *
     * @param listener    请求监听器, 可以为 null
     * @param requestBody 请求体内容
     */
    private void logRequest(@Nullable AIResponseListener listener, String requestBody) {
        if (listener != null) {
            listener.onRequest(config.providerType.getDisplayName(), config.modelName, requestBody, false);
        }
    }

    /**
     * 解析 JSON 对象中的使用统计信息
     * <p>从传入的 JSON 对象中提取并构建 UsageStats 实例, 仅当包含至少一个 token 相关字段 (prompt_tokens,completion_tokens 或 total_tokens) 时才返回有效结果.
     * <p>若 JSON 中不存在 "usage" 字段或其值不是对象, 或其中无任何 token 字段, 则返回 null.
     *
     * @param json 需要解析的 JSON 对象, 不能为空
     * @return 解析后的 UsageStats 实例, 若解析失败或无有效 token 数据则返回 null
     */
    @Nullable
    private static UsageStats parseUsage(@NotNull JsonObject json) {
        if (!json.has("usage") || !json.get("usage").isJsonObject()) {
            return null;
        }
        JsonObject usage = json.getAsJsonObject("usage");
        boolean hasAny = usage.has("prompt_tokens") || usage.has("completion_tokens") || usage.has("total_tokens");
        if (!hasAny) {
            return null;
        }
        int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
        int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
        int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : (promptTokens + completionTokens);
        return new UsageStats(promptTokens, completionTokens, totalTokens);
    }

    /**
     * 记录请求的 Token 消耗信息并通知监听器
     * <p> 该方法通过控制台打印 Token 使用统计信息 (包括 Prompt,Completion 和总 Token 数量), 并调用监听器的 onUsage 方法将相同数据传递给外部处理逻辑.
     * <p> 打印格式示例:Token 消耗: Prompt=100 | Completion=50 | Total=150
     *
     * @param usageStats Token 使用统计对象, 包含 Prompt,Completion 和总 Token 数量
     * @param listener   用于接收 Token 使用数据的监听器, 必须非空
     */
    private void logUsage(@NotNull UsageStats usageStats, @NotNull AIStreamResponseListener listener) {
        AIConsoleLoggerUtil.print(project, String.format("Token 消耗: Prompt=%d | Completion=%d | Total=%d",
                                                         usageStats.promptTokens(),
                                                         usageStats.completionTokens(),
                                                         usageStats.totalTokens()));
        listener.onUsage(config.providerType.getDisplayName(),
                         config.modelName,
                         usageStats.promptTokens(),
                         usageStats.completionTokens(),
                         usageStats.totalTokens());
    }

    private record UsageStats(int promptTokens, int completionTokens, int totalTokens) {
    }

}

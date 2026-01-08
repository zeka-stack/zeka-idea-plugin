package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion;

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
import java.util.function.BiConsumer;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.DashscopeStreamChunkParser;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.MiniMaxStreamChunkParser;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.OllamaStreamChunkParser;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.OpenAiStreamChunkParser;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.StreamChunk;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser.StreamChunkParser;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * AI 流式请求执行器类
 * <p>用于向 AI 服务发送流式请求并处理服务器发送的流式响应数据, 支持多种 AI 提供商 (如 OpenAI,Dashscope,Ollama,MiniMax 等) 的适配解析.
 * <p>该类通过配置的 AI 服务提供者配置 (AIProviderConfig) 和项目上下文 (Project) 来构建 HTTP 请求, 支持动态选择不同的流式响应解析器(StreamChunkParser).
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
public class StreamRequestExecutor {
    /** 应用日志记录器, 用于输出调试和运行时信息 */
    private static final Logger LOG = Logger.getInstance(StreamRequestExecutor.class);

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
        if (config.providerType.requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AIServiceException("需要 API 密钥但未进行配置",
                                         AIServiceException.ErrorCode.CONFIGURATION_ERROR);
        }

        String url = config.baseUrl + "/chat/completions";
        String requestBody = body.toString();
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
            listener.onError("HTTP error: " + e.getMessage(), e);
            throw new AIServiceException("HTTP error: " + e.getMessage(), code, e);
        } catch (IOException e) {
            if (cancellationToken != null && cancellationToken.isCancelled()) {
                listener.onComplete("");
                return;
            }
            listener.onError("网络错误: " + e.getMessage(), e);
            throw new AIServiceException("网络错误: " + e.getMessage(),
                                         AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 读取流式响应并解析内容
     * <p>从 HTTP 连接中逐行读取 SSE(Server-Sent Events)格式的流式响应数据, 解析为流式块并分段输出给监听器.
     * <p>支持处理包含思考内容 (thinking) 和正文内容 (content) 的流式响应, 根据内容类型切换输出样式.
     * <p>示例处理流程:
     * <pre>{@code
     * // 读取流式响应时, 遇到 "data: {" 开头的行, 解析 JSON 数据块
     * // 若包含 "thinking" 字段, 则打印思考过程
     * // 若包含 "content" 字段, 则打印正文内容, 并调用 listener.onChunk
     * // 遇到 "[DONE]" 标记时结束读取
     * }</pre>
     *
     * @param connection HTTP 连接对象, 用于读取响应流
     * @param listener   流式响应监听器, 用于接收分块内容和完成通知
     * @throws IOException 当读取连接或解析流式数据时发生 I/O 错误
     */
    private void readStreamResponse(HttpURLConnection connection,
                                    @NotNull AIStreamResponseListener listener,
                                    @Nullable StreamCancellationToken cancellationToken) throws IOException {
        StringBuilder fullText = new StringBuilder();
        StreamChunkParser parser = selectStreamChunkParser();
        LOG.trace("流式响应解析器: " + parser.getClass().getName());
        boolean[] inThinking = {false};
        boolean[] thinkPrefixPrinted = {false};
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

                LOG.debug(line);

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
                    if (isCancelled(cancellationToken)) {
                        connection.disconnect();
                        break;
                    }
                    if (chunk.thinking() != null && !chunk.thinking().isEmpty()) {
                        printThinking(chunk.thinking(), inThinking, thinkPrefixPrinted);
                        listener.onThinkingChunk(chunk.thinking());
                    }
                    if (chunk.content() != null && !chunk.content().isEmpty()) {
                        if (inThinking[0]) {
                            AIConsoleLoggerUtil.printStreamPlain(
                                project,
                                "\n\n══════════════════════════════ 正文内容 ══════════════════════════════\n\n");
                            inThinking[0] = false;
                            thinkPrefixPrinted[0] = false;
                        }
                        fullText.append(chunk.content());
                        AIConsoleLoggerUtil.printStreamPlain(project, chunk.content());
                        listener.onChunk(chunk.content());
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
            LOG.debug("Failed to parse stream chunk JSON", e);
            return null;
        }
    }

    /**
     * 打印思考内容到控制台.
     * <p>
     * 该方法用于输出流式响应中的思考过程文本, 支持自动添加 "[think]" 前缀标记.
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
            AIConsoleLoggerUtil.printStreamPlain(project, "[think] " + thinking);
            thinkPrefixPrinted[0] = true;
            return;
        }
        AIConsoleLoggerUtil.printStreamPlain(project, thinking);
    }

    /**
     * 根据配置选择合适的流式响应解析器
     * <p> 根据当前配置的基地址, 模型名称和提供者类型, 从预定义的解析器规则中匹配并返回对应的解析器实例.
     * <p> 支持的解析器包括: 通义千问 (Dashscope),Ollama,MiniMax 和 OpenAI 格式.
     * <p> 匹配优先级按规则列表顺序, 若未匹配到则默认返回 OpenAI 解析器.
     * <p> 使用示例:
     * <pre>{@code
     * StreamChunkParser parser = selectStreamChunkParser();
     * }</pre>
     *
     * @return 匹配到的流式响应解析器, 若未匹配到则返回默认的 OpenAI 解析器
     */
    private StreamChunkParser selectStreamChunkParser() {
        List<StreamParserRule> rules = new ArrayList<>();
        rules.add(new StreamParserRule("https://dashscope.aliyuncs.com",
                                       AIProviderType.QIANWEN,
                                       "qwen",
                                       new DashscopeStreamChunkParser()));

        rules.add(new StreamParserRule("http://localhost:11434",
                                       AIProviderType.OLLAMA,
                                       null,
                                       new OllamaStreamChunkParser()));

        rules.add(new StreamParserRule("minimax",
                                       null,
                                       "minimax",
                                       new MiniMaxStreamChunkParser()));

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

    /**
     * 流解析规则记录类
     * <p> 用于定义流式响应内容的匹配规则, 支持根据 URL 前缀,AI 提供商类型, 模型前缀等条件匹配特定的流分块解析器.
     * <p> 该类为不可变数据记录 (record), 封装了匹配条件与对应的解析器实例, 适用于动态路由或插件式流解析场景.
     * <p> 使用示例:
     * <pre>{@code
     * StreamParserRule rule = new StreamParserRule("https://api.openai.com", AIProviderType.OPENAI, "gpt-", parser);
     * boolean matched = rule.matches("https://api.openai.com/v1/chat/completions", AIProviderType.OPENAI, "gpt-4");
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.08
     * @since 1.0.0
     */
    private record StreamParserRule(String urlPrefix, AIProviderType providerType, String modelPrefix, StreamChunkParser parser) {
        /**
         * 构造一个 StreamParserRule 实例
         * <p> 用于定义解析流数据的规则, 根据 URL 前缀,AI 提供商类型和模型前缀来匹配对应的解析器
         *
         * @param urlPrefix    URL 前缀, 可以为 null 或空字符串, 表示不匹配 URL 前缀
         * @param providerType AI 提供商类型, 可以为 null, 表示不匹配提供商类型
         * @param modelPrefix  模型前缀, 可以为 null 或空字符串, 表示不匹配模型前缀
         * @param parser       流数据解析器, 不能为 null
         */
        private StreamParserRule(@Nullable String urlPrefix,
                                 @Nullable AIProviderType providerType,
                                 @Nullable String modelPrefix,
                                 @NotNull StreamChunkParser parser) {
            this.urlPrefix = urlPrefix;
            this.providerType = providerType;
            this.modelPrefix = modelPrefix;
            this.parser = parser;
        }

        /**
         * 判断给定的基础 URL,AI 提供商类型和模型名称是否与当前解析规则匹配
         * <p> 此方法会检查基础 URL 是否以指定的 URL 前缀开头,AI 提供商类型是否与指定类型匹配,
         * 以及模型名称是否以指定的模型前缀开头.
         *
         * @param baseUrl   基础 URL, 不能为空
         * @param provider  AI 提供商类型, 不能为空
         * @param modelName 模型名称, 不能为空
         * @return 如果所有条件都匹配, 则返回 true; 否则返回 false
         */
        private boolean matches(@NotNull String baseUrl,
                                @NotNull AIProviderType provider,
                                @NotNull String modelName) {
            return matchesUrl(baseUrl, urlPrefix)
                   && matchesProvider(provider, providerType)
                   && matchesModel(modelName, modelPrefix);
        }

        /**
         * 判断给定的 URL 是否匹配指定的前缀
         * <p> 该方法将 URL 和前缀都转换为小写后进行匹配, 如果前缀为 null 或空字符串, 则认为匹配成功.
         * <p> 如果前缀包含 "://", 则检查 URL 是否以该前缀开头; 否则检查 URL 是否包含该前缀.
         *
         * @param baseUrl 要匹配的完整 URL, 不能为 null
         * @param prefix  匹配前缀, 可以为 null 或空字符串, 表示无限制匹配
         * @return 如果匹配成功返回 true, 否则返回 false
         */
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

        /**
         * 检查实际的 AI 服务提供商类型是否与预期的提供商类型匹配
         * <p> 如果预期的提供商类型为 null, 则认为匹配; 否则, 只有当实际类型与预期类型相同时才匹配
         *
         * @param actual   实际的 AI 服务提供商类型, 不能为 null
         * @param expected 预期的 AI 服务提供商类型, 可以为 null
         * @return 如果实际类型与预期类型匹配, 返回 true; 否则返回 false
         */
        private static boolean matchesProvider(@NotNull AIProviderType actual, @Nullable AIProviderType expected) {
            return expected == null || expected == actual;
        }

        /**
         * 判断模型名称是否匹配指定前缀
         * <p> 如果前缀为 null 或空字符串, 则始终返回 true. 否则, 将模型名称和前缀都转换为小写后进行比较,
         * 检查模型名称是否以给定的前缀开头 (不区分大小写).
         *
         * @param modelName 要检查的模型名称, 不允许为 null
         * @param prefix    匹配前缀, 可以为 null 或空字符串
         * @return 如果模型名称匹配前缀或前缀为空时返回 true, 否则返回 false
         */
        private static boolean matchesModel(@NotNull String modelName, @Nullable String prefix) {
            if (prefix == null || prefix.isBlank()) {
                return true;
            }
            return modelName.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
        }
    }
}

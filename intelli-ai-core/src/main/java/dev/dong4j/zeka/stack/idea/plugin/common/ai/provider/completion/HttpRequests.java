package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * 纯 JDK 版 HTTP 请求工具类
 * <p>
 * 模拟 IntelliJ 平台 {@code com.intellij.util.io.HttpRequests} 中被 core 执行链使用的 API 子集,
 * 使原 Provider/Executor 代码除 import 外零改动即可脱离 IDE 运行(供 MCP Server 等宿主复用).
 * <p>
 * 签名约束(必须与平台版保持一致, 否则原有 lambda/方法引用调用点无法编译):
 * <ul>
 *   <li>{@link ConnectionTuner} 为单参数函数式接口, 方法声明抛出 IOException</li>
 *   <li>{@link Request#getConnection()} 返回 {@link URLConnection} 且声明抛出 IOException,
 *       调用方负责向下转型为 HttpURLConnection</li>
 *   <li>{@link HttpStatusException} 保持平台版的 (message, statusCode, url) 构造器与
 *       {@link #getStatusCode()} 访问器</li>
 * </ul>
 * 行为差异说明: 默认不自动抛状态码异常, 由 processor 内部读取
 * {@link HttpURLConnection#getErrorStream()} 处理错误体,
 * 错误处理逻辑见 BlockingRequestExecutor 中 [HOM-194] 的注释.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class HttpRequests {

    /**
     * 私有构造函数, 防止实例化工具类
     */
    private HttpRequests() {
    }

    /**
     * 创建 GET 请求构建器
     * <p>
     * 用于模型列表查询等只读接口, 不写请求体.
     *
     * @param url 请求地址, 不能为空
     * @return 请求构建器
     */
    public static @NotNull RequestBuilder request(@NotNull String url) {
        return new RequestBuilder(url, null);
    }

    /**
     * 创建 POST 请求构建器
     *
     * @param url         请求地址, 不能为空
     * @param contentType 请求体 Content-Type, 如 "application/json"
     * @return 请求构建器
     */
    public static @NotNull RequestBuilder post(@NotNull String url, @NotNull String contentType) {
        return new RequestBuilder(url, contentType);
    }

    /**
     * 连接调整器
     * <p>
     * 与平台版一致: 建立连接前回调, 允许设置请求头/超时/流式模式等.
     * 单参数设计, lambda 调用点形如 {@code tuner(connection -> {...})}.
     */
    public interface ConnectionTuner {

        /**
         * 调整连接
         *
         * @param connection 待建立的连接
         * @throws IOException 调整失败时抛出
         */
        void tune(@NotNull URLConnection connection) throws IOException;
    }

    /**
     * HTTP 请求状态码异常
     * <p>
     * 承载非 2xx 响应的状态码与(截断脱敏后的)错误体信息, 上层据此映射为
     * {@code AIServiceException.ErrorCode}(配置错误/鉴权失败/限流/服务不可用等).
     */
    public static class HttpStatusException extends IOException {

        /** HTTP 状态码 */
        private final int statusCode;

        /** 请求地址, 用于错误定位与日志 */
        private final String url;

        /**
         * 构造状态码异常
         *
         * @param message    错误描述(建议已包含 "HTTP <code>: <body>" 格式的错误体摘要)
         * @param statusCode HTTP 状态码
         * @param url        请求地址
         */
        public HttpStatusException(@NotNull String message, int statusCode, @NotNull String url) {
            super(message);
            this.statusCode = statusCode;
            this.url = url;
        }

        /**
         * 获取 HTTP 状态码
         *
         * @return 状态码
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * 获取请求地址
         *
         * @return 请求地址
         */
        public String getUrl() {
            return url;
        }
    }

    /**
     * 请求构建器
     * <p>
     * 提供 fluent 风格的连接配置, 最终通过 {@link #connect(RequestProcessor)} 执行请求.
     */
    public static class RequestBuilder {

        /** 请求地址 */
        private final String url;

        /** 请求体 Content-Type; null 表示 GET 请求 */
        private final String contentType;

        /** 连接调整器: 建立连接前回调 */
        private ConnectionTuner tuner;

        /**
         * 构建请求构建器
         *
         * @param url         请求地址
         * @param contentType 请求体 Content-Type; null 表示 GET 请求
         */
        RequestBuilder(@NotNull String url, String contentType) {
            this.url = url;
            this.contentType = contentType;
        }

        /**
         * 设置连接调整器
         *
         * @param tuner 连接调整器, 不能为空
         * @return 当前构建器
         */
        public @NotNull RequestBuilder tuner(@NotNull ConnectionTuner tuner) {
            this.tuner = tuner;
            return this;
        }

        /**
         * 兼容平台版 API: 是否在非 2xx 时自动抛状态码异常
         * <p>
         * 本实现始终不自动抛出(错误体由 processor 自行读取), 该方法仅保留链式调用兼容性.
         *
         * @param throwStatusCodeError 参数保留, 不影响行为
         * @return 当前构建器
         */
        public @NotNull RequestBuilder throwStatusCodeException(boolean throwStatusCodeError) {
            return this;
        }

        /**
         * 执行请求并处理响应
         * <p>
         * 建立连接 -> 应用 tuner -> 交给 processor 处理;
         * processor 内通过 {@link Request} 读写请求体与响应.
         *
         * @param processor 响应处理器, 不能为空
         * @param <T>       处理结果类型
         * @return processor 的处理结果
         * @throws IOException 网络或 I/O 错误时抛出
         */
        public <T> T connect(@NotNull RequestProcessor<T> processor) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(contentType != null ? "POST" : "GET");
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);
            }
            if (tuner != null) {
                tuner.tune(connection);
            }
            return processor.process(new Request(connection));
        }
    }

    /**
     * 响应处理器接口
     *
     * @param <T> 处理结果类型
     */
    public interface RequestProcessor<T> {

        /**
         * 处理请求连接
         *
         * @param connection 请求封装, 提供写请求体/读响应体/获取原始连接的能力
         * @return 处理结果
         * @throws IOException I/O 错误时抛出
         */
        T process(@NotNull Request connection) throws IOException;
    }

    /**
     * 请求/响应封装
     * <p>
     * 包装 {@link HttpURLConnection}, 提供与平台版 Request 对齐的最小 API.
     */
    public static class Request {

        /** 底层连接 */
        private final HttpURLConnection connection;

        /**
         * 构造请求封装
         *
         * @param connection 底层连接
         */
        Request(@NotNull HttpURLConnection connection) {
            this.connection = connection;
        }

        /**
         * 获取请求 URL
         *
         * @return 请求地址
         */
        public @NotNull String getURL() {
            return connection.getURL().toString();
        }

        /**
         * 获取底层连接
         * <p>
         * 与平台版签名一致: 返回 {@link URLConnection} 且声明抛出 IOException,
         * 调用方负责向下转型为 HttpURLConnection 并处理受检异常.
         *
         * @return 底层连接
         * @throws IOException 连接异常时抛出
         */
        public @NotNull URLConnection getConnection() throws IOException {
            return connection;
        }

        /**
         * 写入请求体(UTF-8)
         * <p>
         * 写入后关闭输出流, 触发请求发送.
         *
         * @param requestBody 请求体文本, 不能为空
         * @throws IOException I/O 错误时抛出
         */
        public void write(@NotNull String requestBody) throws IOException {
            byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }
        }

        /**
         * 读取响应体全量文本
         * <p>
         * 使用响应头声明的字符集解析, 未声明时回退 UTF-8.
         *
         * @return 响应体文本, 无响应体时返回空字符串
         * @throws IOException I/O 错误时抛出
         */
        public @NotNull String readString() throws IOException {
            String charset = StandardCharsets.UTF_8.name();
            String contentTypeHeader = connection.getHeaderField("Content-Type");
            if (contentTypeHeader != null) {
                for (String param : contentTypeHeader.replace(" ", "").split(";")) {
                    if (param.toLowerCase().startsWith("charset=")) {
                        charset = param.substring("charset=".length());
                    }
                }
            }
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), charset))) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[8192];
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, n);
                }
                return sb.toString();
            }
        }
    }
}

package dev.dong4j.zeka.stack.api.plugin.feedback.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import dev.dong4j.zeka.stack.api.plugin.feedback.config.SignatureProperties;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 签名验证过滤器
 * <p> 用于在接收到特定 API 请求时, 对请求头中的签名信息进行校验, 确保请求来源合法, 数据未被篡改.
 * 该过滤器仅处理 POST 方法且路径为 <code>/api/plugin/feedback</code> 的请求, 通过校验客户端 ID, 时间戳, 随机数 (nonce), 请求体哈希值及签名值, 确保通信安全.
 * 本过滤器不负责请求的完整处理, 仅作为前置校验层, 校验通过后将请求传递给后续过滤器链.
 * 依赖组件包括: 签名配置属性,nonce 缓存,JSON 对象映射器 (ObjectMapper), 并发布领域事件用于审计或监控.
 * 该类设计遵循面向对象原则, 避免与基础设施层耦合, 专注于业务逻辑校验.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(1) // 确保在其他过滤器之前执行
@RequiredArgsConstructor
public class SignatureVerificationFilter implements Filter {
    /** 反馈接口的路径, 仅对 POST /api/plugin/feedback/discussion 接口进行签名验证 */
    private static final String FEEDBACK_API_PATH = "/api/plugin/feedback/discussion";
    /** Issue 接口的路径, 仅对 POST /api/plugin/feedback/issue 接口进行签名验证 */
    private static final String FEEDBACK_ISSUE_API_PATH = "/api/plugin/feedback/issue";
    /** 客户端标识头字段名, 用于标识请求来源客户端 */
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    /** 时间戳头字段名, 用于签名验证中标识请求时间. */
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    /** 非重复码 (Nonce) 请求头名称, 用于防止重放攻击 */
    private static final String HEADER_NONCE = "X-Nonce";
    /** 请求体 SHA256 值的 HTTP 请求头名称, 用于签名验证时校验请求体完整性 */
    private static final String HEADER_BODY_SHA256 = "X-Body-SHA256";
    /** 签名头字段名, 用于传输请求签名值 */
    private static final String HEADER_SIGNATURE = "X-Signature";

    /** 签名配置属性, 用于获取客户端密钥, 是否启用签名验证等配置信息. */
    private final SignatureProperties signatureProperties;
    /** 非重复令牌缓存, 用于防止重放攻击, 确保每次请求的 nonce 唯一有效 */
    private final NonceCache nonceCache;
    /** JSON 序列化与反序列化工具, 用于将对象转换为 JSON 字符串或从 JSON 字符串反序列化为对象. */
    private final ObjectMapper objectMapper;

    /**
     * 过滤器入口方法, 用于在请求到达 Controller 之前验证请求签名
     * <p>
     * 该方法仅对 POST /api/plugin/feedback/discussion 和 /api/plugin/feedback/issue 接口进行签名验证, 其他接口跳过验证.
     * 验证内容包括: 客户端 ID, 时间戳, 随机数 (nonce), 请求体哈希值和签名.
     * 若验证失败, 将返回 401 Unauthorized 错误响应.
     * 若验证成功, 将请求转发至后续过滤器链.
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param chain    过滤器链, 用于将请求传递给下一个过滤器
     * @throws IOException      IO 异常, 如写入响应时发生错误
     * @throws ServletException Servlet 异常, 如过滤器处理过程中发生错误
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只验证 POST /api/plugin/feedback/* 接口
        if (!shouldVerify(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 如果签名验证被禁用，直接放行
        if (!signatureProperties.isEnabled()) {
            log.debug("Signature verification is disabled, skipping verification");
            chain.doFilter(request, response);
            return;
        }

        // 读取原始请求体（只能读取一次，需要包装）
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
        byte[] bodyBytes = cachedRequest.getCachedBody();

        try {
            // 提取签名头
            String clientId = cachedRequest.getHeader(HEADER_CLIENT_ID);
            String timestamp = cachedRequest.getHeader(HEADER_TIMESTAMP);
            String nonce = cachedRequest.getHeader(HEADER_NONCE);
            String bodySha256 = cachedRequest.getHeader(HEADER_BODY_SHA256);
            String signature = cachedRequest.getHeader(HEADER_SIGNATURE);

            // 验证必需的签名头
            if (clientId == null || timestamp == null || nonce == null || signature == null) {
                log.debug("Missing required signature headers");
                sendErrorResponse(httpResponse, "Missing required signature headers");
                return;
            }

            // 验证客户端 ID 是否存在
            if (!signatureProperties.hasClient(clientId)) {
                log.debug("Unknown client ID: {}", clientId);
                sendErrorResponse(httpResponse, "Unknown client ID");
                return;
            }

            // 验证 nonce（防止重放攻击）
            if (!nonceCache.checkAndStore(nonce)) {
                log.debug("Duplicate or invalid nonce: {}", nonce);
                sendErrorResponse(httpResponse, "Invalid or duplicate nonce");
                return;
            }

            // 获取客户端的 Secret
            String secret = signatureProperties.getSecret(clientId);
            if (secret == null || secret.isEmpty()) {
                log.debug("Secret not configured for client: {}", clientId);
                sendErrorResponse(httpResponse, "Server configuration error");
                return;
            }

            // 构建路径和查询参数
            String pathWithQuery = buildPathWithQuery(cachedRequest);

            // 验证签名
            boolean isValid = SignatureVerifier.verify(
                secret,
                cachedRequest.getMethod(),
                pathWithQuery,
                bodyBytes,
                timestamp,
                nonce,
                bodySha256,
                signature
                                                      );

            if (!isValid) {
                log.debug("Signature verification failed for client: {}", clientId);
                sendErrorResponse(httpResponse, "Invalid signature");
                return;
            }

            log.debug("Signature verification passed for client: {}", clientId);

            // 验证通过，继续处理请求
            chain.doFilter(cachedRequest, response);

        } catch (Exception e) {
            log.debug("Error during signature verification", e);
            sendErrorResponse(httpResponse, "Signature verification error: " + e.getMessage());
        }
    }

    /**
     * 判断当前请求是否需要进行签名验证
     * <p>仅当请求方法为 POST 且请求路径等于 /api/plugin/feedback/discussion 或 /api/plugin/feedback/issue 时返回 true, 表示需要验证签名</p>
     *
     * @param request HTTP 请求对象, 用于获取请求方法和路径信息
     * @return 如果请求满足验证条件 (POST 方法且路径为 /api/plugin/feedback/*) 则返回 true, 否则返回 false
     */
    private boolean shouldVerify(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return FEEDBACK_API_PATH.equals(path) || FEEDBACK_ISSUE_API_PATH.equals(path);
    }

    /**
     * 构建路径和查询参数
     * <p> 根据 HTTP 请求的路径和查询字符串, 拼接成完整的请求路径. 如果存在查询参数, 则在路径后添加问号和参数字符串; 否则仅返回路径.
     *
     * @param request HTTP 请求对象, 用于获取请求路径和查询字符串
     * @return 路径和查询参数的完整字符串, 例如:/api/plugin/feedback/discussion?x=1
     */
    private String buildPathWithQuery(HttpServletRequest request) {
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            return path + "?" + queryString;
        }
        return path;
    }

    /**
     * 发送签名验证失败的错误响应
     * <p> 设置 HTTP 状态码为 401(UNAUTHORIZED), 并以 JSON 格式返回错误信息
     * <pre>{@code
     * response.setStatus(HttpStatus.UNAUTHORIZED.value());
     * response.setContentType(MediaType.APPLICATION_JSON_VALUE);
     * response.setCharacterEncoding(StandardCharsets.UTF_8.name());
     * FeedbackResponse errorResponse = FeedbackResponse.builder()*     .success(false)
     *     .error("Unauthorized:" + message)
     *     .build();
     * String json = objectMapper.writeValueAsString(errorResponse);
     * response.getWriter().write(json);
     * response.getWriter().flush();
     * }</pre>
     *
     * @param response HTTP 响应对象
     * @param message  错误消息内容
     * @throws IOException 当写入响应内容时发生 I/O 错误
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        FeedbackResponse errorResponse = FeedbackResponse.builder()
            .success(false)
            .error("Unauthorized: " + message)
            .build();

        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}

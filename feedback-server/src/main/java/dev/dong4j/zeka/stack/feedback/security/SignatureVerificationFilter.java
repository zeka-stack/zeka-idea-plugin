package dev.dong4j.zeka.stack.feedback.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import dev.dong4j.zeka.stack.feedback.config.SignatureProperties;
import dev.dong4j.zeka.stack.feedback.dto.FeedbackResponse;
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
 * <p>
 * 在请求到达 Controller 之前验证请求签名，防止请求被伪造和重放攻击。
 * 只验证 POST /api/feedback 接口，其他接口不验证。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.23
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(1) // 确保在其他过滤器之前执行
@RequiredArgsConstructor
public class SignatureVerificationFilter implements Filter {
    private static final String FEEDBACK_API_PATH = "/api/feedback";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_BODY_SHA256 = "X-Body-SHA256";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private final SignatureProperties signatureProperties;
    private final NonceCache nonceCache;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只验证 POST /api/feedback 接口
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
     * 判断是否需要验证签名
     *
     * @param request HTTP 请求
     * @return 如果需要验证返回 true
     */
    private boolean shouldVerify(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "POST".equalsIgnoreCase(method) && FEEDBACK_API_PATH.equals(path);
    }

    /**
     * 构建路径和查询参数
     *
     * @param request HTTP 请求
     * @return 路径和查询参数（例如：/api/feedback?x=1）
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
     * 发送错误响应
     *
     * @param response HTTP 响应
     * @param message  错误消息
     * @throws IOException IO 异常
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


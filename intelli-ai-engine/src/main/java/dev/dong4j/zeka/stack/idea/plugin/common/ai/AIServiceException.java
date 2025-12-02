package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import lombok.Getter;

/**
 * AI 服务异常类
 * <p>
 * 用于处理 AI 服务调用过程中可能出现的各种异常情况, 包含详细的错误码分类
 * 和相应的错误处理逻辑, 支持重试判断和错误信息构建功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Getter
public class AIServiceException extends Exception {

    /** 错误码对象, 用于标识和处理系统中的错误状态 */
    private final ErrorCode errorCode;

    /**
     * 错误码枚举
     * <p>
     * 定义系统中可能出现的各种错误类型, 包括 API 密钥无效, 网络错误, 超时,
     * 速率限制, 服务不可用, 响应无效, 配置错误和未知错误等
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.30
     * @since 1.0.0
     */
    public enum ErrorCode {
        /** API Key 无效或缺失 */
        INVALID_API_KEY,
        /** 网络连接失败 */
        NETWORK_ERROR,
        /** 请求超时 */
        TIMEOUT,
        /** API 限流 */
        RATE_LIMIT,
        /** 服务不可用 */
        SERVICE_UNAVAILABLE,
        /** 无效的响应 */
        INVALID_RESPONSE,
        /** 配置错误 */
        CONFIGURATION_ERROR,
        /** 未知错误 */
        UNKNOWN_ERROR
    }

    /**
     * 构造一个AIServiceException对象，使用指定的错误信息和未知错误码
     *
     * @param message 错误信息
     */
    public AIServiceException(String message) {
        this(message, ErrorCode.UNKNOWN_ERROR);
    }

    /**
     * 构造一个AIServiceException对象
     *
     * @param message   异常的详细信息
     * @param errorCode 错误码
     */
    public AIServiceException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造一个AIServiceException异常对象
     *
     * @param message 异常的详细信息说明
     * @param cause   引起当前异常的底层异常
     */
    public AIServiceException(String message, Throwable cause) {
        this(message, ErrorCode.UNKNOWN_ERROR, cause);
    }

    /**
     * 构造一个AIServiceException异常对象
     *
     * @param message   异常的详细信息说明
     * @param errorCode 错误码，用于标识异常类型
     * @param cause     引起当前异常的底层异常
     */
    public AIServiceException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 重写 fillInStackTrace 方法以禁用堆栈跟踪
     *
     * @return 当前异常对象，不包含堆栈跟踪信息
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    /**
     * 判断是否应该重试
     *
     * @return 如果错误是临时性的可以重试，返回 true
     */
    public boolean isRetryable() {
        return errorCode == ErrorCode.NETWORK_ERROR
               || errorCode == ErrorCode.TIMEOUT
               || errorCode == ErrorCode.RATE_LIMIT
               || errorCode == ErrorCode.SERVICE_UNAVAILABLE;
    }

    /**
     * 根据给定的 AIServiceException 构建对应的错误信息字符串
     * <p>
     * 该方法会根据异常中包含的错误码返回对应的错误提示信息. 如果错误码为空, 则返回通用的失败信息.
     *
     * @param e AIServiceException 异常对象, 包含错误码和错误信息
     * @return 构建后的错误信息字符串
     */
    public static String build(AIServiceException e) {
        ErrorCode errorCode = e.getErrorCode();

        if (errorCode == null) {
            return AICommonBundle.message("error.ai.service.call.failed", e.getMessage());
        }

        return switch (errorCode) {
            case INVALID_API_KEY -> AICommonBundle.message("error.ai.service.invalid.api.key");
            case RATE_LIMIT -> AICommonBundle.message("error.ai.service.rate.limit");
            case SERVICE_UNAVAILABLE -> AICommonBundle.message("error.ai.service.unavailable");
            case NETWORK_ERROR -> AICommonBundle.message("error.ai.service.network.error");
            case CONFIGURATION_ERROR -> AICommonBundle.message("error.ai.service.configuration.error", e.getMessage());
            case INVALID_RESPONSE -> AICommonBundle.message("error.ai.service.invalid.response");
            default -> AICommonBundle.message("error.ai.service.call.failed.with.suggestion", e.getMessage());
        };
    }
}

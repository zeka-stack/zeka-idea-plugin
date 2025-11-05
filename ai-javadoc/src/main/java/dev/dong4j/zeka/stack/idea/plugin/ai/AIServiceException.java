package dev.dong4j.zeka.stack.idea.plugin.ai;

/**
 * AI 服务异常
 *
 * <p>当 AI 服务调用过程中发生错误时抛出此异常。
 *
 * @author dong4j
 * @version 1.0.0
 */
public class AIServiceException extends Exception {

    /**
     * 错误代码
     */
    private final ErrorCode errorCode;

    /**
     * 错误代码枚举
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
     * <p>
     * 初始化异常信息和错误码
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
     * <p>
     * 使用指定的错误信息、错误码和原因构建异常对象
     *
     * @param message 异常的详细信息说明
     * @param cause   引起当前异常的底层异常
     */
    public AIServiceException(String message, Throwable cause) {
        this(message, ErrorCode.UNKNOWN_ERROR, cause);
    }

    /**
     * 构造一个AIServiceException异常对象
     * <p>
     * 初始化异常信息、错误码以及异常原因
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
     * 获取错误码信息
     * <p>
     * 返回当前系统或操作的错误码
     *
     * @return 错误码对象
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 重写 fillInStackTrace 方法以禁用堆栈跟踪
     * <p>
     * 对于业务异常，通常不需要完整的堆栈跟踪信息。
     * 禁用堆栈跟踪可以：
     * <ul>
     *   <li>提高性能（避免收集堆栈信息的开销）</li>
     *   <li>使日志更简洁（只显示错误消息，不显示堆栈）</li>
     *   <li>减少日志文件大小</li>
     * </ul>
     *
     * @return 当前异常对象，不包含堆栈跟踪信息
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        // 返回 this 而不调用 super.fillInStackTrace()
        // 这样就不会收集堆栈跟踪信息
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


    public static String build(AIServiceException e) {
        AIServiceException.ErrorCode errorCode = e.getErrorCode();

        if (errorCode == null) {
            return "AI 服务调用失败: " + e.getMessage();
        }

        return switch (errorCode) {
            case INVALID_API_KEY -> "API Key 无效，请在设置中检查并更新 API Key";
            case RATE_LIMIT -> "请求频率过高，请稍后再试";
            case SERVICE_UNAVAILABLE -> "AI 服务暂时不可用，请稍后再试";
            case NETWORK_ERROR -> "网络连接失败，请检查网络连接或服务器地址";
            case CONFIGURATION_ERROR -> "配置错误: " + e.getMessage();
            case INVALID_RESPONSE -> "服务返回的数据格式错误。可能是模型名称不正确或服务异常";
            default -> "AI 服务调用失败: " + e.getMessage();
        };
    }
}


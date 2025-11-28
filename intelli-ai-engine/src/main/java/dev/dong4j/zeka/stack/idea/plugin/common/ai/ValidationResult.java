package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;

/**
 * 验证结果封装类
 * <p>
 * 用于封装验证操作的结果信息, 包括验证是否成功, 提示信息, 详细错误信息以及异常对象.
 * 该类提供多种静态方法用于创建成功或失败的验证结果对象, 适用于数据校验, 业务规则验证等场景.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public class ValidationResult {

    /** 如果验证成功返回 true，否则返回 false
     * -- GETTER --
     *  判断当前操作是否成功
     *  <p>
     *  返回操作是否成功的布尔值
     *
     * @return 如果操作成功返回 true, 否则返回 false
     */
    @Getter
    private final boolean success;

    /** 成功或失败的消息 */
    private final String message;

    /** 错误详细信息（仅在失败时有值） */
    private final String errorDetails;

    /** 关联的异常（可选） */
    private final Throwable throwable;

    /**
     * 构造一个验证结果对象
     * <p>
     * 初始化验证结果的各个属性, 包括是否成功, 消息, 错误详情和异常对象.
     *
     * @param success      验证是否成功
     * @param message      验证结果的消息描述
     * @param errorDetails 错误的详细信息
     * @param throwable    相关的异常对象
     */
    private ValidationResult(boolean success, String message, String errorDetails, Throwable throwable) {
        this.success = success;
        this.message = message;
        this.errorDetails = errorDetails;
        this.throwable = throwable;
    }

    /**
     * 创建一个表示成功的验证结果对象
     * <p>
     * 该方法用于生成一个验证结果对象, 表示验证成功, 并包含指定的成功消息
     *
     * @param message 成功消息
     * @return 包含成功状态和消息的验证结果对象
     * @since 1.0
     */
    @NotNull
    public static ValidationResult success(@NotNull String message) {
        return new ValidationResult(true, message, null, null);
    }

    /**
     * 创建一个验证失败的结果对象
     * <p>
     * 用于表示验证过程中出现错误的情况, 包含错误信息和错误类型.
     *
     * @param message 错误信息
     * @return 验证失败的结果对象
     */
    @NotNull
    public static ValidationResult failure(@NotNull String message) {
        return new ValidationResult(false, message, null, null);
    }

    /**
     * 创建一个表示验证失败的 {@link ValidationResult} 对象.
     *
     * @param message      失败时的错误信息, 不能为空
     * @param errorDetails 详细错误信息, 可能为 {@code null}
     * @return 一个 {@link ValidationResult} 实例, 表示验证失败
     */
    @NotNull
    public static ValidationResult failure(@NotNull String message, @Nullable String errorDetails) {
        return new ValidationResult(false, message, errorDetails, null);
    }

    /**
     * 创建一个包含错误信息的验证结果对象
     * <p>
     * 该方法用于生成一个表示验证失败的 ValidationResult 对象, 包含错误消息, 异常信息和异常对象.
     *
     * @param message   错误消息
     * @param throwable 引发错误的异常对象
     * @return 包含错误信息的 ValidationResult 对象
     * @throws IllegalArgumentException 如果 message 或 throwable 为 null
     */
    @NotNull
    public static ValidationResult failure(@NotNull String message, @NotNull Throwable throwable) {
        String errorDetails = throwable.getMessage();
        if (errorDetails == null || errorDetails.isEmpty()) {
            errorDetails = throwable.getClass().getSimpleName();
        }
        return new ValidationResult(false, message, errorDetails, throwable);
    }

    /**
     * 创建一个表示验证失败的结果对象
     * <p>
     * 用于在验证过程中表示验证失败的情况, 包含错误信息, 详细错误信息和异常对象
     *
     * @param message      错误信息, 不能为空
     * @param errorDetails 详细错误信息, 可以为空
     * @param throwable    异常对象, 可以为空
     * @return 验证失败的结果对象
     */
    @NotNull
    public static ValidationResult failure(@NotNull String message,
                                           @Nullable String errorDetails,
                                           @Nullable Throwable throwable) {
        return new ValidationResult(false, message, errorDetails, throwable);
    }

    /**
     * 获取消息内容
     * <p>
     * 返回当前存储的消息内容
     *
     * @return 消息内容
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * 获取错误详情信息
     * <p>
     * 返回当前存储的错误详情信息, 可能为 null.
     *
     * @return 错误详情信息, 可能为 null
     */
    @Nullable
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * 获取异常对象
     * <p>
     * 返回内部存储的异常对象, 可能为 null
     *
     * @return 异常对象, 可能为 null
     */
    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 获取完整的错误信息
     * <p>
     * 如果存在错误详细信息, 则将错误信息与详细信息拼接返回, 否则仅返回错误信息.
     *
     * @return 包含错误信息的字符串, 可能包含详细信息
     */
    @NotNull
    public String getFullErrorMessage() {
        if (errorDetails != null && !errorDetails.isEmpty()) {
            return message + "\n详细信息: " + errorDetails;
        }
        return message;
    }

    /**
     * 返回 {@link ValidationResult} 对象的字符串表示形式.
     * <p>
     * 该实现会按以下格式构造字符串:
     * <pre>
     * ValidationResult{success=...,message='...',errorDetails='...',throwable=...}
     * </pre>
     * 其中 {@code errorDetails} 与 {@code throwable} 仅在对应字段不为 {@code null} 时才会被包含.
     *
     * @return {@code ValidationResult} 的字符串描述
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("success=").append(success);
        sb.append(", message='").append(message).append('\'');
        if (errorDetails != null) {
            sb.append(", errorDetails='").append(errorDetails).append('\'');
        }
        if (throwable != null) {
            sb.append(", throwable=").append(throwable.getClass().getSimpleName());
        }
        sb.append('}');
        return sb.toString();
    }
}

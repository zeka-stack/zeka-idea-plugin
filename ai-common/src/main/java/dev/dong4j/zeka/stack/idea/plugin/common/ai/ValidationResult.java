package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 配置验证结果
 *
 * <p>封装配置验证的结果信息，包括验证状态、错误消息和详细信息。
 */
public class ValidationResult {

    /** 如果验证成功返回 true，否则返回 false */
    private final boolean success;

    /** 成功或失败的消息 */
    private final String message;

    /** 错误详细信息（仅在失败时有值） */
    private final String errorDetails;

    /** 关联的异常（可选） */
    private final Throwable throwable;

    private ValidationResult(boolean success, String message, String errorDetails, Throwable throwable) {
        this.success = success;
        this.message = message;
        this.errorDetails = errorDetails;
        this.throwable = throwable;
    }

    @NotNull
    public static ValidationResult success(@NotNull String message) {
        return new ValidationResult(true, message, null, null);
    }

    @NotNull
    public static ValidationResult failure(@NotNull String message) {
        return new ValidationResult(false, message, null, null);
    }

    @NotNull
    public static ValidationResult failure(@NotNull String message, @Nullable String errorDetails) {
        return new ValidationResult(false, message, errorDetails, null);
    }

    @NotNull
    public static ValidationResult failure(@NotNull String message, @NotNull Throwable throwable) {
        String errorDetails = throwable.getMessage();
        if (errorDetails == null || errorDetails.isEmpty()) {
            errorDetails = throwable.getClass().getSimpleName();
        }
        return new ValidationResult(false, message, errorDetails, throwable);
    }

    @NotNull
    public static ValidationResult failure(@NotNull String message,
                                           @Nullable String errorDetails,
                                           @Nullable Throwable throwable) {
        return new ValidationResult(false, message, errorDetails, throwable);
    }

    public boolean isSuccess() {
        return success;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getErrorDetails() {
        return errorDetails;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    @NotNull
    public String getFullErrorMessage() {
        if (errorDetails != null && !errorDetails.isEmpty()) {
            return message + "\n详细信息: " + errorDetails;
        }
        return message;
    }

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

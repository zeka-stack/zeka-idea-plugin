// package dev.dong4j.zeka.stack.idea.plugin.ai;
//
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
//
// import lombok.Getter;
//
// /**
//  * 配置验证结果
//  *
//  * <p>封装配置验证的结果信息，包括验证状态、错误消息和详细信息。
//  * 用于在设置页面显示详细的验证结果，帮助用户快速定位配置问题。
//  *
//  * <p>使用场景：
//  * <ul>
//  *   <li>设置页面的连接测试</li>
//  *   <li>配置验证和诊断</li>
//  *   <li>错误信息展示</li>
//  * </ul>
//  *
//  * <p>使用示例：
//  * <pre>
//  * ValidationResult result = provider.validateConfiguration();
//  * if (result.isSuccess()) {
//  *     showSuccessMessage(result.getMessage());
//  * } else {
//  *     showErrorMessage(result.getErrorMessage(), result.getErrorDetails());
//  * }
//  * </pre>
//  *
//  * @author dong4j
//  * @version 1.0.0
//  * @since 2.0
//  */
// public class ValidationResult {
//
//     /** 如果验证成功返回 true，否则返回 false */
//     @Getter
//     private final boolean success;
//
//     /** 成功或失败的消息 */
//     private final String message;
//
//     /** 错误详细信息（仅在失败时有值） */
//     private final String errorDetails;
//
//     /** 关联的异常（可选） */
//     private final Throwable throwable;
//
//     /**
//      * 私有构造函数，使用工厂方法创建实例
//      */
//     private ValidationResult(boolean success, String message, String errorDetails, Throwable throwable) {
//         this.success = success;
//         this.message = message;
//         this.errorDetails = errorDetails;
//         this.throwable = throwable;
//     }
//
//     /**
//      * 创建成功的验证结果
//      *
//      * @param message 成功消息
//      * @return 验证结果实例
//      */
//     @NotNull
//     public static ValidationResult success(@NotNull String message) {
//         return new ValidationResult(true, message, null, null);
//     }
//
//     /**
//      * 创建失败的验证结果
//      *
//      * @param message 错误消息
//      * @return 验证结果实例
//      */
//     @NotNull
//     public static ValidationResult failure(@NotNull String message) {
//         return new ValidationResult(false, message, null, null);
//     }
//
//     /**
//      * 创建失败的验证结果（带详细错误信息）
//      *
//      * @param message      错误消息
//      * @param errorDetails 错误详细信息
//      * @return 验证结果实例
//      */
//     @NotNull
//     public static ValidationResult failure(@NotNull String message, @Nullable String errorDetails) {
//         return new ValidationResult(false, message, errorDetails, null);
//     }
//
//     /**
//      * 创建失败的验证结果（带异常）
//      *
//      * @param message   错误消息
//      * @param throwable 异常对象
//      * @return 验证结果实例
//      */
//     @NotNull
//     public static ValidationResult failure(@NotNull String message, @NotNull Throwable throwable) {
//         String errorDetails = throwable.getMessage();
//         if (errorDetails == null || errorDetails.isEmpty()) {
//             errorDetails = throwable.getClass().getSimpleName();
//         }
//         return new ValidationResult(false, message, errorDetails, throwable);
//     }
//
//     /**
//      * 创建失败的验证结果（带详细信息和异常）
//      *
//      * @param message      错误消息
//      * @param errorDetails 错误详细信息
//      * @param throwable    异常对象
//      * @return 验证结果实例
//      */
//     @NotNull
//     public static ValidationResult failure(@NotNull String message,
//                                            @Nullable String errorDetails,
//                                            @Nullable Throwable throwable) {
//         return new ValidationResult(false, message, errorDetails, throwable);
//     }
//
//     /**
//      * 获取消息
//      *
//      * @return 成功或失败的消息
//      */
//     @NotNull
//     public String getMessage() {
//         return message;
//     }
//
//     /**
//      * 获取错误详细信息
//      *
//      * @return 错误详细信息，如果没有则返回 null
//      */
//     @Nullable
//     public String getErrorDetails() {
//         return errorDetails;
//     }
//
//     /**
//      * 获取关联的异常
//      *
//      * @return 异常对象，如果没有则返回 null
//      */
//     @Nullable
//     public Throwable getThrowable() {
//         return throwable;
//     }
//
//     /**
//      * 获取完整的错误消息（包含详细信息）
//      *
//      * <p>如果有错误详细信息，将消息和详细信息组合返回。
//      *
//      * @return 完整的错误消息
//      */
//     @NotNull
//     public String getFullErrorMessage() {
//         if (errorDetails != null && !errorDetails.isEmpty()) {
//             return message + "\n详细信息: " + errorDetails;
//         }
//         return message;
//     }
//
//     /**
//      * 返回对象的字符串表示形式
//      * <p>
//      * 构建并返回一个包含对象关键信息的字符串，用于调试或日志记录。
//      *
//      * @return 对象的字符串表示
//      */
//     @Override
//     public String toString() {
//         StringBuilder sb = new StringBuilder();
//         sb.append("ValidationResult{");
//         sb.append("success=").append(success);
//         sb.append(", message='").append(message).append('\'');
//         if (errorDetails != null) {
//             sb.append(", errorDetails='").append(errorDetails).append('\'');
//         }
//         if (throwable != null) {
//             sb.append(", throwable=").append(throwable.getClass().getSimpleName());
//         }
//         sb.append('}');
//         return sb.toString();
//     }
// }
//

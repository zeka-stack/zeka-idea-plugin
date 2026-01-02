package dev.dong4j.zeka.stack.feedback.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import dev.dong4j.zeka.stack.feedback.dto.FeedbackResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器类
 * <p> 用于捕获和处理全局异常, 提供统一的错误响应格式
 * <p> 该类使用了 Spring 的 @RestControllerAdvice 注解, 可以全局处理控制器中的异常
 * <p> 具体功能包括:
 * <ul>
 * <li> 处理参数验证异常 (MethodArgumentNotValidException), 返回详细的错误信息 </li>
 * <li> 处理其他通用异常 (Exception), 返回服务器内部错误信息 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     * <p> 当发生参数验证异常时, 提取验证错误信息并构建反馈响应对象.
     * <p> 每个验证错误包含字段名和错误消息, 并将这些信息组合成一个总的错误信息字符串.
     * <p> 日志记录验证失败的信息以便于后续排查.
     *
     * @param ex 参数验证异常对象
     * @return 返回包含错误信息的反馈响应, 状态码为 400 (BAD_REQUEST)
     * @since 1.0.0
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FeedbackResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = "参数验证失败: " + String.join(", ", errors.values());
        log.warn("Validation failed: {}", errors);

        FeedbackResponse response = FeedbackResponse.builder()
            .success(false)
            .error(errorMessage)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理通用异常
     * <p> 捕获所有未处理的异常, 并返回统一的错误响应
     * <p> 日志记录异常信息以便于后续排查问题
     *
     * @param ex 捕获到的异常对象
     * @return 包含错误信息的响应实体, 状态码为 500
     * @since 1.0.0
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<FeedbackResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        FeedbackResponse response = FeedbackResponse.builder()
            .success(false)
            .error("服务器内部错误: " + ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}


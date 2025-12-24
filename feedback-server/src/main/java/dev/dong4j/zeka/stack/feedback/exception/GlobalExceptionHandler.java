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
 * 全局异常处理器
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
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


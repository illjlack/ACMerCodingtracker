package com.codingtracker.shared.exception;

import com.codingtracker.shared.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全局异常处理器
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("业务异常: {} - {} [{}]", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", ex.getErrorCode());
        errorDetails.put("userMessage", ex.getUserMessage());
        errorDetails.put("developerMessage", ex.getDeveloperMessage());
        errorDetails.put("path", request.getRequestURI());

        // 如果是验证异常，添加字段错误详情
        if (ex instanceof ValidationException) {
            ValidationException validationEx = (ValidationException) ex;
            if (validationEx.hasFieldErrors()) {
                errorDetails.put("fieldErrors", validationEx.getFieldErrors());
            }
        }

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getUserMessage(), ex.getHttpStatus()));
    }

    /**
     * 处理数据验证异常（@Valid注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("数据验证失败 [{}]: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "VALIDATION_FAILED");
        errorDetails.put("fieldErrors", errors);
        errorDetails.put("path", request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("数据验证失败", 400));
    }

    /**
     * 处理约束违规异常（@Validated注解触发）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("约束验证失败 [{}]: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("约束验证失败", 400));
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("认证失败 [{}]: {}", request.getRequestURI(), ex.getMessage());

        String message = "认证失败";
        if (ex instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.unauthorized(message));
    }

    /**
     * 处理授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("权限不足 [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.forbidden("权限不足，无法访问该资源"));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("参数类型不匹配 [{}]: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("参数 '%s' 的值 '%s' 类型不正确，期望类型为 %s",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.validationError(message));
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("非法参数异常 [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.validationError("参数错误: " + ex.getMessage()));
    }

    /**
     * 处理其他运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        log.error("运行时异常 [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        // 生产环境下不暴露详细错误信息
        String message = isProductionEnvironment() ? "系统内部错误" : ex.getMessage();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, 500));
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("未处理异常 [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        // 生产环境下不暴露详细错误信息
        String message = isProductionEnvironment() ? "系统内部错误" : "发生未知错误: " + ex.getMessage();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, 500));
    }

    /**
     * 检查是否为生产环境
     */
    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "prod".equals(profile) || "production".equals(profile);
    }
}
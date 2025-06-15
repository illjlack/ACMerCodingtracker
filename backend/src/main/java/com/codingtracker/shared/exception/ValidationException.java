package com.codingtracker.shared.exception;

import java.util.Map;

/**
 * 数据验证异常
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
public class ValidationException extends BusinessException {

    /**
     * 字段验证错误详情
     */
    private final Map<String, String> fieldErrors;

    /**
     * 构造函数
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_FAILED", 400);
        this.fieldErrors = null;
    }

    /**
     * 构造函数（带字段错误详情）
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_FAILED", 400);
        this.fieldErrors = fieldErrors;
    }

    /**
     * 构造函数（带原因）
     */
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_FAILED", 400, cause);
        this.fieldErrors = null;
    }

    /**
     * 获取字段验证错误详情
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * 检查是否有字段级别的错误
     */
    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }

    /**
     * 常用验证异常工厂方法
     */
    public static ValidationException required(String fieldName) {
        return new ValidationException(String.format("%s不能为空", fieldName));
    }

    public static ValidationException invalidFormat(String fieldName) {
        return new ValidationException(String.format("%s格式不正确", fieldName));
    }

    public static ValidationException tooLong(String fieldName, int maxLength) {
        return new ValidationException(String.format("%s长度不能超过%d个字符", fieldName, maxLength));
    }

    public static ValidationException tooShort(String fieldName, int minLength) {
        return new ValidationException(String.format("%s长度不能少于%d个字符", fieldName, minLength));
    }

    public static ValidationException duplicate(String fieldName, String value) {
        return new ValidationException(String.format("%s '%s' 已存在", fieldName, value));
    }

    public static ValidationException invalid(String fieldName, String reason) {
        return new ValidationException(String.format("%s无效：%s", fieldName, reason));
    }

    public static ValidationException passwordMismatch() {
        return new ValidationException("两次输入的密码不一致");
    }

    public static ValidationException weakPassword() {
        return new ValidationException("密码强度不足，密码必须包含大小写字母、数字和特殊字符");
    }
}
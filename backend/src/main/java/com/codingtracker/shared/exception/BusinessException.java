package com.codingtracker.shared.exception;

import lombok.Getter;

/**
 * 业务异常基类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * HTTP状态码
     */
    private final int httpStatus;

    /**
     * 详细错误信息（开发者友好）
     */
    private final String detailMessage;

    /**
     * 构造函数
     */
    public BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.detailMessage = message;
    }

    /**
     * 构造函数（带详细信息）
     */
    public BusinessException(String message, String detailMessage, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.detailMessage = detailMessage;
    }

    /**
     * 构造函数（带原因）
     */
    public BusinessException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.detailMessage = message;
    }

    /**
     * 构造函数（完整参数）
     */
    public BusinessException(String message, String detailMessage, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.detailMessage = detailMessage;
    }

    /**
     * 用户友好的错误消息
     */
    public String getUserMessage() {
        return getMessage();
    }

    /**
     * 开发者友好的错误消息
     */
    public String getDeveloperMessage() {
        return detailMessage;
    }

    /**
     * 是否为客户端错误（4xx）
     */
    public boolean isClientError() {
        return httpStatus >= 400 && httpStatus < 500;
    }

    /**
     * 是否为服务器错误（5xx）
     */
    public boolean isServerError() {
        return httpStatus >= 500 && httpStatus < 600;
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, httpStatus=%d, message=%s]",
                getClass().getSimpleName(), errorCode, httpStatus, getMessage());
    }
}
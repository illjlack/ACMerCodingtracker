package com.codingtracker.shared.exception;

/**
 * 用户不存在异常
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
public class UserNotFoundException extends BusinessException {

    /**
     * 根据用户ID构造异常
     */
    public UserNotFoundException(Integer userId) {
        super(
                "用户不存在",
                String.format("用户ID %d 不存在", userId),
                "USER_NOT_FOUND",
                404);
    }

    /**
     * 根据用户名构造异常
     */
    public UserNotFoundException(String username) {
        super(
                "用户不存在",
                String.format("用户名 '%s' 不存在", username),
                "USER_NOT_FOUND",
                404);
    }

    /**
     * 自定义消息构造异常
     */
    public UserNotFoundException(String message, String detailMessage) {
        super(message, detailMessage, "USER_NOT_FOUND", 404);
    }

    /**
     * 默认构造函数
     */
    public UserNotFoundException() {
        super("用户不存在", "USER_NOT_FOUND", 404);
    }
}
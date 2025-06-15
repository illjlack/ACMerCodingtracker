package com.codingtracker.exception;

import com.codingtracker.model.OJPlatform;

/**
 * Token失效异常
 * 用于处理需要重新登录获取token的情况
 */
public class TokenExpiredException extends CrawlerException {

    public TokenExpiredException(OJPlatform platform) {
        super(platform, "TOKEN_EXPIRED",
                String.format("%s平台的认证token已失效，请重新登录获取", platform.name()));
    }

    public TokenExpiredException(OJPlatform platform, String message) {
        super(platform, "TOKEN_EXPIRED", message);
    }

    public TokenExpiredException(OJPlatform platform, String message, Throwable cause) {
        super(platform, "TOKEN_EXPIRED", message, cause);
    }
}
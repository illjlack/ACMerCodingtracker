package com.codingtracker.exception;

import com.codingtracker.model.OJPlatform;

/**
 * 网络异常
 * 用于处理网络请求相关的异常
 */
public class NetworkException extends CrawlerException {

    public NetworkException(OJPlatform platform, String message) {
        super(platform, "NETWORK_ERROR", message);
    }

    public NetworkException(OJPlatform platform, String message, Throwable cause) {
        super(platform, "NETWORK_ERROR", message, cause);
    }
}
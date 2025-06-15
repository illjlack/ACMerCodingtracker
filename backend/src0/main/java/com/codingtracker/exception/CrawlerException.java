package com.codingtracker.exception;

import com.codingtracker.model.OJPlatform;

/**
 * 爬虫异常基类
 */
public class CrawlerException extends RuntimeException {

    private final OJPlatform platform;
    private final String errorCode;

    public CrawlerException(OJPlatform platform, String message) {
        super(message);
        this.platform = platform;
        this.errorCode = "CRAWLER_ERROR";
    }

    public CrawlerException(OJPlatform platform, String message, Throwable cause) {
        super(message, cause);
        this.platform = platform;
        this.errorCode = "CRAWLER_ERROR";
    }

    public CrawlerException(OJPlatform platform, String errorCode, String message) {
        super(message);
        this.platform = platform;
        this.errorCode = errorCode;
    }

    public CrawlerException(OJPlatform platform, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.platform = platform;
        this.errorCode = errorCode;
    }

    public OJPlatform getPlatform() {
        return platform;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
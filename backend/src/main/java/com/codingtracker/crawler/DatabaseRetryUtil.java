package com.codingtracker.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;

import java.util.function.Supplier;

/**
 * 数据库操作重试工具类
 * 用于处理并发操作中的死锁和重复键冲突
 */
public class DatabaseRetryUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryUtil.class);

    /**
     * 带重试机制的数据库操作执行
     * 
     * @param operation     要执行的操作
     * @param operationName 操作名称（用于日志）
     * @param maxRetries    最大重试次数
     * @return 操作结果
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (DataIntegrityViolationException e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    logger.warn("{}操作遇到重复键冲突，第 {} 次重试", operationName, attempt);
                    lastException = e;
                } else {
                    throw e; // 其他数据完整性异常直接抛出
                }
            } catch (CannotAcquireLockException e) {
                logger.warn("{}操作遇到数据库锁冲突，第 {} 次重试", operationName, attempt);
                lastException = e;
            } catch (Exception e) {
                if (e.getMessage().contains("Deadlock")) {
                    logger.warn("{}操作遇到死锁，第 {} 次重试", operationName, attempt);
                    lastException = e;
                } else {
                    throw e; // 其他异常直接抛出
                }
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(100 * attempt); // 递增延迟
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("操作被中断", ie);
                }
            }
        }

        // 所有重试都失败了
        logger.error("{}操作失败，已重试 {} 次，最后错误: {}", operationName, maxRetries,
                lastException != null ? lastException.getMessage() : "未知错误");
        return null; // 或者根据需要抛出异常
    }

    /**
     * 执行无返回值的数据库操作
     */
    public static void executeWithRetry(Runnable operation, String operationName, int maxRetries) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, operationName, maxRetries);
    }

    /**
     * 默认重试3次的便捷方法
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, operationName, 3);
    }

    /**
     * 默认重试3次的无返回值便捷方法
     */
    public static void executeWithRetry(Runnable operation, String operationName) {
        executeWithRetry(operation, operationName, 3);
    }
}
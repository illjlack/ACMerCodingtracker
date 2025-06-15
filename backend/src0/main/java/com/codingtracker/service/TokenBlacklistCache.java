package com.codingtracker.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

// Service 默认是单例
@Service
public class TokenBlacklistCache {

    private final ConcurrentHashMap<String, LocalDateTime> blacklist = new ConcurrentHashMap<>();

    // 添加失效token，默认失效时间（比如1小时后）
    public void blacklistToken(String token) {
        blacklist.put(token, LocalDateTime.now().plusHours(1));
    }

    // 检查token是否被失效
    public boolean isTokenBlacklisted(String token) {
        LocalDateTime expireTime = blacklist.get(token);
        if (expireTime == null) {
            return false;
        }
        if (expireTime.isBefore(LocalDateTime.now())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    // 定时清理过期token（可选，防止内存增长）
    @Scheduled(fixedDelay = 10 * 60 * 1000) // 每10分钟执行一次
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}

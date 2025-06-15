package com.codingtracker.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 认证响应DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌类型
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * 访问令牌过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 刷新令牌过期时间（秒）
     */
    private Long refreshExpiresIn;

    /**
     * 令牌颁发时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();

    /**
     * 用户基本信息
     */
    private UserBasicInfo user;

    /**
     * 权限范围（可选）
     */
    private String scope;

    /**
     * 用户基本信息内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBasicInfo {
        private Integer id;
        private String username;
        private String realName;
        private String email;
        private String avatar;
        private boolean active;
        private java.util.Set<String> roles;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastLoginAt;
    }

    /**
     * 检查访问令牌是否即将过期（5分钟内）
     */
    public boolean isAccessTokenExpiringSoon() {
        if (expiresIn == null) {
            return false;
        }
        return expiresIn <= 300; // 5分钟 = 300秒
    }

    /**
     * 检查刷新令牌是否即将过期（1小时内）
     */
    public boolean isRefreshTokenExpiringSoon() {
        if (refreshExpiresIn == null) {
            return false;
        }
        return refreshExpiresIn <= 3600; // 1小时 = 3600秒
    }

    /**
     * 获取完整的授权头
     */
    public String getAuthorizationHeader() {
        return tokenType + " " + accessToken;
    }

    /**
     * 清理敏感信息（用于日志记录）
     */
    public AuthResponse withoutSensitiveData() {
        AuthResponse copy = AuthResponse.builder()
                .tokenType(this.tokenType)
                .expiresIn(this.expiresIn)
                .refreshExpiresIn(this.refreshExpiresIn)
                .issuedAt(this.issuedAt)
                .user(this.user)
                .scope(this.scope)
                .build();

        // 不包含实际的token值
        return copy;
    }
}
package com.codingtracker.shared.dto.response;

import com.codingtracker.core.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户响应DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Integer id;
    private String username;
    private String realName;
    private String major;
    private String email;
    private String avatar;
    private boolean active;
    private LocalDateTime lastTryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<User.Type> roles;
    private List<UserOJResponse> ojAccounts;
    private List<UserTagResponse> tags;

    /**
     * 辅助方法：检查是否为管理员
     */
    public boolean isAdmin() {
        return roles != null && (roles.contains(User.Type.ADMIN) || roles.contains(User.Type.SUPER_ADMIN));
    }

    /**
     * 辅助方法：检查是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return roles != null && roles.contains(User.Type.SUPER_ADMIN);
    }

    /**
     * 辅助方法：获取主要角色显示名称
     */
    public String getPrimaryRoleDisplay() {
        if (roles == null || roles.isEmpty()) {
            return "未知";
        }

        if (roles.contains(User.Type.SUPER_ADMIN)) {
            return User.Type.SUPER_ADMIN.getDisplayName();
        } else if (roles.contains(User.Type.ADMIN)) {
            return User.Type.ADMIN.getDisplayName();
        } else {
            return User.Type.USER.getDisplayName();
        }
    }

    /**
     * 辅助方法：获取OJ账号数量
     */
    public int getOJAccountCount() {
        return ojAccounts != null ? ojAccounts.size() : 0;
    }

    /**
     * 辅助方法：获取标签数量
     */
    public int getTagCount() {
        return tags != null ? tags.size() : 0;
    }
}
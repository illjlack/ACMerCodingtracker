package com.codingtracker.shared.dto.request;

import com.codingtracker.core.domain.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 用户更新请求DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    /**
     * 真实姓名（可选）
     */
    @Size(min = 2, max = 100, message = "真实姓名长度必须在2-100个字符之间")
    private String realName;

    /**
     * 邮箱（可选）
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255个字符")
    private String email;

    /**
     * 专业（可选）
     */
    @Size(min = 2, max = 100, message = "专业长度必须在2-100个字符之间")
    private String major;

    /**
     * 头像URL（可选）
     */
    @Size(max = 1024, message = "头像URL长度不能超过1024个字符")
    private String avatar;

    /**
     * 用户角色（管理员可更新）
     */
    private Set<User.Type> roles;

    /**
     * 用户状态（管理员可更新）
     */
    private Boolean active;

    /**
     * OJ账号列表（可选）
     */
    @Valid
    private List<OJAccountUpdateRequest> ojAccounts;

    /**
     * 标签列表（可选）
     */
    private List<String> tags;

    /**
     * OJ账号更新请求内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OJAccountUpdateRequest {

        /**
         * OJ账号ID（更新现有账号时使用）
         */
        private Integer id;

        /**
         * OJ平台
         */
        private com.codingtracker.core.domain.entity.OJPlatform platform;

        /**
         * 账号名
         */
        @Size(min = 1, max = 100, message = "账号名长度必须在1-100个字符之间")
        private String accountName;

        /**
         * 账号状态
         */
        private Boolean active;

        /**
         * 操作类型：ADD-添加, UPDATE-更新, DELETE-删除
         */
        private OperationType operation;

        public enum OperationType {
            ADD, UPDATE, DELETE
        }
    }

    /**
     * 检查是否有任何更新字段
     */
    public boolean hasAnyUpdate() {
        return realName != null ||
                email != null ||
                major != null ||
                avatar != null ||
                roles != null ||
                active != null ||
                (ojAccounts != null && !ojAccounts.isEmpty()) ||
                (tags != null && !tags.isEmpty());
    }

    /**
     * 检查是否只是普通用户字段更新（非管理员字段）
     */
    public boolean isUserFieldsOnly() {
        return roles == null && active == null;
    }
}
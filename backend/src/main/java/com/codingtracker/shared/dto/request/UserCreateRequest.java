package com.codingtracker.shared.dto.request;

import com.codingtracker.core.domain.entity.OJPlatform;
import com.codingtracker.core.domain.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 用户创建请求DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "密码必须包含大小写字母、数字和特殊字符")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 100, message = "真实姓名长度必须在2-100个字符之间")
    private String realName;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255个字符")
    private String email;

    /**
     * 专业
     */
    @NotBlank(message = "专业不能为空")
    @Size(min = 2, max = 100, message = "专业长度必须在2-100个字符之间")
    private String major;

    /**
     * 用户角色
     */
    private Set<User.Type> roles;

    /**
     * OJ账号列表
     */
    @Valid
    private List<OJAccountRequest> ojAccounts;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * OJ账号请求内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OJAccountRequest {

        @NotNull(message = "OJ平台不能为空")
        private OJPlatform platform;

        @NotBlank(message = "账号名不能为空")
        @Size(min = 1, max = 100, message = "账号名长度必须在1-100个字符之间")
        private String accountName;
    }

    /**
     * 验证密码一致性
     */
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * 清理敏感信息
     */
    public void clearSensitiveData() {
        this.password = null;
        this.confirmPassword = null;
    }
}
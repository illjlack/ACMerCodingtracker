package com.codingtracker.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 255, message = "用户名长度必须在3-255个字符之间")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 100, message = "密码长度不能超过100个字符")
    private String password;

    /**
     * 记住我（可选）
     */
    @Builder.Default
    private Boolean rememberMe = false;

    /**
     * 验证码（可选，未来扩展用）
     */
    private String captcha;

    /**
     * 清理敏感信息
     */
    public void clearSensitiveData() {
        this.password = null;
    }

    /**
     * 检查用户名是否为邮箱格式
     */
    public boolean isEmailLogin() {
        return username != null && username.contains("@");
    }
}
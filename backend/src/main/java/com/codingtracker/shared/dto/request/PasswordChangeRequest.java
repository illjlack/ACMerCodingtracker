package com.codingtracker.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码修改请求DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {

    /**
     * 当前密码
     */
    @NotBlank(message = "当前密码不能为空")
    @Size(min = 1, max = 100, message = "当前密码长度不能超过100个字符")
    private String currentPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "新密码长度必须在8-100个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "新密码必须包含大小写字母、数字和特殊字符")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 验证新密码一致性
     */
    public boolean isNewPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    /**
     * 验证新密码与当前密码不同
     */
    public boolean isPasswordChanged() {
        return currentPassword != null && newPassword != null && !currentPassword.equals(newPassword);
    }

    /**
     * 清理敏感信息
     */
    public void clearSensitiveData() {
        this.currentPassword = null;
        this.newPassword = null;
        this.confirmPassword = null;
    }
}
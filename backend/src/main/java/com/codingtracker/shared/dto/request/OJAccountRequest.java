package com.codingtracker.shared.dto.request;

import com.codingtracker.core.domain.entity.OJPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OJ账号请求DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OJAccountRequest {

    /**
     * OJ平台
     */
    @NotNull(message = "OJ平台不能为空")
    private OJPlatform platform;

    /**
     * 账号名
     */
    @NotBlank(message = "账号名不能为空")
    @Size(min = 1, max = 100, message = "账号名长度必须在1-100个字符之间")
    private String accountName;

    /**
     * 账号状态（可选，默认为激活）
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * 备注信息（可选）
     */
    @Size(max = 255, message = "备注信息长度不能超过255个字符")
    private String remarks;

    /**
     * 验证账号名格式
     */
    public boolean isValidAccountName() {
        if (accountName == null || accountName.trim().isEmpty()) {
            return false;
        }

        // 根据不同平台验证账号名格式
        switch (platform) {
            case CODEFORCES:
                // Codeforces用户名格式：字母数字下划线，3-24字符
                return accountName.matches("^[a-zA-Z0-9_]{3,24}$");
            case LEETCODE:
                // LeetCode用户名格式：字母数字下划线连字符，3-20字符
                return accountName.matches("^[a-zA-Z0-9_-]{3,20}$");
            case ATCODER:
                // AtCoder用户名格式：字母数字下划线，3-16字符
                return accountName.matches("^[a-zA-Z0-9_]{3,16}$");
            default:
                // 默认格式：字母数字下划线连字符，1-50字符
                return accountName.matches("^[a-zA-Z0-9_-]{1,50}$");
        }
    }

    /**
     * 获取平台显示名称
     */
    public String getPlatformDisplayName() {
        return platform != null ? platform.getDisplayName() : "未知平台";
    }

    /**
     * 清理账号名（去除空格等）
     */
    public void cleanAccountName() {
        if (accountName != null) {
            accountName = accountName.trim();
        }
    }
}
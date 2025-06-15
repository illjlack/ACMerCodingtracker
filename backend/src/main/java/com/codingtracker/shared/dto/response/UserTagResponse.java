package com.codingtracker.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户标签响应DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTagResponse {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 标签描述
     */
    private String description;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 标签状态
     */
    private boolean active;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 使用该标签的用户数量
     */
    private Integer userCount;

    /**
     * 检查颜色是否有效
     */
    public boolean isValidColor() {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }

    /**
     * 获取安全的颜色值
     */
    public String getSafeColor() {
        return isValidColor() ? color : "#409EFF";
    }

    /**
     * 获取标签的CSS样式字符串
     */
    public String getCssStyle() {
        return String.format("background-color: %s; color: %s",
                getSafeColor(),
                getContrastColor());
    }

    /**
     * 根据背景色获取对比文字颜色
     */
    public String getContrastColor() {
        if (!isValidColor()) {
            return "#FFFFFF";
        }

        // 移除#号并转换为RGB
        String hex = color.substring(1);
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        // 计算亮度
        double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255;

        // 亮度高于0.5使用黑色文字，否则使用白色文字
        return brightness > 0.5 ? "#000000" : "#FFFFFF";
    }

    /**
     * 获取标签的流行度描述
     */
    public String getPopularityDescription() {
        if (userCount == null || userCount == 0) {
            return "暂无使用";
        } else if (userCount == 1) {
            return "1个用户";
        } else if (userCount < 10) {
            return userCount + "个用户";
        } else if (userCount < 100) {
            return "较热门";
        } else {
            return "热门标签";
        }
    }

    /**
     * 检查标签是否为系统标签（根据名称判断）
     */
    public boolean isSystemTag() {
        if (name == null) {
            return false;
        }

        // 系统标签通常包含这些关键词
        String lowerName = name.toLowerCase();
        return lowerName.contains("管理员") ||
                lowerName.contains("系统") ||
                lowerName.contains("默认") ||
                lowerName.contains("admin") ||
                lowerName.contains("system");
    }

    /**
     * 获取标签显示文本（带用户数量）
     */
    public String getDisplayText() {
        if (userCount != null && userCount > 0) {
            return String.format("%s (%d)", name, userCount);
        }
        return name;
    }
}
package com.codingtracker.shared.dto.response;

import com.codingtracker.core.domain.entity.OJPlatform;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户OJ账号响应DTO
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOJResponse {

    /**
     * OJ账号ID
     */
    private Integer id;

    /**
     * OJ平台
     */
    private OJPlatform platform;

    /**
     * 平台显示名称
     */
    private String platformDisplayName;

    /**
     * 账号名
     */
    private String accountName;

    /**
     * 账号状态
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
     * 最后同步时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncAt;

    /**
     * 账号统计信息（可选）
     */
    private AccountStatistics statistics;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 账号统计信息内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountStatistics {

        /**
         * 总题数
         */
        private Integer totalProblems;

        /**
         * 已解决题数
         */
        private Integer solvedProblems;

        /**
         * 解题率
         */
        private Double solveRate;

        /**
         * 排名
         */
        private Integer ranking;

        /**
         * 评分/等级
         */
        private String rating;

        /**
         * 最高评分
         */
        private String maxRating;

        /**
         * 最近活动时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastActiveAt;

        /**
         * 连续提交天数
         */
        private Integer streakDays;

        /**
         * 本月提交数
         */
        private Integer monthlySubmissions;
    }

    /**
     * 获取平台URL
     */
    public String getPlatformUrl() {
        if (platform == null) {
            return null;
        }

        switch (platform) {
            case CODEFORCES:
                return "https://codeforces.com/profile/" + accountName;
            case LEETCODE:
                return "https://leetcode.cn/u/" + accountName;
            case ATCODER:
                return "https://atcoder.jp/users/" + accountName;
            case LUOGU:
                return "https://www.luogu.com.cn/user/" + accountName;
            case NOWCODER:
                return "https://ac.nowcoder.com/acm/contest/profile/" + accountName;
            default:
                return platform.getAliases().isEmpty() ? null : "https://" + platform.getAliases().get(0);
        }
    }

    /**
     * 检查账号是否需要同步
     */
    public boolean needsSync() {
        if (lastSyncAt == null) {
            return true;
        }
        // 如果超过24小时未同步，则需要同步
        return lastSyncAt.isBefore(LocalDateTime.now().minusHours(24));
    }

    /**
     * 检查账号是否活跃
     */
    public boolean isRecentlyActive() {
        if (statistics == null || statistics.getLastActiveAt() == null) {
            return false;
        }
        // 如果最近7天内有活动，则认为是活跃的
        return statistics.getLastActiveAt().isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * 获取解题进度描述
     */
    public String getProgressDescription() {
        if (statistics == null || statistics.getTotalProblems() == null || statistics.getSolvedProblems() == null) {
            return "暂无数据";
        }

        return String.format("已解决 %d/%d 题 (%.1f%%)",
                statistics.getSolvedProblems(),
                statistics.getTotalProblems(),
                statistics.getSolveRate() != null ? statistics.getSolveRate() : 0.0);
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (!active) {
            return "已停用";
        }

        if (needsSync()) {
            return "需要同步";
        }

        if (isRecentlyActive()) {
            return "活跃";
        }

        return "正常";
    }
}
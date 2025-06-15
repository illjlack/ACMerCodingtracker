package com.codingtracker.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户OJ账户实体类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Entity
@Table(name = "user_oj", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "platform",
        "account_name" }), indexes = {
                @Index(name = "idx_user_oj_user", columnList = "user_id"),
                @Index(name = "idx_user_oj_platform", columnList = "platform")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "user" })
@EqualsAndHashCode(of = { "platform", "accountName" })
public class UserOJ implements Comparable<UserOJ> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 关联的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /**
     * OJ平台
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OJPlatform platform;

    /**
     * 用户在OJ平台上的账号名
     */
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    /**
     * 账号状态（是否有效）
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 最后同步时间
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * JPA生命周期回调
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 更新最后同步时间
     */
    public void updateLastSyncTime() {
        this.lastSyncAt = LocalDateTime.now();
    }

    /**
     * 激活账号
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 停用账号
     */
    public void deactivate() {
        this.active = false;
    }

    @Override
    public int compareTo(UserOJ other) {
        if (other == null)
            return 1;

        // 首先比较用户ID
        int userComparison = Integer.compare(
                this.user != null ? this.user.getId() : 0,
                other.user != null ? other.user.getId() : 0);
        if (userComparison != 0)
            return userComparison;

        // 比较平台
        int platformComparison = this.platform.compareTo(other.platform);
        if (platformComparison != 0)
            return platformComparison;

        // 比较账号名
        if (this.accountName == null && other.accountName == null)
            return 0;
        if (this.accountName == null)
            return -1;
        if (other.accountName == null)
            return 1;
        return this.accountName.compareTo(other.accountName);
    }
}
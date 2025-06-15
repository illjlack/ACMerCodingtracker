package com.codingtracker.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户标签实体类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Entity
@Table(name = "user_tag", indexes = {
        @Index(name = "idx_user_tag_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "users" })
@EqualsAndHashCode(of = { "name" })
public class UserTag implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标签名称，例如 "大一", "算法竞赛", "前端开发"
     */
    @Column(unique = true, nullable = false, length = 128)
    private String name;

    /**
     * 标签颜色，用于前端显示
     */
    @Column(length = 7)
    @Builder.Default
    private String color = "#409EFF";

    /**
     * 标签描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 标签排序权重
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 标签状态（是否启用）
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
     * 反向关联到用户
     */
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<User> users = new HashSet<>();

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
     * 便利构造器
     */
    public UserTag(String name) {
        this.name = name;
        this.color = "#409EFF";
        this.active = true;
        this.sortOrder = 0;
    }

    public UserTag(String name, String color) {
        this.name = name;
        this.color = color;
        this.active = true;
        this.sortOrder = 0;
    }

    public UserTag(String name, String color, String description) {
        this.name = name;
        this.color = color;
        this.description = description;
        this.active = true;
        this.sortOrder = 0;
    }

    /**
     * 激活标签
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 停用标签
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 获取使用该标签的用户数量
     */
    public int getUserCount() {
        return users != null ? users.size() : 0;
    }

    /**
     * 检查颜色格式是否有效
     */
    public boolean isValidColor() {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }

    /**
     * 设置颜色（带验证）
     */
    public void setColorSafely(String color) {
        if (color != null && color.matches("^#[0-9A-Fa-f]{6}$")) {
            this.color = color;
        } else {
            this.color = "#409EFF"; // 默认颜色
        }
    }
}
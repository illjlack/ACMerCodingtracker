package com.codingtracker.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户实体类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Entity
@Table(name = "User", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_real_name", columnList = "realName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "password", "ojAccounts", "tags" })
@EqualsAndHashCode(of = { "username" })
public class User implements Serializable, Comparable<User> {

    /**
     * 用户类型枚举
     */
    @Getter
    public enum Type {
        SUPER_ADMIN("超级管理员"),
        ADMIN("管理员"),
        USER("普通用户");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * 主键ID，自动递增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 用户名，必须唯一且不能为空
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 用户密码，必须填写，不会被序列化
     */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /**
     * 真实姓名
     */
    @Column(name = "real_name", length = 100)
    private String realName;

    /**
     * 专业
     */
    @Column(length = 100)
    private String major;

    /**
     * 邮箱
     */
    @Column(unique = true, nullable = false, length = 255)
    private String email;

    /**
     * 头像地址
     */
    @Column(length = 1024)
    private String avatar;

    /**
     * 最后一次尝试的时间
     */
    @Column(name = "last_attempt_time")
    private LocalDateTime lastTryDate;

    /**
     * 用户状态（是否激活）
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
     * 用户身份（支持多个角色）
     */
    @ElementCollection(targetClass = Type.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Type> roles = new HashSet<>();

    /**
     * 用户 OJ 账户列表
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserOJ> ojAccounts = new ArrayList<>();

    /**
     * 用户标签
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_user_tag", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<UserTag> tags = new HashSet<>();

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
     * 判断用户是否是管理员（包括超级管理员）
     */
    public boolean isAdmin() {
        return roles.contains(Type.ADMIN) || roles.contains(Type.SUPER_ADMIN);
    }

    /**
     * 判断用户是否是超级管理员
     */
    public boolean isSuperAdmin() {
        return roles.contains(Type.SUPER_ADMIN);
    }

    /**
     * 判断用户是否是普通用户
     */
    public boolean isUser() {
        return roles.contains(Type.USER);
    }

    /**
     * 添加角色
     */
    public void addRole(Type role) {
        this.roles.add(role);
    }

    /**
     * 移除角色
     */
    public void removeRole(Type role) {
        this.roles.remove(role);
    }

    /**
     * 检查是否有特定角色
     */
    public boolean hasRole(Type role) {
        return this.roles.contains(role);
    }

    /**
     * 激活用户
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 停用用户
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 用户比较方法（按照 ID 排序）
     */
    @Override
    public int compareTo(User other) {
        return new CompareToBuilder()
                .append(this.id, other.id)
                .toComparison();
    }
}
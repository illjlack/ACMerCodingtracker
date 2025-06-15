package com.codingtracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户实体类
 */
@Entity
@Table(name = "User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User implements Serializable, Comparable<User> {

    /**
     * 用户类型枚举 - 简化为三种角色
     */
    @Getter
    public enum Type {
        SUPER_ADMIN("超级管理员"),
        ADMIN("管理员"),
        USER("普通用户");

        private final String shortStr;

        Type(String shortStr) {
            this.shortStr = shortStr;
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
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 用户密码，必须填写，不会被序列化 (避免暴露在 API 响应中)
     */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 专业 (Major)，可为空
     */
    private String major;

    private String email;
    /**
     * 头像地址，最多支持 1024 个字符
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
    private boolean active = true;

    /**
     * 用户身份（支持多个角色）
     * 使用 `@ElementCollection` 存储多个身份类型，映射为一张表
     */
    @ElementCollection(targetClass = Type.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Type> roles = new HashSet<>();

    /**
     * 用户 OJ 账户列表 (可以有多个)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserOJ> ojAccounts;

    /**
     * 用户标签（多对多关系）
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_user_tag", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<UserTag> tags = new HashSet<>();

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
     * 用户比较方法（按照 ID 排序）
     */
    @Override
    public int compareTo(User o) {
        return new CompareToBuilder()
                .append(this.id, o.id)
                .toComparison();
    }
}

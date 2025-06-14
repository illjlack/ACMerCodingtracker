package com.codingtracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户标签实体
 */
@Entity
@Table(name = "user_tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTag implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** 标签名称，例如 "大一", "算法竞赛", "前端开发" */
    @Column(unique = true, nullable = false, length = 128)
    private String name;

    /** 标签颜色，用于前端显示 */
    @Column(length = 7)
    private String color = "#409EFF";

    /** 标签描述 */
    @Column(length = 500)
    private String description;

    /** 反向关联到用户 */
    @ManyToMany(mappedBy = "tags")
    @JsonIgnore
    private Set<User> users = new HashSet<>();

    public UserTag(String name) {
        this.name = name;
    }

    public UserTag(String name, String color) {
        this.name = name;
        this.color = color;
    }
}
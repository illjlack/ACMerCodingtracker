package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 记录用户尝试的题目信息，冗余表
 */
@Entity
@Table(
        name = "user_try_problem_optimized",
        indexes = {
                //@Index(name = "idx_attempt_time", columnList = "attempt_time"),  // 时间索引
                @Index(name = "idx_user_time", columnList = "username, attempt_time")  // 联合索引：用户名 + 时间
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class UserTryProblemOptimized {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;  // 用户name

    @Column(nullable = false)
    private Long problemId;  // 题目ID

    @Column(nullable = false, length = 64)
    private String ojName;  // OJ 名称

    @Column(length = 200)
    private String pid;  // 题目ID（平台）

    @Column(length = 1024)
    private String problemName;  // 题目名称

    @Column(length = 64)
    private String problemType;  // 题目类型

    private Double points;  // 题目分数

    @Column(length = 256)
    private String url;  // 题目 URL

    @Column(nullable = false, length = 32)
    private String result;  // 用户的尝试结果

    @Column(nullable = false)
    private LocalDateTime attemptTime;  // 尝试时间

    @Column(length = 1024)
    private String tags;  // 标签（以逗号分隔）
}

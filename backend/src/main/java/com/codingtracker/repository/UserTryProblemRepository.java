package com.codingtracker.repository;

import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.ProblemResult;
import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface UserTryProblemRepository extends JpaRepository<UserTryProblem, Long> {
        // @EntityGraph(attributePaths = {"extOjPbInfo", "extOjPbInfo.tags"})
        // List<UserTryProblem> findByUserId(Long userId);

        // 使用 @EntityGraph 避免懒加载
        @EntityGraph(attributePaths = { "extOjPbInfo", "extOjPbInfo.tags" })
        List<UserTryProblem> findAll();

        @EntityGraph(attributePaths = { "extOjPbInfo", "extOjPbInfo.tags" })
        @Query("select utp from UserTryProblem utp where utp.user.id = :userId")
        Page<UserTryProblem> findByUserIdWithProblemAndTags(@Param("userId") Long userId, Pageable pageable);

        List<UserTryProblem> findByUser(User user);

        void deleteByUserId(Long userId);

        // 一次性查出某用户所有尝试记录，连带题目和标签一起加载

        @Query("SELECT COUNT(u) FROM UserTryProblem u WHERE u.user.id = :userId")
        long countByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(u) FROM UserTryProblem u WHERE u.user.id = :userId AND u.attemptTime BETWEEN :start AND :end")
        long countByUserIdAndAttemptTimeBetween(@Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(u) FROM UserTryProblem u WHERE u.extOjPbInfo = :platform AND u.user.id = :userId AND u.attemptTime BETWEEN :start AND :end")
        long countByPlatformAndUserIdAndAttemptTimeBetween(@Param("platform") String platform,
                        @Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // 查询某时间段内所有用户每个平台的尝试数量（基于用户聚合，兼容前端显示）
        @Query("SELECT u.user.id, u.ojName, COUNT(u) " +
                        "FROM UserTryProblem u " +
                        "WHERE u.attemptTime BETWEEN :start AND :end " +
                        "GROUP BY u.user.id, u.ojName")
        List<Object[]> countTryByUserAndPlatformBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // 查询某时间段内所有用户每个平台的AC数量（基于用户聚合，兼容前端显示）
        @Query("SELECT u.user.id, u.ojName, COUNT(u) " +
                        "FROM UserTryProblem u " +
                        "WHERE u.attemptTime BETWEEN :start AND :end AND u.result = :acResult " +
                        "GROUP BY u.user.id, u.ojName")
        List<Object[]> countAcByUserAndPlatformBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("acResult") ProblemResult acResult);

        // 新增：查询某时间段内所有OJ账号每个平台的尝试数量（基于OJ账号）
        @Query("SELECT u.userOj.id, u.userOj.user.id, u.ojName, COUNT(u) " +
                        "FROM UserTryProblem u " +
                        "WHERE u.userOj IS NOT NULL AND u.attemptTime BETWEEN :start AND :end " +
                        "GROUP BY u.userOj.id, u.userOj.user.id, u.ojName")
        List<Object[]> countTryByOjAccountAndPlatformBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // 新增：查询某时间段内所有OJ账号每个平台的AC数量（基于OJ账号）
        @Query("SELECT u.userOj.id, u.userOj.user.id, u.ojName, COUNT(u) " +
                        "FROM UserTryProblem u " +
                        "WHERE u.userOj IS NOT NULL AND u.attemptTime BETWEEN :start AND :end AND u.result = :acResult "
                        +
                        "GROUP BY u.userOj.id, u.userOj.user.id, u.ojName")
        List<Object[]> countAcByOjAccountAndPlatformBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("acResult") ProblemResult acResult);

        // 根据OJ账号查询题目尝试记录
        @EntityGraph(attributePaths = { "extOjPbInfo", "extOjPbInfo.tags" })
        @Query("SELECT utp FROM UserTryProblem utp WHERE utp.userOj.id = :userOjId")
        Page<UserTryProblem> findByUserOjIdWithProblemAndTags(@Param("userOjId") Integer userOjId, Pageable pageable);

        // 根据用户的所有OJ账号查询题目尝试记录
        @EntityGraph(attributePaths = { "extOjPbInfo", "extOjPbInfo.tags" })
        @Query("SELECT utp FROM UserTryProblem utp WHERE utp.userOj.user.id = :userId OR (utp.userOj IS NULL AND utp.user.id = :userId)")
        Page<UserTryProblem> findByUserIdIncludingOjAccountsWithProblemAndTags(@Param("userId") Long userId,
                        Pageable pageable);

        // 统计用户在指定平台的OJ账号数量
        @Query("SELECT COUNT(DISTINCT u.userOj.id) FROM UserTryProblem u WHERE u.user.id = :userId AND u.ojName = :platform AND u.userOj IS NOT NULL")
        long countDistinctOjAccountsByUserAndPlatform(@Param("userId") Integer userId,
                        @Param("platform") OJPlatform platform);

        // 删除指定OJ账号的所有尝试记录
        void deleteByUserOjId(Integer userOjId);
}

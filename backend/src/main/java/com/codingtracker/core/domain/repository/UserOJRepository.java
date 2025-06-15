package com.codingtracker.core.domain.repository;

import com.codingtracker.core.domain.entity.OJPlatform;
import com.codingtracker.core.domain.entity.UserOJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户OJ账号仓储接口
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Repository
public interface UserOJRepository extends JpaRepository<UserOJ, Integer> {

    /**
     * 根据用户ID查找所有OJ账号
     */
    List<UserOJ> findByUserId(Integer userId);

    /**
     * 根据用户ID和平台查找OJ账号
     */
    List<UserOJ> findByUserIdAndPlatform(Integer userId, OJPlatform platform);

    /**
     * 根据用户名查找所有OJ账号
     */
    @Query("SELECT oj FROM UserOJ oj JOIN oj.user u WHERE u.username = :username")
    List<UserOJ> findByUsername(@Param("username") String username);

    /**
     * 根据用户名和平台查找OJ账号
     */
    @Query("SELECT oj FROM UserOJ oj JOIN oj.user u WHERE u.username = :username AND oj.platform = :platform")
    List<UserOJ> findByUsernameAndPlatform(@Param("username") String username, @Param("platform") OJPlatform platform);

    /**
     * 根据平台和账号名查找
     */
    Optional<UserOJ> findByPlatformAndAccountName(OJPlatform platform, String accountName);

    /**
     * 检查平台账号是否已被其他用户使用
     */
    @Query("SELECT COUNT(oj) > 0 FROM UserOJ oj WHERE oj.platform = :platform AND oj.accountName = :accountName AND oj.user.id != :excludeUserId")
    boolean existsByPlatformAndAccountNameAndUserIdNot(@Param("platform") OJPlatform platform,
            @Param("accountName") String accountName,
            @Param("excludeUserId") Integer excludeUserId);

    /**
     * 检查平台账号是否存在
     */
    boolean existsByPlatformAndAccountName(OJPlatform platform, String accountName);

    /**
     * 根据平台查找所有账号
     */
    List<UserOJ> findByPlatform(OJPlatform platform);

    /**
     * 根据平台查找激活的账号
     */
    List<UserOJ> findByPlatformAndActiveTrue(OJPlatform platform);

    /**
     * 查找激活的OJ账号
     */
    List<UserOJ> findByActiveTrue();

    /**
     * 查找非激活的OJ账号
     */
    List<UserOJ> findByActiveFalse();

    /**
     * 查找需要同步的账号（超过指定时间未同步）
     */
    @Query("SELECT oj FROM UserOJ oj WHERE oj.active = true AND (oj.lastSyncAt IS NULL OR oj.lastSyncAt < :cutoffTime)")
    List<UserOJ> findAccountsNeedingSync(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查找用户在指定平台的激活账号数量
     */
    @Query("SELECT COUNT(oj) FROM UserOJ oj WHERE oj.user.id = :userId AND oj.platform = :platform AND oj.active = true")
    long countActiveAccountsByUserAndPlatform(@Param("userId") Integer userId, @Param("platform") OJPlatform platform);

    /**
     * 查找用户的总OJ账号数量
     */
    long countByUserId(Integer userId);

    /**
     * 查找用户的激活OJ账号数量
     */
    long countByUserIdAndActiveTrue(Integer userId);

    /**
     * 统计平台的总账号数量
     */
    long countByPlatform(OJPlatform platform);

    /**
     * 统计平台的激活账号数量
     */
    long countByPlatformAndActiveTrue(OJPlatform platform);

    /**
     * 删除用户的所有OJ账号
     */
    @Modifying
    @Query("DELETE FROM UserOJ oj WHERE oj.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    /**
     * 删除用户在指定平台的所有账号
     */
    @Modifying
    @Query("DELETE FROM UserOJ oj WHERE oj.user.id = :userId AND oj.platform = :platform")
    void deleteByUserIdAndPlatform(@Param("userId") Integer userId, @Param("platform") OJPlatform platform);

    /**
     * 批量激活OJ账号
     */
    @Modifying
    @Query("UPDATE UserOJ oj SET oj.active = true WHERE oj.id IN :ids")
    void activateByIds(@Param("ids") List<Integer> ids);

    /**
     * 批量停用OJ账号
     */
    @Modifying
    @Query("UPDATE UserOJ oj SET oj.active = false WHERE oj.id IN :ids")
    void deactivateByIds(@Param("ids") List<Integer> ids);

    /**
     * 更新同步时间
     */
    @Modifying
    @Query("UPDATE UserOJ oj SET oj.lastSyncAt = :syncTime WHERE oj.id = :id")
    void updateLastSyncTime(@Param("id") Integer id, @Param("syncTime") LocalDateTime syncTime);

    /**
     * 批量更新同步时间
     */
    @Modifying
    @Query("UPDATE UserOJ oj SET oj.lastSyncAt = :syncTime WHERE oj.id IN :ids")
    void updateLastSyncTimeByIds(@Param("ids") List<Integer> ids, @Param("syncTime") LocalDateTime syncTime);

    /**
     * 查找最近创建的OJ账号
     */
    List<UserOJ> findTop10ByOrderByCreatedAtDesc();

    /**
     * 查找最近同步的OJ账号
     */
    @Query("SELECT oj FROM UserOJ oj WHERE oj.lastSyncAt IS NOT NULL ORDER BY oj.lastSyncAt DESC")
    List<UserOJ> findRecentlySyncedAccounts();

    /**
     * 根据账号名模糊查找
     */
    @Query("SELECT oj FROM UserOJ oj WHERE oj.accountName LIKE %:accountName%")
    List<UserOJ> findByAccountNameContaining(@Param("accountName") String accountName);
}
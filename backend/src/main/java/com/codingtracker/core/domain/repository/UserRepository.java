package com.codingtracker.core.domain.repository;

import com.codingtracker.core.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户仓储接口
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据真实姓名查找用户
     */
    Optional<User> findByRealName(String realName);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据角色查找用户
     */
    List<User> findByRolesContains(User.Type role);

    /**
     * 查找所有管理员用户
     */
    @Query("SELECT u FROM User u WHERE :adminRole IN elements(u.roles) OR :superAdminRole IN elements(u.roles)")
    List<User> findAllAdmins(@Param("adminRole") User.Type adminRole,
            @Param("superAdminRole") User.Type superAdminRole);

    /**
     * 查找激活的用户
     */
    List<User> findByActiveTrue();

    /**
     * 查找非激活的用户
     */
    List<User> findByActiveFalse();

    /**
     * 分页查找用户（按创建时间倒序）
     */
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据关键字搜索用户（用户名、真实姓名、邮箱）
     */
    @Query("SELECT u FROM User u WHERE " +
            "u.username LIKE %:keyword% OR " +
            "u.realName LIKE %:keyword% OR " +
            "u.email LIKE %:keyword%")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据专业查找用户
     */
    List<User> findByMajor(String major);

    /**
     * 查找在指定时间之后创建的用户
     */
    List<User> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 查找在指定时间之后最后尝试的用户
     */
    List<User> findByLastTryDateAfter(LocalDateTime dateTime);

    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    /**
     * 统计激活用户数
     */
    long countByActiveTrue();

    /**
     * 统计指定角色的用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE :role IN elements(u.roles)")
    long countByRole(@Param("role") User.Type role);

    /**
     * 批量激活用户
     */
    @Modifying
    @Query("UPDATE User u SET u.active = true WHERE u.id IN :ids")
    void activateUsersByIds(@Param("ids") Set<Integer> ids);

    /**
     * 批量停用用户
     */
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.id IN :ids")
    void deactivateUsersByIds(@Param("ids") Set<Integer> ids);

    /**
     * 更新用户最后尝试时间
     */
    @Modifying
    @Query("UPDATE User u SET u.lastTryDate = :lastTryDate WHERE u.id = :userId")
    void updateLastTryDate(@Param("userId") Integer userId, @Param("lastTryDate") LocalDateTime lastTryDate);

    /**
     * 删除指定时间之前创建的非激活用户
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.active = false AND u.createdAt < :cutoffDate")
    void deleteInactiveUsersCreatedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 查找拥有指定标签的用户
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.tags t WHERE t.name = :tagName")
    List<User> findByTagName(@Param("tagName") String tagName);

    /**
     * 查找拥有多个指定标签的用户
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.tags t WHERE t.name IN :tagNames " +
            "GROUP BY u.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    List<User> findByAllTagNames(@Param("tagNames") List<String> tagNames, @Param("tagCount") long tagCount);

    /**
     * 获取用户的OJ平台统计
     */
    @Query("SELECT u, COUNT(oj) FROM User u LEFT JOIN u.ojAccounts oj WHERE u.id = :userId GROUP BY u")
    Object[] getUserWithOJCount(@Param("userId") Integer userId);
}
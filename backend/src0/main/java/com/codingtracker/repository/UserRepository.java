package com.codingtracker.repository;

import com.codingtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // 根据用户名查找用户
    Optional<User> findByUsername(String username);

    // 根据真实姓名查找用户
    Optional<User> findByRealName(String realName);

    // 根据用户角色查找用户列表
    List<User> findByRolesContaining(User.Type role);

    // 判断用户名是否已存在
    boolean existsByUsername(String username);

    // 判断真实姓名是否已存在
    boolean existsByRealName(String realName);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.realName LIKE %:keyword%")
    List<User> searchByUsernameOrRealName(String keyword);

    List<User> findAllById(Iterable<Integer> ids);

    List<User> findByRolesContains(User.Type role);

    List<User> findByUsernameContainingIgnoreCaseOrRealNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String keyword, String keyword1, String keyword2);

    /**
     * 优化的查询方法：分步加载避免笛卡尔积问题
     * 先加载用户和标签
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.tags " +
            "ORDER BY u.id")
    List<User> findAllWithTags();

    /**
     * 加载用户和OJ账号
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.ojAccounts " +
            "WHERE u.id IN :userIds " +
            "ORDER BY u.id")
    List<User> findUsersWithOJAccountsByIds(List<Integer> userIds);

    /**
     * 根据用户名查找用户，同时加载标签
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.tags " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithTags(String username);

    /**
     * 根据用户名查找用户，同时加载OJ账号
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.ojAccounts " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithOJAccounts(String username);

    /**
     * 优化的搜索方法：根据关键词搜索用户，同时加载标签
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.tags " +
            "WHERE u.username LIKE %:keyword% OR u.realName LIKE %:keyword% " +
            "ORDER BY u.id")
    List<User> searchByUsernameOrRealNameWithTags(String keyword);

    /**
     * 根据用户ID列表搜索用户，同时加载OJ账号
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.ojAccounts " +
            "WHERE u.id IN :userIds " +
            "ORDER BY u.id")
    List<User> findByIdsWithOJAccounts(List<Integer> userIds);
}

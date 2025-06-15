package com.codingtracker.repository;

import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.UserOJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOJRepository extends JpaRepository<UserOJ, Integer> {
    /**
     * 根据用户名和 OJ 类型查找用户的 OJ 账号
     *
     * @param username   用户名
     * @param ojPlatform OJ 类型
     * @return 找到的 OJ 账号列表
     */
    List<UserOJ> findByUserUsernameAndPlatform(String username, OJPlatform ojPlatform); // 返回多个 OJ 账号，改为 List

    /**
     * 根据用户名查找用户的所有 OJ 账号
     *
     * @param username 用户名
     * @return 用户的所有 OJ 账号列表
     */
    List<UserOJ> findByUserUsername(String username); // 返回多个 OJ 账号，改为 List

    /**
     * 根据用户ID删除所有OJ账号
     *
     * @param userId 用户ID
     */
    @Modifying
    @Query("DELETE FROM UserOJ oj WHERE oj.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户ID查找所有OJ账号
     *
     * @param userId 用户ID
     * @return 用户的所有OJ账号列表
     */
    List<UserOJ> findByUserId(Integer userId);
}

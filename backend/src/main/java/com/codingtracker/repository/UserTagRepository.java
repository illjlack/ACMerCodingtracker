package com.codingtracker.repository;

import com.codingtracker.model.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户标签数据访问层
 */
@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    /**
     * 根据标签名称查找标签
     */
    Optional<UserTag> findByName(String name);

    /**
     * 检查标签名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据标签名称模糊查询
     */
    @Query("SELECT t FROM UserTag t WHERE t.name LIKE %:name%")
    List<UserTag> findByNameContaining(@Param("name") String name);

    /**
     * 获取使用该标签的用户数量
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.tags t WHERE t.id = :tagId")
    long countUsersByTagId(@Param("tagId") Long tagId);

    /**
     * 查找所有标签，按名称排序
     */
    List<UserTag> findAllByOrderByNameAsc();
}
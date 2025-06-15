package com.codingtracker.core.domain.repository;

import com.codingtracker.core.domain.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户标签仓储接口
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    /**
     * 根据标签名查找
     */
    Optional<UserTag> findByName(String name);

    /**
     * 根据标签名查找（忽略大小写）
     */
    Optional<UserTag> findByNameIgnoreCase(String name);

    /**
     * 检查标签名是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查标签名是否存在（忽略大小写）
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * 查找激活的标签
     */
    List<UserTag> findByActiveTrueOrderBySortOrderAscNameAsc();

    /**
     * 查找非激活的标签
     */
    List<UserTag> findByActiveFalse();

    /**
     * 根据标签名模糊查找
     */
    @Query("SELECT t FROM UserTag t WHERE t.name LIKE %:name% ORDER BY t.sortOrder ASC, t.name ASC")
    List<UserTag> findByNameContaining(@Param("name") String name);

    /**
     * 根据标签名模糊查找激活的标签
     */
    @Query("SELECT t FROM UserTag t WHERE t.active = true AND t.name LIKE %:name% ORDER BY t.sortOrder ASC, t.name ASC")
    List<UserTag> findActiveByNameContaining(@Param("name") String name);

    /**
     * 查找热门标签（按用户数量排序）
     */
    @Query("SELECT t FROM UserTag t WHERE t.active = true ORDER BY SIZE(t.users) DESC, t.name ASC")
    List<UserTag> findPopularTags();

    /**
     * 查找热门标签（限制数量）
     */
    @Query("SELECT t FROM UserTag t WHERE t.active = true ORDER BY SIZE(t.users) DESC, t.name ASC")
    List<UserTag> findTopPopularTags(@Param("limit") int limit);

    /**
     * 查找未使用的标签
     */
    @Query("SELECT t FROM UserTag t WHERE SIZE(t.users) = 0 ORDER BY t.createdAt DESC")
    List<UserTag> findUnusedTags();

    /**
     * 统计标签总数
     */
    @Query("SELECT COUNT(t) FROM UserTag t")
    long countAllTags();

    /**
     * 统计激活标签数
     */
    long countByActiveTrue();

    /**
     * 统计非激活标签数
     */
    long countByActiveFalse();

    /**
     * 统计有用户使用的标签数
     */
    @Query("SELECT COUNT(t) FROM UserTag t WHERE SIZE(t.users) > 0")
    long countUsedTags();

    /**
     * 统计未使用的标签数
     */
    @Query("SELECT COUNT(t) FROM UserTag t WHERE SIZE(t.users) = 0")
    long countUnusedTags();

    /**
     * 获取标签使用统计
     */
    @Query("SELECT t.name, SIZE(t.users) as userCount FROM UserTag t WHERE t.active = true GROUP BY t.id, t.name ORDER BY userCount DESC")
    List<Object[]> getTagUsageStatistics();

    /**
     * 根据用户ID查找标签
     */
    @Query("SELECT t FROM UserTag t JOIN t.users u WHERE u.id = :userId")
    List<UserTag> findByUserId(@Param("userId") Integer userId);

    /**
     * 根据用户名查找标签
     */
    @Query("SELECT t FROM UserTag t JOIN t.users u WHERE u.username = :username")
    List<UserTag> findByUsername(@Param("username") String username);

    /**
     * 查找指定颜色的标签
     */
    List<UserTag> findByColor(String color);

    /**
     * 按排序权重查找标签
     */
    List<UserTag> findAllByOrderBySortOrderAscNameAsc();

    /**
     * 查找排序权重在指定范围内的标签
     */
    List<UserTag> findBySortOrderBetweenOrderBySortOrderAsc(Integer minOrder, Integer maxOrder);

    /**
     * 批量激活标签
     */
    @Modifying
    @Query("UPDATE UserTag t SET t.active = true WHERE t.id IN :ids")
    void activateByIds(@Param("ids") List<Long> ids);

    /**
     * 批量停用标签
     */
    @Modifying
    @Query("UPDATE UserTag t SET t.active = false WHERE t.id IN :ids")
    void deactivateByIds(@Param("ids") List<Long> ids);

    /**
     * 更新标签排序权重
     */
    @Modifying
    @Query("UPDATE UserTag t SET t.sortOrder = :sortOrder WHERE t.id = :id")
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    /**
     * 删除未使用的标签
     */
    @Modifying
    @Query("DELETE FROM UserTag t WHERE SIZE(t.users) = 0 AND t.active = false")
    void deleteUnusedInactiveTags();

    /**
     * 查找最近创建的标签
     */
    List<UserTag> findTop10ByOrderByCreatedAtDesc();

    /**
     * 查找最近更新的标签
     */
    List<UserTag> findTop10ByOrderByUpdatedAtDesc();

    /**
     * 根据多个标签名查找
     */
    @Query("SELECT t FROM UserTag t WHERE t.name IN :names")
    List<UserTag> findByNameIn(@Param("names") List<String> names);

    /**
     * 查找系统标签（根据名称模式）
     */
    @Query("SELECT t FROM UserTag t WHERE t.name LIKE '%管理员%' OR t.name LIKE '%系统%' OR t.name LIKE '%admin%' OR t.name LIKE '%system%'")
    List<UserTag> findSystemTags();
}
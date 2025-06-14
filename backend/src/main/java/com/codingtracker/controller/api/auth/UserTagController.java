package com.codingtracker.controller.api.auth;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.model.UserTag;
import com.codingtracker.service.UserTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户标签管理控制器
 */
@RestController
@RequestMapping("/api/admin/user-tags")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class UserTagController {

    private static final Logger logger = LoggerFactory.getLogger(UserTagController.class);

    @Autowired
    private UserTagService userTagService;

    /**
     * 获取所有标签
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserTag>>> getAllTags() {
        logger.info("请求获取所有用户标签");
        List<UserTag> tags = userTagService.getAllTags();
        logger.info("成功返回用户标签列表，共{}个标签", tags.size());
        return ResponseEntity.ok(ApiResponse.ok(tags));
    }

    /**
     * 根据ID获取标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserTag>> getTagById(@PathVariable Long id) {
        return userTagService.getTagById(id)
                .map(tag -> ResponseEntity.ok(ApiResponse.ok(tag)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建新标签
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserTag>> createTag(@RequestBody UserTag tag) {
        logger.info("请求创建用户标签: 标签名={}", tag.getName());
        try {
            UserTag createdTag = userTagService.createTag(tag);
            logger.info("用户标签创建成功: 标签ID={}, 标签名={}", createdTag.getId(), createdTag.getName());
            return ResponseEntity.ok(ApiResponse.ok(createdTag));
        } catch (RuntimeException e) {
            logger.error("用户标签创建失败: 标签名={}, 错误={}", tag.getName(), e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserTag>> updateTag(@PathVariable Long id, @RequestBody UserTag tag) {
        logger.info("请求更新用户标签: 标签ID={}, 标签名={}", id, tag.getName());
        try {
            UserTag updatedTag = userTagService.updateTag(id, tag);
            logger.info("用户标签更新成功: 标签ID={}, 标签名={}", id, updatedTag.getName());
            return ResponseEntity.ok(ApiResponse.ok(updatedTag));
        } catch (RuntimeException e) {
            logger.error("用户标签更新失败: 标签ID={}, 错误={}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        logger.info("请求删除用户标签: 标签ID={}", id);
        try {
            userTagService.deleteTag(id);
            logger.info("用户标签删除成功: 标签ID={}", id);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (RuntimeException e) {
            logger.error("用户标签删除失败: 标签ID={}, 错误={}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 为用户添加标签
     */
    @PostMapping("/users/{userId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> addTagToUser(
            @PathVariable Integer userId,
            @PathVariable Long tagId) {
        try {
            userTagService.addTagToUser(userId, tagId);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 从用户移除标签
     */
    @DeleteMapping("/users/{userId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> removeTagFromUser(
            @PathVariable Integer userId,
            @PathVariable Long tagId) {
        try {
            userTagService.removeTagFromUser(userId, tagId);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 设置用户的所有标签
     */
    @PutMapping("/users/{userId}/tags")
    public ResponseEntity<ApiResponse<Void>> setUserTags(
            @PathVariable Integer userId,
            @RequestBody List<Long> tagIds) {
        logger.info("请求设置用户标签: 用户ID={}, 标签数量={}", userId, tagIds.size());
        try {
            userTagService.setUserTags(userId, new HashSet<>(tagIds));
            logger.info("用户标签设置成功: 用户ID={}, 标签ID列表={}", userId, tagIds);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (RuntimeException e) {
            logger.error("用户标签设置失败: 用户ID={}, 错误={}", userId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 搜索标签
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserTag>>> searchTags(@RequestParam String name) {
        return ResponseEntity.ok(ApiResponse.ok(userTagService.searchTagsByName(name)));
    }

    /**
     * 获取标签使用统计
     */
    @GetMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<Long>> getTagUsage(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userTagService.getTagUsageCount(id)));
    }
}
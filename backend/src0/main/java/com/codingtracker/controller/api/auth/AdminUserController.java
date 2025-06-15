package com.codingtracker.controller.api.auth;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.dto.UserCreateRequest;
import com.codingtracker.dto.UserUpdateRequest;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.User;
import com.codingtracker.service.UserService;
import com.codingtracker.service.UserTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserTagService userTagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        logger.info("管理员请求获取所有用户列表");
        List<User> users = userService.getAllUsers();
        logger.info("成功返回用户列表，共{}个用户", users.size());
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Integer id) {
        logger.info("管理员请求获取用户详情: 用户ID={}", id);
        User user = userService.getUserById(id);
        logger.info("成功返回用户详情: 用户ID={}, 用户名={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody UserCreateRequest request) {
        logger.info("管理员请求创建用户: 用户名={}", request.getUsername());
        try {
            // 验证超级管理员唯一性
            userService.validateSuperAdminUniqueness();

            // 检查是否试图创建超级管理员
            if (request.getRoles() != null && request.getRoles().contains(User.Type.SUPER_ADMIN)) {
                logger.warn("尝试创建超级管理员用户被拒绝: 用户名={}", request.getUsername());
                return ResponseEntity.ok(ApiResponse.error("不允许创建超级管理员用户"));
            }

            User createdUser = userService.createUserFromRequest(request);

            // 处理标签设置
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                userTagService.setUserTags(createdUser.getId(), new HashSet<>(request.getTags()));
            }

            logger.info("用户创建成功: 用户ID={}, 用户名={}", createdUser.getId(), createdUser.getUsername());
            return ResponseEntity.ok(ApiResponse.ok(createdUser));
        } catch (Exception e) {
            logger.error("用户创建失败: 用户名={}, 错误={}", request.getUsername(), e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Integer id,
            @RequestBody UserUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        logger.info("管理员请求更新用户: 操作者={}, 目标用户ID={}", currentUsername, id);

        try {
            // 验证超级管理员唯一性
            userService.validateSuperAdminUniqueness();

            // 检查是否试图设置超级管理员角色
            if (request.getRoles() != null && request.getRoles().contains(User.Type.SUPER_ADMIN)) {
                logger.warn("尝试设置超级管理员角色被拒绝: 操作者={}, 目标用户ID={}", currentUsername, id);
                return ResponseEntity.ok(ApiResponse.error("不允许设置超级管理员角色"));
            }

            // 检查是否有权限编辑目标用户
            if (!userService.canEditUser(currentUsername, id)) {
                logger.warn("用户更新权限不足: 操作者={}, 目标用户ID={}", currentUsername, id);
                return ResponseEntity.ok(ApiResponse.error("权限不足：无法编辑该用户"));
            }

            User updatedUser = userService.updateUserByAdminFromRequest(id, request, currentUsername);

            // 处理标签设置
            if (request.getTags() != null) {
                userTagService.setUserTags(updatedUser.getId(), new HashSet<>(request.getTags()));
            }

            logger.info("用户更新成功: 操作者={}, 目标用户ID={}, 用户名={}", currentUsername, id, updatedUser.getUsername());
            return ResponseEntity.ok(ApiResponse.ok(updatedUser));
        } catch (RuntimeException e) {
            logger.warn("用户更新失败: 操作者={}, 目标用户ID={}, 错误={}", currentUsername, id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        logger.info("管理员请求删除用户: 操作者={}, 目标用户ID={}", currentUsername, id);

        try {
            if (!userService.canDeleteUser(currentUsername, id)) {
                logger.warn("用户删除权限不足: 操作者={}, 目标用户ID={}", currentUsername, id);
                return ResponseEntity.ok(ApiResponse.error("权限不足：无法删除该用户"));
            }

            userService.deleteUser(id);
            logger.info("用户删除成功: 操作者={}, 目标用户ID={}", currentUsername, id);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (RuntimeException e) {
            logger.error("用户删除失败: 操作者={}, 目标用户ID={}, 错误={}", currentUsername, id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        logger.info("管理员请求切换用户状态: 操作者={}, 目标用户ID={}", currentUsername, id);

        try {
            if (!userService.canEditUser(currentUsername, id)) {
                logger.warn("用户状态切换权限不足: 操作者={}, 目标用户ID={}", currentUsername, id);
                return ResponseEntity.ok(ApiResponse.error("权限不足：无法修改该用户状态"));
            }

            userService.toggleUserStatus(id);
            logger.info("用户状态切换成功: 操作者={}, 目标用户ID={}", currentUsername, id);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (RuntimeException e) {
            logger.error("用户状态切换失败: 操作者={}, 目标用户ID={}, 错误={}", currentUsername, id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(@RequestParam String keyword) {
        logger.info("管理员搜索用户: 关键词={}", keyword);
        List<User> users = userService.searchUsers(keyword);
        logger.info("用户搜索完成: 关键词={}, 结果数量={}", keyword, users.size());
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    /**
     * 获取支持的OJ平台列表
     */
    @GetMapping("/oj-platforms")
    public ResponseEntity<ApiResponse<List<OJPlatformDTO>>> getOJPlatforms() {
        logger.info("请求获取OJ平台列表");
        List<OJPlatformDTO> platforms = Arrays.stream(OJPlatform.values())
                .filter(platform -> platform != OJPlatform.UNKNOWN) // 过滤掉UNKNOWN
                .map(platform -> new OJPlatformDTO(platform.name(), platform.getNames().get(0)))
                .collect(Collectors.toList());
        logger.info("成功返回OJ平台列表，共{}个平台", platforms.size());
        return ResponseEntity.ok(ApiResponse.ok(platforms));
    }

    /**
     * OJ平台数据传输对象
     */
    public static class OJPlatformDTO {
        private String code;
        private String name;

        public OJPlatformDTO(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
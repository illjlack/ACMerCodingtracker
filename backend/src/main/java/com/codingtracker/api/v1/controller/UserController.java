package com.codingtracker.api.v1.controller;

import com.codingtracker.core.service.UserService;
import com.codingtracker.shared.dto.request.UserCreateRequest;
import com.codingtracker.shared.dto.request.UserUpdateRequest;
import com.codingtracker.shared.dto.response.ApiResponse;
import com.codingtracker.shared.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户管理控制器
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Tag(name = "用户管理", description = "用户相关的CRUD操作")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    // ==================== 用户查询 ====================

    @Operation(summary = "获取所有用户", description = "分页获取用户列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("获取用户列表，分页参数: {}", pageable);
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ApiResponse.success(users, "获取用户列表成功");
    }

    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @userSecurityService.canViewUser(authentication.name, #id)")
    public ApiResponse<UserResponse> getUserById(
            @Parameter(description = "用户ID") @PathVariable Integer id) {

        log.info("获取用户详情，用户ID: {}", id);
        return userService.findById(id)
                .map(user -> ApiResponse.success(user, "获取用户信息成功"))
                .orElse(ApiResponse.error("用户不存在", 404));
    }

    @Operation(summary = "根据用户名获取用户", description = "根据用户名获取用户详细信息")
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or authentication.name == #username")
    public ApiResponse<UserResponse> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {

        log.info("根据用户名获取用户: {}", username);
        return userService.findByUsername(username)
                .map(user -> ApiResponse.success(user, "获取用户信息成功"))
                .orElse(ApiResponse.error("用户不存在", 404));
    }

    @Operation(summary = "搜索用户", description = "根据关键字搜索用户")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<UserResponse>> searchUsers(
            @Parameter(description = "搜索关键字") @RequestParam String keyword) {

        log.info("搜索用户，关键字: {}", keyword);
        List<UserResponse> users = userService.searchUsers(keyword);
        return ApiResponse.success(users, "搜索用户成功");
    }

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("获取当前用户信息: {}", username);

        return userService.findByUsername(username)
                .map(user -> ApiResponse.success(user, "获取当前用户信息成功"))
                .orElse(ApiResponse.error("用户信息获取失败", 404));
    }

    // ==================== 用户创建与更新 ====================

    @Operation(summary = "创建新用户", description = "管理员创建新用户")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("管理员 {} 创建新用户: {}", userDetails.getUsername(), request.getUsername());
        UserResponse user = userService.createUser(request);
        return ApiResponse.success(user, "用户创建成功");
    }

    @Operation(summary = "更新用户信息", description = "更新指定用户的信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @userSecurityService.canEditUser(authentication.name, #id)")
    public ApiResponse<UserResponse> updateUser(
            @Parameter(description = "用户ID") @PathVariable Integer id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("用户 {} 更新用户信息，目标用户ID: {}", userDetails.getUsername(), id);
        UserResponse user = userService.updateUser(id, request);
        return ApiResponse.success(user, "用户信息更新成功");
    }

    @Operation(summary = "管理员更新用户信息", description = "管理员更新用户信息（权限更高）")
    @PutMapping("/{id}/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> updateUserByAdmin(
            @Parameter(description = "用户ID") @PathVariable Integer id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("管理员 {} 更新用户信息，目标用户ID: {}", userDetails.getUsername(), id);
        UserResponse user = userService.updateUserByAdmin(id, request, userDetails.getUsername());
        return ApiResponse.success(user, "用户信息更新成功");
    }

    // ==================== 用户状态管理 ====================

    @Operation(summary = "激活用户", description = "激活指定用户")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> activateUser(
            @Parameter(description = "用户ID") @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("管理员 {} 激活用户，用户ID: {}", userDetails.getUsername(), id);
        userService.activateUser(id);
        return ApiResponse.success(null, "用户激活成功");
    }

    @Operation(summary = "停用用户", description = "停用指定用户")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deactivateUser(
            @Parameter(description = "用户ID") @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("管理员 {} 停用用户，用户ID: {}", userDetails.getUsername(), id);
        userService.deactivateUser(id);
        return ApiResponse.success(null, "用户停用成功");
    }

    @Operation(summary = "删除用户", description = "删除指定用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("超级管理员 {} 删除用户，用户ID: {}", userDetails.getUsername(), id);
        userService.deleteUser(id);
        return ApiResponse.success(null, "用户删除成功");
    }

    // ==================== 头像管理 ====================

    @Operation(summary = "上传用户头像", description = "上传并设置用户头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.info("用户 {} 上传头像", username);

        String avatarUrl = userService.uploadAvatar(username, file);
        return ApiResponse.success(avatarUrl, "头像上传成功");
    }

    // ==================== 用户验证 ====================

    @Operation(summary = "检查用户名是否存在", description = "检查指定用户名是否已被使用")
    @GetMapping("/check-username")
    public ApiResponse<Boolean> checkUsername(
            @Parameter(description = "用户名") @RequestParam String username) {

        boolean exists = userService.existsByUsername(username);
        return ApiResponse.success(exists, exists ? "用户名已存在" : "用户名可用");
    }

    @Operation(summary = "检查邮箱是否存在", description = "检查指定邮箱是否已被使用")
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(
            @Parameter(description = "邮箱") @RequestParam String email) {

        boolean exists = userService.existsByEmail(email);
        return ApiResponse.success(exists, exists ? "邮箱已存在" : "邮箱可用");
    }

    // ==================== 统计信息 ====================

    @Operation(summary = "获取用户统计信息", description = "获取用户相关的统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserStatistics> getUserStatistics() {

        log.info("获取用户统计信息");

        UserStatistics statistics = UserStatistics.builder()
                .totalUsers(userService.getTotalUserCount())
                .activeUsers(userService.getActiveUserCount())
                .adminUsers(userService.getUserCountByRole(com.codingtracker.core.domain.entity.User.Type.ADMIN))
                .superAdminUsers(
                        userService.getUserCountByRole(com.codingtracker.core.domain.entity.User.Type.SUPER_ADMIN))
                .regularUsers(userService.getUserCountByRole(com.codingtracker.core.domain.entity.User.Type.USER))
                .build();

        return ApiResponse.success(statistics, "获取统计信息成功");
    }

    /**
     * 用户统计信息内部类
     */
    @Data
    @Builder
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long adminUsers;
        private long superAdminUsers;
        private long regularUsers;
    }
}
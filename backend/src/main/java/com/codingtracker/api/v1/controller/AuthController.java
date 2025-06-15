package com.codingtracker.api.v1.controller;

import com.codingtracker.core.service.UserAuthService;
import com.codingtracker.shared.dto.request.LoginRequest;
import com.codingtracker.shared.dto.request.RegisterRequest;
import com.codingtracker.shared.dto.request.PasswordChangeRequest;
import com.codingtracker.shared.dto.response.ApiResponse;
import com.codingtracker.shared.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证管理", description = "用户认证相关API")
public class AuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过用户名和密码进行登录")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());

        AuthResponse authResponse = userAuthService.login(request);

        return ResponseEntity.ok(ApiResponse.success(authResponse, "登录成功"));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求: {}", request.getUsername());

        AuthResponse authResponse = userAuthService.register(request);

        return ResponseEntity.ok(ApiResponse.success(authResponse, "注册成功"));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    public ResponseEntity<ApiResponse<Void>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            userAuthService.logout(username);
            log.info("用户登出: {}", username);
        }

        return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.info("用户修改密码: {}", username);

        userAuthService.changePassword(username, request);

        return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌", description = "刷新访问令牌")
    public ResponseEntity<ApiResponse<String>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        log.debug("刷新令牌请求");

        String token = authHeader.substring(7); // 移除 "Bearer " 前缀
        String newToken = userAuthService.refreshToken(token);

        return ResponseEntity.ok(ApiResponse.success(newToken, "令牌刷新成功"));
    }

    @GetMapping("/validate")
    @Operation(summary = "验证令牌", description = "验证当前令牌是否有效")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        log.debug("验证令牌请求");

        String token = authHeader.substring(7); // 移除 "Bearer " 前缀
        boolean isValid = userAuthService.validateToken(token);

        return ResponseEntity.ok(ApiResponse.success(isValid, "令牌验证完成"));
    }

    @GetMapping("/check-username")
    @Operation(summary = "检查用户名", description = "检查用户名是否已存在")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(
            @Parameter(description = "用户名") @RequestParam String username) {
        log.debug("检查用户名: {}", username);

        boolean exists = userAuthService.checkUserExists(username);

        return ResponseEntity.ok(ApiResponse.success(exists, "用户名检查完成"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Parameter(description = "用户名或邮箱") @RequestParam String usernameOrEmail,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        log.info("重置用户密码: {}", usernameOrEmail);

        userAuthService.resetPassword(usernameOrEmail, newPassword);

        return ResponseEntity.ok(ApiResponse.success(null, "密码重置成功"));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息")
    public ResponseEntity<ApiResponse<String>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.debug("获取当前用户信息: {}", username);

        return ResponseEntity.ok(ApiResponse.success(username, "获取用户信息成功"));
    }
}
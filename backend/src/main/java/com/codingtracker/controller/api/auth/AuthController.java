// src/main/java/com/codingtracker/controller/api/auth/AuthController.java
package com.codingtracker.controller.api.auth;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.dto.UserInfoDTO;
import com.codingtracker.model.User;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.service.TokenBlacklistCache;
import com.codingtracker.service.UserService;
import com.codingtracker.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenBlacklistCache tokenBlacklistCache;

    public AuthController(UserService userService, UserRepository userRepository,
            TokenBlacklistCache tokenBlacklistCache) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokenBlacklistCache = tokenBlacklistCache;
    }

    // 用户登录并返回 token
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        logger.info("用户登录尝试: 用户名={}", req.username);
        User user = userService.valid(req.username, req.password);
        if (user != null) {
            String token = JwtUtils.generateToken(req.username);
            logger.info("用户登录成功: 用户名={}", req.username);
            Map<String, Object> data = Map.of(
                    "token", token);
            return ApiResponse.ok("登录成功", data);
        } else {
            logger.warn("用户登录失败: 用户名={}", req.username);
            return ApiResponse.error("用户名或密码错误");
        }
    }

    // 用户注册
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest req) {
        logger.info("用户注册尝试: 用户名={}", req.username);
        User user = new User();
        user.setUsername(req.username);
        user.setPassword(req.password);
        user.setRealName(req.realName);
        user.setMajor(req.major);
        user.setEmail(req.email);

        boolean ok = userService.registerUser(user);
        if (ok) {
            logger.info("用户注册成功: 用户名={}", req.username);
            return ApiResponse.ok("注册成功，请登录！", null);
        } else {
            logger.error("用户注册失败: 用户名={}", req.username);
            return ApiResponse.error("注册失败！");
        }
    }

    // 修改用户信息
    @PutMapping("/modify")
    public ApiResponse<Void> modifyUser(@RequestBody UserInfoDTO user) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("请求获取用户信息: 用户名={}", username);
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("未找到用户信息: 用户名={}", username);
            return ApiResponse.error("未找到用户信息");
        }
        // 调用业务层修改
        userService.modifyUser(user);
        return ApiResponse.ok();
    }

    // 修改密码
    @PutMapping("/modifyPassword")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        logger.info("用户尝试修改密码: 用户名={}", req.username);
        User user = userService.valid(req.username, req.oldPassword);
        if (user != null) {
            userService.modifyUserPassword(user.getId(), req.newPassword);
            logger.info("用户密码修改成功: 用户名={}", req.username);
            return ApiResponse.ok("密码修改成功", null);
        } else {
            logger.warn("用户密码修改失败（旧密码错误或用户不存在）: 用户名={}", req.username);
            return ApiResponse.error("旧密码错误或用户不存在");
        }
    }

    // 获取用户信息
    @GetMapping("/userInfo")
    public ApiResponse<UserInfoDTO> getUserInfo() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("请求获取用户个人信息: 用户名={}", username);
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("未找到用户个人信息: 用户名={}", username);
            return ApiResponse.error("未找到用户信息");
        }
        UserInfoDTO dto = UserInfoDTO.fromUser(userOpt.get());
        logger.info("成功返回用户个人信息: 用户名={}", username);
        return ApiResponse.ok("获取成功", dto);
    }

    @GetMapping("/info")
    public ApiResponse<UserInfoDTO> getUserInfo(@RequestParam("username") String username) {
        logger.info("请求获取用户信息: 用户名={}", username);
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("未找到用户信息: 用户名={}", username);
            return ApiResponse.error("未找到用户信息");
        }
        UserInfoDTO dto = UserInfoDTO.fromUser(userOpt.get());
        logger.info("成功返回用户信息: 用户名={}", username);
        return ApiResponse.ok("获取成功", dto);
    }

    // 用户登出
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("用户登出: 用户名={}", username);

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 将 token 加入黑名单缓存，标记为失效
            tokenBlacklistCache.blacklistToken(token);
            logger.info("将用户 {} 的 token 加入黑名单", username);
        }
        return ApiResponse.ok("登出成功", null);
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("用户上传头像: 用户名={}", username);

        if (file.isEmpty()) {
            return ApiResponse.error("上传文件为空");
        }

        try {
            // 调用业务层保存头像，返回是否成功
            boolean success = userService.storeAvatar(username, file);

            if (success) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    String avatarUrl = userOpt.get().getAvatar();
                    if (avatarUrl == null || avatarUrl.isEmpty()) {
                        logger.warn("头像URL为空: 用户名={}", username);
                        return ApiResponse.ok("上传成功，但头像URL为空", Map.of("url", ""));
                    } else {
                        logger.info("头像上传成功: 用户名={}, 头像URL={}", username, avatarUrl);
                        return ApiResponse.ok("上传成功", Map.of("url", avatarUrl));
                    }
                } else {
                    logger.warn("上传成功但未找到用户信息: 用户名={}", username);
                    return ApiResponse.error("上传成功但未找到用户信息");
                }
            } else {
                logger.error("头像上传失败: 用户名={}", username);
                return ApiResponse.error("上传失败");
            }

        } catch (Exception e) {
            logger.error("头像上传异常: 用户名={}, 异常={}", username, e.getMessage());
            return ApiResponse.error("上传异常");
        }
    }

    /** 请求体和内部 DTO **/
    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String major;
        private String email;
    }

    @Getter
    @Setter
    public static class ChangePasswordRequest {
        private String username;
        private String oldPassword;
        private String newPassword;
    }
}

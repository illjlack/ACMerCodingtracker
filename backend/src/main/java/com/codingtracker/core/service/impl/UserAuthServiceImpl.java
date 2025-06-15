package com.codingtracker.core.service.impl;

import com.codingtracker.core.domain.entity.User;
import com.codingtracker.core.domain.repository.UserRepository;
import com.codingtracker.core.service.UserAuthService;
import com.codingtracker.infrastructure.security.JwtTokenProvider;
import com.codingtracker.shared.dto.request.LoginRequest;
import com.codingtracker.shared.dto.request.RegisterRequest;
import com.codingtracker.shared.dto.request.PasswordChangeRequest;
import com.codingtracker.shared.dto.response.AuthResponse;
import com.codingtracker.shared.exception.UserNotFoundException;
import com.codingtracker.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;

/**
 * 用户认证服务实现类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("用户登录尝试: {}", request.getUsername());

        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        // 检查用户状态
        if (!user.isActive()) {
            throw new ValidationException("用户账号已被禁用，请联系管理员");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastTryDate(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT令牌
        String token = jwtTokenProvider.generateToken(user.getUsername());

        log.info("用户登录成功: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roles(user.getRoles())
                .message("登录成功")
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("用户注册申请: {}", request.getUsername());

        // 验证请求
        validateRegisterRequest(request);

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ValidationException.duplicate("用户名", request.getUsername());
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ValidationException.duplicate("邮箱", request.getEmail());
        }

        // 创建用户实体
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .email(request.getEmail())
                .major(request.getMajor())
                .active(true)
                .roles(Set.of(User.Type.USER))
                .ojAccounts(new ArrayList<>())
                .tags(new HashSet<>())
                .build();

        // 检查是否是第一个用户
        if (userRepository.count() == 0) {
            user.addRole(User.Type.SUPER_ADMIN);
            log.info("注册首个用户为超级管理员: {}", request.getUsername());
        }

        // 保存用户
        User savedUser = userRepository.save(user);

        // 生成JWT令牌
        String token = jwtTokenProvider.generateToken(savedUser.getUsername());

        log.info("用户注册成功: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .realName(savedUser.getRealName())
                .email(savedUser.getEmail())
                .avatar(savedUser.getAvatar())
                .roles(savedUser.getRoles())
                .message("注册成功")
                .build();
    }

    @Override
    public void changePassword(String username, PasswordChangeRequest request) {
        log.info("用户修改密码: {}", username);

        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("原密码错误");
        }

        // 验证新密码确认
        if (!request.isPasswordMatch()) {
            throw ValidationException.passwordMismatch();
        }

        // 检查新密码是否与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ValidationException("新密码不能与原密码相同");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("用户密码修改成功: {}", username);
    }

    @Override
    public void resetPassword(String usernameOrEmail, String newPassword) {
        log.info("重置用户密码: {}", usernameOrEmail);

        // 查找用户（通过用户名或邮箱）
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("用户密码重置成功: {}", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            log.debug("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    @Override
    public void logout(String username) {
        log.info("用户登出: {}", username);
        // 在无状态JWT系统中，登出通常在客户端处理
        // 如果需要服务端登出支持，可以维护一个黑名单
        log.info("用户登出成功: {}", username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserExists(String usernameOrEmail) {
        return userRepository.existsByUsername(usernameOrEmail) ||
                userRepository.existsByEmail(usernameOrEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserActive(String username) {
        return userRepository.findByUsername(username)
                .map(User::isActive)
                .orElse(false);
    }

    @Override
    public String refreshToken(String token) {
        log.debug("刷新令牌");

        if (!jwtTokenProvider.validateToken(token)) {
            throw new ValidationException("无效的令牌");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);

        // 检查用户是否存在且活跃
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (!user.isActive()) {
            throw new ValidationException("用户账号已被禁用");
        }

        // 生成新令牌
        String newToken = jwtTokenProvider.generateToken(username);

        log.debug("令牌刷新成功: {}", username);
        return newToken;
    }

    // ==================== 私有方法 ====================

    /**
     * 验证注册请求
     */
    private void validateRegisterRequest(RegisterRequest request) {
        // 验证密码确认
        if (!request.isPasswordMatch()) {
            throw ValidationException.passwordMismatch();
        }

        // 验证用户名格式
        if (!isValidUsername(request.getUsername())) {
            throw new ValidationException("用户名格式不正确，只能包含字母、数字和下划线，长度3-20位");
        }

        // 验证密码强度
        if (!isValidPassword(request.getPassword())) {
            throw new ValidationException("密码强度不够，至少8位，包含字母和数字");
        }

        // 验证邮箱格式
        if (!isValidEmail(request.getEmail())) {
            throw new ValidationException("邮箱格式不正确");
        }
    }

    /**
     * 根据用户名或邮箱查找用户
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        // 尝试按用户名查找
        Optional<User> userByUsername = userRepository.findByUsername(usernameOrEmail);
        if (userByUsername.isPresent()) {
            return userByUsername.get();
        }

        // 尝试按邮箱查找
        return userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException(usernameOrEmail));
    }

    /**
     * 验证用户名格式
     */
    private boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * 验证密码强度
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        // 至少包含一个字母和一个数字
        return password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
    }

    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }
}
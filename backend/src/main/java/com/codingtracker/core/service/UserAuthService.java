package com.codingtracker.core.service;

import com.codingtracker.shared.dto.request.LoginRequest;
import com.codingtracker.shared.dto.request.RegisterRequest;
import com.codingtracker.shared.dto.request.PasswordChangeRequest;
import com.codingtracker.shared.dto.response.AuthResponse;

/**
 * 用户认证服务接口
 * 专门处理用户登录、注册、密码管理等认证相关业务
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
public interface UserAuthService {

    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录验证
     */
    AuthResponse login(LoginRequest request);

    /**
     * 刷新访问令牌
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 修改密码
     */
    void changePassword(String username, PasswordChangeRequest request);

    /**
     * 重置密码（忘记密码）
     */
    void resetPassword(String email);

    /**
     * 确认重置密码
     */
    void confirmResetPassword(String token, String newPassword);

    /**
     * 验证令牌是否有效
     */
    boolean validateToken(String token);

    /**
     * 从令牌中获取用户名
     */
    String getUsernameFromToken(String token);

    /**
     * 检查密码强度
     */
    boolean isPasswordStrong(String password);

    /**
     * 验证当前密码
     */
    boolean validateCurrentPassword(String username, String password);
}
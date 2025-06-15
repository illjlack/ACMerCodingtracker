package com.codingtracker.core.service;

import com.codingtracker.core.domain.entity.User;
import com.codingtracker.shared.dto.request.UserCreateRequest;
import com.codingtracker.shared.dto.request.UserUpdateRequest;
import com.codingtracker.shared.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 专注于用户的核心业务逻辑
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
public interface UserService {

    // ==================== 用户查询 ====================

    /**
     * 根据用户名查找用户
     */
    Optional<UserResponse> findByUsername(String username);

    /**
     * 根据ID查找用户
     */
    Optional<UserResponse> findById(Integer id);

    /**
     * 根据邮箱查找用户
     */
    Optional<UserResponse> findByEmail(String email);

    /**
     * 获取所有用户（分页）
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * 搜索用户
     */
    List<UserResponse> searchUsers(String keyword);

    /**
     * 根据角色查找用户
     */
    List<UserResponse> findByRole(User.Type role);

    // ==================== 用户创建与更新 ====================

    /**
     * 创建新用户
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * 更新用户信息
     */
    UserResponse updateUser(Integer userId, UserUpdateRequest request);

    /**
     * 管理员更新用户信息
     */
    UserResponse updateUserByAdmin(Integer userId, UserUpdateRequest request, String adminUsername);

    // ==================== 用户状态管理 ====================

    /**
     * 激活用户
     */
    void activateUser(Integer userId);

    /**
     * 停用用户
     */
    void deactivateUser(Integer userId);

    /**
     * 删除用户
     */
    void deleteUser(Integer userId);

    // ==================== 头像管理 ====================

    /**
     * 上传并设置用户头像
     */
    String uploadAvatar(String username, MultipartFile file);

    // ==================== 用户验证 ====================

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    // ==================== 统计信息 ====================

    /**
     * 获取用户总数
     */
    long getTotalUserCount();

    /**
     * 获取激活用户数
     */
    long getActiveUserCount();

    /**
     * 获取指定角色的用户数
     */
    long getUserCountByRole(User.Type role);
}
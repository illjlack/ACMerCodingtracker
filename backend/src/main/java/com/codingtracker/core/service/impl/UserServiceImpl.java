package com.codingtracker.core.service.impl;

import com.codingtracker.core.domain.entity.User;
import com.codingtracker.core.domain.entity.UserOJ;
import com.codingtracker.core.domain.entity.UserTag;
import com.codingtracker.core.domain.repository.UserRepository;
import com.codingtracker.core.domain.repository.UserOJRepository;
import com.codingtracker.core.domain.repository.UserTagRepository;
import com.codingtracker.core.service.UserService;
import com.codingtracker.infrastructure.external.AvatarStorageService;
import com.codingtracker.shared.dto.request.UserCreateRequest;
import com.codingtracker.shared.dto.request.UserUpdateRequest;
import com.codingtracker.shared.dto.response.UserResponse;
import com.codingtracker.shared.dto.response.UserOJResponse;
import com.codingtracker.shared.dto.response.UserTagResponse;
import com.codingtracker.shared.exception.UserNotFoundException;
import com.codingtracker.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserOJRepository userOJRepository;
    private final UserTagRepository userTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> findByUsername(String username) {
        log.debug("查找用户: {}", username);
        return userRepository.findByUsername(username)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> findById(Integer id) {
        log.debug("根据ID查找用户: {}", id);
        return userRepository.findById(id)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> findByEmail(String email) {
        log.debug("根据邮箱查找用户: {}", email);
        return userRepository.findByEmail(email)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("获取用户列表，分页参数: {}", pageable);
        return userRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String keyword) {
        log.debug("搜索用户，关键字: {}", keyword);
        return userRepository.searchByKeyword(keyword)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findByRole(User.Type role) {
        log.debug("根据角色查找用户: {}", role);
        return userRepository.findByRolesContains(role)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        log.info("创建新用户: {}", request.getUsername());

        // 验证请求
        validateCreateRequest(request);

        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ValidationException.duplicate("用户名", request.getUsername());
        }
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
                .roles(request.getRoles() != null ? request.getRoles() : Set.of(User.Type.USER))
                .ojAccounts(new ArrayList<>())
                .tags(new HashSet<>())
                .build();

        // 检查是否是第一个用户
        if (userRepository.count() == 0) {
            user.addRole(User.Type.SUPER_ADMIN);
            log.info("创建首个用户为超级管理员: {}", request.getUsername());
        }

        // 保存用户
        User savedUser = userRepository.save(user);

        // 处理OJ账号
        if (request.getOjAccounts() != null && !request.getOjAccounts().isEmpty()) {
            processOJAccounts(savedUser, request.getOjAccounts());
        }

        // 处理标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            processTags(savedUser, request.getTags());
        }

        log.info("用户创建成功: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return convertToResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Integer userId, UserUpdateRequest request) {
        log.info("更新用户信息，用户ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 更新基本信息
        updateBasicInfo(user, request);

        // 保存用户
        User updatedUser = userRepository.save(user);

        log.info("用户信息更新成功: {} (ID: {})", updatedUser.getUsername(), updatedUser.getId());
        return convertToResponse(updatedUser);
    }

    @Override
    public UserResponse updateUserByAdmin(Integer userId, UserUpdateRequest request, String adminUsername) {
        log.info("管理员 {} 更新用户信息，用户ID: {}", adminUsername, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new UserNotFoundException(adminUsername));

        // 权限验证
        validateAdminPermission(admin, user, request);

        // 更新基本信息
        updateBasicInfo(user, request);

        // 管理员可以更新角色和状态
        if (request.getRoles() != null) {
            user.setRoles(request.getRoles());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        // 保存用户
        User updatedUser = userRepository.save(user);

        log.info("管理员更新用户信息成功: {} (ID: {})", updatedUser.getUsername(), updatedUser.getId());
        return convertToResponse(updatedUser);
    }

    @Override
    public void activateUser(Integer userId) {
        log.info("激活用户，用户ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.activate();
        userRepository.save(user);

        log.info("用户激活成功: {}", user.getUsername());
    }

    @Override
    public void deactivateUser(Integer userId) {
        log.info("停用用户，用户ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.deactivate();
        userRepository.save(user);

        log.info("用户停用成功: {}", user.getUsername());
    }

    @Override
    public void deleteUser(Integer userId) {
        log.info("删除用户，用户ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 删除关联的OJ账号
        userOJRepository.deleteByUserId(userId);

        // 删除用户
        userRepository.delete(user);

        log.info("用户删除成功: {}", user.getUsername());
    }

    @Override
    public String uploadAvatar(String username, MultipartFile file) {
        log.info("用户 {} 上传头像", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        try {
            String avatarUrl = avatarStorageService.store(file);
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            log.info("头像上传成功: {}", avatarUrl);
            return avatarUrl;
        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUserCountByRole(User.Type role) {
        return userRepository.countByRole(role);
    }

    // ==================== 私有方法 ====================

    /**
     * 验证创建用户请求
     */
    private void validateCreateRequest(UserCreateRequest request) {
        if (!request.isPasswordMatch()) {
            throw ValidationException.passwordMismatch();
        }

        // 验证超级管理员角色
        if (request.getRoles() != null && request.getRoles().contains(User.Type.SUPER_ADMIN)) {
            throw new ValidationException("不允许通过API创建超级管理员用户");
        }
    }

    /**
     * 验证管理员权限
     */
    private void validateAdminPermission(User admin, User targetUser, UserUpdateRequest request) {
        // 超级管理员可以操作任何用户
        if (admin.isSuperAdmin()) {
            return;
        }

        // 普通管理员不能操作超级管理员
        if (targetUser.isSuperAdmin()) {
            throw new ValidationException("普通管理员不能操作超级管理员");
        }

        // 普通管理员不能设置超级管理员角色
        if (request.getRoles() != null && request.getRoles().contains(User.Type.SUPER_ADMIN)) {
            throw new ValidationException("普通管理员不能设置超级管理员角色");
        }
    }

    /**
     * 更新基本信息
     */
    private void updateBasicInfo(User user, UserUpdateRequest request) {
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            // 检查邮箱是否被其他用户使用
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw ValidationException.duplicate("邮箱", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getMajor())) {
            user.setMajor(request.getMajor());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
    }

    /**
     * 处理OJ账号
     */
    private void processOJAccounts(User user, List<UserCreateRequest.OJAccountRequest> ojAccountRequests) {
        for (UserCreateRequest.OJAccountRequest ojRequest : ojAccountRequests) {
            if (ojRequest.getPlatform() != null && StringUtils.hasText(ojRequest.getAccountName())) {
                UserOJ userOJ = UserOJ.builder()
                        .user(user)
                        .platform(ojRequest.getPlatform())
                        .accountName(ojRequest.getAccountName().trim())
                        .active(true)
                        .build();
                user.getOjAccounts().add(userOJ);
            }
        }
    }

    /**
     * 处理标签
     */
    private void processTags(User user, List<String> tagNames) {
        for (String tagName : tagNames) {
            if (StringUtils.hasText(tagName)) {
                UserTag tag = userTagRepository.findByName(tagName.trim())
                        .orElseGet(() -> {
                            UserTag newTag = UserTag.builder()
                                    .name(tagName.trim())
                                    .color("#409EFF")
                                    .active(true)
                                    .sortOrder(0)
                                    .build();
                            return userTagRepository.save(newTag);
                        });
                user.getTags().add(tag);
            }
        }
    }

    /**
     * 转换为响应DTO
     */
    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .major(user.getMajor())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .active(user.isActive())
                .lastTryDate(user.getLastTryDate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(user.getRoles())
                .ojAccounts(convertOJAccountsToResponse(user.getOjAccounts()))
                .tags(convertTagsToResponse(user.getTags()))
                .build();
    }

    /**
     * 转换OJ账号为响应DTO
     */
    private List<UserOJResponse> convertOJAccountsToResponse(List<UserOJ> ojAccounts) {
        if (ojAccounts == null) {
            return new ArrayList<>();
        }
        return ojAccounts.stream()
                .map(oj -> UserOJResponse.builder()
                        .id(oj.getId())
                        .platform(oj.getPlatform())
                        .platformDisplayName(oj.getPlatform().getDisplayName())
                        .accountName(oj.getAccountName())
                        .active(oj.isActive())
                        .createdAt(oj.getCreatedAt())
                        .updatedAt(oj.getUpdatedAt())
                        .lastSyncAt(oj.getLastSyncAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 转换标签为响应DTO
     */
    private List<UserTagResponse> convertTagsToResponse(Set<UserTag> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .map(tag -> UserTagResponse.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .description(tag.getDescription())
                        .sortOrder(tag.getSortOrder())
                        .active(tag.isActive())
                        .createdAt(tag.getCreatedAt())
                        .updatedAt(tag.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
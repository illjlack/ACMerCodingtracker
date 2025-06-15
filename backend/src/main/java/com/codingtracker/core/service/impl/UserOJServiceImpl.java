package com.codingtracker.core.service.impl;

import com.codingtracker.core.domain.entity.User;
import com.codingtracker.core.domain.entity.UserOJ;
import com.codingtracker.core.domain.entity.OJPlatform;
import com.codingtracker.core.domain.repository.UserRepository;
import com.codingtracker.core.domain.repository.UserOJRepository;
import com.codingtracker.core.service.UserOJService;
import com.codingtracker.shared.dto.request.OJAccountRequest;
import com.codingtracker.shared.dto.response.UserOJResponse;
import com.codingtracker.shared.exception.UserNotFoundException;
import com.codingtracker.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户OJ账号服务实现类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserOJServiceImpl implements UserOJService {

    private final UserOJRepository userOJRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserOJResponse> getUserOJAccounts(String username) {
        log.debug("获取用户OJ账号列表: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return userOJRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserOJResponse> getOJAccount(Integer accountId) {
        log.debug("获取OJ账号详情: {}", accountId);

        return userOJRepository.findById(accountId)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOJResponse> getOJAccountsByPlatform(OJPlatform platform) {
        log.debug("获取平台OJ账号列表: {}", platform);

        return userOJRepository.findByPlatformAndActiveTrue(platform)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserOJResponse> findOJAccount(String username, OJPlatform platform) {
        log.debug("查找用户OJ账号: {} - {}", username, platform);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return userOJRepository.findByUserIdAndPlatform(user.getId(), platform)
                .map(this::convertToResponse);
    }

    @Override
    public UserOJResponse addOJAccount(String username, OJAccountRequest request) {
        log.info("添加OJ账号: {} - {}", username, request.getPlatform());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 验证请求
        validateOJAccountRequest(request);

        // 检查是否已存在相同平台的账号
        if (userOJRepository.existsByUserIdAndPlatform(user.getId(), request.getPlatform())) {
            throw ValidationException.duplicate("OJ平台账号",
                    user.getUsername() + "-" + request.getPlatform().getDisplayName());
        }

        // 检查账号名是否已被其他用户使用
        if (userOJRepository.existsByPlatformAndAccountName(request.getPlatform(), request.getAccountName())) {
            throw ValidationException.duplicate("账号名",
                    request.getPlatform().getDisplayName() + "-" + request.getAccountName());
        }

        // 创建OJ账号
        UserOJ userOJ = UserOJ.builder()
                .user(user)
                .platform(request.getPlatform())
                .accountName(request.getAccountName().trim())
                .active(true)
                .build();

        UserOJ savedUserOJ = userOJRepository.save(userOJ);

        log.info("OJ账号添加成功: {} - {} (ID: {})",
                username, request.getPlatform(), savedUserOJ.getId());

        return convertToResponse(savedUserOJ);
    }

    @Override
    public UserOJResponse updateOJAccount(Integer accountId, OJAccountRequest request) {
        log.info("更新OJ账号: {}", accountId);

        UserOJ userOJ = userOJRepository.findById(accountId)
                .orElseThrow(() -> new ValidationException("OJ账号不存在"));

        // 验证请求
        validateOJAccountRequest(request);

        // 检查账号名是否已被其他用户使用（排除当前账号）
        if (!request.getAccountName().equals(userOJ.getAccountName()) &&
                userOJRepository.existsByPlatformAndAccountName(request.getPlatform(), request.getAccountName())) {
            throw ValidationException.duplicate("账号名",
                    request.getPlatform().getDisplayName() + "-" + request.getAccountName());
        }

        // 更新信息
        userOJ.setPlatform(request.getPlatform());
        userOJ.setAccountName(request.getAccountName().trim());

        UserOJ updatedUserOJ = userOJRepository.save(userOJ);

        log.info("OJ账号更新成功: {} - {} (ID: {})",
                userOJ.getUser().getUsername(), request.getPlatform(), accountId);

        return convertToResponse(updatedUserOJ);
    }

    @Override
    public void activateOJAccount(Integer accountId) {
        log.info("激活OJ账号: {}", accountId);

        UserOJ userOJ = userOJRepository.findById(accountId)
                .orElseThrow(() -> new ValidationException("OJ账号不存在"));

        userOJ.activate();
        userOJRepository.save(userOJ);

        log.info("OJ账号激活成功: {} - {}",
                userOJ.getUser().getUsername(), userOJ.getPlatform());
    }

    @Override
    public void deactivateOJAccount(Integer accountId) {
        log.info("停用OJ账号: {}", accountId);

        UserOJ userOJ = userOJRepository.findById(accountId)
                .orElseThrow(() -> new ValidationException("OJ账号不存在"));

        userOJ.deactivate();
        userOJRepository.save(userOJ);

        log.info("OJ账号停用成功: {} - {}",
                userOJ.getUser().getUsername(), userOJ.getPlatform());
    }

    @Override
    public void deleteOJAccount(Integer accountId) {
        log.info("删除OJ账号: {}", accountId);

        UserOJ userOJ = userOJRepository.findById(accountId)
                .orElseThrow(() -> new ValidationException("OJ账号不存在"));

        String username = userOJ.getUser().getUsername();
        OJPlatform platform = userOJ.getPlatform();

        userOJRepository.delete(userOJ);

        log.info("OJ账号删除成功: {} - {}", username, platform);
    }

    @Override
    public void deleteUserOJAccounts(String username) {
        log.info("删除用户所有OJ账号: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        int deletedCount = userOJRepository.deleteByUserId(user.getId());

        log.info("用户OJ账号删除成功: {} (删除{}个)", username, deletedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsOJAccount(String username, OJPlatform platform) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return userOJRepository.existsByUserIdAndPlatform(user.getId(), platform);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAccountName(OJPlatform platform, String accountName) {
        return userOJRepository.existsByPlatformAndAccountName(platform, accountName);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalOJAccountCount() {
        return userOJRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveOJAccountCount() {
        return userOJRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getOJAccountCountByPlatform(OJPlatform platform) {
        return userOJRepository.countByPlatform(platform);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOJResponse> getActiveOJAccounts() {
        log.debug("获取所有活跃OJ账号");

        return userOJRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOJResponse> searchOJAccounts(String keyword) {
        log.debug("搜索OJ账号，关键字: {}", keyword);

        return userOJRepository.searchByKeyword(keyword)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 验证OJ账号请求
     */
    private void validateOJAccountRequest(OJAccountRequest request) {
        if (request.getPlatform() == null) {
            throw new ValidationException("OJ平台不能为空");
        }

        if (!StringUtils.hasText(request.getAccountName())) {
            throw new ValidationException("账号名不能为空");
        }

        String accountName = request.getAccountName().trim();
        if (accountName.length() < 2 || accountName.length() > 50) {
            throw new ValidationException("账号名长度必须在2-50字符之间");
        }

        // 验证账号名格式（根据不同平台可能有不同规则）
        if (!isValidAccountName(request.getPlatform(), accountName)) {
            throw new ValidationException("账号名格式不正确");
        }
    }

    /**
     * 验证账号名格式
     */
    private boolean isValidAccountName(OJPlatform platform, String accountName) {
        // 基本格式验证：字母、数字、下划线、连字符
        if (!accountName.matches("^[a-zA-Z0-9_-]+$")) {
            return false;
        }

        // 根据不同平台可以有不同的验证规则
        switch (platform) {
            case CODEFORCES:
                // Codeforces用户名规则
                return accountName.length() >= 3 && accountName.length() <= 24;
            case LEETCODE:
                // LeetCode用户名规则
                return accountName.length() >= 1 && accountName.length() <= 30;
            case ATCODER:
                // AtCoder用户名规则
                return accountName.length() >= 3 && accountName.length() <= 16;
            case NOWCODER:
                // 牛客网用户名规则
                return accountName.length() >= 1 && accountName.length() <= 50;
            default:
                return true;
        }
    }

    /**
     * 转换为响应DTO
     */
    private UserOJResponse convertToResponse(UserOJ userOJ) {
        return UserOJResponse.builder()
                .id(userOJ.getId())
                .userId(userOJ.getUser().getId())
                .username(userOJ.getUser().getUsername())
                .realName(userOJ.getUser().getRealName())
                .platform(userOJ.getPlatform())
                .platformDisplayName(userOJ.getPlatform().getDisplayName())
                .accountName(userOJ.getAccountName())
                .active(userOJ.isActive())
                .createdAt(userOJ.getCreatedAt())
                .updatedAt(userOJ.getUpdatedAt())
                .lastSyncAt(userOJ.getLastSyncAt())
                .build();
    }
}
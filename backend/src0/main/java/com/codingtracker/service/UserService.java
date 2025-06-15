package com.codingtracker.service;

import com.codingtracker.dto.UserInfoDTO;
import com.codingtracker.dto.UserCreateRequest;
import com.codingtracker.dto.UserUpdateRequest;
import com.codingtracker.init.SystemStatsLoader;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.UserOJ;
import com.codingtracker.model.UserTag;
import com.codingtracker.repository.UserOJRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.codingtracker.model.User;
import com.codingtracker.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserOJRepository userOJRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;
    private final SystemStatsLoader statsLoader; // 统计加载器

    @Autowired
    public UserService(UserRepository userRepository,
            UserOJRepository userOJRepository,
            BCryptPasswordEncoder passwordEncoder,
            AvatarStorageService avatarStorageService,
            SystemStatsLoader statsLoader) {
        this.userRepository = userRepository;
        this.userOJRepository = userOJRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
        this.statsLoader = statsLoader;
    }

    /**
     * 用户注册
     */
    public boolean registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return false;
        }
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        // 检查是否是第一个用户，如果是则设为超级管理员
        long userCount = userRepository.count();
        if (userCount == 0) {
            user.getRoles().add(User.Type.SUPER_ADMIN); // 第一个用户为超级管理员
        } else {
            user.getRoles().add(User.Type.USER); // 其他用户默认为普通用户
        }

        userRepository.save(user);

        updateUserCountStat();
        return true;
    }

    /**
     * 用户登录验证
     */
    public User valid(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    /**
     * 修改用户信息
     */
    @Transactional
    public void modifyUser(UserInfoDTO userInfoDTO) {
        User existingUser = userRepository.findByUsername(userInfoDTO.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userInfoDTO.getRealName() != null) {
            existingUser.setRealName(userInfoDTO.getRealName());
        }
        if (userInfoDTO.getMajor() != null) {
            existingUser.setMajor(userInfoDTO.getMajor());
        }
        if (userInfoDTO.getEmail() != null) {
            existingUser.setEmail(userInfoDTO.getEmail());
        }
        if (userInfoDTO.getAvatar() != null) {
            existingUser.setAvatar(userInfoDTO.getAvatar());
        }

        Map<String, String> ojAccountsMap = userInfoDTO.getOjAccounts();
        if (ojAccountsMap != null) {
            List<UserOJ> existingOJList = existingUser.getOjAccounts();

            Map<String, Set<String>> existingMap = new HashMap<>();
            for (UserOJ oj : existingOJList) {
                existingMap.computeIfAbsent(oj.getPlatform().name(), k -> new HashSet<>())
                        .add(oj.getAccountName());
            }

            Map<String, Set<String>> incomingMap = new HashMap<>();
            for (Map.Entry<String, String> entry : ojAccountsMap.entrySet()) {
                String platformName = entry.getKey();
                String accountsStr = entry.getValue();
                if (accountsStr == null || accountsStr.trim().isEmpty())
                    continue;

                Set<String> acctSet = Arrays.stream(accountsStr.split("[；;]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                incomingMap.put(platformName, acctSet);
            }

            Iterator<UserOJ> it = existingOJList.iterator();
            while (it.hasNext()) {
                UserOJ oj = it.next();
                String platform = oj.getPlatform().name();
                String acct = oj.getAccountName();
                Set<String> incomingAccts = incomingMap.get(platform);
                if (incomingAccts == null || !incomingAccts.contains(acct)) {
                    it.remove();
                    userOJRepository.delete(oj);
                }
            }

            for (Map.Entry<String, Set<String>> entry : incomingMap.entrySet()) {
                String platformName = entry.getKey();
                Set<String> accounts = entry.getValue();

                Set<String> existAccts = existingMap.getOrDefault(platformName, Collections.emptySet());
                for (String acctName : accounts) {
                    if (!existAccts.contains(acctName)) {
                        UserOJ userOJ = new UserOJ();
                        userOJ.setPlatform(OJPlatform.valueOf(platformName));
                        userOJ.setAccountName(acctName);
                        userOJ.setUser(existingUser);
                        existingOJList.add(userOJ);
                    }
                }
            }
        }

        userRepository.save(existingUser);
    }

    /**
     * 保存用户头像文件
     */
    public boolean storeAvatar(String username, MultipartFile file) {
        try {
            String avatarUrl = avatarStorageService.store(file);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 修改用户密码
     */
    @Transactional
    public User modifyUserPassword(Integer userId, String password) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        userRepository.save(user);
        return user;
    }

    /**
     * 获取所有用户（不包括管理员）
     */
    public List<User> allUser() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isAdmin())
                .collect(Collectors.toList());
    }

    /**
     * 判断用户名是否存在
     */
    public boolean hasUser(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 根据真实姓名查找用户
     */
    public Optional<User> findByRealName(String realName) {
        return userRepository.findByRealName(realName);
    }

    /**
     * 根据角色查找用户
     */
    public List<User> findByRole(User.Type role) {
        return userRepository.findByRolesContains(role);
    }

    /**
     * 添加 OJ 账号
     */
    @Transactional
    public boolean addOJAccount(String username, String platform, String accountName) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            UserOJ ojAccount = new UserOJ();
            ojAccount.setUser(user);
            ojAccount.setPlatform(OJPlatform.fromName(platform));
            ojAccount.setAccountName(accountName);

            user.getOjAccounts().add(ojAccount);
            userRepository.save(user);

            return true;
        }
        return false;
    }

    /**
     * 获取指定用户名的所有 OJ 账号
     */
    public List<UserOJ> getOJAccounts(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        return userOptional.map(User::getOjAccounts).orElse(null);
    }

    /**
     * 删除指定用户的 OJ 账号
     */
    @Transactional
    public boolean deleteOJAccount(String username, String platform, String accountName) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String ojName = OJPlatform.fromName(platform).toString();
            List<UserOJ> ojAccounts = user.getOjAccounts();
            for (UserOJ ojAccount : ojAccounts) {
                if (ojAccount.getPlatform().toString().equals(ojName)
                        && ojAccount.getAccountName().equals(accountName)) {
                    ojAccounts.remove(ojAccount);
                    userOJRepository.delete(ojAccount);
                    userRepository.save(user);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查询所有用户
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 根据 ID 查找用户
     */
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * 创建新用户
     */
    @Transactional
    public User createUser(User user) {
        // 验证必填字段
        if (!StringUtils.hasText(user.getUsername())) {
            throw new RuntimeException("Username is required");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new RuntimeException("Password is required");
        }
        if (!StringUtils.hasText(user.getRealName())) {
            throw new RuntimeException("Real name is required");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new RuntimeException("Email is required");
        }
        if (!StringUtils.hasText(user.getMajor())) {
            throw new RuntimeException("Major is required");
        }

        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 验证超级管理员角色：不允许通过API创建超级管理员
        if (user.getRoles() != null && user.getRoles().contains(User.Type.SUPER_ADMIN)) {
            throw new RuntimeException("不允许创建超级管理员用户");
        }

        // 设置默认值
        user.setActive(true);
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(User.Type.USER)); // 默认设置为普通用户
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 处理 OJ 账号 - 先保存用户，再处理 OJ 账号
        List<UserOJ> ojAccountsToAdd = new ArrayList<>();
        Set<String> addedAccounts = new HashSet<>(); // 用于防止重复

        if (user.getOjAccounts() != null) {
            for (UserOJ ojAccount : user.getOjAccounts()) {
                if (ojAccount.getPlatform() != null && StringUtils.hasText(ojAccount.getAccountName())) {
                    String accountName = ojAccount.getAccountName().trim();
                    String accountKey = ojAccount.getPlatform().name() + ":" + accountName;

                    // 检查账号名是否有效
                    if (accountName.isEmpty()) {
                        throw new RuntimeException("OJ账号名不能为空");
                    }

                    // 检查是否重复
                    if (!addedAccounts.contains(accountKey)) {
                        UserOJ newOJ = new UserOJ();
                        newOJ.setPlatform(ojAccount.getPlatform());
                        newOJ.setAccountName(accountName);
                        ojAccountsToAdd.add(newOJ);
                        addedAccounts.add(accountKey);
                    }
                }
            }
        }

        // 临时清空 OJ 账号，先保存用户
        user.getOjAccounts().clear();
        User savedUser = userRepository.save(user);

        // 再添加 OJ 账号
        for (UserOJ ojAccount : ojAccountsToAdd) {
            ojAccount.setUser(savedUser);
            savedUser.getOjAccounts().add(ojAccount);
        }

        return userRepository.save(savedUser);
    }

    /**
     * 从请求DTO创建新用户
     */
    @Transactional
    public User createUserFromRequest(UserCreateRequest request) {
        // 创建User实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setMajor(request.getMajor());
        user.setRoles(request.getRoles() != null ? request.getRoles() : Set.of(User.Type.USER));

        // 处理OJ账号
        List<UserOJ> ojAccounts = new ArrayList<>();
        if (request.getOjAccounts() != null) {
            for (UserCreateRequest.OJAccountRequest ojRequest : request.getOjAccounts()) {
                if (ojRequest.getPlatform() != null && StringUtils.hasText(ojRequest.getAccountName())) {
                    UserOJ ojAccount = new UserOJ();
                    ojAccount.setPlatform(ojRequest.getPlatform());
                    ojAccount.setAccountName(ojRequest.getAccountName().trim());
                    ojAccounts.add(ojAccount);
                }
            }
        }
        user.setOjAccounts(ojAccounts);

        // 调用原有的创建用户方法
        User createdUser = createUser(user);

        // 处理标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            // 这里需要UserTagService来设置标签，但为了避免循环依赖，我们暂时跳过
            // 可以在Controller层单独处理标签设置
        }

        return createdUser;
    }

    /**
     * 更新已有用户
     */
    @Transactional
    public User updateUser(Integer id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 更新基本信息
        if (StringUtils.hasText(user.getRealName())) {
            existingUser.setRealName(user.getRealName());
        }
        if (StringUtils.hasText(user.getEmail())) {
            if (!user.getEmail().equals(existingUser.getEmail()) &&
                    userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            existingUser.setEmail(user.getEmail());
        }
        if (StringUtils.hasText(user.getMajor())) {
            existingUser.setMajor(user.getMajor());
        }
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            existingUser.setRoles(user.getRoles());
        }

        return userRepository.save(existingUser);
    }

    /**
     * 管理员更新用户信息（带权限验证）
     */
    @Transactional
    public User updateUserByAdmin(Integer id, User user, String currentUsername) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // 权限验证
        validateAdminPermission(currentUser, existingUser, user);

        // 更新基本信息
        if (StringUtils.hasText(user.getRealName())) {
            existingUser.setRealName(user.getRealName());
        }
        if (StringUtils.hasText(user.getEmail())) {
            if (!user.getEmail().equals(existingUser.getEmail()) &&
                    userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            existingUser.setEmail(user.getEmail());
        }
        if (StringUtils.hasText(user.getMajor())) {
            existingUser.setMajor(user.getMajor());
        }
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            existingUser.setRoles(user.getRoles());
        }

        // 处理 OJ 账号更新 - 使用完全替换策略
        if (user.getOjAccounts() != null) {
            // 使用@Modifying注解的方法直接删除数据库中的记录
            userOJRepository.deleteByUserId(existingUser.getId());
            // 立即刷新，确保删除操作生效
            userOJRepository.flush();

            // 清空实体中的关联
            existingUser.getOjAccounts().clear();

            // 添加新的 OJ 账号
            Set<String> addedAccounts = new HashSet<>(); // 用于防止重复
            for (UserOJ newOJ : user.getOjAccounts()) {
                if (newOJ.getPlatform() != null && StringUtils.hasText(newOJ.getAccountName())) {
                    String accountName = newOJ.getAccountName().trim();
                    String accountKey = newOJ.getPlatform().name() + ":" + accountName;

                    // 检查账号名是否有效
                    if (accountName.isEmpty()) {
                        throw new RuntimeException("OJ账号名不能为空");
                    }

                    // 检查是否重复
                    if (!addedAccounts.contains(accountKey)) {
                        UserOJ ojAccount = new UserOJ();
                        ojAccount.setUser(existingUser);
                        ojAccount.setPlatform(newOJ.getPlatform());
                        ojAccount.setAccountName(accountName);
                        existingUser.getOjAccounts().add(ojAccount);
                        addedAccounts.add(accountKey);
                    }
                }
            }
        }

        return userRepository.save(existingUser);
    }

    /**
     * 从请求DTO更新用户信息
     */
    @Transactional
    public User updateUserByAdminFromRequest(Integer id, UserUpdateRequest request, String currentUsername) {
        // 创建User对象用于更新
        User user = new User();
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setMajor(request.getMajor());
        user.setRoles(request.getRoles());

        // 处理OJ账号
        List<UserOJ> ojAccounts = new ArrayList<>();
        if (request.getOjAccounts() != null) {
            for (UserUpdateRequest.OJAccountRequest ojRequest : request.getOjAccounts()) {
                if (ojRequest.getPlatform() != null && StringUtils.hasText(ojRequest.getAccountName())) {
                    UserOJ ojAccount = new UserOJ();
                    ojAccount.setPlatform(ojRequest.getPlatform());
                    ojAccount.setAccountName(ojRequest.getAccountName().trim());
                    ojAccounts.add(ojAccount);
                }
            }
        }
        user.setOjAccounts(ojAccounts);

        // 调用原有的更新用户方法
        User updatedUser = updateUserByAdmin(id, user, currentUsername);

        // 处理标签
        if (request.getTags() != null) {
            // 这里需要UserTagService来设置标签，但为了避免循环依赖，我们暂时跳过
            // 可以在Controller层单独处理标签设置
        }

        return updatedUser;
    }

    /**
     * 验证管理员权限
     */
    private void validateAdminPermission(User currentUser, User targetUser, User updateData) {
        boolean isCurrentSuperAdmin = currentUser.isSuperAdmin();
        boolean isCurrentAdmin = currentUser.isAdmin();
        boolean isTargetAdmin = targetUser.isAdmin();
        boolean isTargetSuperAdmin = targetUser.isSuperAdmin();

        // 如果当前用户不是管理员，直接拒绝
        if (!isCurrentAdmin) {
            throw new RuntimeException("权限不足：只有管理员才能执行此操作");
        }

        // 禁止编辑超级管理员用户
        if (isTargetSuperAdmin) {
            throw new RuntimeException("权限不足：超级管理员用户不能被编辑");
        }

        // 禁止设置超级管理员角色
        if (updateData.getRoles() != null && updateData.getRoles().contains(User.Type.SUPER_ADMIN)) {
            throw new RuntimeException("权限不足：不允许设置超级管理员角色");
        }

        // 如果当前用户是普通管理员
        if (isCurrentAdmin && !isCurrentSuperAdmin) {
            // 不能编辑其他管理员
            if (isTargetAdmin) {
                throw new RuntimeException("权限不足：管理员不能编辑其他管理员");
            }

            // 不能设置管理员角色
            if (updateData.getRoles() != null && updateData.getRoles().contains(User.Type.ADMIN)) {
                throw new RuntimeException("权限不足：管理员不能设置管理员角色");
            }
        }
    }

    /**
     * 检查用户是否可以被当前管理员编辑
     */
    public boolean canEditUser(String currentUsername, Integer targetUserId) {
        try {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            boolean isCurrentSuperAdmin = currentUser.isSuperAdmin();
            boolean isCurrentAdmin = currentUser.isAdmin();
            boolean isTargetAdmin = targetUser.isAdmin();
            boolean isTargetSuperAdmin = targetUser.isSuperAdmin();

            // 超级管理员不能被编辑
            if (isTargetSuperAdmin) {
                return false;
            }

            // 超级管理员可以编辑普通管理员和普通用户
            if (isCurrentSuperAdmin) {
                return true;
            }

            // 普通管理员不能编辑管理员
            if (isCurrentAdmin && !isCurrentSuperAdmin) {
                return !isTargetAdmin;
            }

            // 非管理员不能编辑任何用户
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户是否可以被当前管理员删除
     */
    public boolean canDeleteUser(String currentUsername, Integer targetUserId) {
        try {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            boolean isCurrentSuperAdmin = currentUser.isSuperAdmin();
            boolean isCurrentAdmin = currentUser.isAdmin();
            boolean isTargetAdmin = targetUser.isAdmin();
            boolean isTargetSuperAdmin = targetUser.isSuperAdmin();

            // 超级管理员不能被删除
            if (isTargetSuperAdmin) {
                return false;
            }

            // 超级管理员可以删除普通管理员和普通用户
            if (isCurrentSuperAdmin) {
                return true;
            }

            // 普通管理员不能删除管理员
            if (isCurrentAdmin && !isCurrentSuperAdmin) {
                return !isTargetAdmin;
            }

            // 非管理员不能删除任何用户
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断用户是否存在（根据 ID）
     */
    public boolean existsById(Integer id) {
        return userRepository.existsById(id);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    /**
     * 根据用户名获取所有 OJ 账号
     */
    public List<UserOJ> getOJAccountsByUsername(String username) {
        return userOJRepository.findByUserUsername(username);
    }

    /**
     * 更新统计文件中的用户总数等数据
     */
    private void updateUserCountStat() {
        long userCount = userRepository.count();
        long problemCount = statsLoader.getSumProblemCount();
        long tryCount = statsLoader.getSumTryCount();
        statsLoader.updateStats(userCount, problemCount, tryCount);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 获取所有用户数据，包含标签和OJ账号，使用优化查询避免N+1问题
     */
    @Transactional(readOnly = true)
    private List<User> getAllUsersWithCompleteData() {
        // 先获取所有用户的基本信息
        List<User> allUsers = userRepository.findAll();

        if (allUsers.isEmpty()) {
            return allUsers;
        }

        // 收集用户ID
        List<Integer> userIds = allUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // 分别获取标签和OJ账号数据
        List<User> usersWithTags = userRepository.findAllWithTags();
        List<User> usersWithOJAccounts = userRepository.findUsersWithOJAccountsByIds(userIds);

        // 创建映射
        Map<Integer, Set<UserTag>> tagsMap = usersWithTags.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> new HashSet<>(user.getTags()),
                        (existing, replacement) -> existing));

        Map<Integer, List<UserOJ>> ojAccountsMap = usersWithOJAccounts.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> new ArrayList<>(user.getOjAccounts()),
                        (existing, replacement) -> existing));

        // 构建完整的用户数据
        for (User user : allUsers) {
            // 初始化集合（确保不是懒加载的代理对象）
            if (user.getTags() == null) {
                user.setTags(new HashSet<>());
            } else {
                user.getTags().clear();
            }

            if (user.getOjAccounts() == null) {
                user.setOjAccounts(new ArrayList<>());
            } else {
                user.getOjAccounts().clear();
            }

            // 设置标签
            Set<UserTag> tags = tagsMap.getOrDefault(user.getId(), new HashSet<>());
            user.getTags().addAll(tags);

            // 设置OJ账号
            List<UserOJ> ojAccounts = ojAccountsMap.getOrDefault(user.getId(), new ArrayList<>());
            user.getOjAccounts().addAll(ojAccounts);
        }

        return allUsers;
    }

    public List<User> getAllUsers() {
        // 使用优化查询获取完整用户数据
        return getAllUsersWithCompleteData();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void toggleUserStatus(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            // 如果没有关键词，返回所有用户
            return getAllUsersWithCompleteData();
        }
        // 使用优化查询搜索用户
        List<User> allUsers = getAllUsersWithCompleteData();
        return allUsers.stream()
                .filter(user -> user.getUsername().toLowerCase().contains(keyword.toLowerCase()) ||
                        user.getRealName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 检查系统中是否已存在超级管理员
     */
    public boolean hasSuperAdmin() {
        return userRepository.findAll().stream()
                .anyMatch(User::isSuperAdmin);
    }

    /**
     * 获取超级管理员数量
     */
    public long getSuperAdminCount() {
        return userRepository.findAll().stream()
                .filter(User::isSuperAdmin)
                .count();
    }

    /**
     * 验证超级管理员的唯一性
     */
    public void validateSuperAdminUniqueness() {
        long count = getSuperAdminCount();
        if (count > 1) {
            throw new RuntimeException("系统错误：存在多个超级管理员，请联系系统管理员");
        }
    }
}

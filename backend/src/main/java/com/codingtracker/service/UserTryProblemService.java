package com.codingtracker.service;

import com.codingtracker.dto.UserStatsDTO;
import com.codingtracker.dto.UserTryProblemDTO;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.ProblemResult;
import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;

@Service
public class UserTryProblemService {

    private final UserTryProblemRepository repository;
    private final UserRepository userRepository; // 你需要注入UserRepository来查用户名
    private final DataMigrationService dataMigrationService;

    public UserTryProblemService(UserTryProblemRepository repository, UserRepository userRepository,
            DataMigrationService dataMigrationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.dataMigrationService = dataMigrationService;
    }

    public List<UserStatsDTO> getTryCounts(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rawList = repository.countTryByUserAndPlatformBetween(start, end);
        return convertRawListToDTOWithName(rawList);
    }

    public List<UserStatsDTO> getAcCounts(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rawList = repository.countAcByUserAndPlatformBetween(start, end, ProblemResult.AC);
        return convertRawListToDTOWithName(rawList);
    }

    private List<UserStatsDTO> convertRawListToDTOWithName(List<Object[]> rawList) {
        Map<Integer, UserStatsDTO> map = new LinkedHashMap<>();

        // 收集所有userId
        Set<Integer> userIds = new HashSet<>();
        for (Object[] row : rawList) {
            Number userIdNum = (Number) row[0];
            int userId = userIdNum.intValue();
            userIds.add(userId);
        }

        // 批量查用户信息，包含标签
        List<User> usersWithTags = userRepository.findAllWithTags();
        Map<Integer, User> userMap = new HashMap<>();
        for (User user : usersWithTags) {
            if (userIds.contains(user.getId())) {
                userMap.put(user.getId(), user);
            }
        }

        // 构造 DTO
        for (Object[] row : rawList) {
            Number userIdNum = (Number) row[0];
            Integer userId = userIdNum.intValue();
            OJPlatform platform = (OJPlatform) row[1];
            Long count = (Long) row[2];

            User user = userMap.get(userId);
            String username = user != null ? user.getUsername() : "未知用户";
            String realName = user != null ? user.getRealName() : "未知姓名";

            UserStatsDTO userStats = map.computeIfAbsent(userId, id -> {
                UserStatsDTO dto = new UserStatsDTO(userId, username, realName);
                if (user != null && user.getTags() != null) {
                    dto.setTags(user.getTags());
                }
                return dto;
            });
            userStats.addCount(platform, count);
        }

        return new ArrayList<>(map.values());
    }

    public Page<UserTryProblemDTO> getUserTryProblemsDTO(String username, int page, int size) {
        // 创建分页参数
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptTime"));
        // 调用数据迁移服务的方法，传入 username 和 pageable 参数
        Page<UserTryProblemDTO> pageResult = dataMigrationService.getOptimizedUserTryProblems(pageable, username);

        return pageResult;
    }

    /**
     * 根据OJ账号ID获取题目尝试记录
     */
    public Page<UserTryProblemDTO> getUserTryProblemsByOjAccount(Integer userOjId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptTime"));
        Page<UserTryProblem> problemPage = repository.findByUserOjIdWithProblemAndTags(userOjId, pageable);

        return problemPage.map(utp -> new UserTryProblemDTO(utp, utp.getUser().getUsername()));
    }

    /**
     * 获取用户的所有尝试记录（包括所有OJ账号）
     */
    public Page<UserTryProblemDTO> getAllUserTryProblems(String username, int page, int size) {
        // 先找到用户
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Page.empty();
        }

        User user = userOpt.get();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptTime"));
        Page<UserTryProblem> problemPage = repository.findByUserIdIncludingOjAccountsWithProblemAndTags(
                user.getId().longValue(), pageable);

        return problemPage.map(utp -> new UserTryProblemDTO(utp, username));
    }

    /**
     * 获取基于OJ账号的统计数据（用于后台分析）
     */
    public List<OjAccountStatsDTO> getOjAccountStats(LocalDateTime start, LocalDateTime end) {
        List<Object[]> acList = repository.countAcByOjAccountAndPlatformBetween(start, end, ProblemResult.AC);
        List<Object[]> tryList = repository.countTryByOjAccountAndPlatformBetween(start, end);

        Map<String, OjAccountStatsDTO> statsMap = new HashMap<>();

        // 处理AC数据
        for (Object[] row : acList) {
            Integer userOjId = (Integer) row[0];
            Integer userId = (Integer) row[1];
            OJPlatform platform = (OJPlatform) row[2];
            Long acCount = (Long) row[3];

            String key = userOjId + "_" + platform.name();
            OjAccountStatsDTO stats = statsMap.computeIfAbsent(key,
                    k -> new OjAccountStatsDTO(userOjId, userId, platform));
            stats.setAcCount(acCount);
        }

        // 处理尝试数据
        for (Object[] row : tryList) {
            Integer userOjId = (Integer) row[0];
            Integer userId = (Integer) row[1];
            OJPlatform platform = (OJPlatform) row[2];
            Long tryCount = (Long) row[3];

            String key = userOjId + "_" + platform.name();
            OjAccountStatsDTO stats = statsMap.computeIfAbsent(key,
                    k -> new OjAccountStatsDTO(userOjId, userId, platform));
            stats.setTryCount(tryCount);
        }

        return new ArrayList<>(statsMap.values());
    }

    /**
     * OJ账号统计DTO
     */
    public static class OjAccountStatsDTO {
        private Integer userOjId;
        private Integer userId;
        private OJPlatform platform;
        private Long acCount = 0L;
        private Long tryCount = 0L;

        public OjAccountStatsDTO(Integer userOjId, Integer userId, OJPlatform platform) {
            this.userOjId = userOjId;
            this.userId = userId;
            this.platform = platform;
        }

        // Getters and setters
        public Integer getUserOjId() {
            return userOjId;
        }

        public void setUserOjId(Integer userOjId) {
            this.userOjId = userOjId;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public OJPlatform getPlatform() {
            return platform;
        }

        public void setPlatform(OJPlatform platform) {
            this.platform = platform;
        }

        public Long getAcCount() {
            return acCount;
        }

        public void setAcCount(Long acCount) {
            this.acCount = acCount;
        }

        public Long getTryCount() {
            return tryCount;
        }

        public void setTryCount(Long tryCount) {
            this.tryCount = tryCount;
        }
    }

}

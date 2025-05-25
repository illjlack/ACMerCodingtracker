package com.codingtracker.service;

import com.codingtracker.dto.UserTryProblemDTO;
import com.codingtracker.model.UserTryProblemOptimized;
import com.codingtracker.repository.UserTryProblemOptimizedRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataMigrationService {

    @Autowired
    private UserTryProblemOptimizedRepository userTryProblemOptimizedRepository;

    @Autowired
    private UserTryProblemRepository userTryProblemRepository;

    private boolean isUpdating = false; // 用来标记是否正在执行更新操作

    /**
     * 重建冗余表
     */
    @Transactional
    public void rebuildUserTryProblemOptimizedTable() {
        if (isUpdating) {
            throw new RuntimeException("数据迁移正在进行中，请稍后再试");
        }

        isUpdating = true; // 设置更新标记为正在更新

        try {
            // Step 1: 删除旧数据
            userTryProblemOptimizedRepository.deleteAll();

            // Step 2: 获取所有 UserTryProblem 数据并转化为 DTO
            List<UserTryProblemDTO> dtos = userTryProblemRepository.findAll()
                    .stream()
                    .map(userTryProblem -> new UserTryProblemDTO(userTryProblem, userTryProblem.getUser().getUsername()))
                    .toList();

            // Step 3: 将 DTO 转化为 UserTryProblemOptimized 并保存到冗余表
            for (UserTryProblemDTO dto : dtos) {
                UserTryProblemOptimized optimized = UserTryProblemOptimized.builder()
                        .username(dto.getUsername()) // 根据需要设置用户名为 User ID
                        .problemId(dto.getProblemId())
                        .ojName(dto.getOjName())
                        .pid(dto.getPid())
                        .problemName(dto.getName())
                        .problemType(dto.getType())
                        .points(dto.getPoints())
                        .url(dto.getUrl())
                        .result(dto.getResult())
                        .attemptTime(dto.getAttemptTime())
                        .tags(String.join(",", dto.getTags())) // 以逗号分隔标签
                        .build();
                userTryProblemOptimizedRepository.save(optimized); // 保存到冗余表
            }
        } finally {
            isUpdating = false; // 更新完成后，将标记恢复
        }
    }

    @Transactional
    public Page<UserTryProblemDTO> getOptimizedUserTryProblems(Pageable pageable, String username) {
        // 分页查询 UserTryProblemOptimized 数据并转换为 DTO
        Page<UserTryProblemOptimized> pageResult = userTryProblemOptimizedRepository.findByUsername(username, pageable);

        return pageResult.map(optimized -> new UserTryProblemDTO(
                optimized.getUsername(),
                optimized.getProblemId(),
                optimized.getOjName(),
                optimized.getPid(),
                optimized.getProblemName(),
                optimized.getProblemType(),
                optimized.getPoints(),
                optimized.getUrl(),
                // 将标签字符串拆分为 Set<String>
                tagsToSet(optimized.getTags()),
                optimized.getResult(),
                optimized.getAttemptTime()
        ));
    }

    /**
     * 将逗号分隔的标签字符串转化为 Set<String>
     * @param tags 逗号分隔的标签字符串
     * @return Set<String>
     */
    private Set<String> tagsToSet(String tags) {
        if (tags == null || tags.isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim) // 移除空格
                .collect(Collectors.toSet());
    }

}

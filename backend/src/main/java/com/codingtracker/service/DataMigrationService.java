package com.codingtracker.service;

import com.codingtracker.dto.UserTryProblemDTO;
import com.codingtracker.model.UserTryProblemOptimized;
import com.codingtracker.repository.UserTryProblemOptimizedRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataMigrationService {

    private final UserTryProblemOptimizedRepository optimizedRepo;
    private final UserTryProblemRepository tryRepo;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    @Autowired
    public DataMigrationService(UserTryProblemOptimizedRepository optimizedRepo,
                                UserTryProblemRepository tryRepo) {
        this.optimizedRepo = optimizedRepo;
        this.tryRepo = tryRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildUserTryProblemOptimizedTable() {
        if (!isUpdating.compareAndSet(false, true)) {
            throw new IllegalStateException("冗余表正在重建中，请稍后重试");
        }

        log.info("【DataMigration】开始重建冗余表");

        try {
            optimizedRepo.deleteAllInBatch();

            List<UserTryProblemDTO> dtos = tryRepo.findAll()
                .stream()
                .map(utp -> new UserTryProblemDTO(utp, utp.getUser().getUsername()))
                .collect(Collectors.toList());

            List<UserTryProblemOptimized> records = dtos.stream()
                .map(dto -> UserTryProblemOptimized.builder()
                    .username(dto.getUsername())
                    .problemId(dto.getProblemId())
                    .ojName(dto.getOjName())
                    .pid(dto.getPid())
                    .problemName(dto.getName())
                    .problemType(dto.getType())
                    .points(dto.getPoints())
                    .url(dto.getUrl())
                    .result(dto.getResult())
                    .attemptTime(dto.getAttemptTime())
                    .tags(String.join(",", dto.getTags()))
                    .build())
                .collect(Collectors.toList());

            if (records.isEmpty()) {
                log.warn("【DataMigration】没有可保存的数据，跳过写入");
                return;
            }

            optimizedRepo.saveAll(records);
            log.info("【DataMigration】成功保存 {} 条冗余记录", records.size());

        } catch (Exception e) {
            log.error("【DataMigration】重建冗余表异常", e);
            throw e;
        } finally {
            isUpdating.set(false);
        }
    }

    @Transactional(readOnly = true)
    public Page<UserTryProblemDTO> getOptimizedUserTryProblems(Pageable pageable, String username) {
        return optimizedRepo.findByUsername(username, pageable).map(opt ->
            new UserTryProblemDTO(
                opt.getUsername(),
                opt.getProblemId(),
                opt.getOjName(),
                opt.getPid(),
                opt.getProblemName(),
                opt.getProblemType(),
                opt.getPoints(),
                opt.getUrl(),
                tagsToSet(opt.getTags()),
                opt.getResult(),
                opt.getAttemptTime()
            )
        );
    }

    private Set<String> tagsToSet(String tags) {
        if (tags == null || tags.isBlank()) return new HashSet<>();
        return Arrays.stream(tags.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }
}

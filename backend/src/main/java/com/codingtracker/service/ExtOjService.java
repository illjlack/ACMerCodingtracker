package com.codingtracker.service;

import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import com.codingtracker.service.extoj.IExtOJAdapter;
import com.codingtracker.init.SystemStatsLoader;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class ExtOjService {

    private static final Logger logger = LoggerFactory.getLogger(ExtOjService.class);

    private final UserRepository userRepository;
    private final UserTryProblemRepository tryRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final SystemStatsLoader statsLoader;
    private final List<IExtOJAdapter> adapters;
    private final DataMigrationService dataMigrationService;

    // 代理自身
    @Lazy
    @Autowired
    private ExtOjService selfProxy;

    // 代理 DataMigrationService
    @Lazy
    @Autowired
    private DataMigrationService selfProxyMigration;

    @Getter
    private volatile boolean updating = false;

    public ExtOjService(UserRepository userRepository,
                        UserTryProblemRepository tryRepo,
                        ExtOjPbInfoRepository pbInfoRepo,
                        SystemStatsLoader statsLoader,
                        List<IExtOJAdapter> adapters,
                        DataMigrationService dataMigrationService) {
        this.userRepository = userRepository;
        this.tryRepo = tryRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.statsLoader = statsLoader;
        this.adapters = adapters;
        this.dataMigrationService = dataMigrationService;
    }

    public synchronized boolean triggerFlushTriesDB() {
        if (updating) return false;
        updating = true;
        selfProxy.asyncFlushTriesDB();
        return true;
    }

    @Async
    @Transactional
    public void asyncFlushTriesDB() {
        try {
            flushTriesDB();
            logger.info("尝试重建冗余表...");
            selfProxyMigration.rebuildUserTryProblemOptimizedTable();
        } catch (Exception e) {
            logger.error("异步刷新尝试记录异常", e);
        } finally {
            updating = false;
        }
    }

    private SortedSet<UserTryProblem> fetchAllUserTries(List<User> users) {
        SortedSet<UserTryProblem> set = new TreeSet<>();
        logger.info("开始抓取 {} 位用户的尝试记录", users.size());

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<UserTryProblem>>> futures = new ArrayList<>();

        for (IExtOJAdapter adapter : adapters) {
            for (User user : users) {
                futures.add(pool.submit(() -> adapter.getUserTriesOnline(user)));
            }
        }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(100, TimeUnit.MINUTES)) {
                logger.warn("所有任务未在指定时间内完成");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (Future<List<UserTryProblem>> f : futures) {
            try {
                List<UserTryProblem> problems = f.get(30, TimeUnit.SECONDS);
                if (problems != null) set.addAll(problems);
            } catch (Exception e) {
                logger.error("获取尝试记录失败", e);
            }
        }

        logger.info("抓取完成，共 {} 条尝试记录", set.size());
        return set;
    }

    @Transactional
    public void flushTriesDB() {
        logger.info("刷新所有用户的尝试记录...");
        List<User> users = userRepository.findAll();
        SortedSet<UserTryProblem> current = fetchAllUserTries(users);
        List<UserTryProblem> existing = tryRepo.findAll();
        Set<UserTryProblem> added = new HashSet<>(current);
        existing.forEach(added::remove);
        tryRepo.saveAll(added);
        flushUserLastTryDate(added);
        statsLoader.updateStats(
                statsLoader.getUserCount(),
                statsLoader.getSumProblemCount(),
                statsLoader.getSumTryCount()
        );
        logger.info("刷新完成，新增 {} 条记录，最后更新时间 {}", added.size(), statsLoader.getLastUpdateTime());
    }

    @Transactional
    public void flushUserLastTryDate(Set<UserTryProblem> tries) {
        Map<User, LocalDateTime> lastTimes = tries.stream().collect(Collectors.toMap(
                UserTryProblem::getUser,
                UserTryProblem::getAttemptTime,
                BinaryOperator.maxBy(Comparator.naturalOrder())
        ));
        lastTimes.forEach(User::setLastTryDate);
        userRepository.saveAll(lastTimes.keySet());
        logger.info("已更新 {} 位用户的最后尝试时间", lastTimes.size());
    }

    public LocalDateTime getLastUpdateTime() {
        return statsLoader.getLastUpdateTime();
    }

    @Scheduled(cron = "0 0 0/6 * * ?")
    public void scheduledFlushTriesDB() {
        triggerFlushTriesDB();
    }
}

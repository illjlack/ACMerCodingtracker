package com.codingtracker.controller.api.training;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.dto.UserTryProblemDTO;
import com.codingtracker.model.User;
import com.codingtracker.service.ExtOjService;
import com.codingtracker.service.UserService;
import com.codingtracker.service.UserTryProblemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户尝试记录相关接口控制器
 */
@RestController
@RequestMapping("/api/usertry")
public class UserTryController {

    private static final Logger logger = LoggerFactory.getLogger(UserTryController.class);

    @Autowired private ExtOjService extOjService;
    @Autowired private UserService userService;
    @Autowired private UserTryProblemService userTryProblemService;

    /**
     * 获取指定用户的所有尝试记录（分页）
     */
    @GetMapping("/list/{username}")
    public ApiResponse<Map<String, Object>> list(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("用户不存在：{}", username);
            return ApiResponse.error("用户未找到");
        }

        Page<UserTryProblemDTO> pageResult = userTryProblemService
                .getUserTryProblemsDTO(username, page, size);

        Map<String, Object> data = Map.of(
                "username", username,
                "items", pageResult.getContent(),
                "total", pageResult.getTotalElements()
        );

        return ApiResponse.ok("查询成功", data);
    }

    /**
     * 更新当前登录用户的尝试记录（管理员更新所有）
     */
    @PostMapping("/updatedb")
    public ApiResponse<Void> updatedb() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("尝试更新失败，未登录用户：{}", username);
            return ApiResponse.error("您没有登录");
        }

        logger.info("管理员 {} 触发了所有用户尝试记录的刷新", username);
        extOjService.flushTriesDB();

        return ApiResponse.ok("开始更新，这需要几分钟", null);
    }

    /**
     * 查询尝试记录数量（按用户分组）
     */
    @GetMapping("/stats/try-counts")
    public ApiResponse<?> getTryCounts(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<?> data = userTryProblemService.getTryCounts(start, end);
        return ApiResponse.ok("查询成功", data);
    }

    /**
     * 查询 AC 数量（按用户分组）
     */
    @GetMapping("/stats/ac-counts")
    public ApiResponse<?> getAcCounts(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<?> data = userTryProblemService.getAcCounts(start, end);
        return ApiResponse.ok("查询成功", data);
    }

    /**
     * 手动异步触发系统重建用户尝试记录（仅管理员可用）
     */
    @PostMapping("/stats/rebuild")
    public ApiResponse<Void> manualRebuild() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("未登录用户尝试重建：{}", username);
            return ApiResponse.error("您没有登录");
        }

        if (!userOpt.get().isAdmin()) {
            logger.warn("非管理员用户尝试触发系统重建：{}", username);
            return ApiResponse.error("权限不足，仅管理员可操作");
        }

        if (extOjService.isUpdating()) {
            logger.warn("当前已有更新进行中，用户 {} 的重建请求被拒绝", username);
            return ApiResponse.error("系统正在更新，请稍后再试");
        }

        boolean started = extOjService.triggerFlushTriesDB();
        if (started) {
            logger.info("管理员 {} 成功触发手动重建任务（异步）", username);
            return ApiResponse.ok("重建任务已启动，请稍后查看结果", null);
        } else {
            logger.warn("手动重建任务未能启动，可能系统仍在更新中，用户：{}", username);
            return ApiResponse.error("系统正在更新，请稍后再试");
        }
    }

    /**
     * 强制同步刷新一次（不建议前端开放，调试用）
     */
    @PostMapping("/stats/force-rebuild")
    public ApiResponse<Void> forceRebuild() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            logger.warn("非管理员或未登录用户尝试强制重建：{}", username);
            return ApiResponse.error("您没有权限");
        }

        logger.warn("管理员 {} 强制执行刷新操作", username);
        extOjService.flushTriesDB();
        return ApiResponse.ok("强制刷新成功", null);
    }

    /**
     * 获取最近一次数据更新时间（抓取 + 重建）
     */
    @GetMapping("/stats/last-update")
    public ApiResponse<Map<String, Object>> getLastUpdate() {
        try {
            LocalDateTime lastUpdateTime = extOjService.getLastUpdateTime();
            String lastUpdateStr = lastUpdateTime != null ? lastUpdateTime.toString() : null;
            Map<String, Object> data = Map.of("lastUpdate", lastUpdateStr);
            return ApiResponse.ok("查询成功", data);
        } catch (Exception e) {
            logger.error("查询上次更新时间失败", e);
            return ApiResponse.error("查询失败");
        }
    }

    /**
     * 查询当前系统是否正在执行重建任务
     */
    @GetMapping("/stats/status")
    public ApiResponse<Map<String, Object>> getUpdateStatus() {
        Map<String, Object> data = Map.of(
                "updating", extOjService.isUpdating()
        );
        return ApiResponse.ok("状态查询成功", data);
    }
}

package com.codingtracker.controller.api.auth;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.dto.UserOJDTO;
import com.codingtracker.model.UserOJ;
import com.codingtracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/ojaccounts")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户的所有 OJ 账号
     */
    @GetMapping
    public ApiResponse<List<UserOJDTO>> getOJAccounts() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("用户请求获取OJ账号列表: 用户名={}", username);
        List<UserOJ> ojAccounts = userService.getOJAccountsByUsername(username);
        List<UserOJDTO> dtos = ojAccounts.stream()
                .map(UserOJDTO::new)
                .collect(Collectors.toList());
        logger.info("成功返回OJ账号列表: 用户名={}, 账号数量={}", username, dtos.size());
        return ApiResponse.ok(dtos);
    }

    /**
     * 添加一个新的 OJ 账号
     */
    @PostMapping
    public ApiResponse<Void> addOJAccount(@RequestParam("platform") String platform,
            @RequestParam("accountName") String accountName) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("用户请求添加OJ账号: 用户名={}, 平台={}, 账号名={}", username, platform, accountName);
        boolean added = userService.addOJAccount(username, platform, accountName);
        if (added) {
            logger.info("OJ账号添加成功: 用户名={}, 平台={}, 账号名={}", username, platform, accountName);
            return ApiResponse.ok("添加成功", null);
        }
        logger.warn("OJ账号添加失败: 用户名={}, 平台={}, 账号名={}", username, platform, accountName);
        return ApiResponse.error("添加失败");
    }

    /**
     * 删除指定用户的 OJ 账号
     */
    @DeleteMapping
    public ApiResponse<Void> deleteOJAccount(@RequestParam("platform") String platform,
            @RequestParam("accountName") String accountName) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("用户请求删除OJ账号: 用户名={}, 平台={}, 账号名={}", username, platform, accountName);
        boolean deleted = userService.deleteOJAccount(username, platform, accountName);
        if (deleted) {
            logger.info("OJ账号删除成功: 用户名={}, 平台={}, 账号名={}", username, platform, accountName);
            return ApiResponse.ok("删除成功", null);
        }
        logger.warn("OJ账号删除失败，未找到账号: 用户名={}, 平台={}, 账号名={}", username, platform, accountName);
        return ApiResponse.error("未找到该 OJ 账号");
    }
}

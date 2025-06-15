package com.codingtracker.controller.api;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.model.ExtOjLink;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.User;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.service.ExtOjService;
import com.codingtracker.service.UserService;
import com.codingtracker.service.extoj.IExtOJAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Token管理控制器
 */
@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

    @Autowired
    private ExtOjLinkRepository linkRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ExtOjService extOjService;

    @Autowired
    private List<IExtOJAdapter> adapters;

    /**
     * 获取所有平台的token配置状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getTokenStatus() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("用户 {} 查询token状态", username);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ApiResponse.error("您没有登录");
        }

        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> platforms = new HashMap<>();

            for (OJPlatform platform : OJPlatform.values()) {
                Map<String, Object> platformInfo = new HashMap<>();

                Optional<ExtOjLink> linkOpt = linkRepository.findById(platform);
                if (linkOpt.isPresent()) {
                    ExtOjLink link = linkOpt.get();
                    platformInfo.put("hasToken", link.getAuthToken() != null && !link.getAuthToken().isBlank());
                    platformInfo.put("tokenLength", link.getAuthToken() != null ? link.getAuthToken().length() : 0);
                } else {
                    platformInfo.put("hasToken", false);
                    platformInfo.put("tokenLength", 0);
                }

                platforms.put(platform.name(), platformInfo);
            }

            result.put("platforms", platforms);
            return ApiResponse.ok("查询成功", result);
        } catch (Exception e) {
            logger.error("查询token状态时发生异常", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新指定平台的token
     */
    @PostMapping("/{platform}/update")
    public ApiResponse<Void> updateToken(@PathVariable String platform, @RequestBody Map<String, String> request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("用户 {} 请求更新 {} 平台的token", username, platform);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ApiResponse.error("您没有登录");
        }

        if (!userOpt.get().isAdmin()) {
            return ApiResponse.error("权限不足，仅管理员可操作");
        }

        try {
            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());
            String newToken = request.get("token");

            if (newToken == null || newToken.isBlank()) {
                return ApiResponse.error("Token不能为空");
            }

            // 获取或创建ExtOjLink
            ExtOjLink link = linkRepository.findById(ojPlatform)
                    .orElse(ExtOjLink.builder()
                            .oj(ojPlatform)
                            .build());

            link.setAuthToken(newToken);
            linkRepository.save(link);

            logger.info("管理员 {} 成功更新了 {} 平台的token", username, platform);
            return ApiResponse.ok("Token更新成功", null);
        } catch (IllegalArgumentException e) {
            logger.warn("用户 {} 请求更新无效的平台: {}", username, platform);
            return ApiResponse.error("不支持的平台: " + platform);
        } catch (Exception e) {
            logger.error("更新 {} 平台token时发生异常", platform, e);
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 验证指定平台的token
     */
    @PostMapping("/{platform}/validate")
    public ApiResponse<Map<String, Object>> validatePlatformToken(@PathVariable String platform) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("用户 {} 请求验证 {} 平台的token", username, platform);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ApiResponse.error("您没有登录");
        }

        try {
            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());

            // 找到对应的适配器
            Optional<IExtOJAdapter> adapterOpt = adapters.stream()
                    .filter(adapter -> adapter.getOjType() == ojPlatform)
                    .findFirst();

            if (adapterOpt.isEmpty()) {
                return ApiResponse.error("不支持的平台: " + platform);
            }

            IExtOJAdapter.TokenValidationResult result = adapterOpt.get().validateToken();
            Map<String, Object> data = new HashMap<>();
            data.put("platform", platform);
            data.put("valid", result.isValid());
            data.put("message", result.getMessage());
            data.put("errorCode", result.getErrorCode());

            logger.info("平台 {} token验证结果: {}", platform, result.isValid());
            return ApiResponse.ok("验证完成", data);
        } catch (IllegalArgumentException e) {
            logger.warn("用户 {} 请求验证无效的平台: {}", username, platform);
            return ApiResponse.error("不支持的平台: " + platform);
        } catch (Exception e) {
            logger.error("验证 {} 平台token时发生异常", platform, e);
            return ApiResponse.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定平台的token
     */
    @DeleteMapping("/{platform}")
    public ApiResponse<Void> deleteToken(@PathVariable String platform) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("用户 {} 请求删除 {} 平台的token", username, platform);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ApiResponse.error("您没有登录");
        }

        if (!userOpt.get().isAdmin()) {
            return ApiResponse.error("权限不足，仅管理员可操作");
        }

        try {
            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());

            Optional<ExtOjLink> linkOpt = linkRepository.findById(ojPlatform);
            if (linkOpt.isPresent()) {
                ExtOjLink link = linkOpt.get();
                link.setAuthToken(null);
                linkRepository.save(link);
                logger.info("管理员 {} 成功删除了 {} 平台的token", username, platform);
            }

            return ApiResponse.ok("Token删除成功", null);
        } catch (IllegalArgumentException e) {
            logger.warn("用户 {} 请求删除无效的平台token: {}", username, platform);
            return ApiResponse.error("不支持的平台: " + platform);
        } catch (Exception e) {
            logger.error("删除 {} 平台token时发生异常", platform, e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }
}
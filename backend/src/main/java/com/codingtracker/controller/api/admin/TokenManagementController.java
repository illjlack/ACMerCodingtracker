package com.codingtracker.controller.api.admin;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.service.extoj.IExtOJAdapter;
import com.codingtracker.service.extoj.IExtOJAdapter.TokenValidationResult;
import com.codingtracker.service.extoj.IExtOJAdapter.TokenFormatValidationResult;
import com.codingtracker.service.ExtOjService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Token管理控制器 - 管理各个OJ平台的认证token
 */
@RestController
@RequestMapping("/api/admin/tokens")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class TokenManagementController {

    private static final Logger logger = LoggerFactory.getLogger(TokenManagementController.class);

    @Autowired
    private ExtOjService extOjService;

    @Autowired
    private List<IExtOJAdapter> adapters;

    /**
     * 根据平台类型获取适配器
     */
    private IExtOJAdapter getAdapter(OJPlatform platform) {
        return adapters.stream()
                .filter(adapter -> adapter.getOjType() == platform)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有平台的token状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getTokenStatus() {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Map<String, Object>> platforms = new HashMap<>();

            for (OJPlatform platform : OJPlatform.values()) {
                try {
                    IExtOJAdapter adapter = getAdapter(platform);
                    if (adapter != null) {
                        Map<String, Object> platformInfo = new HashMap<>();

                        // 基本信息
                        platformInfo.put("requiresToken", adapter.requiresToken());
                        platformInfo.put("tokenFormat", adapter.getTokenFormat());

                        // Token状态
                        String token = adapter.getOjLink() != null ? adapter.getOjLink().getAuthToken() : null;
                        platformInfo.put("hasToken", token != null && !token.trim().isEmpty());
                        platformInfo.put("tokenLength", token != null ? token.length() : 0);

                        platforms.put(platform.name(), platformInfo);
                    }
                } catch (Exception e) {
                    logger.error("获取平台 {} token状态失败: {}", platform, e.getMessage());
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("error", "获取状态失败: " + e.getMessage());
                    platforms.put(platform.name(), errorInfo);
                }
            }

            result.put("platforms", platforms);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            logger.error("获取token状态失败: {}", e.getMessage());
            return ApiResponse.error("获取token状态失败: " + e.getMessage());
        }
    }

    /**
     * 验证指定平台的token
     */
    @PostMapping("/{platform}/validate")
    public ApiResponse<Map<String, Object>> validateToken(@PathVariable String platform) {
        try {
            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());
            IExtOJAdapter adapter = getAdapter(ojPlatform);

            if (adapter == null) {
                return ApiResponse.error("不支持的平台: " + platform);
            }

            TokenValidationResult result = adapter.validateToken();

            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("errorCode", result.getErrorCode());

            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的平台名称: " + platform);
        } catch (Exception e) {
            logger.error("验证平台 {} token失败: {}", platform, e.getMessage());
            return ApiResponse.error("验证token失败: " + e.getMessage());
        }
    }

    /**
     * 验证所有平台的token
     */
    @PostMapping("/validate-all")
    public ApiResponse<Map<String, Object>> validateAllTokens() {
        try {
            Map<String, Object> result = extOjService.validateAllTokens();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            logger.error("验证所有token失败: {}", e.getMessage());
            return ApiResponse.error("验证所有token失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定平台的详细信息
     */
    @GetMapping("/{platform}/info")
    public ApiResponse<Map<String, Object>> getPlatformInfo(@PathVariable String platform) {
        try {
            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());
            IExtOJAdapter adapter = getAdapter(ojPlatform);

            if (adapter == null) {
                return ApiResponse.error("不支持的平台: " + platform);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("platform", ojPlatform.name());
            info.put("requiresToken", adapter.requiresToken());
            info.put("tokenFormat", adapter.getTokenFormat());

            if (adapter.getOjLink() != null) {
                info.put("homepageLink", adapter.getOjLink().getHomepageLink());
                info.put("loginPageLink", adapter.getOjLink().getLoginPageLink());
                info.put("hasToken", adapter.getOjLink().getAuthToken() != null &&
                        !adapter.getOjLink().getAuthToken().trim().isEmpty());
            }

            return ApiResponse.ok(info);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的平台名称: " + platform);
        } catch (Exception e) {
            logger.error("获取平台 {} 信息失败: {}", platform, e.getMessage());
            return ApiResponse.error("获取平台信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新指定平台的token
     */
    @PutMapping("/{platform}")
    public ApiResponse<String> updateToken(@PathVariable String platform, @RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null) {
                return ApiResponse.error("缺少token参数");
            }

            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());
            IExtOJAdapter adapter = getAdapter(ojPlatform);

            if (adapter == null) {
                return ApiResponse.error("不支持的平台: " + platform);
            }

            // 验证token格式
            TokenFormatValidationResult formatResult = adapter.validateTokenFormat(token);
            if (!formatResult.isValid()) {
                return ApiResponse.error("Token格式错误: " + formatResult.getMessage());
            }

            // 更新token
            extOjService.updatePlatformToken(ojPlatform, token);

            logger.info("管理员更新了平台 {} 的token", platform);
            return ApiResponse.ok("Token更新成功");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的平台名称: " + platform);
        } catch (Exception e) {
            logger.error("更新平台 {} token失败: {}", platform, e.getMessage());
            return ApiResponse.error("更新token失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定平台的token
     */
    @DeleteMapping("/{platform}")
    public ApiResponse<String> deleteToken(@PathVariable String platform) {
        try {
            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());

            extOjService.updatePlatformToken(ojPlatform, "");

            logger.info("管理员删除了平台 {} 的token", platform);
            return ApiResponse.ok("Token删除成功");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的平台名称: " + platform);
        } catch (Exception e) {
            logger.error("删除平台 {} token失败: {}", platform, e.getMessage());
            return ApiResponse.error("删除token失败: " + e.getMessage());
        }
    }

    /**
     * 验证token格式
     */
    @PostMapping("/{platform}/validate-format")
    public ApiResponse<Map<String, Object>> validateTokenFormat(@PathVariable String platform,
            @RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null) {
                return ApiResponse.error("缺少token参数");
            }

            OJPlatform ojPlatform = OJPlatform.valueOf(platform.toUpperCase());
            IExtOJAdapter adapter = getAdapter(ojPlatform);

            if (adapter == null) {
                return ApiResponse.error("不支持的平台: " + platform);
            }

            TokenFormatValidationResult result = adapter.validateTokenFormat(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("requiredFields", result.getRequiredFields());
            response.put("missingFields", result.getMissingFields());

            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的平台名称: " + platform);
        } catch (Exception e) {
            logger.error("验证平台 {} token格式失败: {}", platform, e.getMessage());
            return ApiResponse.error("验证token格式失败: " + e.getMessage());
        }
    }
}
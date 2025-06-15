package com.codingtracker.service.extoj;

import com.codingtracker.crawler.CFCrawler;
import com.codingtracker.crawler.HttpUtil;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Codeforces 平台实现
 */
@Service
public class CFService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CFService.class);

    private final CFCrawler cfCrawler;
    private final ExtOjLinkRepository extOjLinkRepository;
    private final ExtOjPbInfoRepository extOjPbInfoRepository;
    private final HttpUtil httpUtil;
    private final ObjectMapper mapper = new ObjectMapper();

    public CFService(CFCrawler cfCrawler,
            ExtOjLinkRepository extOjLinkRepository,
            ExtOjPbInfoRepository extOjPbInfoRepository,
            HttpUtil httpUtil) {
        this.cfCrawler = cfCrawler;
        this.extOjLinkRepository = extOjLinkRepository;
        this.extOjPbInfoRepository = extOjPbInfoRepository;
        this.httpUtil = httpUtil;
    }

    @Override
    public ExtOjLink getOjLink() {
        OJPlatform platform = getOjType();
        return extOjLinkRepository.findById(platform)
                .orElseThrow(() -> new RuntimeException("Missing link config for " + platform));
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.CODEFORCES;
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = cfCrawler.userTryProblems(user);

        logger.info("Codeforces 用户 {} 共抓取到 {} 条尝试记录",
                user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        // 使用本地存储的 CF 题目信息
        return extOjPbInfoRepository.findByOjName(getOjType());
    }

    @Override
    public TokenValidationResult validateToken() {
        try {
            ExtOjLink link = getOjLink();
            if (!requiresToken()) {
                return new TokenValidationResult(true, "Codeforces不需要token认证");
            }

            if (link == null || link.getAuthToken() == null || link.getAuthToken().trim().isEmpty()) {
                return new TokenValidationResult(false, "未配置Codeforces认证token", "TOKEN_MISSING");
            }

            // Codeforces API通常不需要特殊的认证token，只需要验证API可用性
            logger.info("验证Codeforces API可用性");

            boolean isValid = cfCrawler.validateConnection();
            if (isValid) {
                return new TokenValidationResult(true, "Codeforces API连接正常");
            } else {
                return new TokenValidationResult(false, "Codeforces API连接失败", "API_ERROR");
            }
        } catch (Exception e) {
            logger.error("验证Codeforces API时发生异常: {}", e.getMessage());
            return new TokenValidationResult(false, "Codeforces API连接异常: " + e.getMessage(), "NETWORK_ERROR");
        }
    }

    @Override
    public boolean requiresToken() {
        ExtOjLink link = getOjLink();
        return link != null && Boolean.TRUE.equals(link.getRequiresToken());
    }

    @Override
    public String getTokenFormat() {
        ExtOjLink link = getOjLink();
        return link != null ? link.getTokenFormat() : "RCPC=xxx; 39ce7=xxx; JSESSIONID=xxx";
    }

    @Override
    public Map<String, String> parseToken(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            return Map.of();
        }
        return Arrays.stream(tokenString.split(";"))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }

    @Override
    public TokenFormatValidationResult validateTokenFormat(String tokenString) {
        if (!requiresToken()) {
            return new TokenFormatValidationResult(true, "Codeforces不需要token认证");
        }

        if (tokenString == null || tokenString.trim().isEmpty()) {
            return new TokenFormatValidationResult(false, "Token不能为空");
        }

        List<String> requiredFields = Arrays.asList("RCPC", "39ce7", "JSESSIONID");
        List<String> missingFields = new ArrayList<>();

        Map<String, String> cookies = parseToken(tokenString);

        for (String field : requiredFields) {
            if (!cookies.containsKey(field) || cookies.get(field).trim().isEmpty()) {
                missingFields.add(field);
            }
        }

        if (missingFields.isEmpty()) {
            return new TokenFormatValidationResult(true, "Codeforces token格式正确");
        } else {
            String message = String.format("Codeforces token缺少必需字段: %s。正确格式: %s",
                    String.join(", ", missingFields), getTokenFormat());
            return new TokenFormatValidationResult(false, message, requiredFields, missingFields);
        }
    }
}

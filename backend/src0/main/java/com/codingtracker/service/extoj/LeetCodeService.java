package com.codingtracker.service.extoj;

import com.codingtracker.crawler.HttpUtil;
import com.codingtracker.crawler.LeetCodeCrawler;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LeetCode 平台实现
 */
@Service
public class LeetCodeService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LeetCodeService.class);

    private final LeetCodeCrawler leetCodeCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final HttpUtil httpUtil;
    private final ObjectMapper mapper = new ObjectMapper();

    public LeetCodeService(LeetCodeCrawler leetCodeCrawler,
            ExtOjLinkRepository linkRepo,
            ExtOjPbInfoRepository pbInfoRepo,
            HttpUtil httpUtil) {
        this.leetCodeCrawler = leetCodeCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.httpUtil = httpUtil;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.LEETCODE;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = leetCodeCrawler.userTryProblems(user);
        logger.info("LeetCode 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        return pbInfoRepo.findByOjName(getOjType());
    }

    @Override
    public TokenValidationResult validateToken() {
        try {
            ExtOjLink link = getOjLink();
            if (link.getAuthToken() == null || link.getAuthToken().isBlank()) {
                return new TokenValidationResult(false, "LeetCode平台未配置认证token", "TOKEN_MISSING");
            }

            Map<String, String> cookies = parseToken(link.getAuthToken());
            if (cookies.isEmpty()) {
                return new TokenValidationResult(false, "LeetCode平台认证token格式无效", "TOKEN_FORMAT_ERROR");
            }

            logger.info("验证LeetCode token");

            boolean isValid = leetCodeCrawler.validateConnection(cookies);
            if (isValid) {
                return new TokenValidationResult(true, "LeetCode平台认证token有效");
            } else {
                return new TokenValidationResult(false, "LeetCode平台认证token已失效，需要重新登录", "TOKEN_EXPIRED");
            }
        } catch (Exception e) {
            logger.error("验证LeetCode token时发生异常: {}", e.getMessage());
            return new TokenValidationResult(false, "LeetCode平台token验证异常: " + e.getMessage(), "VALIDATION_ERROR");
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
        return link != null ? link.getTokenFormat() : "csrftoken=xxx; LEETCODE_SESSION=xxx; sessionid=xxx";
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
        if (tokenString == null || tokenString.trim().isEmpty()) {
            return new TokenFormatValidationResult(false, "Token不能为空");
        }

        List<String> requiredFields = Arrays.asList("csrftoken", "LEETCODE_SESSION", "sessionid");
        List<String> missingFields = new ArrayList<>();

        Map<String, String> cookies = parseToken(tokenString);

        for (String field : requiredFields) {
            if (!cookies.containsKey(field) || cookies.get(field).trim().isEmpty()) {
                missingFields.add(field);
            }
        }

        if (missingFields.isEmpty()) {
            return new TokenFormatValidationResult(true, "LeetCode token格式正确");
        } else {
            String message = String.format("LeetCode token缺少必需字段: %s。正确格式: %s",
                    String.join(", ", missingFields), getTokenFormat());
            return new TokenFormatValidationResult(false, message, requiredFields, missingFields);
        }
    }
}
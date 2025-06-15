package com.codingtracker.service.extoj;

import com.codingtracker.crawler.HttpUtil;
import com.codingtracker.crawler.POJCrawler;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * POJ 平台实现
 */
@Service
public class POJService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(POJService.class);

    private final POJCrawler pojCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final HttpUtil httpUtil;

    public POJService(POJCrawler pojCrawler,
            ExtOjLinkRepository linkRepo,
            ExtOjPbInfoRepository pbInfoRepo,
            HttpUtil httpUtil) {
        this.pojCrawler = pojCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.httpUtil = httpUtil;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.POJ;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = pojCrawler.userTryProblems(user);
        logger.info("POJ 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        // Return all problems stored locally for POJ
        return pbInfoRepo.findByOjName(getOjType());
    }

    @Override
    public TokenValidationResult validateToken() {
        try {
            ExtOjLink link = getOjLink();
            if (!requiresToken()) {
                return new TokenValidationResult(true, "POJ不需要token认证");
            }

            if (link == null || link.getAuthToken() == null || link.getAuthToken().trim().isEmpty()) {
                return new TokenValidationResult(false, "未配置POJ认证token", "TOKEN_MISSING");
            }

            // POJ不需要特殊认证，验证网站可访问性
            logger.info("验证POJ网站可访问性");

            boolean isValid = pojCrawler.validateConnection();
            if (isValid) {
                return new TokenValidationResult(true, "POJ网站连接正常");
            } else {
                return new TokenValidationResult(false, "POJ网站连接失败", "SITE_ERROR");
            }
        } catch (Exception e) {
            logger.error("验证POJ网站时发生异常: {}", e.getMessage());
            return new TokenValidationResult(false, "POJ网站连接异常: " + e.getMessage(), "NETWORK_ERROR");
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
        return link != null ? link.getTokenFormat() : "JSESSIONID=xxx";
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
            return new TokenFormatValidationResult(true, "POJ不需要token认证");
        }

        if (tokenString == null || tokenString.trim().isEmpty()) {
            return new TokenFormatValidationResult(false, "Token不能为空");
        }

        List<String> requiredFields = Arrays.asList("JSESSIONID");
        List<String> missingFields = new ArrayList<>();

        Map<String, String> cookies = parseToken(tokenString);

        for (String field : requiredFields) {
            if (!cookies.containsKey(field) || cookies.get(field).trim().isEmpty()) {
                missingFields.add(field);
            }
        }

        if (missingFields.isEmpty()) {
            return new TokenFormatValidationResult(true, "POJ token格式正确");
        } else {
            String message = String.format("POJ token缺少必需字段: %s。正确格式: %s",
                    String.join(", ", missingFields), getTokenFormat());
            return new TokenFormatValidationResult(false, message, requiredFields, missingFields);
        }
    }
}

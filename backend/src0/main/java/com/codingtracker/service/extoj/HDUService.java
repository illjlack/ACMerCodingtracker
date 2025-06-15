package com.codingtracker.service.extoj;

import com.codingtracker.crawler.HDUCrawler;
import com.codingtracker.crawler.HttpUtil;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HDU 平台实现
 */
@Service
public class HDUService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HDUService.class);

    private final HDUCrawler hduCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final HttpUtil httpUtil;

    public HDUService(HDUCrawler hduCrawler,
            ExtOjLinkRepository linkRepo,
            ExtOjPbInfoRepository pbInfoRepo,
            HttpUtil httpUtil) {
        this.hduCrawler = hduCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.httpUtil = httpUtil;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.HDU;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = hduCrawler.userTryProblems(user);
        logger.info("HDU 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        // 返回本地保存的 HDU 题目信息
        return pbInfoRepo.findByOjName(getOjType());
    }

    @Override
    public TokenValidationResult validateToken() {
        try {
            ExtOjLink link = getOjLink();
            if (!requiresToken()) {
                return new TokenValidationResult(true, "HDU不需要token认证");
            }

            if (link == null || link.getAuthToken() == null || link.getAuthToken().trim().isEmpty()) {
                return new TokenValidationResult(false, "未配置HDU认证token", "TOKEN_MISSING");
            }

            // HDU不需要特殊认证，验证网站可访问性
            logger.info("验证HDU网站可访问性");

            boolean isValid = hduCrawler.validateConnection();
            if (isValid) {
                return new TokenValidationResult(true, "HDU网站连接正常");
            } else {
                return new TokenValidationResult(false, "HDU网站连接失败", "SITE_ERROR");
            }
        } catch (Exception e) {
            logger.error("验证HDU网站时发生异常: {}", e.getMessage());
            return new TokenValidationResult(false, "HDU网站连接异常: " + e.getMessage(), "NETWORK_ERROR");
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
        return link != null ? link.getTokenFormat() : "PHPSESSID=xxx";
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
            return new TokenFormatValidationResult(true, "HDU不需要token认证");
        }

        if (tokenString == null || tokenString.trim().isEmpty()) {
            return new TokenFormatValidationResult(false, "Token不能为空");
        }

        List<String> requiredFields = Arrays.asList("PHPSESSID");
        List<String> missingFields = new ArrayList<>();

        Map<String, String> cookies = parseToken(tokenString);

        for (String field : requiredFields) {
            if (!cookies.containsKey(field) || cookies.get(field).trim().isEmpty()) {
                missingFields.add(field);
            }
        }

        if (missingFields.isEmpty()) {
            return new TokenFormatValidationResult(true, "HDU token格式正确");
        } else {
            String message = String.format("HDU token缺少必需字段: %s。正确格式: %s",
                    String.join(", ", missingFields), getTokenFormat());
            return new TokenFormatValidationResult(false, message, requiredFields, missingFields);
        }
    }
}

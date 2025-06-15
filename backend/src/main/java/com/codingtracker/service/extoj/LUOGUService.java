package com.codingtracker.service.extoj;

import com.codingtracker.crawler.HttpUtil;
import com.codingtracker.crawler.LuoguCrawler;
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
 * LOUGU 平台实现
 */
@Service
public class LUOGUService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LUOGUService.class);

    private final LuoguCrawler luoguCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final HttpUtil httpUtil;

    public LUOGUService(LuoguCrawler luoguCrawler,
            ExtOjLinkRepository linkRepo,
            ExtOjPbInfoRepository pbInfoRepo,
            HttpUtil httpUtil) {
        this.luoguCrawler = luoguCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.httpUtil = httpUtil;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.LUOGU;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = luoguCrawler.userTryProblems(user);
        logger.info("Luogu 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
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
                return new TokenValidationResult(false, "洛谷平台未配置认证token", "TOKEN_MISSING");
            }

            Map<String, String> cookies = parseToken(link.getAuthToken());
            if (cookies.isEmpty()) {
                return new TokenValidationResult(false, "洛谷平台认证token格式无效", "TOKEN_FORMAT_ERROR");
            }

            logger.info("验证洛谷token");

            boolean isValid = luoguCrawler.validateConnection(cookies);
            if (isValid) {
                return new TokenValidationResult(true, "洛谷平台认证token有效");
            } else {
                return new TokenValidationResult(false, "洛谷平台认证token已失效，需要重新登录", "TOKEN_EXPIRED");
            }
        } catch (Exception e) {
            logger.error("验证洛谷token时发生异常: {}", e.getMessage());
            return new TokenValidationResult(false, "洛谷平台token验证异常: " + e.getMessage(), "VALIDATION_ERROR");
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
        return link != null ? link.getTokenFormat() : "__client_id=xxx; _uid=xxx";
    }

    @Override
    public Map<String, String> parseToken(String tokenString) {
        return LuoguCrawler.parseCookies(tokenString);
    }

    @Override
    public TokenFormatValidationResult validateTokenFormat(String tokenString) {
        if (tokenString == null || tokenString.trim().isEmpty()) {
            return new TokenFormatValidationResult(false, "Token不能为空");
        }

        List<String> requiredFields = Arrays.asList("__client_id", "_uid");
        List<String> missingFields = new ArrayList<>();

        Map<String, String> cookies = parseToken(tokenString);

        for (String field : requiredFields) {
            if (!cookies.containsKey(field) || cookies.get(field).trim().isEmpty()) {
                missingFields.add(field);
            }
        }

        if (missingFields.isEmpty()) {
            return new TokenFormatValidationResult(true, "洛谷token格式正确");
        } else {
            String message = String.format("洛谷token缺少必需字段: %s。正确格式: %s",
                    String.join(", ", missingFields), getTokenFormat());
            return new TokenFormatValidationResult(false, message, requiredFields, missingFields);
        }
    }
}

package com.codingtracker.crawler;

import com.codingtracker.exception.CrawlerException;
import com.codingtracker.exception.TokenExpiredException;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.ProblemTagRepository;
import com.codingtracker.repository.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LeetCodeCrawler 类（力扣API客户端），使用GraphQL API获取用户提交记录
 * 支持力扣中国站(leetcode.cn)
 */
@Component
public class LeetCodeCrawler {

    private static final Logger logger = LoggerFactory.getLogger(LeetCodeCrawler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private HttpUtil httpUtil;

    @Autowired
    private ExtOjPbInfoRepository extOjPbInfoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ExtOjLinkRepository extOjLinkRepository;

    @Autowired
    private ProblemTagRepository problemTagRepository;

    public OJPlatform getOjType() {
        return OJPlatform.LEETCODE;
    }

    /**
     * 解析Cookie字符串为Map
     */
    public static Map<String, String> parseCookies(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Map.of();
        }
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }

    /**
     * 验证LeetCode连接状态（使用token）
     */
    public boolean validateConnection(Map<String, String> cookies) {
        try {
            ExtOjLink link = extOjLinkRepository.findById(getOjType()).orElse(null);
            if (link == null || link.getHomepageLink() == null) {
                logger.warn("LeetCode平台链接配置不完整");
                return false;
            }

            // 使用配置的首页链接进行验证，尝试访问用户相关页面
            String testUrl = link.getHomepageLink() + "u/user/";

            int statusCode = httpUtil.checkHttpStatus(testUrl, cookies);

            // 200表示成功访问，说明token有效
            // 302可能是重定向到登录页面，说明token失效
            // 401/403表示未授权，说明token失效
            if (statusCode == 200) {
                return true;
            } else if (statusCode == 302 || statusCode == 401 || statusCode == 403) {
                return false;
            } else {
                // 其他状态码，尝试获取内容进行进一步判断
                try {
                    String response = httpUtil.readURL(testUrl, cookies);
                    return response != null && !response.contains("登录") && !response.contains("sign-in");
                } catch (Exception e) {
                    logger.debug("LeetCode连接内容验证失败: {}", e.getMessage());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("验证LeetCode连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户在LeetCode的基本信息
     */
    public Map<String, Object> getUserInfo(String username) {
        if (StringUtils.isBlank(username)) {
            logger.warn("LeetCode用户名为空");
            return Collections.emptyMap();
        }

        try {
            ExtOjLink ojLink = extOjLinkRepository.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing link config for " + getOjType()));

            String userInfoTemplate = ojLink.getUserInfoLink();
            Map<String, String> cookies = parseCookies(ojLink.getAuthToken());

            String url = String.format(userInfoTemplate, username);
            logger.info("调用 LeetCode 用户信息接口，url：{}", url);

            String response = httpUtil.readURL(url, cookies);
            JsonNode root = mapper.readTree(response);

            // 检查是否有错误
            if (root.has("errors")) {
                JsonNode errors = root.path("errors");
                if (errors.isArray() && !errors.isEmpty()) {
                    String errorMsg = errors.get(0).path("message").asText("Unknown error");
                    logger.warn("LeetCode API 返回错误: {}", errorMsg);

                    // 检查是否是认证失效
                    if (errorMsg.contains("not authenticated") || errorMsg.contains("unauthorized")) {
                        throw new TokenExpiredException(getOjType(), "LeetCode认证token已失效");
                    }
                }
                return Collections.emptyMap();
            }

            JsonNode data = root.path("data").path("matchedUser");
            if (data.isMissingNode()) {
                logger.warn("未找到用户 {} 的信息", username);
                return Collections.emptyMap();
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", data.path("username").asText());
            userInfo.put("realName", data.path("profile").path("realName").asText());
            userInfo.put("ranking", data.path("profile").path("ranking").asInt());
            userInfo.put("reputation", data.path("profile").path("reputation").asInt());

            logger.info("成功获取LeetCode用户 {} 的信息", username);
            return userInfo;
        } catch (TokenExpiredException e) {
            // 重新抛出token失效异常
            throw e;
        } catch (IOException e) {
            logger.error("获取LeetCode用户信息网络请求失败: {}", e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            logger.error("获取LeetCode用户信息时发生未知异常: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取某用户的所有提交记录，并映射成 UserTryProblem 实体列表
     */
    @Transactional
    public List<UserTryProblem> userTryProblems(User user) {
        try {
            // 1. 获取 OJ 配置
            ExtOjLink ojLink = extOjLinkRepository.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing link config for " + getOjType()));
            String submissionTemplate = ojLink.getUserInfoLink();
            String problemPageTemplate = ojLink.getProblemLink();
            Map<String, String> cookies = parseCookies(ojLink.getAuthToken());

            // 2. 收集所有用户名
            List<String> usernames = user.getOjAccounts().stream()
                    .filter(uo -> uo.getPlatform() == getOjType())
                    .map(UserOJ::getAccountName)
                    .flatMap(h -> Arrays.stream(h.split("\\s*,\\s*")))
                    .filter(StringUtils::isNotBlank)
                    .toList();
            if (usernames.isEmpty()) {
                logger.warn("用户 {} 未配置 {} 账号", user.getUsername(), getOjType());
                return Collections.emptyList();
            }

            // 3. 获取所有提交记录
            List<JsonNode> submissions = new ArrayList<>();
            for (String username : usernames) {
                try {
                    // 找到对应的UserOJ实体
                    UserOJ userOj = user.getOjAccounts().stream()
                            .filter(uo -> uo.getPlatform() == getOjType() && uo.getAccountName().equals(username))
                            .findFirst()
                            .orElse(null);

                    if (userOj == null) {
                        logger.warn("未找到用户 {} 的 {} 平台账号: {}", user.getUsername(), getOjType(), username);
                        continue;
                    }

                    // LeetCode使用GraphQL，需要POST请求
                    String graphqlQuery = buildSubmissionsQuery(username);
                    String url = String.format(submissionTemplate, username);

                    // 这里需要使用POST方法发送GraphQL查询
                    // 简化起见，我们先使用GET方式获取已Accept的题目列表
                    logger.info("调用 LeetCode 用户提交记录接口，username：{}", username);

                    String response = httpUtil.readURL(url, cookies);
                    JsonNode root = mapper.readTree(response);

                    // 检查认证状态
                    if (root.has("errors")) {
                        JsonNode errors = root.path("errors");
                        if (errors.isArray() && !errors.isEmpty()) {
                            String errorMsg = errors.get(0).path("message").asText("Unknown error");
                            if (errorMsg.contains("not authenticated") || errorMsg.contains("unauthorized")) {
                                throw new TokenExpiredException(getOjType(), "LeetCode认证token已失效，请重新登录");
                            }
                        }
                        logger.warn("获取用户 {} 提交记录失败: {}", username, errors);
                        continue;
                    }

                    JsonNode data = root.path("data");
                    if (!data.isMissingNode()) {
                        JsonNode submissionList = data.path("recentAcSubmissionList");
                        if (submissionList.isArray()) {
                            // 为每个提交记录关联当前的UserOJ
                            for (JsonNode submission : submissionList) {
                                // 在submission节点中添加userOj信息（临时存储，用于后续处理）
                                ((com.fasterxml.jackson.databind.node.ObjectNode) submission).put("userOjId",
                                        userOj.getId());
                                submissions.add(submission);
                            }
                        }
                    }
                } catch (TokenExpiredException e) {
                    // 重新抛出token失效异常，让全局异常处理器处理
                    throw e;
                } catch (IOException e) {
                    logger.error("获取用户 {} 提交记录网络请求失败: {}", username, e.getMessage());
                } catch (Exception e) {
                    logger.error("获取用户 {} 提交记录时发生异常: {}", username, e.getMessage());
                }
            }

            if (submissions.isEmpty()) {
                logger.info("用户 {} 在 {} 平台没有提交记录", user.getUsername(), getOjType());
                return Collections.emptyList();
            }

            // 4. 批量收集题目ID和标签
            Set<String> allPids = new HashSet<>();
            Map<String, String> pidToTitle = new HashMap<>();
            for (JsonNode sub : submissions) {
                String titleSlug = sub.path("titleSlug").asText();
                String title = sub.path("title").asText();
                allPids.add(titleSlug);
                pidToTitle.put(titleSlug, title);
            }

            // 5. 批量查询已有题目信息
            List<ExtOjPbInfo> existInfos = extOjPbInfoRepository.findByOjNameAndPidIn(getOjType(), allPids);
            Map<String, ExtOjPbInfo> infosMap = existInfos.stream()
                    .collect(Collectors.toMap(ExtOjPbInfo::getPid, Function.identity()));

            // 6. 批量插入缺失的题目
            List<ExtOjPbInfo> newInfos = allPids.stream()
                    .filter(pid -> !infosMap.containsKey(pid))
                    .map(pid -> {
                        String url = String.format(problemPageTemplate, pid);
                        return ExtOjPbInfo.builder()
                                .ojName(getOjType())
                                .pid(pid)
                                .name(pidToTitle.getOrDefault(pid, ""))
                                .url(url)
                                .points(null)
                                .tags(new HashSet<>())
                                .build();
                    })
                    .toList();

            if (!newInfos.isEmpty()) {
                extOjPbInfoRepository.saveAll(newInfos);
                newInfos.forEach(e -> infosMap.put(e.getPid(), e));
            }

            // 7. 构造并返回尝试记录
            List<UserTryProblem> tries = submissions.stream()
                    .map(sub -> {
                        String titleSlug = sub.path("titleSlug").asText();
                        long timestamp = sub.path("timestamp").asLong();
                        LocalDateTime attemptTime = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);

                        // 获取关联的UserOJ实体
                        Integer userOjId = sub.path("userOjId").asInt();
                        UserOJ userOj = user.getOjAccounts().stream()
                                .filter(uo -> uo.getId().equals(userOjId))
                                .findFirst()
                                .orElse(null);

                        return UserTryProblem.builder()
                                .user(user)
                                .userOj(userOj) // 设置关联的OJ账号
                                .extOjPbInfo(infosMap.get(titleSlug))
                                .ojName(getOjType())
                                .result(ProblemResult.AC) // LeetCode API通常只返回AC的提交
                                .attemptTime(attemptTime)
                                .build();
                    })
                    .filter(utp -> utp.getExtOjPbInfo() != null)
                    .toList();

            logger.info("用户 {} 共抓取 {} 条LeetCode尝试记录", user.getUsername(), tries.size());
            return tries;
        } catch (TokenExpiredException e) {
            // 重新抛出token失效异常
            throw e;
        } catch (CrawlerException e) {
            logger.error("LeetCode爬虫异常: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("获取LeetCode用户尝试记录时发生未知异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建GraphQL查询字符串
     */
    private String buildSubmissionsQuery(String username) {
        return String.format("""
                {
                  recentAcSubmissionList(username: "%s", limit: 100) {
                    id
                    titleSlug
                    title
                    timestamp
                    statusDisplay
                    lang
                  }
                }
                """, username);
    }

    /**
     * 批量获取题目信息
     */
    public List<ExtOjPbInfo> getAllPbInfo(List<String> titleSlugs) {
        if (titleSlugs == null || titleSlugs.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<ExtOjPbInfo> list = new ArrayList<>();
            ExtOjLink ojLink = extOjLinkRepository.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing link config for " + getOjType()));
            String problemPageTemplate = ojLink.getProblemLink();

            for (String titleSlug : titleSlugs) {
                ExtOjPbInfo info = extOjPbInfoRepository.findByOjNameAndPid(getOjType(), titleSlug)
                        .orElseGet(() -> {
                            try {
                                ExtOjPbInfo newInfo = fetchProblem(titleSlug, problemPageTemplate);
                                return newInfo != null ? extOjPbInfoRepository.save(newInfo) : null;
                            } catch (Exception e) {
                                logger.error("获取LeetCode题目 {} 信息失败: {}", titleSlug, e.getMessage());
                                return null;
                            }
                        });
                if (info != null) {
                    list.add(info);
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("批量获取LeetCode题目信息时发生异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 拉取单个题目信息
     */
    private ExtOjPbInfo fetchProblem(String titleSlug, String problemPageTemplate) {
        try {
            String url = String.format(problemPageTemplate, titleSlug);
            logger.info("调用 LeetCode 题目详情页面，url：{}", url);

            ExtOjLink ojLink = extOjLinkRepository.findById(getOjType()).orElse(null);
            Map<String, String> cookies = ojLink != null ? parseCookies(ojLink.getAuthToken()) : Map.of();

            String response = httpUtil.readURL(url, cookies);
            JsonNode root = mapper.readTree(response);

            JsonNode questionData = root.path("data").path("question");
            if (questionData.isMissingNode()) {
                logger.warn("未找到题目 {} 的详细信息", titleSlug);
                return null;
            }

            String title = questionData.path("title").asText();
            String difficulty = questionData.path("difficulty").asText();

            return ExtOjPbInfo.builder()
                    .ojName(getOjType())
                    .pid(titleSlug)
                    .name(title)
                    .type("PROGRAMMING")
                    .points(mapDifficultyToPoints(difficulty))
                    .url(url)
                    .tags(new HashSet<>())
                    .build();
        } catch (Exception e) {
            logger.error("拉取LeetCode题目 {} 信息失败: {}", titleSlug, e.getMessage());
            return null;
        }
    }

    /**
     * 将难度映射为分数
     */
    private Double mapDifficultyToPoints(String difficulty) {
        return switch (difficulty.toLowerCase()) {
            case "easy" -> 100.0;
            case "medium" -> 200.0;
            case "hard" -> 300.0;
            default -> null;
        };
    }
}
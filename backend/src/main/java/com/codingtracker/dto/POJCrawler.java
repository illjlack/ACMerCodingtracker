package com.codingtracker.crawler;

import com.codingtracker.exception.CrawlerException;
import com.codingtracker.exception.NetworkException;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import io.micrometer.common.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * POJCrawler 类（POJ OJ 爬虫），
 * 使用 Jsoup 解析 HTML，获取用户尝试记录并映射为 UserTryProblem，
 * 同时支持题目信息抓取。
 */
@Component
public class POJCrawler {

    private static final Logger logger = LoggerFactory.getLogger(POJCrawler.class);

    @Autowired
    private ExtOjLinkRepository linkRepo;

    @Autowired
    private ExtOjPbInfoRepository pbInfoRepo;

    @Autowired
    private HttpUtil httpUtil;

    /**
     * 当前爬虫对应的平台类型
     */
    public OJPlatform getOjType() {
        return OJPlatform.POJ;
    }

    /**
     * 验证POJ网站连接状态
     */
    public boolean validateConnection() {
        try {
            String testUrl = "http://poj.org/";
            Document doc = httpUtil.readJsoupURL(testUrl);
            return doc.title().toLowerCase().contains("poj") || doc.text().contains("Peking University");
        } catch (Exception e) {
            logger.error("验证POJ连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 拉取或创建单个 POJ 题目信息实体
     */
    private ExtOjPbInfo fetchProblem(String pid) {
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing POJ link config"));
        String problemUrl = String.format(link.getProblemLink(), pid);
        logger.info("调用 POJ 题目页面，url：{}", problemUrl);
        try {
            Document doc = Jsoup.connect(problemUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
            String title = Optional.ofNullable(doc.selectFirst("h1")).map(Element::text).orElse(doc.title());
            return ExtOjPbInfo.builder()
                    .ojName(getOjType())
                    .pid(pid)
                    .name(title)
                    .type("PROGRAMMING")
                    .points(null)
                    .url(problemUrl)
                    .tags(Collections.emptySet())
                    .build();
        } catch (IOException e) {
            logger.error("拉取 POJ 题目 {} 信息失败", pid, e);
            return null;
        }
    }

    /**
     * 获取指定用户的所有尝试记录（含 Accepted），映射为 UserTryProblem 列表
     */
    public List<UserTryProblem> userTryProblems(User user) {
        try {
            ExtOjLink link = linkRepo.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing POJ link config"));
            String statusTpl = link.getUserInfoLink(); // e.g. "http://poj.org/status?user_id=%s"

            // 收集用户所有 POJ 账号
            List<String> handles = user.getOjAccounts().stream()
                    .filter(uo -> uo.getPlatform() == getOjType())
                    .map(UserOJ::getAccountName)
                    .flatMap(h -> Arrays.stream(h.split("\\s*,\\s*")))
                    .filter(StringUtils::isNotBlank)
                    .toList();
            if (handles.isEmpty()) {
                logger.warn("用户 {} 未配置 POJ 账号", user.getUsername());
                return Collections.emptyList();
            }

            List<UserTryProblem> tries = new ArrayList<>();
            for (String handle : handles) {
                try {
                    // 找到对应的UserOJ实体
                    UserOJ userOj = user.getOjAccounts().stream()
                            .filter(uo -> uo.getPlatform() == getOjType() && uo.getAccountName().equals(handle))
                            .findFirst()
                            .orElse(null);

                    if (userOj == null) {
                        logger.warn("未找到用户 {} 的 {} 平台账号: {}", user.getUsername(), getOjType(), handle);
                        continue;
                    }

                    String url = String.format(statusTpl, handle);
                    logger.info("调用 POJ 用户状态页面，url：{}", url);
                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .timeout(10000)
                            .get();
                    Element table = doc.selectFirst("table.a");
                    if (table == null) {
                        logger.warn("用户 {} 的提交记录表格未找到", handle);
                        continue;
                    }
                    Elements rows = table.select("tr");
                    for (Element row : rows) {
                        try {
                            Elements cols = row.select("td");
                            if (cols.size() >= 9) {
                                String pid = cols.get(2).text().trim();
                                String verdict = cols.get(3).text().trim();
                                // 只记录 AC
                                if (!"Accepted".equalsIgnoreCase(verdict))
                                    continue;
                                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                                ExtOjPbInfo info = pbInfoRepo.findByOjNameAndPid(getOjType(), pid)
                                        .orElseGet(() -> {
                                            try {
                                                ExtOjPbInfo p = fetchProblem(pid);
                                                return (p != null) ? pbInfoRepo.save(p) : null;
                                            } catch (Exception e) {
                                                logger.error("获取POJ题目 {} 信息失败: {}", pid, e.getMessage());
                                                return null;
                                            }
                                        });
                                if (info == null)
                                    continue;
                                tries.add(UserTryProblem.builder()
                                        .user(user)
                                        .userOj(userOj) // 设置关联的OJ账号
                                        .extOjPbInfo(info)
                                        .ojName(getOjType())
                                        .result(ProblemResult.AC)
                                        .attemptTime(now)
                                        .build());
                            }
                        } catch (Exception e) {
                            logger.warn("解析POJ用户 {} 的某行提交记录时发生异常: {}", handle, e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    logger.error("获取 POJ 用户 {} 提交记录网络请求失败: {}", handle, e.getMessage());
                } catch (Exception e) {
                    logger.error("获取 POJ 用户 {} 提交记录时发生异常: {}", handle, e.getMessage());
                }
            }
            logger.info("POJ 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
            return tries;
        } catch (CrawlerException e) {
            logger.error("POJ爬虫异常: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("获取POJ用户尝试记录时发生未知异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 批量获取题目信息
     */
    public List<ExtOjPbInfo> getAllPbInfo(List<String> pids) {
        List<ExtOjPbInfo> list = new ArrayList<>();
        for (String pid : pids) {
            ExtOjPbInfo info = pbInfoRepo.findByOjNameAndPid(getOjType(), pid)
                    .orElseGet(() -> {
                        ExtOjPbInfo p = fetchProblem(pid);
                        return (p != null) ? pbInfoRepo.save(p) : null;
                    });
            if (info != null)
                list.add(info);
        }
        return list;
    }

    /**
     * 解析Cookie字符串为Map
     */
    public static Map<String, String> parseCookies(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Map.of();
        }

        // 处理key:value格式
        if (cookieHeader.contains(":")) {
            String[] parts = cookieHeader.split(":", 2);
            return Map.of(parts[0].trim(), parts[1].trim());
        }

        // 处理传统的cookie格式
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }
}
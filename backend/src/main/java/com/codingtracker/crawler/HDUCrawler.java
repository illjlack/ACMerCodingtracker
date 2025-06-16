package com.codingtracker.crawler;

import com.codingtracker.exception.CrawlerException;
import com.codingtracker.exception.NetworkException;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.TagRepository;
import io.micrometer.common.util.StringUtils;
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
 * HDUCrawler 类（HDU OJ 爬虫），使用 Jsoup 解析 HTML，
 * 提供与 CFCrawler 类似的接口：获取用户尝试记录和题目信息
 */
@Component
public class HDUCrawler {

    private static final Logger logger = LoggerFactory.getLogger(HDUCrawler.class);

    @Autowired
    private HttpUtil httpUtil;

    @Autowired
    private ExtOjPbInfoRepository pbInfoRepo;

    @Autowired
    private TagRepository tagRepo;

    @Autowired
    private ExtOjLinkRepository linkRepo;

    /**
     * 本爬虫对应的平台类型
     */
    public OJPlatform getOjType() {
        return OJPlatform.HDU;
    }

    /**
     * 验证HDU网站连接状态
     */
    public boolean validateConnection() {
        try {
            String testUrl = "http://acm.hdu.edu.cn/";
            Document doc = httpUtil.readJsoupURL(testUrl);
            return doc.title().contains("HDU") || doc.title().contains("杭电");
        } catch (Exception e) {
            logger.error("验证HDU连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取或创建指定 HDU 题目的基础信息，包括链接
     */
    private ExtOjPbInfo fetchProblem(String pid) {
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing HDU link config"));
        String problemUrl = String.format(link.getProblemLink(), pid);
        logger.info("调用 HDU problem 页面，url：{}", problemUrl);
        try {
            Document doc = httpUtil.readJsoupURL(problemUrl);
            // 示例：题目名称在 .panel_title 或 title
            String title = Optional.ofNullable(doc.selectFirst(".panel_title"))
                    .map(Element::text)
                    .orElse(doc.title());

            return ExtOjPbInfo.builder()
                    .ojName(getOjType())
                    .pid(pid)
                    .name(title)
                    .type("PROGRAMMING")
                    .points(null)
                    .url(problemUrl)
                    .tags(Collections.emptySet())
                    .build();
        } catch (Exception e) {
            logger.error("拉取 HDU 题目 {} 信息失败", pid, e);
            return null;
        }
    }

    /**
     * 获取某用户的所有尝试记录（仅 Accepted），映射为 UserTryProblem 列表
     */
    public List<UserTryProblem> userTryProblems(User user) {
        try {
            ExtOjLink link = linkRepo.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing HDU link config"));
            String statusUrlTpl = link.getUserInfoLink(); // e.g. "http://acm.hdu.edu.cn/status.php?user=%s"

            List<String> handles = user.getOjAccounts().stream()
                    .filter(uo -> uo.getPlatform() == getOjType())
                    .map(UserOJ::getAccountName)
                    .flatMap(h -> Arrays.stream(h.split("\\s*,\\s*")))
                    .filter(StringUtils::isNotBlank)
                    .toList();
            if (handles.isEmpty()) {
                logger.warn("用户 {} 未配置 {} 账号", user.getUsername(), getOjType());
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

                    String statusUrl = String.format(statusUrlTpl, handle);
                    logger.info("调用 HDU user status 页面，url：{}", statusUrl);
                    Document doc = httpUtil.readJsoupURL(statusUrl);
                    Element table = doc.selectFirst("table.table_text");
                    if (table == null) {
                        logger.warn("未找到用户 {} 的提交记录表格", handle);
                        continue;
                    }
                    Elements rows = table.select("tr");
                    for (Element row : rows) {
                        try {
                            Elements cols = row.select("td");
                            if (cols.size() > 5 && "Accepted".equalsIgnoreCase(cols.get(2).text().trim())) {
                                String pid = cols.get(3).text().trim();
                                LocalDateTime now = LocalDateTime.now();
                                // 获取或创建题目信息并保存
                                ExtOjPbInfo info = pbInfoRepo.findByOjNameAndPid(getOjType(), pid)
                                        .orElseGet(() -> {
                                            try {
                                                ExtOjPbInfo pInfo = fetchProblem(pid);
                                                return (pInfo != null) ? pbInfoRepo.save(pInfo) : null;
                                            } catch (Exception e) {
                                                logger.error("获取HDU题目 {} 信息失败: {}", pid, e.getMessage());
                                                return null;
                                            }
                                        });
                                if (info == null)
                                    continue;
                                // 构造 UserTryProblem
                                UserTryProblem utp = UserTryProblem.builder()
                                        .user(user)
                                        .userOj(userOj) // 设置关联的OJ账号
                                        .extOjPbInfo(info)
                                        .ojName(getOjType())
                                        .result(ProblemResult.AC)
                                        .attemptTime(now)
                                        .build();
                                tries.add(utp);
                            }
                        } catch (Exception e) {
                            logger.warn("解析HDU用户 {} 的某行提交记录时发生异常: {}", handle, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("获取HDU用户 {} 提交记录时发生异常: {}", handle, e.getMessage());
                }
            }
            logger.info("HDU 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
            return tries;
        } catch (CrawlerException e) {
            logger.error("HDU爬虫异常: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("获取HDU用户尝试记录时发生未知异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 批量获取题目信息
     */
    public List<ExtOjPbInfo> getAllPbInfo(int startId, int endId) {
        List<ExtOjPbInfo> list = new ArrayList<>();
        for (int i = startId; i <= endId; i++) {
            String pid = String.valueOf(i);
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

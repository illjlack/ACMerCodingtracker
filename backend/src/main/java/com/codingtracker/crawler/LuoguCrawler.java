package com.codingtracker.crawler;

import com.codingtracker.exception.CrawlerException;
import com.codingtracker.exception.TokenExpiredException;
import com.codingtracker.init.TagMetaLoader;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * LuoguCrawler 类（Luogu OJ 爬虫），
 * 使用 JSON API 获取用户 AC 列表，
 * 使用 Jsoup 解析题目详情并构建 ExtOjPbInfo
 */
@Component
public class LuoguCrawler {

    private static final Logger logger = LoggerFactory.getLogger(LuoguCrawler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private HttpUtil httpUtil;

    @Autowired
    private ExtOjLinkRepository linkRepo;

    @Autowired
    private ExtOjPbInfoRepository pbInfoRepo;

    @Autowired
    private TagRepository tagRepo;

    @Autowired
    private TagMetaLoader tagMetaLoader;

    /**
     * 本爬虫对应的平台类型
     */
    public OJPlatform getOjType() {
        return OJPlatform.LUOGU;
    }

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
     * 拉取单个 Luogu 题目信息并构建实体
     */
    public ExtOjPbInfo fetchProblem(String pid) {
        try {
            ExtOjLink link = linkRepo.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing Luogu link config"));
            String url = String.format(link.getProblemLink(), pid);
            Map<String, String> cookies = parseCookies(link.getAuthToken());

            logger.info("调用 Luogu 题目详情页面，url：{}", url);

            // 1) 取页面并解析 JSON
            Document doc = httpUtil.readJsoupURL(url, cookies);

            // 检查是否需要登录
            if (doc.location().contains("auth/login") || doc.title().contains("登录")) {
                throw new TokenExpiredException(getOjType(), "Luogu认证token已失效，请重新登录");
            }

            Element contextElement = doc.getElementById("lentille-context");
            if (contextElement == null) {
                logger.warn("未找到Luogu题目 {} 的上下文数据", pid);
                return null;
            }

            String ctxJson = contextElement.html();
            JsonNode problemNode = new ObjectMapper()
                    .readTree(ctxJson)
                    .path("data")
                    .path("problem");

            if (problemNode.isMissingNode()) {
                logger.warn("Luogu题目 {} 的JSON数据中未找到问题信息", pid);
                return null;
            }

            // 2) 标题优先取 JSON，再 fallback 到 DOM
            String title = Optional.ofNullable(problemNode.path("title").asText(null))
                    .filter(t -> !t.isEmpty())
                    .orElseGet(() -> Optional.ofNullable(doc.selectFirst(".ttitle"))
                            .map(Element::text)
                            .orElse(doc.title()));

            // 3) 拿到题目所有 tag ID
            List<Integer> tagIds = new ArrayList<>();
            problemNode.path("tags").forEach(n -> tagIds.add(n.asInt()));

            // 4) 使用 TagMetaLoader 从内存中映射出每个 TagMetaDTO，然后存库
            Set<Tag> tags = new HashSet<>();
            try {
                tags = tagIds.stream()
                        .map(tagMetaLoader::get) // 从内存 Map 拿 DTO
                        .filter(Objects::nonNull)
                        .map(dto -> tagRepo
                                .findByName(dto.getName()) // 先按 name 查
                                .orElseGet(() -> tagRepo.save(
                                        Tag.builder()
                                                .name(dto.getName())
                                                .build())))
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                logger.warn("处理Luogu题目 {} 的标签时发生异常: {}", pid, e.getMessage());
            }

            // 5) 构建并返回
            return ExtOjPbInfo.builder()
                    .ojName(getOjType())
                    .pid(pid)
                    .name(title)
                    .type("PROGRAMMING")
                    .points(null)
                    .url(url)
                    .tags(tags)
                    .build();

        } catch (TokenExpiredException e) {
            // 重新抛出token失效异常
            throw e;
        } catch (Exception e) {
            logger.error("拉取 Luogu 题目 {} 信息失败: {}", pid, e.getMessage());
            return null;
        }
    }

    @Transactional
    public List<UserTryProblem> userTryProblems(User user) {
        try {
            // 1. 获取 Luogu 链接配置
            ExtOjLink link = linkRepo.findById(getOjType())
                    .orElseThrow(() -> new CrawlerException(getOjType(), "Missing Luogu link config"));
            String userInfoTemplate = link.getUserInfoLink();
            String problemPageTemplate = link.getProblemLink();
            Map<String, String> cookies = parseCookies(link.getAuthToken());

            // 2. 收集所有 uid
            List<String> uids = user.getOjAccounts().stream()
                    .filter(uo -> uo.getPlatform() == getOjType())
                    .map(UserOJ::getAccountName)
                    .flatMap(s -> Arrays.stream(s.split("\\s*,\\s*")))
                    .filter(StringUtils::isNotBlank)
                    .toList();
            if (uids.isEmpty()) {
                logger.warn("用户 {} 未配置 {} 账号", user.getUsername(), getOjType());
                return Collections.emptyList();
            }

            // 3. 拉取所有提交记录（分页）
            List<JsonNode> allRecs = new ArrayList<>();
            for (String uid : uids) {
                int page = 1;
                int consecutiveErrors = 0;
                while (consecutiveErrors < 3) { // 连续3次错误则停止该用户
                    String url = String.format(userInfoTemplate, uid, page);
                    logger.info("调用 Luogu 用户 AC 接口，url：{}", url);
                    try {
                        String json = httpUtil.readURL(url, cookies);

                        // 检查返回内容是否为HTML（可能是登录页面）
                        if (json.trim().startsWith("<")) {
                            logger.error("Luogu用户 {} 第 {} 页返回HTML内容，可能需要重新登录", uid, page);
                            throw new TokenExpiredException(getOjType(), "Luogu认证token已失效，返回登录页面");
                        }

                        JsonNode root = mapper.readTree(json);

                        // 检查API响应状态
                        int code = root.path("code").asInt();
                        if (code == 403 || code == 401) {
                            throw new TokenExpiredException(getOjType(), "Luogu认证token已失效，请重新登录");
                        }
                        if (code != 200) {
                            String message = root.path("message").asText("Unknown error");
                            logger.error("Luogu用户 {} 第 {} 页API返回错误状态 {}: {}", uid, page, code, message);
                            consecutiveErrors++;
                            continue;
                        }

                        JsonNode arr = root.path("currentData").path("records").path("result");
                        if (!arr.isArray() || arr.isEmpty()) {
                            break; // 没有更多数据
                        }
                        arr.forEach(allRecs::add);
                        page++;
                        consecutiveErrors = 0; // 重置错误计数
                    } catch (TokenExpiredException e) {
                        // 重新抛出token失效异常
                        throw e;
                    } catch (IOException e) {
                        logger.error("Luogu用户 {} 第 {} 页网络请求失败: {}", uid, page, e.getMessage());
                        consecutiveErrors++;
                    } catch (Exception e) {
                        logger.error("Luogu用户 {} 第 {} 页请求异常: {}", uid, page, e.getMessage());
                        consecutiveErrors++;
                    }
                }
            }
            if (allRecs.isEmpty()) {
                logger.info("用户 {} 在 {} 平台没有提交记录", user.getUsername(), getOjType());
                return Collections.emptyList();
            }

            // 4. 收集所有 PID，并批量查询题目信息
            Set<String> allPids = allRecs.stream()
                    .map(r -> r.path("problem").path("pid").asText())
                    .collect(Collectors.toSet());
            List<ExtOjPbInfo> existInfos = pbInfoRepo.findAllByOjNameAndPidIn(getOjType(), allPids);
            Map<String, ExtOjPbInfo> infoMap = existInfos.stream()
                    .collect(Collectors.toMap(ExtOjPbInfo::getPid, Function.identity()));

            // 5. 只处理不存在的题目，避免重复插入
            Set<String> missingPids = allPids.stream()
                    .filter(pid -> !infoMap.containsKey(pid))
                    .collect(Collectors.toSet());

            List<ExtOjPbInfo> toInsert = new ArrayList<>();
            List<ExtOjPbInfo> toUpdate = new ArrayList<>();
            List<UserTryProblem> tries = new ArrayList<>();

            // 6. 构建或更新题目信息，并准备尝试记录
            for (JsonNode rec : allRecs) {
                int status = rec.path("status").asInt();
                ProblemResult result = LuoguProblemResultMapping.fromCode(status);

                String pid = rec.path("problem").path("pid").asText();
                String title = rec.path("problem").path("title").asText();
                long secs = rec.path("submitTime").asLong();
                LocalDateTime attemptTime = LocalDateTime.ofEpochSecond(secs, 0, ZoneOffset.UTC);

                ExtOjPbInfo info = infoMap.get(pid);
                if (info == null && missingPids.contains(pid)) {
                    // 新题目：初始化 tags 保持为空
                    info = ExtOjPbInfo.builder()
                            .ojName(getOjType())
                            .pid(pid)
                            .name(title)
                            .type("PROGRAMMING")
                            .points(null)
                            .url(String.format(problemPageTemplate, pid))
                            .tags(new HashSet<>())
                            .build();
                    toInsert.add(info);
                    infoMap.put(pid, info);
                    missingPids.remove(pid); // 避免重复添加
                } else if (info != null && !Objects.equals(info.getName(), title)) {
                    // 题目名称有变化：更新名称，保留原有 tags
                    info.setName(title);
                    toUpdate.add(info);
                }

                // 构造尝试记录
                if (info != null) {
                    tries.add(UserTryProblem.builder()
                            .user(user)
                            .extOjPbInfo(info)
                            .ojName(getOjType())
                            .result(result)
                            .attemptTime(attemptTime)
                            .build());
                }
            }

            // 7. 批量保存题目信息，使用重试机制处理并发冲突
            if (!toInsert.isEmpty()) {
                DatabaseRetryUtil.executeWithRetry(() -> pbInfoRepo.saveAll(toInsert), "插入新题目");
            }
            if (!toUpdate.isEmpty()) {
                DatabaseRetryUtil.executeWithRetry(() -> pbInfoRepo.saveAll(toUpdate), "更新题目信息");
            }

            logger.info("Luogu 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
            return tries;
        } catch (TokenExpiredException e) {
            // 重新抛出token失效异常
            throw e;
        } catch (CrawlerException e) {
            logger.error("Luogu爬虫异常: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("获取Luogu用户尝试记录时发生未知异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 批量获取 Luogu 题目信息
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
}

class LuoguProblemResultMapping {

    private static final Map<Integer, ProblemResult> codeToResult = new HashMap<>();
    private static final Map<ProblemResult, Integer> resultToCode = new HashMap<>();

    static {
        codeToResult.put(12, ProblemResult.AC);
        codeToResult.put(13, ProblemResult.WA);
        codeToResult.put(14, ProblemResult.TLE);
        codeToResult.put(15, ProblemResult.RE);
        codeToResult.put(16, ProblemResult.CE);
        codeToResult.put(17, ProblemResult.OLE);
        codeToResult.put(18, ProblemResult.MLE);
        codeToResult.put(19, ProblemResult.PE);
        codeToResult.put(20, ProblemResult.SUBE);
        codeToResult.put(21, ProblemResult.INQ);
        codeToResult.put(22, ProblemResult.NOJ);
        codeToResult.put(23, ProblemResult.RTL);
        codeToResult.put(24, ProblemResult.REJ);
        codeToResult.put(-1, ProblemResult.UNKNOWN);

        for (Map.Entry<Integer, ProblemResult> entry : codeToResult.entrySet()) {
            resultToCode.put(entry.getValue(), entry.getKey());
        }
    }

    public static ProblemResult fromCode(int code) {
        return codeToResult.getOrDefault(code, ProblemResult.UNKNOWN);
    }

    public static int toCode(ProblemResult result) {
        return resultToCode.getOrDefault(result, -1);
    }
}

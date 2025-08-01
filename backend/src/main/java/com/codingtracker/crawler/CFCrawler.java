package com.codingtracker.crawler;

import com.codingtracker.exception.CrawlerException;
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
import java.util.stream.StreamSupport;

/**
 * CFCrawler 类（Codeforces API 客户端），使用 Jackson 解析 JSON，映射用户基本信息与提交记录。
 */
@Component
public class CFCrawler {

  private static final Logger logger = LoggerFactory.getLogger(CFCrawler.class);
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
    return OJPlatform.CODEFORCES;
  }

  /**
   * 验证Codeforces API连接状态
   */
  public boolean validateConnection() {
    try {
      String testUrl = "https://codeforces.com/api/user.info?handles=tourist";
      String response = httpUtil.readURL(testUrl);
      JsonNode root = mapper.readTree(response);
      return "OK".equals(root.path("status").asText());
    } catch (Exception e) {
      logger.error("验证Codeforces连接失败: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 获取某用户的所有提交记录，并映射成 UserTryProblem 实体列表
   *
   * @param user 当前用户名
   * @return UserTryProblem 列表，发生异常时返回空列表
   */
  @Transactional
  public List<UserTryProblem> userTryProblems(User user) {
    try {
      // 1. 获取 OJ 配置
      ExtOjLink ojLink = extOjLinkRepository.findById(getOjType())
          .orElseThrow(() -> new CrawlerException(getOjType(), "Missing link config for " + getOjType()));
      String userInfoTemplate = ojLink.getUserInfoLink();
      String problemPageTemplate = ojLink.getProblemLink();

      // 2. 收集所有 handles 并获取所有提交
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

      List<JsonNode> submissions = new ArrayList<>();
      for (String handle : handles) {
        String url = String.format(userInfoTemplate, handle);
        try {
          String json = httpUtil.readURL(url);
          JsonNode root = mapper.readTree(json);
          if (!"OK".equals(root.path("status").asText())) {
            String comment = root.path("comment").asText("Unknown error");
            logger.warn("获取用户 {} 提交记录失败: {}", handle, comment);
            continue;
          }
          root.path("result").forEach(submissions::add);
        } catch (IOException e) {
          logger.error("获取用户 {} 提交记录网络请求失败: {}", handle, e.getMessage());
        } catch (Exception e) {
          logger.error("获取用户 {} 提交记录时发生异常: {}", handle, e.getMessage());
        }
      }
      if (submissions.isEmpty()) {
        logger.info("用户 {} 在 {} 平台没有提交记录", user.getUsername(), getOjType());
        return Collections.emptyList();
      }

      // 3. 批量收集 pid 和标签名
      Set<String> allPids = new HashSet<>();
      Map<String, Set<String>> pidToTags = new HashMap<>();
      for (JsonNode sub : submissions) {
        JsonNode p = sub.path("problem");
        String pid = p.path("contestId").asText() + p.path("index").asText();
        allPids.add(pid);
        pidToTags.computeIfAbsent(pid, k -> new HashSet<>())
            .addAll(StreamSupport.stream(p.path("tags").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toSet()));
      }
      Set<String> allTagNames = pidToTags.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

      // 4. 批量查询已有题目和标签
      List<ExtOjPbInfo> existInfos = extOjPbInfoRepository.findByOjNameAndPidIn(getOjType(), allPids);
      List<Tag> existTags = tagRepository.findByNameIn(allTagNames);
      Map<String, ExtOjPbInfo> infosMap = existInfos.stream()
          .collect(Collectors.toMap(ExtOjPbInfo::getPid, Function.identity()));
      Map<String, Tag> tagsMap = existTags.stream()
          .collect(Collectors.toMap(Tag::getName, Function.identity()));

      // 5. 批量插入缺失的题目
      List<ExtOjPbInfo> newInfos = allPids.stream()
          .filter(pid -> !infosMap.containsKey(pid))
          .map(pid -> {
            // 拆 contestId 和 index
            String contestId = pid.replaceAll("\\D.*", "");
            String index = pid.substring(contestId.length());
            String url = String.format(problemPageTemplate, contestId, index);
            return ExtOjPbInfo.builder()
                .ojName(getOjType())
                .pid(pid)
                .name("") // 可选：待更新
                .url(url)
                .points(null)
                .tags(new HashSet<>())
                .build();
          })
          .toList();
      newInfos.forEach(e -> infosMap.put(e.getPid(), e));

      // 6. 批量插入缺失标签
      List<Tag> newTags = allTagNames.stream()
          .filter(name -> !tagsMap.containsKey(name))
          .map(Tag::new)
          .toList();
      newTags.forEach(t -> tagsMap.put(t.getName(), t));

      // 保存获得主键
      extOjPbInfoRepository.saveAll(newInfos);
      tagRepository.saveAll(newTags);

      // 7. 同步题目-标签关系，构建批量插入数据结构
      // 重新查询所有题目和标签，确保都是持久化对象且有ID
      List<ExtOjPbInfo> allInfos = extOjPbInfoRepository.findByOjNameAndPidInWithTags(getOjType(), allPids);
      List<Tag> allTags = tagRepository.findByNameIn(allTagNames);

      // 构建映射和problemTagsMap
      Map<String, Tag> tagNameToTag = allTags.stream()
          .collect(Collectors.toMap(Tag::getName, Function.identity()));

      Map<Long, Set<Long>> problemTagsMap = new HashMap<>();
      for (ExtOjPbInfo info : allInfos) {
        Set<String> desiredTagNames = pidToTags.getOrDefault(info.getPid(), Collections.emptySet());
        Set<Long> tagIds = desiredTagNames.stream()
            .map(tagNameToTag::get)
            .filter(Objects::nonNull)
            .map(Tag::getId)
            .collect(Collectors.toSet());
        problemTagsMap.put(info.getId(), tagIds);
      }

      // 调用批量插入，处理关联关系
      problemTagRepository.batchInsertProblemTags(problemTagsMap);

      // 8. 构造并保存尝试记录
      List<UserTryProblem> tries = submissions.stream().map(sub -> {
        JsonNode p = sub.path("problem");
        String pid = p.path("contestId").asText() + p.path("index").asText();
        LocalDateTime time = LocalDateTime.ofEpochSecond(sub.path("creationTimeSeconds").asLong(), 0, ZoneOffset.UTC);
        ProblemResult result = switch (sub.path("verdict").asText()) {
          case "OK" -> ProblemResult.AC;
          case "WRONG_ANSWER" -> ProblemResult.WA;
          case "TIME_LIMIT_EXCEEDED" -> ProblemResult.TLE;
          case "COMPILATION_ERROR" -> ProblemResult.CE;
          case "RUNTIME_ERROR" -> ProblemResult.RE;
          default -> ProblemResult.UNKNOWN;
        };
        return UserTryProblem.builder()
            .user(user)
            .extOjPbInfo(infosMap.get(pid))
            .ojName(getOjType())
            .result(result)
            .attemptTime(time)
            .build();
      }).toList();

      logger.info("用户 {} 共抓取 {} 条尝试记录", user.getUsername(), tries.size());
      return tries;
    } catch (CrawlerException e) {
      logger.error("CF爬虫异常: {}", e.getMessage());
      return Collections.emptyList();
    } catch (Exception e) {
      logger.error("获取CF用户尝试记录时发生未知异常: {}", e.getMessage());
      return Collections.emptyList();
    }
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

/*
 * https://codeforces.com/api/user.status?handle=%s
 * 
 * {
 * "status": "OK", // API 调用状态；"OK" 表示请求成功
 * "result": [ // 返回的提交记录数组
 * {
 * "id": 317112508, // 提交的唯一 ID
 * "contestId": 2094, // 所属比赛或题库编号
 * "creationTimeSeconds": 1745535740,// 提交创建的 Unix 时间戳（秒）
 * "relativeTimeSeconds": 2147483647,// 相对比赛开始的秒数；2147483647 表示练习模式
 * "problem": { // 提交对应的题目信息
 * "contestId": 2094, // 题目所属比赛编号
 * "index": "A", // 题目在比赛中的标号
 * "name": "Trippi Troppi", // 题目名称
 * "type": "PROGRAMMING", // 题目类型
 * "rating": 800, // 推荐难度
 * "tags": [ // 题目标签列表
 * "strings"
 * ]
 * },
 * "author": { // 提交作者的参赛信息
 * "contestId": 2094, // 参赛比赛编号
 * "participantId": 209280352, // 参赛者 ID
 * "members": [ // 团队成员数组；个人赛通常只有一个成员
 * {
 * "handle": "illjlack" // 成员的 Codeforces handle
 * }
 * ],
 * "participantType": "PRACTICE", // 参赛模式，如 PRACTICE、CONTESTANT 等
 * "ghost": false, // 是否为"鬼"提交（不计排名）
 * "startTimeSeconds": 1744558500 // 参赛开始的 Unix 时间戳（秒）
 * },
 * "programmingLanguage": "C++20 (GCC 13-64)", // 使用的编程语言和版本
 * "verdict": "OK", // 判题结果；"OK" 表示通过 (WRONG_ANSWER, RUNTIME_ERROR,
 * COMPILATION_ERROR)
 * "testset": "TESTS", // 运行的测试集类型
 * "passedTestCount": 4, // 通过的测试用例数量
 * "timeConsumedMillis": 46, // 程序运行耗时（毫秒）
 * "memoryConsumedBytes": 0 // 程序运行耗用内存（字节）
 * },
 * {
 * // … 其他提交记录，格式同上 …
 * }
 * ]
 * }
 *
 */
package com.codingtracker.crawler;

import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
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

    public OJPlatform getOjType() {
        return OJPlatform.CODEFORCES;
    }

    /**
     * 批量调用 Codeforces user.info API，获取用户基本信息。
     * 包括 handle、rating、maxRating、rank、maxRank、avatar、titlePhoto、
     * registrationTime、lastOnlineTime。
     *
     * @param cfNameList 用户 handle 列表
     * @return CFUserInfo 列表，若 API 返回状态非 OK 则返回 null
     */
    public List<CFUserInfo> getUserInfos(List<String> cfNameList) {
        StringJoiner joiner = new StringJoiner(";",
                "https://codeforces.com/api/user.info?handles=", "");
        cfNameList.forEach(joiner::add);
        String url = joiner.toString();
        logger.info("调用 Codeforces user.info 接口，url：{}", url);

        try {
            String response = httpUtil.readURL(url);
            JsonNode root = mapper.readTree(response);
            if (!"OK".equals(root.path("status").asText())) {
                return null;
            }
            JsonNode result = root.path("result");
            List<CFUserInfo> users = new ArrayList<>();
            for (JsonNode node : result) {
                String handle = node.path("handle").asText();
                int rating = node.path("rating").asInt();
                int maxRating = node.path("maxRating").asInt();
                String rank = node.path("rank").asText();
                String maxRank = node.path("maxRank").asText();
                String avatar = node.path("avatar").asText(null);
                String titlePhoto = node.path("titlePhoto").asText(null);
                long regSec = node.path("registrationTimeSeconds").asLong(0L);
                long lastOnlineSec = node.path("lastOnlineTimeSeconds").asLong(0L);
                LocalDateTime registrationTime = LocalDateTime.ofEpochSecond(regSec, 0, ZoneOffset.UTC);
                LocalDateTime lastOnlineTime = LocalDateTime.ofEpochSecond(lastOnlineSec, 0, ZoneOffset.UTC);

                CFUserInfo info = new CFUserInfo(
                        handle,
                        rating,
                        maxRating,
                        rank,
                        maxRank,
                        avatar,
                        titlePhoto,
                        registrationTime,
                        lastOnlineTime
                );
                users.add(info);
            }
            return users;
        } catch (IOException e) {
            logger.error("获取 CF 用户信息失败", e);
            return null;
        }
    }

    /**
     * 获取某用户的所有提交记录，并映射成 UserTryProblem 实体列表
     *
     * @param user 当前用户名
     * @return UserTryProblem 列表
     */
    @Transactional
    public List<UserTryProblem> userTryProblems(User user) {
        // 1. 获取 OJ 配置
        ExtOjLink ojLink = extOjLinkRepository.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
        String userInfoTemplate    = ojLink.getUserInfoLink();
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
                if (!"OK".equals(root.path("status").asText())) continue;
                root.path("result").forEach(submissions::add);
            } catch (IOException e) {
                logger.error("获取用户 {} 提交失败", handle, e);
            }
        }
        if (submissions.isEmpty()) {
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
                    String index     = pid.substring(contestId.length());
                    String url       = String.format(problemPageTemplate, contestId, index);
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
        extOjPbInfoRepository.saveAll(newInfos);
        newInfos.forEach(e -> infosMap.put(e.getPid(), e));

        // 6. 批量插入缺失标签
        List<Tag> newTags = allTagNames.stream()
                .filter(name -> !tagsMap.containsKey(name))
                .map(Tag::new)
                .toList();
        tagRepository.saveAll(newTags);
        newTags.forEach(t -> tagsMap.put(t.getName(), t));

        // 7. 同步题目-标签关系：查询带 tags 实体，再差集更新
        List<ExtOjPbInfo> allInfos = extOjPbInfoRepository.findByOjNameAndPidInWithTags(getOjType(), allPids);
        for (ExtOjPbInfo info : allInfos) {
            String pid = info.getPid();
            Set<String> desired = pidToTags.getOrDefault(pid, Collections.emptySet());
            Set<Tag> current = info.getTags();
            Set<String> currentNames = current.stream().map(Tag::getName).collect(Collectors.toSet());
            // 计算差集
            Set<String> toAdd = new HashSet<>(desired);
            toAdd.removeAll(currentNames);
            Set<String> toRemove = new HashSet<>(currentNames);
            toRemove.removeAll(desired);
            // 删除多余
            current.removeIf(t -> toRemove.contains(t.getName()));
            // 新增缺失
            toAdd.forEach(name -> current.add(tagsMap.get(name)));
        }
        extOjPbInfoRepository.saveAll(allInfos);

        // 8. 构造并保存尝试记录
        List<UserTryProblem> tries = submissions.stream().map(sub -> {
            JsonNode p = sub.path("problem");
            String pid = p.path("contestId").asText() + p.path("index").asText();
            LocalDateTime time = LocalDateTime.ofEpochSecond(sub.path("creationTimeSeconds").asLong(), 0, ZoneOffset.UTC);
            ProblemResult result = switch (sub.path("verdict").asText()) {
                case "OK"                  -> ProblemResult.AC;
                case "WRONG_ANSWER"        -> ProblemResult.WA;
                case "TIME_LIMIT_EXCEEDED" -> ProblemResult.TLE;
                case "COMPILATION_ERROR"   -> ProblemResult.CE;
                case "RUNTIME_ERROR"       -> ProblemResult.RE;
                default                     -> ProblemResult.UNKNOWN;
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
    }
}


/*
https://codeforces.com/api/user.status?handle=%s

{
  "status": "OK",           // API 调用状态；"OK" 表示请求成功
  "result": [               // 返回的提交记录数组
    {
      "id": 317112508,                  // 提交的唯一 ID
      "contestId": 2094,                // 所属比赛或题库编号
      "creationTimeSeconds": 1745535740,// 提交创建的 Unix 时间戳（秒）
      "relativeTimeSeconds": 2147483647,// 相对比赛开始的秒数；2147483647 表示练习模式
      "problem": {                      // 提交对应的题目信息
        "contestId": 2094,              // 题目所属比赛编号
        "index": "A",                   // 题目在比赛中的标号
        "name": "Trippi Troppi",        // 题目名称
        "type": "PROGRAMMING",          // 题目类型
        "rating": 800,                  // 推荐难度
        "tags": [                       // 题目标签列表
          "strings"
        ]
      },
      "author": {                       // 提交作者的参赛信息
        "contestId": 2094,              // 参赛比赛编号
        "participantId": 209280352,     // 参赛者 ID
        "members": [                    // 团队成员数组；个人赛通常只有一个成员
          {
            "handle": "illjlack"        // 成员的 Codeforces handle
          }
        ],
        "participantType": "PRACTICE",  // 参赛模式，如 PRACTICE、CONTESTANT 等
        "ghost": false,                 // 是否为“鬼”提交（不计排名）
        "startTimeSeconds": 1744558500  // 参赛开始的 Unix 时间戳（秒）
      },
      "programmingLanguage": "C++20 (GCC 13-64)", // 使用的编程语言和版本
      "verdict": "OK",                  // 判题结果；"OK" 表示通过 (WRONG_ANSWER, RUNTIME_ERROR, COMPILATION_ERROR)
      "testset": "TESTS",               // 运行的测试集类型
      "passedTestCount": 4,             // 通过的测试用例数量
      "timeConsumedMillis": 46,         // 程序运行耗时（毫秒）
      "memoryConsumedBytes": 0          // 程序运行耗用内存（字节）
    },
    {
      // … 其他提交记录，格式同上 …
    }
  ]
}
*
* */
package com.codingtracker.crawler;

import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public List<UserTryProblem> userTryProblems(User user) {
        // 获取当前 OJ 平台的链接配置
        ExtOjLink ojLink = extOjLinkRepository.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
        String userInfoTemplate = ojLink.getUserInfoLink();    // e.g. "https://codeforces.com/api/user.status?handle=%s"
        String problemPageTemplate = ojLink.getProblemLink();  // e.g. "https://codeforces.com/contest/%s/problem/%s"

        // 收集该用户在本平台的所有 handle，并拆分逗号
        List<String> handles = user.getOjAccounts().stream()
                .filter(uo -> uo.getPlatform() == getOjType())
                .map(UserOJ::getAccountName)
                .flatMap(h -> Arrays.stream(h.split("\\s*,\\s*")))
                .filter(h -> !h.isBlank())
                .toList();

        if (handles.isEmpty()) {
            logger.warn("用户 {} 未配置 {} 账号", user.getUsername(), getOjType());
            return Collections.emptyList();
        }

        List<UserTryProblem> tries = new ArrayList<>();
        for (String handle : handles) {
            String url = String.format(userInfoTemplate, handle);
            logger.info("调用 Codeforces user.status 接口，url：{}", url);

            try {
                String json = httpUtil.readURL(url);
                JsonNode root = mapper.readTree(json);
                if (!"OK".equals(root.path("status").asText())) {
                    logger.warn("handle={} 返回状态非 OK，跳过", handle);
                    continue;
                }

                for (JsonNode sub : root.path("result")) {
                    // 1. 解析提交时间
                    long secs = sub.path("creationTimeSeconds").asLong();
                    LocalDateTime attemptTime = LocalDateTime.ofEpochSecond(secs, 0, ZoneOffset.UTC);

                    // 2. 解析 verdict
                    String v = sub.path("verdict").asText();
                    ProblemResult result = switch (v) {
                        case "OK"                   -> ProblemResult.AC;
                        case "WRONG_ANSWER"         -> ProblemResult.WA;
                        case "TIME_LIMIT_EXCEEDED"  -> ProblemResult.TLE;
                        case "COMPILATION_ERROR"    -> ProblemResult.CE;
                        case "RUNTIME_ERROR"        -> ProblemResult.RE;
                        default                     -> ProblemResult.UNKNOWN;
                    };

                    // 3. 获取或创建题目信息
                    JsonNode p = sub.path("problem");
                    String contestId = p.path("contestId").asText();
                    String index     = p.path("index").asText();
                    String pid       = contestId + index;

                    // 根据模板生成题目链接
                    String problemUrl = String.format(problemPageTemplate, contestId, index);

                    ExtOjPbInfo info = extOjPbInfoRepository
                            .findByOjNameAndPid(getOjType(), pid)
                            .orElseGet(() -> {
                                ExtOjPbInfo e = ExtOjPbInfo.builder()
                                        .ojName(getOjType())
                                        .pid(Integer.parseInt(contestId)+index)
                                        .name(p.path("name").asText())
                                        .type(p.path("type").asText())
                                        .points(p.has("points") ? p.path("points").asDouble() : null)
                                        .url(problemUrl)
                                        .tags(Set.of())
                                        .build();
                                return extOjPbInfoRepository.save(e);
                            });

                    // 4. 构造尝试记录
                    tries.add(UserTryProblem.builder()
                            .user(user)
                            .extOjPbInfo(info)
                            .result(result)
                            .attemptTime(attemptTime)
                            .build()
                    );
                }
            } catch (IOException e) {
                logger.error("获取 CF 用户 {} 提交记录失败", handle, e);
            }
        }

        logger.info("Codeforces 用户 {} 共抓取到 {} 条尝试记录",
                user.getUsername(), tries.size());
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
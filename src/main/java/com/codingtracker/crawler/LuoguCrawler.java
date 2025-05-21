package com.codingtracker.crawler;

import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /**
     * 本爬虫对应的平台类型
     */
    public OJPlatform getOjType() {
        return OJPlatform.LUOGU;
    }

    public static Map<String,String> parseCookies(String cookieHeader) {
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
    private ExtOjPbInfo fetchProblem(String pid) {
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing Luogu link config"));
        String url = String.format(link.getProblemLink(), pid);
        String rawCookie = link.getAuthToken();
        Map<String,String> cookies = parseCookies(rawCookie);

        logger.info("调用 Luogu 题目详情页面，url：{}", url);
        try {
            Document doc = httpUtil.readJsoupURL(url, cookies);
            // 解析题目标题
            String title = Optional.ofNullable(doc.selectFirst(".ttitle"))
                    .map(Element::text)
                    .orElse(doc.title());
            // 解析标签
            Set<Tag> tags = new HashSet<>();
            Elements tagEls = doc.select(".tags .tag");
            for (Element t : tagEls) {
                String name = t.text();
                Tag tag = tagRepo.findByName(name)
                        .orElseGet(() -> tagRepo.save(Tag.builder().name(name).build()));
                tags.add(tag);
            }
            return ExtOjPbInfo.builder()
                    .ojName(getOjType())
                    .pid(pid)
                    .name(title)
                    .type("PROGRAMMING")
                    .points(null)
                    .url(url)
                    .tags(tags)
                    .build();
        } catch (Exception e) {
            logger.error("拉取 Luogu 题目 {} 信息失败", pid, e);
            return null;
        }
    }

    /**
     * 获取指定用户的所有 AC 题目尝试记录
     */
    public List<UserTryProblem> userTryProblems(User user) {
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing Luogu link config"));
        String tpl = link.getUserInfoLink(); // e.g. "https://www.luogu.com.cn/api/problem/user?uid=%s&page=%d"

        // 收集用户所有 Luogu 账号（UID）
        List<String> uids = user.getOjAccounts().stream()
                .filter(uo -> uo.getPlatform() == getOjType())
                .map(UserOJ::getAccountName)
                .flatMap(s -> Arrays.stream(s.split("\\s*,\\s*")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        if (uids.isEmpty()) {
            logger.warn("用户 {} 未配置 {} 账号", user.getUsername(), getOjType());
            return Collections.emptyList();
        }

        List<UserTryProblem> tries = new ArrayList<>();
        for (String uid : uids) {
            int page = 1;
            while (true) {
                String url = String.format(tpl, uid, page);
                logger.info("调用 Luogu 用户 AC 接口，url：{}", url);
                try {
                    String json = httpUtil.readURL(url);
                    JsonNode arr = mapper.readTree(json)
                            .path("currentData").path("records").path("result");
                    if (!arr.isArray() || arr.size() == 0) break;
                    for (JsonNode rec : arr) {
                        String pid = rec.path("problem").path("pid").asText();
                        LocalDateTime now = LocalDateTime.now();
                        ExtOjPbInfo info = pbInfoRepo.findByOjNameAndPid(getOjType(), pid)
                                .orElseGet(() -> {
                                    ExtOjPbInfo p = fetchProblem(pid);
                                    return (p != null) ? pbInfoRepo.save(p) : null;
                                });
                        if (info == null) continue;
                        tries.add(UserTryProblem.builder()
                                .user(user)
                                .extOjPbInfo(info)
                                .result(ProblemResult.AC)
                                .attemptTime(now)
                                .build());
                    }
                    page++;
                } catch (IOException e) {
                    logger.error("获取 Luogu 用户 {} 第 {} 页记录失败", uid, page, e);
                    break;
                }
            }
        }
        logger.info("Luogu 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
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
            if (info != null) list.add(info);
        }
        return list;
    }
}

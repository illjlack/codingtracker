package com.codingtracker.crawler;

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
    public ExtOjPbInfo fetchProblem(String pid) {
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing Luogu link config"));
        String url = String.format(link.getProblemLink(), pid);
        Map<String, String> cookies = parseCookies(link.getAuthToken());

        logger.info("调用 Luogu 题目详情页面，url：{}", url);
        try {
            // 1) 取页面并解析 JSON
            Document doc = httpUtil.readJsoupURL(url, cookies);
            String ctxJson = doc.getElementById("lentille-context").html();
            JsonNode problemNode = new ObjectMapper()
                    .readTree(ctxJson)
                    .path("data")
                    .path("problem");

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
            Set<Tag> tags = tagIds.stream()
                    .map(tagMetaLoader::get)                    // 从内存 Map 拿 DTO
                    .filter(Objects::nonNull)
                    .map(dto -> tagRepo
                            .findByName(dto.getName())         // 先按 name 查
                            .orElseGet(() -> tagRepo.save(
                                    Tag.builder()
                                            .name(dto.getName())
                                            .build())))
                    .collect(Collectors.toSet());

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

        } catch (Exception e) {
            logger.error("拉取 Luogu 题目 {} 信息失败", pid, e);
            return null;
        }
    }

    @Transactional
    public List<UserTryProblem> userTryProblems(User user) {
        // 1. 获取 Luogu 链接配置
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing Luogu link config"));
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
            while (true) {
                String url = String.format(userInfoTemplate, uid, page);
                logger.info("调用 Luogu 用户 AC 接口，url：{}", url);
                try {
                    String json = httpUtil.readURL(url, cookies);
                    JsonNode arr = mapper.readTree(json)
                            .path("currentData").path("records").path("result");
                    if (!arr.isArray() || arr.isEmpty()) break;
                    arr.forEach(allRecs::add);
                    page++;
                } catch (IOException e) {
                    logger.error("获取 Luogu 用户 {} 第 {} 页记录失败", uid, page, e);
                    break;
                }
            }
        }
        if (allRecs.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 收集所有 PID，并批量查询题目信息
        Set<String> allPids = allRecs.stream()
                .map(r -> r.path("problem").path("pid").asText())
                .collect(Collectors.toSet());
        List<ExtOjPbInfo> existInfos = pbInfoRepo.findAllByOjNameAndPidIn(getOjType(), allPids);
        Map<String, ExtOjPbInfo> infoMap = existInfos.stream()
                .collect(Collectors.toMap(ExtOjPbInfo::getPid, Function.identity()));

        List<ExtOjPbInfo> toInsert = new ArrayList<>();
        List<ExtOjPbInfo> toUpdate = new ArrayList<>();
        List<UserTryProblem> tries = new ArrayList<>();

        // 5. 构建或更新题目信息，并准备尝试记录
        for (JsonNode rec : allRecs) {
            String pid = rec.path("problem").path("pid").asText();
            String title = rec.path("problem").path("title").asText();
            long secs = rec.path("submitTime").asLong();
            LocalDateTime attemptTime = LocalDateTime.ofEpochSecond(secs, 0, ZoneOffset.UTC);

            ExtOjPbInfo info = infoMap.get(pid);
            if (info == null) {
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
            } else if (!Objects.equals(info.getName(), title)) {
                // 题目名称有变化：更新名称，保留原有 tags
                info.setName(title);
                toUpdate.add(info);
            }

            // 构造尝试记录
            tries.add(UserTryProblem.builder()
                    .user(user)
                    .extOjPbInfo(info)
                    .ojName(getOjType())
                    .result(ProblemResult.AC)
                    .attemptTime(attemptTime)
                    .build());
        }

        // 6. 批量保存题目信息
        if (!toInsert.isEmpty()) {
            pbInfoRepo.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            pbInfoRepo.saveAll(toUpdate);
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

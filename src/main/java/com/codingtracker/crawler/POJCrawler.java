package com.codingtracker.crawler;

import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
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
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing POJ link config"));
        String statusTpl = link.getUserInfoLink(); // e.g. "http://poj.org/status?user_id=%s"

        // 收集用户所有 POJ 账号
        List<String> handles = user.getOjAccounts().stream()
                .filter(uo -> uo.getPlatform() == getOjType())
                .map(UserOJ::getAccountName)
                .collect(Collectors.toList());
        if (handles.isEmpty()) {
            logger.warn("用户 {} 未配置 POJ 账号", user.getUsername());
            return Collections.emptyList();
        }

        List<UserTryProblem> tries = new ArrayList<>();
        for (String handle : handles) {
            String url = String.format(statusTpl, handle);
            logger.info("调用 POJ 用户状态页面，url：{}", url);
            try {
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
                    Elements cols = row.select("td");
                    if (cols.size() >= 9) {
                        String pid = cols.get(2).text().trim();
                        String verdict = cols.get(3).text().trim();
                        // 只记录 AC
                        if (!"Accepted".equalsIgnoreCase(verdict)) continue;
                        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                        ExtOjPbInfo info = pbInfoRepo.findByOjNameAndPid(getOjType(), pid)
                                .orElseGet(() -> {
                                    ExtOjPbInfo p = fetchProblem(pid);
                                    return (p != null) ? pbInfoRepo.save(p) : null;
                                });
                        if (info == null) continue;
                        tries.add(UserTryProblem.builder()
                                .user(user)
                                .extOjPbInfo(info)
                                .ojName(getOjType())
                                .result(ProblemResult.AC)
                                .attemptTime(now)
                                .build());
                    }
                }
            } catch (IOException e) {
                logger.error("获取 POJ 用户 {} 提交记录失败", handle, e);
            }
        }
        logger.info("POJ 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
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
            if (info != null) list.add(info);
        }
        return list;
    }
}
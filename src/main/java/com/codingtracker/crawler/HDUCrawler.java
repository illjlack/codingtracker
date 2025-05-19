package com.codingtracker.crawler;

import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.TagRepository;
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
        ExtOjLink link = linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing HDU link config"));
        String statusUrlTpl = link.getUserInfoLink(); // e.g. "http://acm.hdu.edu.cn/status.php?user=%s"

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
                Elements cols = row.select("td");
                if (cols.size() > 5 && "Accepted".equalsIgnoreCase(cols.get(2).text().trim())) {
                    String pid = cols.get(3).text().trim();
                    LocalDateTime now = LocalDateTime.now();
                    // 获取或创建题目信息并保存
                    ExtOjPbInfo info = pbInfoRepo.findByOjNameAndPid(getOjType(), pid)
                            .orElseGet(() -> {
                                ExtOjPbInfo pInfo = fetchProblem(pid);
                                return (pInfo != null) ? pbInfoRepo.save(pInfo) : null;
                            });
                    if (info == null) continue;
                    // 构造 UserTryProblem
                    UserTryProblem utp = UserTryProblem.builder()
                            .user(user)
                            .extOjPbInfo(info)
                            .result(ProblemResult.AC)
                            .attemptTime(now)
                            .build();
                    tries.add(utp);
                }
            }
        }
        logger.info("HDU 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
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
            if (info != null) list.add(info);
        }
        return list;
    }


}

package com.codingtracker.service.extoj;

import com.codingtracker.crawler.CFCrawler;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Codeforces 平台实现
 */
@Service
public class CFService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CFService.class);

    private final CFCrawler cfCrawler;
    private final ExtOjLinkRepository extOjLinkRepository;
    private final ExtOjPbInfoRepository extOjPbInfoRepository;

    public CFService(CFCrawler cfCrawler,
                     ExtOjLinkRepository extOjLinkRepository,
                     ExtOjPbInfoRepository extOjPbInfoRepository) {
        this.cfCrawler = cfCrawler;
        this.extOjLinkRepository = extOjLinkRepository;
        this.extOjPbInfoRepository = extOjPbInfoRepository;
    }

    @Override
    public ExtOjLink getOjLink() {
        OJPlatform platform = getOjType();
        return extOjLinkRepository.findById(platform)
                .orElseThrow(() -> new RuntimeException("Missing link config for " + platform));
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.CODEFORCES;
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = cfCrawler.userTryProblems(user);

        logger.info("Codeforces 用户 {} 共抓取到 {} 条尝试记录",
                user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        // 使用本地存储的 CF 题目信息
        return extOjPbInfoRepository.findByOjName(getOjType());
    }
}

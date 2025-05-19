package com.codingtracker.service.extoj;

import com.codingtracker.crawler.HDUCrawler;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * HDU 平台实现
 */
@Service
public class HDUService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HDUService.class);

    private final HDUCrawler hduCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;

    public HDUService(HDUCrawler hduCrawler,
                      ExtOjLinkRepository linkRepo,
                      ExtOjPbInfoRepository pbInfoRepo) {
        this.hduCrawler = hduCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.HDU;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = hduCrawler.userTryProblems(user);
        logger.info("HDU 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        // 返回本地保存的 HDU 题目信息
        return pbInfoRepo.findByOjName(getOjType());
    }
}

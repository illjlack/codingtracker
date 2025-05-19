package com.codingtracker.service.extoj;

import com.codingtracker.crawler.POJCrawler;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * POJ 平台实现
 */
@Service
public class POJService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(POJService.class);

    private final POJCrawler pojCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;

    public POJService(POJCrawler pojCrawler,
                      ExtOjLinkRepository linkRepo,
                      ExtOjPbInfoRepository pbInfoRepo) {
        this.pojCrawler = pojCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.POJ;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        // Delegate to POJCrawler which returns UserTryProblem list
        List<UserTryProblem> tries = pojCrawler.userTryProblems(user);
        logger.info("POJ 用户 {} 共抓取到 {} 条尝试记录",
                user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        // Return all problems stored locally for POJ
        return pbInfoRepo.findByOjName(getOjType());
    }
}

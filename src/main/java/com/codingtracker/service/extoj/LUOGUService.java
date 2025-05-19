package com.codingtracker.service.extoj;

import com.codingtracker.crawler.LuoguCrawler;
import com.codingtracker.model.*;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LOUGU 平台实现
 */
@Service
public class LUOGUService implements IExtOJAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LUOGUService.class);

    private final LuoguCrawler luoguCrawler;
    private final ExtOjLinkRepository linkRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;

    public LUOGUService(LuoguCrawler luoguCrawler,
                        ExtOjLinkRepository linkRepo,
                        ExtOjPbInfoRepository pbInfoRepo) {
        this.luoguCrawler = luoguCrawler;
        this.linkRepo = linkRepo;
        this.pbInfoRepo = pbInfoRepo;
    }

    @Override
    public OJPlatform getOjType() {
        return OJPlatform.LUOGU;
    }

    @Override
    public ExtOjLink getOjLink() {
        return linkRepo.findById(getOjType())
                .orElseThrow(() -> new RuntimeException("Missing link config for " + getOjType()));
    }

    @Override
    public List<UserTryProblem> getUserTriesOnline(User user) {
        List<UserTryProblem> tries = luoguCrawler.userTryProblems(user);
        logger.info("Luogu 用户 {} 共抓取到 {} 条尝试记录", user.getUsername(), tries.size());
        return tries;
    }

    @Override
    public List<ExtOjPbInfo> getAllPbInfoOnline() {
        return pbInfoRepo.findByOjName(getOjType());
    }
}

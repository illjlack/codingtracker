package com.codingtracker.service;

import com.codingtracker.model.SystemState;
import com.codingtracker.repository.SystemStateRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

/**
 * 系统状态服务类，用于定期保存和查询系统关键统计数据
 * 记录：
 * - 用户总数
 * - 累计尝试记录数
 * - 已抓取题目总数
 */
@Service
public class SystemService {

    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);

    private final SystemStateRepository systemStateRepo;
    private final UserRepository userRepo;
    private final UserTryProblemRepository tryRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;

    @Autowired
    public SystemService(SystemStateRepository systemStateRepo,
                         UserRepository userRepo,
                         UserTryProblemRepository tryRepo,
                         ExtOjPbInfoRepository pbInfoRepo) {
        this.systemStateRepo = systemStateRepo;
        this.userRepo = userRepo;
        this.tryRepo = tryRepo;
        this.pbInfoRepo = pbInfoRepo;
    }

    /**
     * 组装当前系统状态数据
     */
    private SystemState assembleCurrentState() {
        return SystemState.builder()
                .date(LocalDate.now())
                .userCount(userRepo.count())
                .sumTryCount(tryRepo.count())
                .sumProblemCount(pbInfoRepo.count())
                .build();
    }

    /**
     * 每日凌晨执行，保存当前系统状态到数据库
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void saveCurrentState() {
        SystemState current = assembleCurrentState();
        systemStateRepo.save(current);
        logger.info("保存当前系统状态: {}", current);
    }

    /**
     * 获取按日期升序排列的系统状态历史
     */
    @Transactional(readOnly = true)
    public List<SystemState> getStateHistory() {
        return systemStateRepo.findAll(Sort.by(Sort.Direction.ASC, "date"));
    }
}

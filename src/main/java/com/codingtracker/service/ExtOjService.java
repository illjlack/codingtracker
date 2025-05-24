package com.codingtracker.service;

import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import com.codingtracker.service.extoj.IExtOJAdapter;
import com.codingtracker.init.SystemStatsLoader;  // 引入加载器
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class ExtOjService {

    private static final Logger logger = LoggerFactory.getLogger(ExtOjService.class);

    private final UserRepository userRepository;
    private final UserTryProblemRepository tryRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final SystemStatsLoader statsLoader;  // 注入加载器
    private final List<IExtOJAdapter> adapters;

    @Lazy
    @Autowired
    private ExtOjService selfProxy;
    // 是否正在更新
    @Getter
    private volatile boolean updating = false;

    public ExtOjService(UserRepository userRepository,
                        UserTryProblemRepository tryRepo,
                        ExtOjPbInfoRepository pbInfoRepo,
                        SystemStatsLoader statsLoader,
                        List<IExtOJAdapter> adapters) {  // 注入自己
        this.userRepository = userRepository;
        this.tryRepo = tryRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.statsLoader = statsLoader;
        this.adapters = adapters;
        this.selfProxy = selfProxy;
    }

    // 触发异步刷新所有用户尝试记录
    public synchronized boolean triggerFlushTriesDB() {
        if (updating) {
            return false; // 已经在更新中
        }
        updating = true;
        selfProxy.asyncFlushTriesDB();
        return true;
    }

    @Async  // 需要配置 @EnableAsync
    @Transactional
    void asyncFlushTriesDB() {
        try {
            flushTriesDB();
        } catch (Exception e) {
            logger.error("异步刷新尝试记录异常", e);
        } finally {
            updating = false;
        }
    }

    private List<IExtOJAdapter> allExtOjServices() {
        return adapters;
    }

    private SortedSet<UserTryProblem> fetchAllUserTries(List<User> users) {
        SortedSet<UserTryProblem> set = new TreeSet<>();
        logger.info("开始抓取 {} 位用户的尝试记录", users.size());

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<UserTryProblem>>> futures = new ArrayList<>();

        for (IExtOJAdapter adapter : adapters) {
            for (User user : users) {
                futures.add(pool.submit(() -> adapter.getUserTriesOnline(user)));
            }
        }

        try {
            pool.shutdown();
            if (!pool.awaitTermination(100, TimeUnit.MINUTES)) {
                logger.warn("所有任务未在指定时间内完成，强制关闭线程池");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("线程池等待任务完成时发生中断", e);
        }

        for (Future<List<UserTryProblem>> f : futures) {
            try {
                List<UserTryProblem> problems = f.get(30, TimeUnit.SECONDS);
                if (problems != null) {
                    set.addAll(problems);
                }
            } catch (TimeoutException | InterruptedException e) {
                logger.error("任务超时", e);
            } catch (ExecutionException e) {
                logger.error("获取尝试记录出错", e);
            }
        }

        logger.info("抓取完成，共 {} 条尝试记录", set.size());
        return set;
    }

    private SortedSet<ExtOjPbInfo> fetchAllProblemInfo() {
        SortedSet<ExtOjPbInfo> set = new TreeSet<>();
        for (IExtOJAdapter adapter : adapters) {
            set.addAll(adapter.getAllPbInfoOnline());
        }
        logger.info("抓取完成，共 {} 道题目信息", set.size());
        return set;
    }

    @Transactional
    public void flushUserLastTryDate(Set<UserTryProblem> tries) {
        Map<User, LocalDateTime> lastTimes = tries.stream()
                .collect(Collectors.toMap(
                        UserTryProblem::getUser,
                        UserTryProblem::getAttemptTime,
                        BinaryOperator.maxBy(Comparator.naturalOrder())
                ));
        lastTimes.forEach((user, time) -> user.setLastTryDate(time));
        userRepository.saveAll(lastTimes.keySet());
        logger.info("已更新 {} 位用户的最后尝试时间", lastTimes.size());
    }

    @Transactional
    public void flushTriesByUser(User user) {
        logger.info("刷新用户 {} 的尝试记录", user.getUsername());
        SortedSet<UserTryProblem> current = fetchAllUserTries(Collections.singletonList(user));
        List<UserTryProblem> existing = tryRepo.findByUser(user);
        Set<UserTryProblem> added = new HashSet<>(current);
        existing.forEach(added::remove);
        tryRepo.saveAll(added);
        flushUserLastTryDate(added);
        logger.info("用户 {} 新增 {} 条尝试记录", user.getUsername(), added.size());
    }

    @Transactional
    public void flushTriesDB() {
        logger.info("刷新所有用户的尝试记录");
        List<User> users = userRepository.findAll();
        SortedSet<UserTryProblem> current = fetchAllUserTries(users);
        List<UserTryProblem> existing = tryRepo.findAll();
        Set<UserTryProblem> added = new HashSet<>(current);
        existing.forEach(added::remove);
        tryRepo.saveAll(added);
        flushUserLastTryDate(added);

        statsLoader.updateStats(
                statsLoader.getUserCount(),
                statsLoader.getSumProblemCount(),
                statsLoader.getSumTryCount()
        );

        logger.info("刷新完成，新增 {} 条尝试记录，更新时间 {}", added.size(), statsLoader.getLastUpdateTime());
    }

    @Transactional
    public void flushPbInfoDB() {
        logger.info("刷新题目信息库");
        SortedSet<ExtOjPbInfo> current = fetchAllProblemInfo();
        List<ExtOjPbInfo> existing = pbInfoRepo.findAll();
        List<ExtOjPbInfo> merged = new ArrayList<>(current);
        existing.stream().filter(info -> !current.contains(info)).forEach(merged::add);
        pbInfoRepo.deleteAll();
        pbInfoRepo.saveAll(merged);
        logger.info("题目信息刷新完成，共 {} 条记录", merged.size());
    }

    public List<UserTryProblem> getUserTries(User user) {
        return tryRepo.findByUser(user);
    }

    public LocalDateTime getLastUpdateTime() {
        return statsLoader.getLastUpdateTime();
    }
}

package com.codingtracker.service;

import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.repository.ExtOjPbInfoRepository;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.UserTryProblemRepository;
import com.codingtracker.service.extoj.IExtOJAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * 外部 OJ 服务，负责并行抓取各平台用户尝试记录与题目信息，并同步到本地数据库。
 */
@Service
public class ExtOjService {

    private static final Logger logger = LoggerFactory.getLogger(ExtOjService.class);

    private final UserRepository userRepository;
    private final UserTryProblemRepository tryRepo;
    private final ExtOjPbInfoRepository pbInfoRepo;
    private final List<IExtOJAdapter> adapters;

    public ExtOjService(UserRepository userRepository,
                        UserTryProblemRepository tryRepo,
                        ExtOjPbInfoRepository pbInfoRepo,
                        List<IExtOJAdapter> adapters) {
        this.userRepository = userRepository;
        this.tryRepo = tryRepo;
        this.pbInfoRepo = pbInfoRepo;
        this.adapters = adapters;
    }

    /**
     * 获取所有注入的外部 OJ 适配器
     */
    private List<IExtOJAdapter> allExtOjServices() {
        return adapters;
    }

    /**
     * 并行抓取指定用户列表的尝试记录
     */
    private SortedSet<UserTryProblem> fetchAllUserTries(List<User> users) {
        SortedSet<UserTryProblem> set = new TreeSet<>();
        logger.info("开始抓取 {} 位用户的尝试记录", users.size());

        ExecutorService pool = Executors.newFixedThreadPool(adapters.size());
        List<Future<List<UserTryProblem>>> futures = new ArrayList<>();

        for (IExtOJAdapter adapter : adapters) {
            for (User user : users) {
                futures.add(pool.submit(() -> adapter.getUserTriesOnline(user)));
            }
        }
        pool.shutdown();

        for (Future<List<UserTryProblem>> f : futures) {
            try {
                set.addAll(f.get(60, TimeUnit.SECONDS));
            } catch (Exception e) {
                logger.error("获取尝试记录出错", e);
            }
        }
        logger.info("抓取完成，共 {} 条尝试记录", set.size());
        return set;
    }

    /**
     * 根据用户名查找并更新单个用户的尝试记录
     */
    public SortedSet<UserTryProblem> fetchUserTriesByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);  // 根据用户名查找用户
        if (user.isEmpty()) {
            logger.error("找不到用户名为 {} 的用户", username);
            return new TreeSet<>();
        }

        // 通过适配器抓取用户的尝试记录
        SortedSet<UserTryProblem> set = new TreeSet<>();
        for (IExtOJAdapter adapter : adapters) {
            try {
                set.addAll(adapter.getUserTriesOnline(user.get()));  // 获取用户的尝试记录
            } catch (Exception e) {
                logger.error("获取用户名为 {} 的用户尝试记录出错", username, e);
            }
        }
        logger.info("抓取用户 {} 的尝试记录，共 {} 条", username, set.size());
        return set;
    }



    /**
     * 并行抓取所有题目信息
     */
    private SortedSet<ExtOjPbInfo> fetchAllProblemInfo() {
        SortedSet<ExtOjPbInfo> set = new TreeSet<>();
        for (IExtOJAdapter adapter : adapters) {
            set.addAll(adapter.getAllPbInfoOnline());
        }
        logger.info("抓取完成，共 {} 道题目信息", set.size());
        return set;
    }

    /**
     * 更新用户最后一次尝试时间（取最新 attemptTime）
     */
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

    /**
     * 刷新单个用户的尝试数据
     */
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

    /**
     * 刷新所有用户的尝试数据
     */
    @Transactional
    public void flushTriesDB() {
        logger.info("刷新所有用户的尝试记录");
        List<User> users = userRepository.findAll();
        SortedSet<UserTryProblem> current = fetchAllUserTries(users);
        List<UserTryProblem> existing = tryRepo.findAll();
        Set<UserTryProblem> added = new HashSet<>(current);
        added.removeAll(existing);
        tryRepo.saveAll(added);
        flushUserLastTryDate(added);
        logger.info("刷新完成，新增 {} 条尝试记录", added.size());
    }

    /**
     * 刷新题目信息库
     */
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

    /**
     * 获取指定用户的尝试记录
     */
    public List<UserTryProblem> getUserTries(User user) {
        return tryRepo.findByUser(user);
    }
}

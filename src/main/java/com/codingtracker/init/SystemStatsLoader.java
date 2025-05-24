package com.codingtracker.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Component
public class SystemStatsLoader {

    private static final Logger logger = LoggerFactory.getLogger(SystemStatsLoader.class);

    private static final String STATS_FILE = "system_stats.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 统计字段
    private long userCount;
    private long sumProblemCount;
    private long sumTryCount;
    private LocalDateTime lastUpdateTime;

    @PostConstruct
    public synchronized void init() {
        File file = Paths.get(STATS_FILE).toFile();
        if (file.exists()) {
            try {
                ObjectNode root = (ObjectNode) objectMapper.readTree(file);
                userCount = root.path("userCount").asLong(0);
                sumProblemCount = root.path("sumProblemCount").asLong(0);
                sumTryCount = root.path("sumTryCount").asLong(0);
                String timeStr = root.path("lastUpdateTime").asText(null);
                lastUpdateTime = (timeStr != null) ? LocalDateTime.parse(timeStr) : LocalDateTime.MIN;
                logger.info("系统统计数据加载成功： userCount={}, sumProblemCount={}, sumTryCount={}, lastUpdateTime={}",
                        userCount, sumProblemCount, sumTryCount, lastUpdateTime);
            } catch (IOException e) {
                logger.error("加载系统统计数据失败，初始化默认值", e);
                initDefaults();
            }
        } else {
            logger.info("系统统计数据文件不存在，初始化默认值");
            initDefaults();
        }
    }

    private void initDefaults() {
        userCount = 0L;
        sumProblemCount = 0L;
        sumTryCount = 0L;
        lastUpdateTime = LocalDateTime.MIN;
    }

    public synchronized long getUserCount() {
        return userCount;
    }

    public synchronized long getSumProblemCount() {
        return sumProblemCount;
    }

    public synchronized long getSumTryCount() {
        return sumTryCount;
    }

    public synchronized LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * 更新统计数据并保存
     */
    public synchronized void updateStats(long userCount, long sumProblemCount, long sumTryCount) {
        this.userCount = userCount;
        this.sumProblemCount = sumProblemCount;
        this.sumTryCount = sumTryCount;
        this.lastUpdateTime = LocalDateTime.now();
        saveToFile();
    }

    private void saveToFile() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("userCount", userCount);
        root.put("sumProblemCount", sumProblemCount);
        root.put("sumTryCount", sumTryCount);
        root.put("lastUpdateTime", lastUpdateTime.toString());

        try {
            File file = Paths.get(STATS_FILE).toFile();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            logger.info("系统统计数据写入文件成功");
        } catch (IOException e) {
            logger.error("写入系统统计数据失败", e);
        }
    }
}

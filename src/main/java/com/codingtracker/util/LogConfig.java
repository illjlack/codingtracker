package com.codingtracker.util;

import lombok.Getter;

import java.io.IOException;
import java.util.logging.*;

public class LogConfig {

    @Getter
    private static final Logger logger = Logger.getLogger(LogConfig.class.getName());

    public static void configure() {
        try {
            // 加载配置文件，指定配置文件路径
            LogManager.getLogManager().readConfiguration(LogConfig.class.getResourceAsStream("/logging.properties"));

            // 获取并使用默认Logger配置
            logger.setLevel(Level.INFO);  // 设置默认的日志级别为INFO
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
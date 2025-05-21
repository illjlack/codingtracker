package com.codingtracker.crawler;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * HttpUtil：提供多种 HTTP 请求方式，包括原生 Java URL、Jsoup 及 HTTPS 支持。
 * 通过 repeatDo 方法重试调用，保证请求的可靠性。
 */
@Component
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    /**
     * 重试执行任务，直到返回非空结果或达到最大重试次数后抛出最后一次异常
     *
     * @param task 要执行的 Callable 任务
     * @param times 最大重试次数
     * @param <T>  返回类型
     * @return 任务返回值
     * @throws Exception 最后一次异常
     */
    public <T> T repeatDo(Callable<T> task, int times) throws Exception {
        Exception lastEx = null;
        for (int i = 1; i <= times; i++) {
            try {
                T result = task.call();
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                lastEx = e;
                logger.warn("第 {} 次执行失败，重试中...", i, e);
            }
        }
        throw lastEx;
    }

    /**
     * 通过 Java 原生 URL 读取文本内容（UTF-8），最多重试 5 次
     *
     * @param urlString 请求地址
     * @return 响应文本
     */
    public String readURL(String urlString) {
        try {
            return repeatDo(() -> {
                logger.info("[*] readURL: {}", urlString);
                return IOUtils.toString(new URL(urlString), "UTF-8");
            }, 5);
        } catch (Exception e) {
            throw new RuntimeException("readURL 失败: " + urlString, e);
        }
    }

    /**
     * 使用 Jsoup 发起 GET 请求并解析为 Document，最多重试 5 次
     *
     * @param urlString 请求地址
     * @return 解析后的 Document
     */
    public Document readJsoupURL(String urlString) {
        try {
            return repeatDo(() -> {
                logger.info("[*] readJsoupURL: {}", urlString);
                return Jsoup.connect(urlString)
                        .timeout(8000)
                        .ignoreContentType(true)
                        .get();
            }, 5);
        } catch (Exception e) {
            throw new RuntimeException("readJsoupURL 失败: " + urlString, e);
        }
    }

    /**
     * 通过原生 HTTPS（HttpURLConnection）获取文本响应，最多重试 5 次
     *
     * @param urlString 请求地址
     * @return 响应文本
     */
    public String readHttpsURL(String urlString) {
        try {
            return repeatDo(() -> {
                logger.info("[*] readHttpsURL: {}", urlString);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8")
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return sb.toString();
            }, 5);
        } catch (Exception e) {
            throw new RuntimeException("readHttpsURL 失败: " + urlString, e);
        }
    }


// =====================================带cookie的

    /**
     * 带 Cookie 的原生 URL GET 请求，最多重试 5 次
     *
     * @param urlString 请求地址
     * @param cookies   要注入的 Cookie（key→value）
     * @return 响应文本
     */
    public String readURL(String urlString, Map<String, String> cookies) {
        try {
            return repeatDo((Callable<String>) () -> {
                logger.info("[*] readURL with cookies: {}", urlString);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 拼装 Cookie 头
                if (cookies != null && !cookies.isEmpty()) {
                    String cookieHeader = cookies.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining("; "));
                    conn.setRequestProperty("Cookie", cookieHeader);
                }

                try (InputStream in = conn.getInputStream()) {
                    return IOUtils.toString(in, StandardCharsets.UTF_8);
                }
            }, 5);
        } catch (Exception e) {
            throw new RuntimeException("readURL with cookies 失败: " + urlString, e);
        }
    }

    /**
     * 带 Cookie 的 Jsoup GET 请求，最多重试 5 次
     *
     * @param urlString 请求地址
     * @param cookies   要注入的 Cookie（key→value）
     * @return 解析后的 Document
     */
    public Document readJsoupURL(String urlString, Map<String, String> cookies) {
        try {
            return repeatDo((Callable<Document>) () -> {
                logger.info("[*] readJsoupURL with cookies: {}", urlString);
                return Jsoup.connect(urlString)
                        .timeout(8000)
                        .ignoreContentType(true)
                        .cookies(cookies == null ? Map.of() : cookies)
                        .get();
            }, 5);
        } catch (Exception e) {
            throw new RuntimeException("readJsoupURL with cookies 失败: " + urlString, e);
        }
    }

    /**
     * 带 Cookie 的原生 HTTPS GET 请求，最多重试 5 次
     *
     * @param urlString 请求地址
     * @param cookies   要注入的 Cookie（key→value）
     * @return 响应文本
     */
    public String readHttpsURL(String urlString, Map<String, String> cookies) {
        try {
            return repeatDo((Callable<String>) () -> {
                logger.info("[*] readHttpsURL with cookies: {}", urlString);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 拼装 Cookie 头
                if (cookies != null && !cookies.isEmpty()) {
                    String cookieHeader = cookies.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining("; "));
                    conn.setRequestProperty("Cookie", cookieHeader);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return sb.toString();
            }, 5);
        } catch (Exception e) {
            throw new RuntimeException("readHttpsURL with cookies 失败: " + urlString, e);
        }
    }
}

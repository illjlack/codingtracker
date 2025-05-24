package com.codingtracker.controller.api.training;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.dto.UserTryProblemDTO;
import com.codingtracker.model.User;
import com.codingtracker.service.ExtOjService;
import com.codingtracker.service.UserService;
import com.codingtracker.service.UserTryProblemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户尝试记录相关接口
 */
@RestController
@RequestMapping("/api/usertry")
public class UserTryController {

    private static final Logger logger = LoggerFactory.getLogger(UserTryController.class);

    @Autowired private ExtOjService extOjService;
    @Autowired private UserService userService;
    @Autowired private UserTryProblemService userTryProblemService;

    /**
     * 获取指定用户的所有尝试记录
     */
    @GetMapping("/list/{username}")
    public ApiResponse<Map<String, Object>> list(@PathVariable String username) {
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("User not found: {}", username);
            return ApiResponse.error("用户未找到");
        }
        List<UserTryProblemDTO> dtoList = extOjService.getUserTries(userOpt.get()).stream()
                .map(utp -> new UserTryProblemDTO(utp, username))
                .collect(Collectors.toList());

        Map<String, Object> data = Map.of(
                "username", username,
                "userTryProblems", dtoList
        );
        return ApiResponse.ok("查询成功", data);
    }

    /**
     * 更新数据库中的用户尝试记录
     */
    @PostMapping("/updatedb")
    public ApiResponse<Void> updatedb() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("Unauthorized update attempt");
            return ApiResponse.error("您没有登录");
        }

        if (userOpt.get().isAdmin()) {
            logger.info("Admin {} flushes all user tries", username);
            extOjService.flushTriesDB();
        } else {
            logger.info("User {} flushes own tries", username);
            extOjService.flushTriesByUser(userOpt.get());
        }

        return ApiResponse.ok("更新完毕", null);
    }

    @GetMapping("/stats/try-counts")
    public ApiResponse<?> getTryCounts(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<?> data = userTryProblemService.getTryCounts(start, end);
        return ApiResponse.ok("查询成功", data);
    }

    @GetMapping("/stats/ac-counts")
    public ApiResponse<?> getAcCounts(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<?> data = userTryProblemService.getAcCounts(start, end);
        return ApiResponse.ok("查询成功", data);
    }

    /**
     * 手动触发重新爬取（重建数据）
     * 仅管理员可调用
     */
    @PostMapping("/stats/rebuild")
    public ApiResponse<Void> manualRebuild() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("Unauthorized rebuild attempt: no login");
            return ApiResponse.error("您没有登录");
        }
//        if (!userOpt.get().isAdmin()) {
//            logger.warn("Unauthorized rebuild attempt by user: {}", username);
//            return ApiResponse.error("权限不足");
//        }

        if (extOjService.isUpdating()) {
            logger.warn("Manual rebuild rejected: update already in progress");
            return ApiResponse.error("系统正在更新，请稍后再试");
        }

        boolean started = extOjService.triggerFlushTriesDB();
        if (started) {
            logger.info("Admin {} triggered manual rebuild of user tries asynchronously", username);
            return ApiResponse.ok("手动重建已启动，请稍后查看结果", null);
        } else {
            logger.warn("Manual rebuild rejected by trigger method");
            return ApiResponse.error("系统正在更新，请稍后再试");
        }
    }


    /**
     * 查询上次爬虫数据更新时间
     * 这里假设 ExtOjService 有 getLastUpdateTime 方法，返回 LocalDateTime
     */
    @GetMapping("/stats/last-update")
    public ApiResponse<Map<String, Object>> getLastUpdate() {
        try {
            LocalDateTime lastUpdateTime = extOjService.getLastUpdateTime();
            String lastUpdateStr = lastUpdateTime != null ? lastUpdateTime.toString() : null;
            Map<String, Object> data = Map.of("lastUpdate", lastUpdateStr);
            return ApiResponse.ok("查询成功", data);
        } catch (Exception e) {
            logger.error("Failed to get last update time", e);
            return ApiResponse.error("查询失败");
        }
    }
}

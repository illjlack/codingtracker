package com.codingtracker.controller.api.training;

import com.codingtracker.DTO.ApiResponse;
import com.codingtracker.DTO.UserTryProblemDTO;
import com.codingtracker.model.User;
import com.codingtracker.service.ExtOjService;
import com.codingtracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户尝试记录相关接口
 */
@RestController
@RequestMapping("/usertry")
public class UserTryController {

    private static final Logger logger = LoggerFactory.getLogger(UserTryController.class);

    @Autowired private ExtOjService extOjService;
    @Autowired private UserService userService;

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
}

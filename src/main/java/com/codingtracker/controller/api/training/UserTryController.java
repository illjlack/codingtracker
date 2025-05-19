package com.codingtracker.controller.api.training;

import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.service.ExtOjService;
import com.codingtracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/usertry")
public class UserTryController {

    private static final Logger logger = LoggerFactory.getLogger(UserTryController.class);

    @Autowired private ExtOjService extOjService;
    @Autowired private UserService userService;

    /**
     * 获取指定用户的所有尝试记录
     */
    @GetMapping("/{username}/list")
    public ResponseEntity<Map<String, Object>> list(@PathVariable String username) {
        Optional<User> user = userService.getUserByUsername(username);
        if (user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<UserTryProblem> list= extOjService.getUserTries(user.get());
        Map<String, Object> result = Map.of(
                "username", username,
                "userTryProblems", list
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 更新数据库中的用户尝试记录
     */
    @PostMapping(value = "/updatedb", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> updatedb() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userService.getUserByUsername(username);
        if (user.isEmpty()) {
            return new ResponseEntity<>("您没有登录", HttpStatus.BAD_REQUEST);
        }
        if (user.get().isAdmin()) {
            extOjService.flushTriesDB();  // 修改为刷新所有用户的尝试记录
        } else {
            extOjService.flushTriesByUser(user.get());  // 修改为刷新单个用户的尝试记录
        }
        return new ResponseEntity<>("更新完毕", HttpStatus.OK);
    }

    /**
     * 更新单个用户的尝试记录
     */
    @PutMapping("/{username}/update")
    public ResponseEntity<String> updateUserTries(@PathVariable String username) {
        try {
            extOjService.fetchUserTriesByUsername(username);
            return ResponseEntity.ok("用户 " + username + " 的尝试记录已更新");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失败: " + e.getMessage());
        }
    }


}

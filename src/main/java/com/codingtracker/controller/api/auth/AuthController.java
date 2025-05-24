// src/main/java/com/codingtracker/controller/api/auth/AuthController.java
package com.codingtracker.controller.api.auth;

import com.codingtracker.DTO.ApiResponse;
import com.codingtracker.DTO.UserInfoDTO;
import com.codingtracker.model.User;
import com.codingtracker.service.UserService;
import com.codingtracker.util.JwtUtils;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 用户登录并返回 token
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>>  login(@RequestBody LoginRequest req) {
        logger.info("用户登录尝试: 用户名={}", req.username);
        User user = userService.valid(req.username, req.password);
        if (user != null) {
            String token = JwtUtils.generateToken(req.username);
            logger.info("用户登录成功: 用户名={}", req.username);
            Map<String, Object> data = Map.of(
                    "token", token
            );
            return ApiResponse.ok("登录成功", data);
        } else {
            logger.warn("用户登录失败: 用户名={}", req.username);
            return ApiResponse.error("用户名或密码错误");
        }
    }

    // 用户注册
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest req) {
        logger.info("用户注册尝试: 用户名={}", req.username);
        User user = new User();
        user.setUsername(req.username);
        user.setPassword(req.password);
        user.setRealName(req.realName);
        user.setMajor(req.major);
        user.setEmail(req.email);
        user.getRoles().add(User.Type.NEW);

        boolean ok = userService.registerUser(user);
        if (ok) {
            logger.info("用户注册成功: 用户名={}", req.username);
            return ApiResponse.ok("注册成功，请登录！", null);
        } else {
            logger.error("用户注册失败: 用户名={}", req.username);
            return ApiResponse.error("注册失败！");
        }
    }

    // 修改用户信息
    @PutMapping("/modify")
    public ApiResponse<Void> modifyUser(@RequestBody UserInfoDTO user) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("请求获取用户信息: 用户名={}", username);
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("未找到用户信息: 用户名={}", username);
            return ApiResponse.error("未找到用户信息");
        }
        UserInfoDTO dto = UserInfoDTO.fromUser(userOpt.get());

        // 调用业务层修改
        userService.modifyUser(user);
        return ApiResponse.ok();
    }

    // 修改密码
    @PutMapping("/modifyPassword")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        logger.info("用户尝试修改密码: 用户名={}", req.username);
        User user = userService.valid(req.username, req.oldPassword);
        if (user != null) {
            userService.modifyUserPassword(user.getId(), req.newPassword);
            logger.info("用户密码修改成功: 用户名={}", req.username);
            return ApiResponse.ok("密码修改成功", null);
        } else {
            logger.warn("用户密码修改失败（旧密码错误或用户不存在）: 用户名={}", req.username);
            return ApiResponse.error("旧密码错误或用户不存在");
        }
    }

    // 获取用户信息
    @GetMapping("/userInfo")
    public ApiResponse<UserInfoDTO> getUserInfo() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("请求获取用户信息: 用户名={}", username);
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("未找到用户信息: 用户名={}", username);
            return ApiResponse.error("未找到用户信息");
        }
        UserInfoDTO dto = UserInfoDTO.fromUser(userOpt.get());
        logger.info("成功返回用户信息: 用户名={}", username);
        return ApiResponse.ok("获取成功", dto);
    }

    // 用户登出
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        logger.info("用户登出: 用户名={}", username);

        // 此处可执行其它登出清理操作

        return ApiResponse.ok("登出成功", null);
    }

    /** 请求体和内部 DTO **/
    @Getter @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }
    @Getter @Setter
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String major;
        private String email;
    }
    @Getter @Setter
    public static class ChangePasswordRequest {
        private String username;
        private String oldPassword;
        private String newPassword;
    }
}

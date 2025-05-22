// src/main/java/com/codingtracker/controller/api/auth/AuthController.java
package com.codingtracker.controller.api.auth;

import com.codingtracker.DTO.ApiResponse;
import com.codingtracker.model.User;
import com.codingtracker.service.UserService;
import com.codingtracker.util.JwtUtils;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<String> login(@RequestBody LoginRequest req) {
        logger.info("Login attempt for user: {}", req.username);
        User user = userService.valid(req.username, req.password);
        if (user != null) {
            String token = JwtUtils.generateToken(req.username);
            logger.info("Login successful for user: {}", req.username);
            return ApiResponse.ok("登录成功", token);
        } else {
            logger.warn("Login failed for user: {}", req.username);
            return ApiResponse.error("用户名或密码错误");
        }
    }

    // 用户注册
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest req) {
        logger.info("Registration attempt for user: {}", req.username);
        User user = new User();
        user.setUsername(req.username);
        user.setPassword(req.password);
        user.setRealName(req.realName);
        user.setMajor(req.major);
        user.setEmail(req.email);
        user.getRoles().add(User.Type.NEW);

        boolean ok = userService.registerUser(user);
        if (ok) {
            logger.info("User registered successfully: {}", req.username);
            return ApiResponse.ok("注册成功，请登录！", null);
        } else {
            logger.error("Registration failed for user: {}", req.username);
            return ApiResponse.error("注册失败！");
        }
    }

    // 修改用户信息
    @PutMapping("/modify")
    public ApiResponse<Void> modifyUser(@RequestBody User user,
                                        @RequestHeader("Authorization") String token) {
        String username = JwtUtils.getUsernameFromToken(token.replace("Bearer ", ""));
        if (username == null) {
            logger.warn("Invalid or expired token for modification attempt.");
            return ApiResponse.error("Token 无效或已过期");
        }
        logger.info("Modifying user information for: {}", username);
        userService.modifyUser(user);
        return ApiResponse.ok("信息修改成功", null);
    }

    // 修改密码
    @PutMapping("/modifyPassword")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        logger.info("尝试修改密码，用户: {}", req.username);
        User user = userService.valid(req.username, req.oldPassword);
        if (user != null) {
            userService.modifyUserPassword(user.getId(), req.newPassword);
            logger.info("密码修改成功，用户: {}", req.username);
            return ApiResponse.ok("密码修改成功", null);
        } else {
            logger.warn("旧密码错误或用户不存在，用户: {}", req.username);
            return ApiResponse.error("旧密码错误或用户不存在");
        }
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

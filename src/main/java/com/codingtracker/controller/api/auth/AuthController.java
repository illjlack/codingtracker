package com.codingtracker.controller.api.auth;

import org.springframework.web.bind.annotation.*;

import com.codingtracker.model.User;
import com.codingtracker.service.UserService;
import com.codingtracker.util.JwtUtils;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 用户登录并返回 token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        logger.info("Login attempt for user: {}", username);

        // 验证用户
        User user = userService.valid(username, password);
        if (user != null) {
            String token = JwtUtils.generateToken(username);
            logger.info("Login successful for user: {}", username);
            return ResponseEntity.ok(new LoginResponse("登录成功", true , token));
        } else {
            logger.warn("Login failed for user: {}. Invalid username or password.", username);
            return ResponseEntity.status(401).body(new ErrorResponse("用户名或密码错误", 401));
        }
    }

    // 用户注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String realName = registerRequest.getRealName();
        String major = registerRequest.getMajor();
        String email = registerRequest.getEmail();

        logger.info("Registration attempt for user: {}", username);
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setMajor(major);
        user.setEmail(email);
        user.getRoles().add(User.Type.NEW);

        if (userService.registerUser(user)) {
            logger.info("User registered successfully: {}", username);
            return ResponseEntity.ok(new SuccessResponse("注册成功，请登录！", true));
        } else {
            logger.error("Registration failed for user: {}", username);
            return ResponseEntity.status(400).body(new ErrorResponse("注册失败！", 400));
        }
    }

    // 修改用户信息
    @PutMapping("/modify")
    public ResponseEntity<?> modifyUser(@RequestBody User user, @RequestHeader("Authorization") String token) {
        String username = JwtUtils.getUsernameFromToken(token.replace("Bearer ", ""));
        if (username == null) {
            logger.warn("Invalid or expired token for modification attempt.");
            return ResponseEntity.status(401).body(new ErrorResponse("Token无效或已过期", 401));
        }

        logger.info("Modifying user information for: {}", username);
        userService.modifyUser(user);
        return ResponseEntity.ok(new SuccessResponse("信息修改成功", true));
    }

    // 修改密码
    @PutMapping("/modifyPassword")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        String username = request.getUsername();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        logger.info("尝试修改密码，用户: {}", username);

        // 复用 valid 方法验证旧密码
        User user = userService.valid(username, oldPassword);
        if (user != null) {
            // 加密新密码并更新
            userService.modifyUserPassword(user.getId(), newPassword);
            logger.info("密码修改成功，用户: {}", username);
            return ResponseEntity.ok(new SuccessResponse("密码修改成功", true));
        } else {
            logger.warn("旧密码错误或用户不存在，用户: {}", username);
            return ResponseEntity.status(400).body(new ErrorResponse("旧密码错误或用户不存在", 400));
        }
    }

    // 错误响应
    @Getter
    @Setter
    public static class ErrorResponse {
        private String message;
        private int errorCode;  // 错误码
        public ErrorResponse(String message, int errorCode) { 
            this.message = message; 
            this.errorCode = errorCode; 
        }
    }

    // 成功响应
    @Getter
    @Setter
    public static class SuccessResponse {
        private boolean success;
        private String message;
        public SuccessResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }

    // 登录请求体
    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    // 注册请求体
    @Getter
    @Setter
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String major;
        private String email;
    }

    // 修改密码请求体
    @Getter
    @Setter
    public static class ChangePasswordRequest {
        private String username;
        private String oldPassword;
        private String newPassword;
    }

    // 登录响应体
    @Getter
    @Setter
    public static class LoginResponse {
        private String message;
        private String token;
        private boolean success;
        public LoginResponse(String message,boolean success, String token) {
            this.message = message;
            this.token = token;
            this.success = success;
        }
    }
}

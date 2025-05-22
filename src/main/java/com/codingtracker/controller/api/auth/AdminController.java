package com.codingtracker.controller.api.auth;

import com.codingtracker.DTO.ApiResponse;
import com.codingtracker.model.User;
import com.codingtracker.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员专用接口：用户信息的增删改查（单个/批量）
 */
@RestController
@RequestMapping("/api/auth/admin")
@Slf4j
@RolesAllowed("ADMIN")   // 只有 ADMIN 角色能调用
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /** 列出所有用户 */
    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> listUsers() {
        List<User> users = userService.findAllUsers();
        List<UserResponse> resp = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(resp);
    }

    /** 获取单个用户详情 */
    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Integer id) {
        return userService.findById(id)
                .map(u -> ApiResponse.ok(UserResponse.from(u)))
                .orElseGet(() -> ApiResponse.error("用户未找到"));
    }

    /** 创建新用户 */
    @PostMapping("/users")
    public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest req) {
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(req.getPassword());
        u.setRealName(req.getRealName());
        u.setMajor(req.getMajor());
        u.setEmail(req.getEmail());
        u.setBlogUrl(req.getBlogUrl());
        u.getRoles().clear();
        if (req.getRoles() != null) {
            req.getRoles().forEach(r -> u.getRoles().add(User.Type.valueOf(r)));
        }
        User saved = userService.createUser(u);
        log.info("Admin created user {}", saved.getUsername());
        return ApiResponse.ok("创建成功", UserResponse.from(saved));
    }

    /** 更新单个用户 */
    @PutMapping("/users/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Integer id,
                                                @RequestBody UpdateUserRequest req) {
        return userService.findById(id).map(u -> {
            if (req.getRealName() != null) u.setRealName(req.getRealName());
            if (req.getMajor()   != null) u.setMajor(req.getMajor());
            if (req.getEmail()   != null) u.setEmail(req.getEmail());
            if (req.getBlogUrl() != null) u.setBlogUrl(req.getBlogUrl());
            if (req.getRoles()   != null) {
                u.getRoles().clear();
                req.getRoles().forEach(r -> u.getRoles().add(User.Type.valueOf(r)));
            }
            User updated = userService.updateUser(u);
            log.info("Admin updated user {}", updated.getUsername());
            return ApiResponse.ok("更新成功", UserResponse.from(updated));
        }).orElseGet(() -> ApiResponse.error("用户未找到"));
    }

    /** 删除单个用户 */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Integer id) {
        if (!userService.existsById(id)) {
            return ApiResponse.error("用户未找到");
        }
        userService.deleteUser(id);
        log.info("Admin deleted user id={}", id);
        return ApiResponse.ok("删除成功", null);
    }

    /** 批量更新用户信息 */
    @PostMapping("/users/batch")
    public ApiResponse<List<UserResponse>> batchUpdate(@RequestBody List<UpdateUserRequest> reqs) {
        List<UserResponse> results = new ArrayList<>();
        for (UpdateUserRequest req : reqs) {
            userService.findById(req.getId()).ifPresent(u -> {
                if (req.getRealName() != null) u.setRealName(req.getRealName());
                if (req.getMajor()   != null) u.setMajor(req.getMajor());
                if (req.getEmail()   != null) u.setEmail(req.getEmail());
                if (req.getBlogUrl() != null) u.setBlogUrl(req.getBlogUrl());
                if (req.getRoles()   != null) {
                    u.getRoles().clear();
                    req.getRoles().forEach(r -> u.getRoles().add(User.Type.valueOf(r)));
                }
                User updated = userService.updateUser(u);
                results.add(UserResponse.from(updated));
                log.info("Admin batch updated user {}", updated.getUsername());
            });
        }
        return ApiResponse.ok("批量更新完成", results);
    }

    // —— DTOs —— //

    @Getter @Setter
    public static class UserResponse {
        private Integer id;
        private String username;
        private String realName;
        private String major;
        private String email;
        private String blogUrl;
        private Set<User.Type> roles;
        private Date lastTryDate;

        public static UserResponse from(User u) {
            UserResponse r = new UserResponse();
            r.setId(u.getId());
            r.setUsername(u.getUsername());
            r.setRealName(u.getRealName());
            r.setMajor(u.getMajor());
            r.setEmail(u.getEmail());
            r.setBlogUrl(u.getBlogUrl());
            r.setRoles(u.getRoles());
            if (u.getLastTryDate() != null) {
                r.setLastTryDate(java.sql.Timestamp.valueOf(u.getLastTryDate()));
            }
            return r;
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String realName;
        private String major;
        private String email;
        private String blogUrl;
        private Set<String> roles;
    }

    @Getter @Setter @NoArgsConstructor
    public static class UpdateUserRequest {
        private Integer id;
        private String realName;
        private String major;
        private String email;
        private String blogUrl;
        private Set<String> roles;
    }
}

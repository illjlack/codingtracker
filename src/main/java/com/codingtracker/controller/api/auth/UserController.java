package com.codingtracker.controller.api.auth;

import com.codingtracker.DTO.ApiResponse;
import com.codingtracker.DTO.UserOJDTO;
import com.codingtracker.model.UserOJ;
import com.codingtracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/ojaccounts")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户的所有 OJ 账号
     */
    @GetMapping
    public ApiResponse<List<UserOJDTO>> getOJAccounts() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        List<UserOJ> ojAccounts = userService.getOJAccountsByUsername(username);
        List<UserOJDTO> dtos = ojAccounts.stream()
                .map(UserOJDTO::new)
                .collect(Collectors.toList());
        return ApiResponse.ok(dtos);
    }

    /**
     * 添加一个新的 OJ 账号
     */
    @PostMapping
    public ApiResponse<Void> addOJAccount(@RequestParam("platform") String platform,
                                          @RequestParam("accountName") String accountName) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        boolean added = userService.addOJAccount(username, platform, accountName);
        if (added) {
            return ApiResponse.ok("添加成功", null);
        }
        return ApiResponse.error("添加失败");
    }

    /**
     * 删除指定用户的 OJ 账号
     */
    @DeleteMapping
    public ApiResponse<Void> deleteOJAccount(@RequestParam("platform") String platform,
                                             @RequestParam("accountName") String accountName) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        boolean deleted = userService.deleteOJAccount(username, platform, accountName);
        if (deleted) {
            return ApiResponse.ok("删除成功", null);
        }
        return ApiResponse.error("未找到该 OJ 账号");
    }
}

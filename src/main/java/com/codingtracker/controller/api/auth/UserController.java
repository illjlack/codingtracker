package com.codingtracker.controller.api.auth;

import com.codingtracker.DTO.UserOJDTO;
import com.codingtracker.model.UserOJ;
import com.codingtracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/ojaccounts")  // 修改后的请求路径，不再需要 userId
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户的所有 OJ 账号
     * 通过 SecurityContextHolder 获取当前用户
     *
     * @return 用户的 OJ 账号列表
     */
    @GetMapping
    public ResponseEntity<List<UserOJDTO>> getOJAccounts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();  // 从 SecurityContext 获取用户名
        List<UserOJ> ojAccounts = userService.getOJAccountsByUsername(username);  // 根据用户名获取 OJ 账号

        // 将 UserOJ 实体对象转换为 UserOJDTO
        List<UserOJDTO> ojAccountDTOs = ojAccounts.stream()
                .map(UserOJDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ojAccountDTOs);
    }

    /**
     * 添加一个新的 OJ 账号
     *
     * @param platform OJ 平台
     * @param accountName OJ 账号名称
     * @return 返回成功或失败的 HTTP 状态码
     */
    @PostMapping
    public ResponseEntity<Void> addOJAccount(@RequestParam("platform") String platform,
                                             @RequestParam("accountName") String accountName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();  // 从 SecurityContext 获取用户名
        boolean added = userService.addOJAccount(username, platform, accountName);  // 根据用户名添加 OJ 账号
        if (added) {
            return ResponseEntity.status(201).build();  // 返回 201 Created，表示 OJ 账号添加成功
        }
        return ResponseEntity.badRequest().build();  // 返回 400 Bad Request，表示请求失败
    }

    /**
     * 删除指定用户的 OJ 账号
     * 通过 OJ 平台、账号和用户名来查找 OJ 账号并删除
     *
     * @param platform OJ 平台
     * @param accountName OJ 账号名称
     * @return 删除操作的结果
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteOJAccount(
            @RequestParam("platform") String platform,  // OJ 平台
            @RequestParam("accountName") String accountName) {  // OJ 账号名称

        // 从 SecurityContext 获取当前用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 调用服务层方法删除 OJ 账号
        boolean deleted = userService.deleteOJAccount(username, platform, accountName);

        // 如果删除成功，返回 204 No Content
        if (deleted) {
            return ResponseEntity.noContent().build();
        }

        // 如果找不到符合条件的 OJ 账号，返回 404 Not Found
        return ResponseEntity.notFound().build();
    }
}

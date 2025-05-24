package com.codingtracker.DTO;

import com.codingtracker.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserInfoDTO {
    private Set<User.Type> roles;
    private String name;        // username
    private String realName;
    private String major;
    private String email;
    private String avatar;

    /**
     * OJ账号，按平台分组，账号用分号分隔
     * key=平台名（字符串），value=账号字符串，如"acct1；acct2"
     */
    private Map<String, String> ojAccounts;

    // 从User实体转换到DTO
    public static UserInfoDTO fromUser(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setRoles(user.getRoles());
        dto.setName(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setMajor(user.getMajor());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());

        if (user.getOjAccounts() != null) {
            Map<String, String> ojMap = user.getOjAccounts().stream()
                    .collect(Collectors.groupingBy(
                            oj -> oj.getPlatform().name(),
                            Collectors.mapping(
                                    oj -> oj.getAccountName(),
                                    Collectors.joining("；")
                            )
                    ));
            dto.setOjAccounts(ojMap);
        } else {
            dto.setOjAccounts(Map.of());
        }

        return dto;
    }
}

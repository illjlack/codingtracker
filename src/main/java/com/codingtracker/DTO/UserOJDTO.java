package com.codingtracker.DTO;

import com.codingtracker.model.UserOJ;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserOJDTO {
    private String platform;
    private String username;
    private String accountName;

    // 构造函数
    public UserOJDTO(UserOJ userOJ) {
        this.username = userOJ.getUser().getUsername();
        this.platform = userOJ.getPlatform().toString();
        this.accountName = userOJ.getAccountName();
    }
}

package com.codingtracker.dto;

import com.codingtracker.model.OJPlatform;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    private Integer userId;
    private String username;
    private String realName;      // 新增真实姓名字段
    private Map<OJPlatform, Long> counts = new HashMap<>();

    public UserStatsDTO(Integer userId, String username, String realName) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
    }

    // 添加单个平台计数
    public void addCount(OJPlatform platform, Long count) {
        this.counts.put(platform, count);
    }
}


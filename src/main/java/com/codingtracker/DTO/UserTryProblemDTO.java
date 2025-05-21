package com.codingtracker.DTO;

import com.codingtracker.model.ProblemResult;
import com.codingtracker.model.UserTryProblem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserTryProblemDTO {
    private String username;
    private Long problemId;
    private String result;
    private LocalDateTime attemptTime;

    // 构造函数
    public UserTryProblemDTO(UserTryProblem userTryProblem, String username) {
        this.username = username; // 避免懒加载的加载
        this.problemId = userTryProblem.getExtOjPbInfo().getId(); // 提取 ExtOjPbInfo 实体中的 id
        this.result = userTryProblem.getResult().name(); // 将枚举转换为字符串
        this.attemptTime = userTryProblem.getAttemptTime();
    }
}

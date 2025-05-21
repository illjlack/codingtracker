package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 保存 Codeforces 用户信息的实体类
 */
@Entity
@Table(name = "cf_user_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CFUserInfo {

    @Id
    @Column(name = "cfname")
    private String cfname; // 用户名

    // rating 不冲突，让它自动映射为 rating
    private Integer rating; // 当前评分

    // 映射最高评分列为 max_rating
    @Column(name = "max_rating")
    private Integer maxRating; // 最高评分

    // 重命名属性为 currentRank，映射到 user_rank
    @Column(name = "user_rank")
    private String currentRank; // 当前排名

    // 映射最高排名列为 max_rank
    @Column(name = "max_rank")
    private String maxRank; // 最高排名

    private String avatar;     // 头像链接
    private String titlePhoto; // 头衔图片链接

    private LocalDateTime registrationTime; // 注册时间
    private LocalDateTime lastOnlineTime;   // 最后在线时间
}

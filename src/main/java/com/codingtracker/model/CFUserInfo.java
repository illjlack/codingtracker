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
    private String cfname; // 用户名

    @Column(name = "user_rank")
    private Integer rating; // 当前评分
    private Integer maxRating; // 最高评分
    private String rank; // 当前排名
    private String maxRank; // 最高排名
    private String avatar; // 头像链接
    private String titlePhoto; // 头衔图片链接

    private LocalDateTime registrationTime; // 注册时间
    private LocalDateTime lastOnlineTime; // 最后在线时间
}

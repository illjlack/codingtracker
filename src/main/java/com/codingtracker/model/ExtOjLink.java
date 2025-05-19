package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * 外部 OJ 链接配置实体，保存各平台的接口或页面 URL 模板及登录令牌配置
 */
@Entity
@Table(name = "extoj_link")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtOjLink implements Serializable {
    /** OJ 平台 */
    @Id
    @Enumerated(EnumType.STRING)
    private OJPlatform oj;

    /** OJ 平台首页或题目列表链接模板 */
    @Column(length = 1024)
    private String indexLink;

    /** 获取用户 AC/提交记录的链接模板 */
    @Column(length = 1024)
    private String userInfoLink;

    /** 获取题目状态/统计信息的链接模板 */
    @Column(length = 1024)
    private String pbStatusLink;

    /** 获取题目详情的链接模板 */
    @Column(length = 1024)
    private String problemLink;

    /** 登录 API 链接，用于获取登录令牌 */
    @Column(length = 1024)
    private String loginLink;

    /** 登录后返回的令牌（Token） */
    @Column(length = 2048)
    private String authToken;
}

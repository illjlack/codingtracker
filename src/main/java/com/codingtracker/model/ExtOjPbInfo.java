package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 外部 OJ 题目信息实体，保存题目基础元数据和标签
 */
@Entity
@Table(name = "extoj_pb_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtOjPbInfo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OJ 平台 */
    @Enumerated(EnumType.STRING)
    private OJPlatform ojName;

    /** 在 cf 中比赛 id + 题号 **/
    @Column(length = 10)
    private String pid;

    /** 题目名称 */
    @Column(length = 1024)
    private String name;

    /** 题目类型（PROGRAMMING 等） */
    @Column(length = 64)
    private String type;

    /** 题目分值 */
    private Double points;

    /** 题目链接 URL */
    @Column(length = 256)
    private String url;

    /**
     * 题目标签，通过中间表 problem_tags 关联到 tag 表
     */
    @ManyToMany(fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}

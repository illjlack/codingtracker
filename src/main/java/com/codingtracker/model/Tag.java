package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Set;

/**
 * 题目标签实体
 */
@Entity
@Table(name = "tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 标签名称，例如 "binary search" */
    @Column(unique = true, nullable = false, length = 128)
    private String name;

    /** 反向关联到题目 */
    @ManyToMany(mappedBy = "tags")
    private Set<ExtOjPbInfo> problems;
}

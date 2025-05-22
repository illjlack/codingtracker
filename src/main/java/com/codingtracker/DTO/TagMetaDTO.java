package com.codingtracker.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Luogu 标签元数据 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagMetaDTO {
    /** 标签 ID */
    private int id;
    /** 标签名称 */
    private String name;
    /** 标签类型（对应 types 数组里的 type ID） */
    private int    type;
    /** 父标签 ID，null 表示没有父标签 */
    private Integer parent;
}
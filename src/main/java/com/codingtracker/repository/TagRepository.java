package com.codingtracker.repository;

import com.codingtracker.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 题目标签仓库，提供标签的 CRUD 操作以及根据名称查找接口
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 根据标签名称查找 Tag 实体
     *
     * @param name 标签名称
     * @return 对应的 Tag 实体（若不存在返回 Optional.empty()）
     */
    Optional<Tag> findByName(String name);
}

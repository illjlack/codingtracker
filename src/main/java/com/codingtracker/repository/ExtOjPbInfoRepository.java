package com.codingtracker.repository;

import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.OJPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 外部 OJ 题目信息仓库，
 * 提供对 ExtOjPbInfo 实体的 CRUD 操作及按平台查询方法。
 */
@Repository
public interface ExtOjPbInfoRepository extends JpaRepository<ExtOjPbInfo, Long> {

    /**
     * 按 OJ 平台枚举值查询对应的题目统计信息列表
     */
    List<ExtOjPbInfo> findByOjName(OJPlatform ojName);

    /**
     * 按 OJ 平台 + 题目 ID 查询单个题目信息
     */
    Optional<ExtOjPbInfo> findByOjNameAndPid(OJPlatform ojName, String pid);

    List<ExtOjPbInfo> findAllByOjNameAndPidIn(OJPlatform ojType, Set<String> allPids);
}

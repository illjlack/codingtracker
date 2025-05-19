package com.codingtracker.repository;

import com.codingtracker.model.ExtOjLink;
import com.codingtracker.model.OJPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 外部 OJ 链接配置仓库，
 * 提供根据 OJPlatform 查询 ExtOjLink 的 CRUD 操作。
 */
@Repository
public interface ExtOjLinkRepository extends JpaRepository<ExtOjLink, OJPlatform> {
}

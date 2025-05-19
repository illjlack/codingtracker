package com.codingtracker.repository;

import com.codingtracker.model.SystemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 系统状态仓库，用于持久化和查询 SystemState 实体
 * 主键为日期 (LocalDate)，对应数据库表中的 date 列
 */
@Repository
public interface SystemStateRepository extends JpaRepository<SystemState, LocalDate> {
    // Spring Data JPA 已提供基本的 CRUD 和排序功能，无需额外定义方法
}

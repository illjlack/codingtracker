package com.codingtracker.repository;

import com.codingtracker.model.CFUserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CFUserInfoRepository extends JpaRepository<CFUserInfo, String> {
    // JpaRepository 提供了基本的 CRUD 操作，这里不需要额外定义方法
}

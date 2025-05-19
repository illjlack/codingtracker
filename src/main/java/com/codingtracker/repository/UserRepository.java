package com.codingtracker.repository;

import com.codingtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // 根据用户名查找用户
    Optional<User> findByUsername(String username);

    // 根据真实姓名查找用户
    Optional<User> findByRealName(String realName);

    // 根据用户角色查找用户列表
    List<User> findByRolesContains(User.Type role);

    // 判断用户名是否已存在
    boolean existsByUsername(String username);

    // 判断真实姓名是否已存在
    boolean existsByRealName(String realName);
}

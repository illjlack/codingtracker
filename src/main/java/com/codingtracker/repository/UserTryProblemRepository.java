package com.codingtracker.repository;

import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTryProblemRepository extends JpaRepository<UserTryProblem, Long> {
    List<UserTryProblem> findByUserId(Long userId);
    List<UserTryProblem> findByUser(User user);
    void deleteByUserId(Long userId);
}

package com.codingtracker.repository;

import com.codingtracker.model.ProblemResult;
import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserTryProblemRepository extends JpaRepository<UserTryProblem, Long> {
    List<UserTryProblem> findByUserId(Long userId);
    List<UserTryProblem> findByUser(User user);
    void deleteByUserId(Long userId);

    @Query("SELECT COUNT(u) FROM UserTryProblem u WHERE u.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM UserTryProblem u WHERE u.user.id = :userId AND u.attemptTime BETWEEN :start AND :end")
    long countByUserIdAndAttemptTimeBetween(@Param("userId") Long userId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM UserTryProblem u WHERE u.extOjPbInfo = :platform AND u.user.id = :userId AND u.attemptTime BETWEEN :start AND :end")
    long countByPlatformAndUserIdAndAttemptTimeBetween(@Param("platform") String platform,
                                                       @Param("userId") Long userId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    // 查询某时间段内所有用户每个平台的尝试数量
    @Query("SELECT u.user.id, u.ojName, COUNT(u) " +
            "FROM UserTryProblem u " +
            "WHERE u.attemptTime BETWEEN :start AND :end " +
            "GROUP BY u.user.id, u.ojName")
    List<Object[]> countTryByUserAndPlatformBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 查询某时间段内所有用户每个平台的AC数量
    @Query("SELECT u.user.id, u.ojName, COUNT(u) " +
            "FROM UserTryProblem u " +
            "WHERE u.attemptTime BETWEEN :start AND :end AND u.result = :acResult " +
            "GROUP BY u.user.id, u.ojName")
    List<Object[]> countAcByUserAndPlatformBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("acResult") ProblemResult acResult);
}

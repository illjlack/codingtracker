package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 记录用户尝试的题目及其结果
 */
@Entity
@Table(name = "user_try_problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserTryProblem implements Serializable,Comparable<UserTryProblem>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 关联的题目
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private ExtOjPbInfo extOjPbInfo;

    /**
     * 记录用户尝试该题目的结果
     * 比如 Accepted, Wrong Answer, Time Limit Exceeded 等
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemResult result;

    /**
     * 尝试时间
     */
    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;


    @Override
    public int compareTo(UserTryProblem other) {
        int userComparison = this.getUser().getId().compareTo(other.getUser().getId());
        if (userComparison != 0) {
            return userComparison;
        }
        int problemComparison = this.getExtOjPbInfo().getId().compareTo(other.getExtOjPbInfo().getId());
        if (problemComparison != 0) {
            return problemComparison;
        }
        int timeComparison = this.getAttemptTime().compareTo(other.getAttemptTime());
        if (timeComparison != 0) {
            return timeComparison;
        }
        return this.getResult().compareTo(other.getResult());
    }
}

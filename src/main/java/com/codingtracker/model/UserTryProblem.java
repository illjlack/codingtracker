package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 记录用户尝试的题目及其结果
 */
@Entity
@Table(
        name = "user_try_problem",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_user_id_attempt_time", columnList = "user_id, attempt_time"),
                @Index(name = "idx_platform_user_time", columnList = "oj_name, user_id, attempt_time"),
                @Index(name = "idx_ac_platform_user_time_result", columnList = "oj_name, user_id, attempt_time, result")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_problem_time", columnNames = {"user_id", "problem_id", "attempt_time", "result"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserTryProblem implements Serializable, Comparable<UserTryProblem> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "utp_seq")
    @SequenceGenerator(name = "utp_seq", sequenceName = "utp_seq", allocationSize = 50)
    private Long id;

    /** 关联的用户 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 关联的题目 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private ExtOjPbInfo extOjPbInfo;

    /** oj名 */
    @Enumerated(EnumType.STRING)
    private OJPlatform ojName;

    /** 记录用户尝试该题目的结果 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemResult result;

    /** 尝试时间 */
    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserTryProblem)) return false;
        UserTryProblem that = (UserTryProblem) o;
        return Objects.equals(user.getId(), that.user.getId()) &&
                Objects.equals(extOjPbInfo.getId(), that.extOjPbInfo.getId()) &&
                Objects.equals(attemptTime, that.attemptTime) &&
                result == that.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                user.getId(),
                extOjPbInfo.getId(),
                attemptTime,
                result
        );
    }

    @Override
    public int compareTo(UserTryProblem other) {
        int c = this.user.getId().compareTo(other.user.getId());
        if (c != 0) return c;
        c = this.extOjPbInfo.getId().compareTo(other.extOjPbInfo.getId());
        if (c != 0) return c;
        c = this.attemptTime.compareTo(other.attemptTime);
        if (c != 0) return c;
        return this.result.compareTo(other.result);
    }
}

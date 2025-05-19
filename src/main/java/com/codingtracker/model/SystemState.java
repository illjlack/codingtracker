package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 系统状态实体类，用于记录每日系统用户数、AC 总数及竞赛总数
 */
@Entity
@Table(name = "system_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemState implements Serializable, Comparable<SystemState> {

    /** 记录日期（主键） */
    @Id
    private LocalDate date;

    /** 注册用户总数 */
    private Long userCount;

    /** 累计做过题目总数 */
    private Long sumProblemCount;

    /** 累计做题记录 */
    private Long sumTryCount;

    @Override
    public int compareTo(SystemState other) {
        return new CompareToBuilder()
                .append(this.date, other.date)
                .toComparison();
    }
}

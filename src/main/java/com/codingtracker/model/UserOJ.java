package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户 OJ 账户类
 */
@Entity
@Table(name = "user_oj",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "platform", "account_name"}))  // 添加联合唯一约束
@Getter
@Setter
@NoArgsConstructor
@ToString
public class UserOJ implements Comparable<UserOJ>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 关联的 User
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OJ 平台
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OJPlatform platform;

    /**
     * 用户在 OJ 平台上的账号
     */
    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Override
    public int compareTo(UserOJ other) {
        int c = Integer.compare(this.user.getId(), other.user.getId());
        if (c != 0) return c;

        // 比较 platform 枚举，枚举默认实现 Comparable，可以直接比
        c = this.platform.compareTo(other.platform);
        if (c != 0) return c;

        // 比较账号名字符串
        if (this.accountName == null && other.accountName == null) return 0;
        if (this.accountName == null) return -1;
        if (other.accountName == null) return 1;
        return this.accountName.compareTo(other.accountName);
    }
}


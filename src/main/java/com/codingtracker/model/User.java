package com.codingtracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户实体类
 */
@Entity
@Table(name = "User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User implements Serializable, Comparable<User> {

    /**
     * 用户类型枚举
     */
    @Getter
    public enum Type {
        RETIRED("退役"),
        QUIT("退出"),
        ACMER("正式队员"),
        REJECT("拒绝"),
        VERIFYING("待审核"),
        NEW("新人"),
        COACH("教练"),
        ADMIN("管理员");

        private final String shortStr;

        Type(String shortStr) {
            this.shortStr = shortStr;
        }
    }

    /**
     * 主键ID，自动递增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 用户名，必须唯一且不能为空
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 用户密码，必须填写，不会被序列化 (避免暴露在 API 响应中)
     */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 专业 (Major)，可为空
     */
    private String major;

    private String email;
    /**
     * 博客地址，最多支持 1024 个字符
     */
    @Column(length = 1024)
    private String blogUrl;

    /**
     * 最后一次尝试的时间
     */
    @Column(name = "last_attempt_time")
    private LocalDateTime lastTryDate;

    /**
     * 用户身份（支持多个角色）
     * 使用 `@ElementCollection` 存储多个身份类型，映射为一张表
     */
    @ElementCollection(targetClass = Type.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Type> roles = new HashSet<>();

    /**
     * 用户 OJ 账户列表 (可以有多个)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserOJ> ojAccounts;

    /**
     * 判断用户是否是管理员或教练
     */
    public boolean isAdmin() {
        return roles.contains(Type.ADMIN) || roles.contains(Type.COACH);
    }

    /**
     * 判断用户是否是正式队员
     */
    public boolean isACMer() {
        return roles.contains(Type.ACMER);
    }

    /**
     * 用户比较方法（按照 ID 排序）
     */
    @Override
    public int compareTo(User o) {
        return new CompareToBuilder()
                .append(this.id, o.id)
                .toComparison();
    }
}

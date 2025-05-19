package com.codingtracker.model;

import org.aspectj.apache.bcel.classfile.Unknown;

import java.util.Arrays;
import java.util.List;
/**
 * OJ (Online Judge) 平台类型枚举
 * 关联对应的 JSON 文件中的 key，方便从 JSON 反序列化时匹配
 */
public enum OJPlatform {

    CODEFORCES("codeforces", "cf", "codeforces.com"),      // Codeforces 平台
    VIRTUAL_JUDGE("vjudge", "vjudge.com"),                   // Virtual Judge 平台
    BEE_CROWD("beecrowd", "uri", "beecrowd.com"),            // Beecrowd 平台（原 URI OJ）
    HDU("hdu", "hdu.ac.cn"),                                // 杭电 OJ (HDU)
    POJ("poj", "poj.org"),                                  // 北京大学 OJ (POJ)
    LEETCODE("leetcode", "leetcode.cn"),                    // 力扣中国站 (leetcode.cn)
    LUOGU("luogu", "luogu.org"),                            // 洛谷平台
    ATCODER("atcoder", "atcoder.jp"),                       // AtCoder 平台（日本）
    CODECHEF("codechef", "codechef.com"),                  // CodeChef 平台
    TOPCODER("topcoder", "topcoder.com"),                   // TopCoder 平台
    SPOJ("spoj", "spoj.com"),                               // SPOJ 平台
    HACKERRANK("hackerrank", "hackerrank.com"),             // HackerRank 平台
    HACKEREARTH("hackerearth", "hackerearth.com"),         // HackerEarth 平台
    CSES("cses", "cses.fi"),                                // CSES Problem Set
    KATTIS("kattis", "kattis.com"),                         // Kattis 平台
    GYM("gym", "codeforces.com/gym"),                       // Codeforces Gym
    NOWCODER("nowcoder", "nowcoder.com"),                   // 牛客网 (Nowcoder)
    UVA("uva", "uva.onlinejudge.org"),                      // UVA Online Judge
    UNKNOWN("unknown");                                     // Unknown Platform

    private final List<String> names;  // 用于存储多个名字（别名）

    // 构造函数，将多个名字（别名）传入
    OJPlatform(String... names) {
        this.names = Arrays.asList(names);  // 将所有名称存入列表
    }

    // 获取所有别名
    public List<String> getNames() {
        return names;
    }

    // 根据名字获取枚举值
    public static OJPlatform fromName(String name) {
        for (OJPlatform platform : OJPlatform.values()) {
            if (platform.getNames().contains(name.toLowerCase())) {
                return platform;
            }
        }
        return UNKNOWN;  // 如果没有匹配的名称，返回 UNKNOWN
    }
}

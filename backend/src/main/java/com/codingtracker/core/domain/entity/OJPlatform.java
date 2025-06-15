package com.codingtracker.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * OJ (Online Judge) 平台类型枚举
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Getter
public enum OJPlatform {

    CODEFORCES("Codeforces", "codeforces", "cf", "codeforces.com"),
    VIRTUAL_JUDGE("Virtual Judge", "vjudge", "vjudge.com"),
    BEE_CROWD("Beecrowd", "beecrowd", "uri", "beecrowd.com"),
    HDU("HDU Online Judge", "hdu", "hdu.ac.cn"),
    POJ("Peking University Online Judge", "poj", "poj.org"),
    LEETCODE("LeetCode", "leetcode", "leetcode.cn"),
    LUOGU("洛谷", "luogu", "luogu.org"),
    ATCODER("AtCoder", "atcoder", "atcoder.jp"),
    CODECHEF("CodeChef", "codechef", "codechef.com"),
    TOPCODER("TopCoder", "topcoder", "topcoder.com"),
    SPOJ("SPOJ", "spoj", "spoj.com"),
    HACKERRANK("HackerRank", "hackerrank", "hackerrank.com"),
    HACKEREARTH("HackerEarth", "hackerearth", "hackerearth.com"),
    CSES("CSES Problem Set", "cses", "cses.fi"),
    KATTIS("Kattis", "kattis", "kattis.com"),
    GYM("Codeforces Gym", "gym", "codeforces.com/gym"),
    NOWCODER("牛客网", "nowcoder", "nowcoder.com"),
    UVA("UVA Online Judge", "uva", "uva.onlinejudge.org"),
    UNKNOWN("Unknown Platform", "unknown");

    private final String displayName;
    private final List<String> aliases;

    OJPlatform(String displayName, String... aliases) {
        this.displayName = displayName;
        this.aliases = Arrays.asList(aliases);
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取所有别名
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * 获取主要标识符（第一个别名）
     */
    public String getPrimaryIdentifier() {
        return aliases.isEmpty() ? name().toLowerCase() : aliases.get(0);
    }

    /**
     * JSON序列化时使用主要标识符
     */
    @JsonValue
    public String toValue() {
        return getPrimaryIdentifier();
    }

    /**
     * 根据名字获取枚举值（支持枚举名、别名、显示名）
     */
    public static OJPlatform fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return UNKNOWN;
        }

        String normalizedName = name.trim();

        // 首先尝试直接匹配枚举名称
        try {
            return OJPlatform.valueOf(normalizedName.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // 继续尝试其他匹配方式
        }

        // 尝试匹配别名（不区分大小写）
        return Stream.of(OJPlatform.values())
                .filter(platform -> platform.getAliases().stream()
                        .anyMatch(alias -> alias.equalsIgnoreCase(normalizedName)))
                .findFirst()
                .orElseGet(() ->
                // 最后尝试匹配显示名称
                Stream.of(OJPlatform.values())
                        .filter(platform -> platform.getDisplayName().equalsIgnoreCase(normalizedName))
                        .findFirst()
                        .orElse(UNKNOWN));
    }

    /**
     * 根据JSON值获取枚举
     */
    public static OJPlatform fromValue(String value) {
        return fromName(value);
    }

    /**
     * 检查是否为已知平台
     */
    public boolean isKnown() {
        return this != UNKNOWN;
    }

    /**
     * 检查是否为竞赛类平台
     */
    public boolean isCompetitivePlatform() {
        return this == CODEFORCES || this == ATCODER || this == CODECHEF ||
                this == TOPCODER || this == GYM;
    }

    /**
     * 检查是否为练习类平台
     */
    public boolean isPracticePlatform() {
        return this == LEETCODE || this == HACKERRANK || this == HACKEREARTH ||
                this == HDU || this == POJ || this == UVA || this == SPOJ;
    }

    /**
     * 检查是否为中文平台
     */
    public boolean isChinesePlatform() {
        return this == LUOGU || this == NOWCODER || this == LEETCODE;
    }
}
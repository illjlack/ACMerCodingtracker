package com.codingtracker.dto;

import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.UserTag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    private Integer userId;
    private String username;
    private String realName; // 新增真实姓名字段
    private Map<OJPlatform, Long> counts = new HashMap<>();
    private Set<UserTag> tags = new HashSet<>(); // 用户标签

    public UserStatsDTO(Integer userId, String username, String realName) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.tags = new HashSet<>();
    }

    // 添加单个平台计数
    public void addCount(OJPlatform platform, Long count) {
        this.counts.put(platform, count);
    }

    // 设置用户标签
    public void setTags(Set<UserTag> tags) {
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
    }
}

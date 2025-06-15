package com.codingtracker.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ProblemTagRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 多题目多标签批量插入关联，自动去重已有关联
     * @param problemTagsMap  key = problemId，value = 该题目的标签ID集合
     */
    @Transactional
    public void batchInsertProblemTags(Map<Long, Set<Long>> problemTagsMap) {
        if (problemTagsMap == null || problemTagsMap.isEmpty()) {
            return;
        }

        // 先批量查询数据库中已有的所有对应关系，避免逐条查询
        // 拼接所有 problemId 条件
        List<Long> problemIds = new ArrayList<>(problemTagsMap.keySet());

        // 查询已有的 problem_tag 关系
        String sql = String.format(
                "SELECT problem_id, tag_id FROM problem_tags WHERE problem_id IN (%s)",
                problemIds.stream().map(id -> "?").collect(Collectors.joining(", "))
        );

        List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(sql, problemIds.toArray());

        // 构造 Map<problemId, Set<tagId>> 表示已有关系
        Map<Long, Set<Long>> existingMap = new HashMap<>();
        for (Map<String, Object> row : existingRows) {
            Long pid = ((Number) row.get("problem_id")).longValue();
            Long tid = ((Number) row.get("tag_id")).longValue();
            existingMap.computeIfAbsent(pid, k -> new HashSet<>()).add(tid);
        }

        // 准备要插入的( problem_id, tag_id )列表，排除已有的关系
        List<Object[]> batchArgs = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : problemTagsMap.entrySet()) {
            Long pid = entry.getKey();
            Set<Long> tagIds = entry.getValue();

            Set<Long> existTagIds = existingMap.getOrDefault(pid, Collections.emptySet());

            for (Long tid : tagIds) {
                if (!existTagIds.contains(tid)) {
                    batchArgs.add(new Object[]{pid, tid});
                }
            }
        }

        if (batchArgs.isEmpty()) {
            return; // 全部已有，无需插入
        }

        // 批量插入
        String insertSql = "INSERT INTO problem_tags (problem_id, tag_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

}

package com.codingtracker.service;

import com.codingtracker.model.User;
import com.codingtracker.model.UserTag;
import com.codingtracker.repository.UserRepository;
import com.codingtracker.repository.UserTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * 用户标签业务逻辑层
 */
@Service
public class UserTagService {

    private final UserTagRepository userTagRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserTagService(UserTagRepository userTagRepository, UserRepository userRepository) {
        this.userTagRepository = userTagRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取所有标签
     */
    public List<UserTag> getAllTags() {
        return userTagRepository.findAllByOrderByNameAsc();
    }

    /**
     * 根据ID获取标签
     */
    public Optional<UserTag> getTagById(Long id) {
        return userTagRepository.findById(id);
    }

    /**
     * 根据名称获取标签
     */
    public Optional<UserTag> getTagByName(String name) {
        return userTagRepository.findByName(name);
    }

    /**
     * 创建新标签
     */
    @Transactional
    public UserTag createTag(UserTag tag) {
        if (!StringUtils.hasText(tag.getName())) {
            throw new RuntimeException("标签名称不能为空");
        }

        if (userTagRepository.existsByName(tag.getName())) {
            throw new RuntimeException("标签名称已存在");
        }

        // 设置默认颜色
        if (!StringUtils.hasText(tag.getColor())) {
            tag.setColor("#409EFF");
        }

        return userTagRepository.save(tag);
    }

    /**
     * 更新标签
     */
    @Transactional
    public UserTag updateTag(Long id, UserTag tag) {
        UserTag existingTag = userTagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        if (StringUtils.hasText(tag.getName())) {
            // 检查名称是否已被其他标签使用
            Optional<UserTag> tagWithSameName = userTagRepository.findByName(tag.getName());
            if (tagWithSameName.isPresent() && !tagWithSameName.get().getId().equals(id)) {
                throw new RuntimeException("标签名称已存在");
            }
            existingTag.setName(tag.getName());
        }

        if (StringUtils.hasText(tag.getColor())) {
            existingTag.setColor(tag.getColor());
        }

        if (tag.getDescription() != null) {
            existingTag.setDescription(tag.getDescription());
        }

        return userTagRepository.save(existingTag);
    }

    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Long id) {
        UserTag tag = userTagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 先从所有用户中移除这个标签
        for (User user : tag.getUsers()) {
            user.getTags().remove(tag);
            userRepository.save(user);
        }

        userTagRepository.delete(tag);
    }

    /**
     * 为用户添加标签
     */
    @Transactional
    public void addTagToUser(Integer userId, Long tagId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserTag tag = userTagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        user.getTags().add(tag);
        userRepository.save(user);
    }

    /**
     * 从用户移除标签
     */
    @Transactional
    public void removeTagFromUser(Integer userId, Long tagId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserTag tag = userTagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        user.getTags().remove(tag);
        userRepository.save(user);
    }

    /**
     * 设置用户的所有标签
     */
    @Transactional
    public void setUserTags(Integer userId, Set<Long> tagIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证标签ID是否有效
        for (Long tagId : tagIds) {
            if (!userTagRepository.existsById(tagId)) {
                throw new RuntimeException("标签不存在: " + tagId);
            }
        }

        // 清除现有标签并强制刷新
        user.getTags().clear();
        userRepository.flush();

        // 批量获取所有标签，避免N+1查询
        List<UserTag> tagsToAdd = userTagRepository.findAllById(tagIds);

        // 验证获取到的标签数量是否匹配
        if (tagsToAdd.size() != tagIds.size()) {
            throw new RuntimeException("部分标签不存在");
        }

        // 添加新标签
        user.getTags().addAll(tagsToAdd);

        userRepository.save(user);
    }

    /**
     * 根据标签名称搜索标签
     */
    public List<UserTag> searchTagsByName(String name) {
        if (!StringUtils.hasText(name)) {
            return getAllTags();
        }
        return userTagRepository.findByNameContaining(name);
    }

    /**
     * 获取标签的使用统计
     */
    public long getTagUsageCount(Long tagId) {
        return userTagRepository.countUsersByTagId(tagId);
    }
}
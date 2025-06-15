package com.codingtracker.core.service;

import com.codingtracker.core.domain.entity.OJPlatform;
import com.codingtracker.core.domain.entity.UserOJ;
import com.codingtracker.shared.dto.request.OJAccountRequest;
import com.codingtracker.shared.dto.response.UserOJResponse;

import java.util.List;

/**
 * 用户OJ账号服务接口
 * 专门处理用户在各个OJ平台的账号管理
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
public interface UserOJService {

    /**
     * 添加OJ账号
     */
    UserOJResponse addOJAccount(String username, OJAccountRequest request);

    /**
     * 删除OJ账号
     */
    void deleteOJAccount(String username, OJPlatform platform, String accountName);

    /**
     * 更新OJ账号
     */
    UserOJResponse updateOJAccount(String username, Integer ojAccountId, OJAccountRequest request);

    /**
     * 获取用户的所有OJ账号
     */
    List<UserOJResponse> getUserOJAccounts(String username);

    /**
     * 获取用户在指定平台的账号
     */
    List<UserOJResponse> getUserOJAccountsByPlatform(String username, OJPlatform platform);

    /**
     * 验证OJ账号是否有效
     */
    boolean validateOJAccount(OJPlatform platform, String accountName);

    /**
     * 同步OJ账号数据
     */
    void syncOJAccountData(String username, OJPlatform platform, String accountName);

    /**
     * 批量同步用户的所有OJ账号数据
     */
    void syncAllOJAccounts(String username);

    /**
     * 激活OJ账号
     */
    void activateOJAccount(Integer ojAccountId);

    /**
     * 停用OJ账号
     */
    void deactivateOJAccount(Integer ojAccountId);

    /**
     * 检查用户是否拥有指定平台的账号
     */
    boolean hasOJAccount(String username, OJPlatform platform);

    /**
     * 获取指定平台的所有用户账号
     */
    List<UserOJResponse> getAllAccountsByPlatform(OJPlatform platform);

    /**
     * 获取用户OJ账号统计信息
     */
    long getUserOJAccountCount(String username);

    /**
     * 获取平台账号统计信息
     */
    long getPlatformAccountCount(OJPlatform platform);
}
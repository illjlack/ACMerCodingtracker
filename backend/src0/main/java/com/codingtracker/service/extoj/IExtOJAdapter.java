package com.codingtracker.service.extoj;

import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.ExtOjLink;
import com.codingtracker.model.OJPlatform;

import java.util.List;
import java.util.Map;

/**
 * 外部 OJ 适配器接口，定义从各 OJ 平台抓取数据的统一方法
 */
public interface IExtOJAdapter {

    /**
     * 在线获取某用户在本 OJ 平台的所有尝试记录（含 AC、WA 等）
     *
     * @param user 用户实体
     * @return 用户尝试记录列表
     */
    List<UserTryProblem> getUserTriesOnline(User user);

    /**
     * 在线获取本 OJ 平台所有题目的统计信息
     *
     * @return 题目统计信息列表
     */
    List<ExtOjPbInfo> getAllPbInfoOnline();

    /**
     * 获取本 OJ 平台的链接配置，包含用户记录和题目统计页链接
     *
     * @return 平台链接对象
     */
    ExtOjLink getOjLink();

    /**
     * 获取本 OJ 平台的枚举类型
     *
     * @return OJ 平台枚举
     */
    OJPlatform getOjType();

    /**
     * 验证当前平台的认证token是否有效
     *
     * @return 验证结果，包含验证状态和错误信息
     */
    TokenValidationResult validateToken();

    /**
     * 检查当前平台是否需要Token认证
     *
     * @return true表示需要Token认证，false表示不需要
     */
    boolean requiresToken();

    /**
     * 获取当前平台的Token格式说明
     *
     * @return Token格式说明字符串
     */
    String getTokenFormat();

    /**
     * 解析Token字符串为Cookie映射
     *
     * @param tokenString Token字符串
     * @return Cookie键值对映射
     */
    Map<String, String> parseToken(String tokenString);

    /**
     * 验证Token格式是否正确
     *
     * @param tokenString Token字符串
     * @return 格式验证结果
     */
    TokenFormatValidationResult validateTokenFormat(String tokenString);

    /**
     * Token验证结果类
     */
    class TokenValidationResult {
        private final boolean valid;
        private final String message;
        private final String errorCode;

        public TokenValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
            this.errorCode = valid ? null : "TOKEN_EXPIRED";
        }

        public TokenValidationResult(boolean valid, String message, String errorCode) {
            this.valid = valid;
            this.message = message;
            this.errorCode = errorCode;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Token格式验证结果类
     */
    class TokenFormatValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> requiredFields;
        private final List<String> missingFields;

        public TokenFormatValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
            this.requiredFields = List.of();
            this.missingFields = List.of();
        }

        public TokenFormatValidationResult(boolean valid, String message, List<String> requiredFields,
                List<String> missingFields) {
            this.valid = valid;
            this.message = message;
            this.requiredFields = requiredFields != null ? requiredFields : List.of();
            this.missingFields = missingFields != null ? missingFields : List.of();
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getRequiredFields() {
            return requiredFields;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }
    }
}

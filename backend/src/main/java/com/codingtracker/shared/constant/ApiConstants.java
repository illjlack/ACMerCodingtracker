package com.codingtracker.shared.constant;

/**
 * API常量类
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
public class ApiConstants {

    // ==================== API版本 ====================
    public static final String API_VERSION_V1 = "/api/v1";

    // ==================== 认证相关 ====================
    public static final String AUTH_BASE_PATH = API_VERSION_V1 + "/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_REFRESH_TOKEN = "/refresh-token";
    public static final String AUTH_CHANGE_PASSWORD = "/change-password";
    public static final String AUTH_RESET_PASSWORD = "/reset-password";
    public static final String AUTH_VALIDATE_TOKEN = "/validate";
    public static final String AUTH_CHECK_USERNAME = "/check-username";
    public static final String AUTH_CURRENT_USER = "/me";

    // ==================== 用户相关 ====================
    public static final String USER_BASE_PATH = API_VERSION_V1 + "/users";
    public static final String USER_PROFILE = "/profile";
    public static final String USER_AVATAR = "/avatar";
    public static final String USER_OJ_ACCOUNTS = "/oj-accounts";
    public static final String USER_TAGS = "/tags";
    public static final String USER_SEARCH = "/search";
    public static final String USER_BATCH = "/batch";

    // ==================== 管理员相关 ====================
    public static final String ADMIN_BASE_PATH = API_VERSION_V1 + "/admin";
    public static final String ADMIN_USERS = "/users";
    public static final String ADMIN_STATS = "/stats";
    public static final String ADMIN_SYSTEM = "/system";

    // ==================== 文件相关 ====================
    public static final String FILE_BASE_PATH = "/api/files";
    public static final String FILE_AVATARS = "/avatars";
    public static final String FILE_UPLOAD = "/upload";
    public static final String FILE_DOWNLOAD = "/download";

    // ==================== HTTP状态码相关 ====================
    public static final int SUCCESS_CODE = 200;
    public static final int CREATED_CODE = 201;
    public static final int BAD_REQUEST_CODE = 400;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int NOT_FOUND_CODE = 404;
    public static final int CONFLICT_CODE = 409;
    public static final int INTERNAL_SERVER_ERROR_CODE = 500;

    // ==================== 响应消息 ====================
    public static final String SUCCESS_MESSAGE = "操作成功";
    public static final String CREATED_MESSAGE = "创建成功";
    public static final String UPDATED_MESSAGE = "更新成功";
    public static final String DELETED_MESSAGE = "删除成功";
    public static final String BAD_REQUEST_MESSAGE = "请求参数错误";
    public static final String UNAUTHORIZED_MESSAGE = "未认证或认证失败";
    public static final String FORBIDDEN_MESSAGE = "权限不足";
    public static final String NOT_FOUND_MESSAGE = "资源不存在";
    public static final String CONFLICT_MESSAGE = "资源冲突";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "服务器内部错误";

    // ==================== 分页相关 ====================
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final String DEFAULT_SORT_DIRECTION = "desc";
    public static final String DEFAULT_SORT_PROPERTY = "createdAt";

    // ==================== 验证相关 ====================
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 50;
    public static final int MIN_REAL_NAME_LENGTH = 2;
    public static final int MAX_REAL_NAME_LENGTH = 20;
    public static final int MAX_EMAIL_LENGTH = 100;
    public static final int MAX_MAJOR_LENGTH = 50;
    public static final int MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    public static final int MAX_OJ_ACCOUNT_NAME_LENGTH = 50;
    public static final int MAX_TAG_NAME_LENGTH = 20;

    // ==================== JWT相关 ====================
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000L; // 24小时
    public static final long JWT_REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7天

    // ==================== 缓存相关 ====================
    public static final String CACHE_USER_PREFIX = "user:";
    public static final String CACHE_TOKEN_PREFIX = "token:";
    public static final String CACHE_BLACKLIST_PREFIX = "blacklist:";
    public static final long CACHE_USER_TTL = 30 * 60; // 30分钟
    public static final long CACHE_TOKEN_TTL = 24 * 60 * 60; // 24小时

    // ==================== 业务规则 ====================
    public static final int MAX_OJ_ACCOUNTS_PER_USER = 10;
    public static final int MAX_TAGS_PER_USER = 20;
    public static final int MAX_SEARCH_RESULTS = 100;
    public static final int MIN_SEARCH_KEYWORD_LENGTH = 2;
    public static final int MAX_SEARCH_KEYWORD_LENGTH = 50;

    // ==================== 文件类型 ====================
    public static final String[] ALLOWED_IMAGE_TYPES = { "image/jpeg", "image/png", "image/gif" };
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = { "jpg", "jpeg", "png", "gif" };

    // ==================== 正则表达式 ====================
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]+$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    // ==================== 默认值 ====================
    public static final String DEFAULT_AVATAR = "/api/files/avatars/default-avatar.png";
    public static final String DEFAULT_TAG_COLOR = "#409EFF";
    public static final String DEFAULT_LOCALE = "zh_CN";
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    // ==================== 系统配置 ====================
    public static final String SYSTEM_NAME = "CodingTracker";
    public static final String SYSTEM_VERSION = "2.0.0";
    public static final String SYSTEM_DESCRIPTION = "编程练习跟踪系统";
    public static final String SYSTEM_AUTHOR = "CodingTracker Team";

    // 私有构造函数，防止实例化
    private ApiConstants() {
        throw new IllegalStateException("Utility class");
    }
}
# 共享层架构说明

## 📋 概述

共享层包含系统中被多个层次共同使用的组件，包括数据传输对象(DTO)、异常定义、工具类和常量。该层为整个系统提供统一的数据模型和通用功能。

## 🏗️ 架构设计

### 目录结构
```
shared/
├── dto/                   # 数据传输对象
│   ├── request/          # 请求DTO
│   │   ├── UserCreateRequest.java      # 用户创建请求
│   │   ├── UserUpdateRequest.java      # 用户更新请求
│   │   ├── LoginRequest.java           # 登录请求
│   │   ├── RegisterRequest.java        # 注册请求
│   │   ├── PasswordChangeRequest.java  # 密码修改请求
│   │   └── OJAccountRequest.java       # OJ账号请求
│   └── response/         # 响应DTO
│       ├── ApiResponse.java            # 统一API响应
│       ├── UserResponse.java           # 用户响应
│       ├── AuthResponse.java           # 认证响应
│       ├── UserOJResponse.java         # OJ账号响应
│       └── UserTagResponse.java        # 标签响应
├── exception/            # 异常定义
│   ├── BusinessException.java          # 业务异常基类
│   ├── ValidationException.java        # 验证异常
│   ├── UserNotFoundException.java      # 用户不存在异常
│   └── GlobalExceptionHandler.java     # 全局异常处理器
├── util/                 # 工具类
│   └── DateUtils.java               # 日期工具类
├── constant/             # 常量定义
│   └── ApiConstants.java            # API常量
└── README.md            # 本文档
```

### 设计原则

1. **数据一致性**：统一的数据模型和格式
2. **类型安全**：强类型定义避免运行时错误
3. **可复用性**：通用组件可在多处使用
4. **标准化**：统一的异常处理和响应格式
5. **文档化**：清晰的注释和示例

## 📄 数据传输对象(DTO)

### 设计模式
- **请求DTO**：封装客户端请求数据
- **响应DTO**：封装服务端响应数据
- **分层隔离**：避免直接暴露实体类
- **数据验证**：集成Bean Validation

### 1. 请求DTO (Request)

#### UserCreateRequest - 用户创建请求
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度应在3-20字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度应在8-50字符之间")
    private String password;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 20, message = "真实姓名长度应在2-20字符之间")
    private String realName;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    private String email;
    
    @Size(max = 50, message = "专业长度不能超过50字符")
    private String major;
    
    private Set<User.Type> roles;
    private List<OJAccountRequest> ojAccounts;
    private List<String> tags;
    
    // 业务验证方法
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
```

#### LoginRequest - 登录请求
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private Boolean rememberMe = false;
}
```

### 2. 响应DTO (Response)

#### ApiResponse - 统一API响应
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;        // 操作是否成功
    private int code;              // HTTP状态码
    private String message;        // 响应消息
    private T data;               // 响应数据
    private String timestamp;     // 时间戳
    private String path;          // 请求路径
    
    // 成功响应静态方法
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "操作成功");
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
    
    // 失败响应静态方法
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
```

#### UserResponse - 用户响应
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Integer id;
    private String username;
    private String realName;
    private String major;
    private String email;
    private String avatar;
    private boolean active;
    private LocalDateTime lastTryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<User.Type> roles;
    private List<UserOJResponse> ojAccounts;
    private List<UserTagResponse> tags;
    
    // 辅助方法
    public boolean isAdmin() {
        return roles != null && roles.contains(User.Type.ADMIN);
    }
    
    public boolean isSuperAdmin() {
        return roles != null && roles.contains(User.Type.SUPER_ADMIN);
    }
}
```

## ⚠️ 异常处理

### 异常层次结构
```
Exception
└── RuntimeException
    └── BusinessException (业务异常基类)
        ├── ValidationException (验证异常)
        ├── UserNotFoundException (用户不存在异常)
        ├── AuthenticationException (认证异常)
        └── AuthorizationException (授权异常)
```

### 1. BusinessException - 业务异常基类
```java
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final String message;
    private final Object[] args;
    
    public BusinessException(String message) {
        this(500, message);
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.args = null;
    }
    
    public BusinessException(int code, String message, Object... args) {
        super(message);
        this.code = code;
        this.message = message;
        this.args = args;
    }
}
```

### 2. ValidationException - 验证异常
```java
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(400, message);
    }
    
    // 静态工厂方法
    public static ValidationException required(String field) {
        return new ValidationException(field + "不能为空");
    }
    
    public static ValidationException invalid(String field) {
        return new ValidationException(field + "格式不正确");
    }
    
    public static ValidationException duplicate(String field, String value) {
        return new ValidationException(field + " '" + value + "' 已存在");
    }
    
    public static ValidationException passwordMismatch() {
        return new ValidationException("两次输入的密码不一致");
    }
}
```

### 3. GlobalExceptionHandler - 全局异常处理器
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity
                .status(e.getCode())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        
        log.warn("参数验证失败: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, "参数验证失败"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.error(500, "系统内部错误"));
    }
}
```

## 🛠️ 工具类

### DateUtils - 日期工具类
**功能特性**：
- 日期格式化和解析
- 日期计算和比较
- 时区处理
- 常用日期格式常量

**使用示例**：
```java
// 获取当前日期字符串
String today = DateUtils.getCurrentDate(); // "2024-01-01"

// 格式化日期时间
String dateTimeStr = DateUtils.formatDateTime(LocalDateTime.now());

// 计算日期差
long days = DateUtils.daysBetween(startDate, endDate);

// 判断是否为今天
boolean isToday = DateUtils.isToday(someDate);

// 获取本月开始和结束日期
LocalDate monthStart = DateUtils.getStartOfMonth();
LocalDate monthEnd = DateUtils.getEndOfMonth();
```

## 📊 常量定义

### ApiConstants - API常量
**包含内容**：
- API路径常量
- HTTP状态码
- 响应消息
- 验证规则
- 业务规则
- 系统配置

**使用示例**：
```java
// API路径
public static final String USER_BASE_PATH = "/api/v1/users";
public static final String AUTH_LOGIN = "/login";

// 状态码
public static final int SUCCESS_CODE = 200;
public static final int UNAUTHORIZED_CODE = 401;

// 验证规则
public static final int MIN_USERNAME_LENGTH = 3;
public static final int MAX_USERNAME_LENGTH = 20;
public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]+$";

// 业务规则
public static final int MAX_OJ_ACCOUNTS_PER_USER = 10;
public static final int MAX_TAGS_PER_USER = 20;
```

## 🔍 数据验证

### Bean Validation注解
```java
public class ExampleRequest {
    
    @NotNull(message = "ID不能为空")
    @Positive(message = "ID必须为正数")
    private Integer id;
    
    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 50, message = "名称长度应在2-50字符之间")
    private String name;
    
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;
    
    @Valid  // 级联验证
    private List<ChildRequest> children;
}
```

### 自定义验证注解
```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UsernameValidator.class)
public @interface ValidUsername {
    String message() default "用户名格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {
    
    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null) return false;
        
        // 自定义验证逻辑
        return username.length() >= 3 && 
               username.length() <= 20 && 
               username.matches("^[a-zA-Z0-9_]+$");
    }
}
```

## 📈 最佳实践

### 1. DTO设计
- **不可变性**：尽量使用不可变对象
- **验证完整**：添加完整的验证注解
- **文档齐全**：提供清晰的字段说明
- **版本兼容**：考虑向后兼容性

### 2. 异常处理
- **层次清晰**：建立清晰的异常层次
- **信息丰富**：提供详细的错误信息
- **安全考虑**：避免泄露敏感信息
- **统一处理**：使用全局异常处理器

### 3. 工具类设计
- **无状态**：工具类方法应该是无状态的
- **线程安全**：确保多线程环境下的安全性
- **性能优化**：避免重复计算和对象创建
- **测试完整**：提供完整的单元测试

### 4. 常量管理
- **分类清晰**：按功能分组管理常量
- **命名规范**：使用清晰的命名约定
- **文档说明**：提供常量用途说明
- **类型安全**：使用类型安全的常量定义

## 🧪 测试策略

### DTO测试
```java
@Test
void userCreateRequest_Validation() {
    UserCreateRequest request = UserCreateRequest.builder()
            .username("te")  // 太短
            .password("123") // 太短
            .email("invalid-email") // 格式错误
            .build();
    
    Set<ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);
    
    assertThat(violations).hasSize(3);
}
```

### 异常处理测试
```java
@Test
void globalExceptionHandler_BusinessException() {
    BusinessException exception = new ValidationException("测试异常");
    
    ResponseEntity<ApiResponse<Void>> response = 
            globalExceptionHandler.handleBusinessException(exception);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).isEqualTo("测试异常");
}
```

### 工具类测试
```java
@Test
void dateUtils_FormatAndParse() {
    LocalDate date = LocalDate.of(2024, 1, 1);
    String formatted = DateUtils.formatDate(date);
    LocalDate parsed = DateUtils.parseDate(formatted);
    
    assertThat(parsed).isEqualTo(date);
}
```

## 📊 性能考虑

### DTO优化
- 使用`@Builder`模式减少对象创建开销
- 避免过度嵌套的DTO结构
- 合理使用懒加载

### 异常优化
- 避免在循环中抛出异常
- 使用异常池重用异常对象
- 合理设置异常堆栈跟踪

### 工具类优化
- 使用缓存避免重复计算
- 优化正则表达式编译
- 使用高效的数据结构

## 📝 开发指南

### 1. 添加新的请求DTO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YourRequest {
    
    @NotBlank(message = "字段不能为空")
    @Size(min = 1, max = 100, message = "字段长度应在1-100字符之间")
    private String field;
    
    // 添加验证注解和业务方法
}
```

### 2. 添加新的异常类型
```java
public class YourException extends BusinessException {
    
    public YourException(String message) {
        super(400, message);
    }
    
    public static YourException create(String detail) {
        return new YourException("业务错误: " + detail);
    }
}
```

### 3. 添加新的工具类
```java
public class YourUtils {
    
    // 私有构造函数防止实例化
    private YourUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    public static String yourMethod(String input) {
        // 实现工具方法
        return input;
    }
}
```

## 🚨 注意事项

1. **数据安全**：
   - 敏感数据不在DTO中传输
   - 使用合适的验证注解
   - 避免数据泄露

2. **性能影响**：
   - 避免创建过多临时对象
   - 合理使用缓存
   - 注意内存泄露

3. **兼容性**：
   - 考虑API版本兼容
   - 谨慎修改已有DTO
   - 保持向后兼容

4. **代码质量**：
   - 保持代码简洁
   - 添加适当注释
   - 编写完整测试

5. **团队协作**：
   - 遵循命名规范
   - 及时更新文档
   - 代码审查机制 
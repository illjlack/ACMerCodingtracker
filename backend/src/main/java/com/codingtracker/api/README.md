# API层架构说明

## 📋 概述

API层是CodingTracker系统的表现层，负责处理HTTP请求、参数验证、权限控制和响应格式化。采用RESTful API设计风格，支持版本化管理。

## 🏗️ 架构设计

### 目录结构
```
api/
├── v1/                     # API版本1
│   └── controller/         # 控制器层
│       ├── UserController.java      # 用户管理控制器
│       ├── AuthController.java      # 认证控制器
│       ├── UserOJController.java    # OJ账号控制器
│       └── AdminController.java     # 管理员控制器
└── README.md              # 本文档
```

### 设计原则

1. **职责单一**：每个控制器只负责特定业务领域
2. **RESTful设计**：遵循REST架构风格
3. **版本化管理**：支持API版本演进
4. **统一响应格式**：使用ApiResponse统一包装响应
5. **完整文档**：使用Swagger/OpenAPI 3.0生成文档

## 🔧 技术特性

### 核心注解
- `@RestController`：标识REST控制器
- `@RequestMapping`：定义请求映射
- `@Valid`：启用参数验证
- `@PreAuthorize`：方法级权限控制
- `@Operation`：Swagger API文档注解

### 统一响应格式
```java
public class ApiResponse<T> {
    private boolean success;    // 操作是否成功
    private int code;          // 响应状态码
    private String message;    // 响应消息
    private T data;           // 响应数据
    private String timestamp; // 时间戳
}
```

### 异常处理
- 全局异常处理器：`GlobalExceptionHandler`
- 统一错误响应格式
- 详细错误信息记录

## 📚 控制器说明

### 1. AuthController - 认证控制器
**职责**：用户认证、授权相关操作

**主要端点**：
- `POST /api/v1/auth/login` - 用户登录
- `POST /api/v1/auth/register` - 用户注册  
- `POST /api/v1/auth/logout` - 用户登出
- `POST /api/v1/auth/change-password` - 修改密码
- `POST /api/v1/auth/refresh-token` - 刷新令牌
- `GET /api/v1/auth/me` - 获取当前用户信息

**特性**：
- JWT令牌认证
- 密码强度验证
- 登录失败次数限制
- 完整的认证流程

### 2. UserController - 用户管理控制器
**职责**：用户基本信息管理

**主要端点**：
- `GET /api/v1/users` - 获取用户列表（分页）
- `GET /api/v1/users/{id}` - 获取用户详情
- `POST /api/v1/users` - 创建用户
- `PUT /api/v1/users/{id}` - 更新用户信息
- `DELETE /api/v1/users/{id}` - 删除用户
- `POST /api/v1/users/{id}/avatar` - 上传头像
- `GET /api/v1/users/search` - 搜索用户

**特性**：
- 分页查询支持
- 参数验证
- 权限控制
- 文件上传处理

### 3. UserOJController - OJ账号控制器
**职责**：用户OJ账号管理

**主要端点**：
- `GET /api/v1/users/{userId}/oj-accounts` - 获取用户OJ账号列表
- `POST /api/v1/users/{userId}/oj-accounts` - 添加OJ账号
- `PUT /api/v1/oj-accounts/{id}` - 更新OJ账号
- `DELETE /api/v1/oj-accounts/{id}` - 删除OJ账号
- `GET /api/v1/oj-accounts/{id}` - 获取OJ账号详情

**特性**：
- 多平台支持
- 账号验证
- 状态管理

### 4. AdminController - 管理员控制器
**职责**：系统管理功能

**主要端点**：
- `GET /api/v1/admin/stats` - 获取系统统计信息
- `GET /api/v1/admin/users` - 管理员用户管理
- `POST /api/v1/admin/users/{id}/activate` - 激活用户
- `POST /api/v1/admin/users/{id}/deactivate` - 停用用户
- `GET /api/v1/admin/system/health` - 系统健康检查

**特性**：
- 管理员权限控制
- 系统监控
- 批量操作
- 统计报表

## 🔒 安全控制

### 认证机制
- JWT Bearer Token认证
- 令牌自动刷新
- 登出令牌失效

### 权限控制
```java
@PreAuthorize("hasRole('ADMIN')")           // 管理员权限
@PreAuthorize("hasRole('USER')")            // 普通用户权限
@PreAuthorize("hasRole('SUPER_ADMIN')")     // 超级管理员权限
@PreAuthorize("#username == authentication.name") // 资源所有者权限
```

### 数据验证
- 请求参数验证：使用`@Valid`注解
- 自定义验证器：业务规则验证
- 安全过滤：XSS、SQL注入防护

## 📊 监控与日志

### 访问日志
- 请求/响应日志记录
- 性能监控
- 错误追踪

### 指标监控
- API调用次数
- 响应时间统计
- 错误率监控

## 🚀 使用示例

### 1. 用户登录
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 2. 获取用户列表
```bash
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. 上传头像
```bash
curl -X POST http://localhost:8080/api/v1/users/1/avatar \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@avatar.jpg"
```

## 📖 文档访问

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API文档**: http://localhost:8080/v3/api-docs
- **分组文档**:
  - 认证API: http://localhost:8080/swagger-ui/index.html#/auth
  - 用户API: http://localhost:8080/swagger-ui/index.html#/user
  - 管理API: http://localhost:8080/swagger-ui/index.html#/admin

## 🛠️ 开发指南

### 1. 添加新的控制器
```java
@RestController
@RequestMapping("/api/v1/your-resource")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "资源管理", description = "资源相关API")
public class YourController {
    
    private final YourService yourService;
    
    @GetMapping
    @Operation(summary = "获取资源列表")
    public ResponseEntity<ApiResponse<List<YourResponse>>> getResources() {
        // 实现逻辑
    }
}
```

### 2. 添加权限控制
```java
@PreAuthorize("hasRole('ADMIN') or @yourService.isOwner(#id, authentication.name)")
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<YourResponse>> getResource(@PathVariable Integer id) {
    // 实现逻辑
}
```

### 3. 参数验证
```java
@PostMapping
public ResponseEntity<ApiResponse<YourResponse>> createResource(
    @Valid @RequestBody YourCreateRequest request) {
    // 实现逻辑
}
```

## 🔄 版本管理

### 当前版本：v1
- 基础用户管理功能
- 认证授权功能
- OJ账号管理功能
- 管理员功能

### 版本升级策略
1. 向后兼容的更改直接在当前版本更新
2. 破坏性更改创建新版本（v2, v3...）
3. 旧版本维护周期：至少6个月
4. 版本弃用通知：提前3个月通知

## 📝 注意事项

1. **响应格式**：所有API必须使用`ApiResponse`包装响应
2. **异常处理**：使用全局异常处理器统一处理异常
3. **日志记录**：关键操作必须记录日志
4. **参数验证**：所有输入参数必须进行验证
5. **权限控制**：敏感操作必须进行权限检查
6. **文档维护**：API变更时同步更新Swagger文档 
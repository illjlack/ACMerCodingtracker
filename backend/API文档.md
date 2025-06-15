# CodingTracker API 文档

## 项目概述

CodingTracker 是一个编程竞赛和练习管理系统，提供用户管理、OJ平台账号管理、题目跟踪等功能。

### 版本信息
- **API版本**: v1
- **项目版本**: 2.0.0
- **基础URL**: `/api/v1`

### 技术栈
- **后端框架**: Spring Boot 3.4.3
- **数据库**: MySQL
- **认证方式**: JWT
- **文档工具**: OpenAPI 3.0 (Swagger)

## 认证机制

### JWT Token认证
系统使用JWT Token进行身份认证，Token需要在请求头中携带：

```http
Authorization: Bearer <your-jwt-token>
```

### 用户角色
- **SUPER_ADMIN**: 超级管理员，拥有所有权限
- **ADMIN**: 管理员，拥有用户管理权限
- **USER**: 普通用户，只能管理自己的信息

## 通用响应格式

### 成功响应
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    // 具体数据
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### 错误响应
```json
{
  "success": false,
  "message": "错误信息",
  "data": null,
  "timestamp": "2024-01-01T12:00:00Z",
  "code": 400
}
```

## API接口

### 1. 用户管理 API

#### 1.1 获取所有用户
- **接口**: `GET /api/v1/users`
- **权限**: ADMIN, SUPER_ADMIN
- **参数**:
  - `page`: 页码（从0开始）
  - `size`: 每页大小（默认20）
  - `sort`: 排序字段

**示例请求**:
```http
GET /api/v1/users?page=0&size=10&sort=createdAt,desc
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应示例**:
```json
{
  "success": true,
  "message": "获取用户列表成功",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "admin",
        "realName": "管理员",
        "email": "admin@example.com",
        "major": "计算机科学",
        "active": true,
        "roles": ["ADMIN"],
        "createdAt": "2024-01-01T12:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### 1.2 根据ID获取用户
- **接口**: `GET /api/v1/users/{id}`
- **权限**: ADMIN, SUPER_ADMIN 或 用户本人

**示例请求**:
```http
GET /api/v1/users/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### 1.3 根据用户名获取用户
- **接口**: `GET /api/v1/users/username/{username}`
- **权限**: ADMIN, SUPER_ADMIN 或 用户本人

#### 1.4 搜索用户
- **接口**: `GET /api/v1/users/search`
- **权限**: ADMIN, SUPER_ADMIN
- **参数**:
  - `keyword`: 搜索关键字（用户名、真实姓名、邮箱）

#### 1.5 获取当前用户信息
- **接口**: `GET /api/v1/users/me`
- **权限**: 任何已认证用户

#### 1.6 创建新用户
- **接口**: `POST /api/v1/users`
- **权限**: ADMIN, SUPER_ADMIN

**请求体示例**:
```json
{
  "username": "newuser",
  "password": "StrongPassword123!",
  "realName": "新用户",
  "email": "newuser@example.com",
  "major": "软件工程",
  "roles": ["USER"],
  "ojAccounts": [
    {
      "platform": "CODEFORCES",
      "accountName": "newuser_cf"
    }
  ]
}
```

#### 1.7 更新用户信息
- **接口**: `PUT /api/v1/users/{id}`
- **权限**: ADMIN, SUPER_ADMIN 或 用户本人

#### 1.8 管理员更新用户信息
- **接口**: `PUT /api/v1/users/{id}/admin`
- **权限**: ADMIN, SUPER_ADMIN

#### 1.9 激活用户
- **接口**: `POST /api/v1/users/{id}/activate`
- **权限**: ADMIN, SUPER_ADMIN

#### 1.10 停用用户
- **接口**: `POST /api/v1/users/{id}/deactivate`
- **权限**: ADMIN, SUPER_ADMIN

#### 1.11 删除用户
- **接口**: `DELETE /api/v1/users/{id}`
- **权限**: SUPER_ADMIN

#### 1.12 上传用户头像
- **接口**: `POST /api/v1/users/avatar`
- **权限**: 任何已认证用户
- **Content-Type**: `multipart/form-data`

**请求示例**:
```http
POST /api/v1/users/avatar
Content-Type: multipart/form-data
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

file=@avatar.jpg
```

#### 1.13 检查用户名是否存在
- **接口**: `GET /api/v1/users/check-username?username=test`
- **权限**: 公开

#### 1.14 检查邮箱是否存在
- **接口**: `GET /api/v1/users/check-email?email=test@example.com`
- **权限**: 公开

#### 1.15 获取用户统计信息
- **接口**: `GET /api/v1/users/statistics`
- **权限**: ADMIN, SUPER_ADMIN

**响应示例**:
```json
{
  "success": true,
  "message": "获取统计信息成功",
  "data": {
    "totalUsers": 100,
    "activeUsers": 95,
    "adminUsers": 5,
    "superAdminUsers": 1,
    "regularUsers": 94
  }
}
```

### 2. 用户认证 API

#### 2.1 用户注册
- **接口**: `POST /api/v1/auth/register`
- **权限**: 公开

**请求体示例**:
```json
{
  "username": "newuser",
  "password": "StrongPassword123!",
  "confirmPassword": "StrongPassword123!",
  "realName": "新用户",
  "email": "newuser@example.com",
  "major": "计算机科学"
}
```

#### 2.2 用户登录
- **接口**: `POST /api/v1/auth/login`
- **权限**: 公开

**请求体示例**:
```json
{
  "username": "admin",
  "password": "password123"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "roles": ["ADMIN"]
    }
  }
}
```

#### 2.3 刷新令牌
- **接口**: `POST /api/v1/auth/refresh`
- **权限**: 需要有效的刷新令牌

#### 2.4 用户登出
- **接口**: `POST /api/v1/auth/logout`
- **权限**: 已认证用户

#### 2.5 修改密码
- **接口**: `PUT /api/v1/auth/password`
- **权限**: 已认证用户

**请求体示例**:
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "NewStrongPassword123!",
  "confirmPassword": "NewStrongPassword123!"
}
```

### 3. OJ账号管理 API

#### 3.1 获取用户OJ账号
- **接口**: `GET /api/v1/users/{username}/oj-accounts`
- **权限**: ADMIN, SUPER_ADMIN 或 用户本人

#### 3.2 添加OJ账号
- **接口**: `POST /api/v1/users/{username}/oj-accounts`
- **权限**: ADMIN, SUPER_ADMIN 或 用户本人

**请求体示例**:
```json
{
  "platform": "CODEFORCES",
  "accountName": "user_cf_account"
}
```

#### 3.3 删除OJ账号
- **接口**: `DELETE /api/v1/users/{username}/oj-accounts/{accountId}`
- **权限**: ADMIN, SUPER_ADMIN 或 用户本人

#### 3.4 同步OJ账号数据
- **接口**: `POST /api/v1/users/{username}/oj-accounts/sync`
- **权限**: 已认证用户

## 数据模型

### User（用户）
```json
{
  "id": 1,
  "username": "admin",
  "realName": "管理员",
  "major": "计算机科学",
  "email": "admin@example.com",
  "avatar": "http://example.com/avatar.jpg",
  "active": true,
  "lastTryDate": "2024-01-01T12:00:00Z",
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z",
  "roles": ["ADMIN"],
  "ojAccounts": [...],
  "tags": [...]
}
```

### UserOJ（用户OJ账号）
```json
{
  "id": 1,
  "platform": "CODEFORCES",
  "accountName": "user_cf",
  "active": true,
  "createdAt": "2024-01-01T10:00:00Z",
  "lastSyncAt": "2024-01-01T12:00:00Z"
}
```

### OJ平台枚举值
- `CODEFORCES` - Codeforces
- `VIRTUAL_JUDGE` - Virtual Judge
- `BEE_CROWD` - Beecrowd
- `HDU` - 杭电OJ
- `POJ` - 北京大学OJ
- `LEETCODE` - 力扣
- `LUOGU` - 洛谷
- `ATCODER` - AtCoder
- `CODECHEF` - CodeChef
- `TOPCODER` - TopCoder
- `SPOJ` - SPOJ
- `HACKERRANK` - HackerRank
- `HACKEREARTH` - HackerEarth
- `CSES` - CSES Problem Set
- `KATTIS` - Kattis
- `GYM` - Codeforces Gym
- `NOWCODER` - 牛客网
- `UVA` - UVA Online Judge

## 错误代码

| 错误代码 | 说明 |
|---------|------|
| 400 | 请求参数错误 |
| 401 | 未认证或令牌无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如用户名已存在） |
| 422 | 数据验证失败 |
| 500 | 服务器内部错误 |

## 开发环境配置

### 本地开发
1. 确保Java 17+环境
2. 配置MySQL数据库
3. 修改`application.properties`配置文件
4. 运行`mvn spring-boot:run`

### API测试
- **本地地址**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API文档**: http://localhost:8080/v3/api-docs

## 更新日志

### v2.0.0 (2024-01-01)
- 重构代码架构，采用DDD设计
- 优化API接口设计
- 改进用户权限管理
- 增强错误处理和日志记录
- 完善API文档

### v1.0.0 (2023-12-01)
- 初始版本发布
- 基础用户管理功能
- OJ账号绑定功能
- 简单的权限控制 
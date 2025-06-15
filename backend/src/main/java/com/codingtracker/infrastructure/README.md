# 基础设施层架构说明

## 📋 概述

基础设施层为CodingTracker系统提供技术支撑服务，包括安全认证、数据持久化、外部服务集成等。该层不包含业务逻辑，专注于技术实现和系统基础功能。

## 🏗️ 架构设计

### 目录结构
```
infrastructure/
├── security/              # 安全组件
│   ├── JwtTokenProvider.java           # JWT令牌提供者
│   ├── JwtAuthenticationFilter.java    # JWT认证过滤器
│   ├── JwtAuthenticationEntryPoint.java # JWT认证入口点
│   └── UserDetailsServiceImpl.java     # 用户详情服务实现
├── external/              # 外部服务
│   └── AvatarStorageService.java       # 头像存储服务
└── README.md             # 本文档
```

### 设计原则

1. **关注分离**：基础设施与业务逻辑分离
2. **可替换性**：各组件可独立替换
3. **配置驱动**：通过配置文件控制行为
4. **故障隔离**：外部依赖故障不影响核心功能
5. **监控友好**：提供完整的监控和日志

## 🔐 安全组件

### 1. JwtTokenProvider - JWT令牌提供者
**职责**：JWT令牌的生成、验证和解析

**核心功能**：
- 生成JWT令牌
- 验证令牌有效性
- 解析令牌内容
- 检查令牌过期时间

**配置参数**：
```yaml
app:
  jwt:
    secret: coding-tracker-jwt-secret-key-for-authentication-2024
    expiration: 86400000  # 24小时
```

**使用示例**：
```java
@Service
public class AuthService {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    public String login(String username, String password) {
        // 验证用户名密码
        if (validateCredentials(username, password)) {
            return jwtTokenProvider.generateToken(username);
        }
        throw new BadCredentialsException("Invalid credentials");
    }
}
```

### 2. JwtAuthenticationFilter - JWT认证过滤器
**职责**：拦截HTTP请求并验证JWT令牌

**处理流程**：
1. 从请求头提取JWT令牌
2. 验证令牌有效性
3. 解析用户信息
4. 设置Spring Security上下文
5. 继续过滤链

**过滤规则**：
- 公开端点不需要认证
- 认证端点特殊处理
- 其他端点需要有效令牌

### 3. JwtAuthenticationEntryPoint - 认证入口点
**职责**：处理未认证请求的统一响应

**响应格式**：
```json
{
  "success": false,
  "code": 401,
  "message": "认证失败，请先登录",
  "data": null,
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/users"
}
```

**错误类型处理**：
- BadCredentialsException → "用户名或密码错误"
- AccountExpiredException → "账户已过期"
- DisabledException → "账户已被禁用"
- LockedException → "账户已被锁定"

### 4. UserDetailsServiceImpl - 用户详情服务
**职责**：为Spring Security提供用户认证信息

**主要功能**：
- 根据用户名加载用户信息
- 转换为Spring Security UserDetails
- 处理用户权限信息

**UserPrincipal设计**：
```java
public class UserPrincipal implements UserDetails {
    private final Integer id;
    private final String username;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    
    // Spring Security 接口实现
    @Override
    public boolean isEnabled() {
        return active;  // 基于用户激活状态
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;  // 基于用户角色
    }
}
```

## 📁 外部服务

### 1. AvatarStorageService - 头像存储服务
**职责**：用户头像文件的存储、管理和访问

**核心功能**：
- 头像文件上传
- 文件验证（类型、大小）
- 唯一文件名生成
- 文件访问URL生成
- 文件删除管理

**配置参数**：
```yaml
app:
  upload:
    avatar:
      dir: uploads/avatars
      base-url: http://localhost:8080
      max-size: 2097152  # 2MB
```

**文件验证规则**：
- 支持格式：jpg, jpeg, png, gif
- 最大大小：2MB
- MIME类型检查：image/*
- 文件名安全性检查

**存储策略**：
- 按日期分目录：`yyyy/MM/dd/`
- UUID文件名：`32位UUID.扩展名`
- 防止文件名冲突
- 支持文件覆盖

**访问URL格式**：
```
http://localhost:8080/api/files/avatars/2024/01/01/uuid.jpg
```

## 🛠️ 技术特性

### 安全特性
1. **JWT无状态认证**：
   - 分布式友好
   - 性能优异
   - 水平扩展支持

2. **密码安全**：
   - BCrypt加密
   - 盐值随机生成
   - 强度验证

3. **权限控制**：
   - 基于角色的访问控制(RBAC)
   - 方法级权限验证
   - 资源级权限控制

### 存储特性
1. **文件安全**：
   - 类型验证
   - 大小限制
   - 路径安全

2. **性能优化**：
   - 静态资源缓存
   - 并发访问支持
   - 磁盘空间管理

3. **可扩展性**：
   - 支持多种存储后端
   - 云存储集成准备
   - CDN集成友好

## 📊 配置管理

### JWT配置
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:coding-tracker-jwt-secret-key-for-authentication-2024}
    expiration: ${JWT_EXPIRATION:86400000}  # 24小时
```

### 文件上传配置
```yaml
app:
  upload:
    avatar:
      dir: ${AVATAR_UPLOAD_DIR:uploads/avatars}
      base-url: ${AVATAR_BASE_URL:http://localhost:8080}
      max-size: ${AVATAR_MAX_SIZE:2097152}  # 2MB
```

### CORS配置
```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://127.0.0.1:3000}
    allowed-methods: ${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}
    allowed-headers: ${CORS_ALLOWED_HEADERS:*}
    allow-credentials: ${CORS_ALLOW_CREDENTIALS:true}
```

## 🔧 集成配置

### Spring Security配置
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/files/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### Web MVC配置
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像文件静态资源映射
        registry.addResourceHandler("/api/files/avatars/**")
                .addResourceLocations("file:" + avatarUploadDir + "/")
                .setCachePeriod(3600);
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowCredentials(true);
    }
}
```

## 🚨 异常处理

### JWT异常
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) {
        try {
            // JWT处理逻辑
        } catch (JwtException e) {
            log.error("JWT异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("认证过滤器异常: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 文件上传异常
```java
@Service
public class AvatarStorageService {
    
    public String store(MultipartFile file) throws IOException {
        try {
            validateFile(file);
            return doStore(file);
        } catch (IllegalArgumentException e) {
            log.warn("文件验证失败: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("文件存储失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件存储失败", e);
        }
    }
}
```

## 📈 监控与日志

### 安全监控
```java
@Component
@Slf4j
public class SecurityAuditLogger {
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("用户登录成功: {}", username);
    }
    
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        log.warn("用户登录失败: {}, 原因: {}", username, event.getException().getMessage());
    }
}
```

### 文件操作日志
```java
@Service
@Slf4j
public class AvatarStorageService {
    
    public String store(MultipartFile file) throws IOException {
        log.info("开始存储头像文件: {}, 大小: {}", 
            file.getOriginalFilename(), file.getSize());
        
        String avatarUrl = doStore(file);
        
        log.info("头像文件存储成功: {}", avatarUrl);
        return avatarUrl;
    }
}
```

## 🧪 测试策略

### 单元测试
```java
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    
    @Test
    void generateToken_Success() {
        // Given
        String username = "testuser";
        
        // When
        String token = jwtTokenProvider.generateToken(username);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }
}
```

### 集成测试
```java
@SpringBootTest
@TestPropertySource(properties = {
    "app.upload.avatar.dir=test-uploads"
})
class AvatarStorageServiceIntegrationTest {
    
    @Autowired
    private AvatarStorageService avatarStorageService;
    
    @Test
    void store_WithValidImage_Success() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes());
        
        // When
        String avatarUrl = avatarStorageService.store(file);
        
        // Then
        assertThat(avatarUrl).contains("/api/files/avatars/");
        assertThat(avatarStorageService.exists(avatarUrl)).isTrue();
    }
}
```

## 📊 性能优化

### JWT优化
- 使用HMAC256算法
- 合理设置过期时间
- 避免令牌过大

### 文件存储优化
- 静态资源缓存
- 文件压缩
- 并发处理

### 内存优化
- 避免大文件全量加载
- 流式处理
- 及时释放资源

## 🔄 扩展性设计

### 存储后端扩展
```java
public interface FileStorageService {
    String store(MultipartFile file) throws IOException;
    void delete(String fileUrl);
    boolean exists(String fileUrl);
}

@Service
@Profile("local")
public class LocalFileStorageService implements FileStorageService {
    // 本地文件存储实现
}

@Service
@Profile("cloud")
public class CloudFileStorageService implements FileStorageService {
    // 云存储实现
}
```

### 认证方式扩展
```java
public interface TokenProvider {
    String generateToken(String username);
    boolean validateToken(String token);
    String getUsernameFromToken(String token);
}

@Component
public class JwtTokenProvider implements TokenProvider {
    // JWT实现
}

@Component
@Profile("oauth")
public class OAuthTokenProvider implements TokenProvider {
    // OAuth实现
}
```

## 📝 开发指南

### 1. 添加新的外部服务
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class YourExternalService {
    
    @Value("${your.service.config}")
    private String config;
    
    public String callExternalApi() {
        try {
            // 调用外部API
            return "result";
        } catch (Exception e) {
            log.error("外部服务调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("外部服务不可用");
        }
    }
}
```

### 2. 添加新的安全组件
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class YourSecurityComponent {
    
    public boolean authorize(String resource, String permission) {
        // 权限检查逻辑
        return true;
    }
}
```

### 3. 配置新的过滤器
```java
@Component
public class YourFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // 过滤器逻辑
        chain.doFilter(request, response);
    }
}
```

## 🚨 注意事项

1. **安全性**：
   - 密钥安全存储
   - 敏感信息加密
   - 输入验证

2. **性能**：
   - 避免同步阻塞
   - 合理使用缓存
   - 资源及时释放

3. **可靠性**：
   - 异常处理完整
   - 故障恢复机制
   - 监控告警

4. **可维护性**：
   - 配置外部化
   - 日志完整性
   - 文档及时更新

5. **扩展性**：
   - 接口抽象设计
   - 组件可替换
   - 配置灵活性 
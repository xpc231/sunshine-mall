# JWT 认证组件

## 概述

JWT 认证组件是 Sunshine Mall 微服务架构中的通用认证框架，提供统一的 JWT Token 生成、验证、黑名单管理等功能。

## 核心功能

1. **JWT Token 管理**
   - Token 生成与解析
   - Token 验证与黑名单管理
   - 自定义声明支持

2. **认证上下文**
   - 线程级用户信息存储
   - 便捷的用户信息获取

3. **注解驱动**
   - `@RequireAuth` 注解标记需要认证的接口
   - 自动拦截器处理认证逻辑

4. **灵活配置**
   - 支持多种配置方式
   - 默认配置开箱即用

## 快速开始

### 1. 添加依赖

在需要使用认证功能的服务中添加依赖：

```xml
<dependency>
    <groupId>com.xpcjsu.sunshinemall</groupId>
    <artifactId>sunshine-mall-framework-auth</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
sunshine-mall:
  auth:
    jwt:
      secret: your-secret-key
      expiration: 604800000  # 7天，单位毫秒
      issuer: sunshine-mall
      header-name: Authorization
      token-prefix: "Bearer "
```

### 3. 使用认证注解

```java
@RestController
@RequestMapping("/api/orders")
@RequireAuth  // 类级别注解，整个Controller需要认证
public class OrderController {

    @GetMapping("/list")
    public Result<List<Order>> getOrders() {
        // 获取当前用户ID
        Long userId = AuthContext.getCurrentUserId();
        // 业务逻辑...
        return Result.success(orders);
    }

    @PostMapping("/create")
    @RequireAuth(message = "创建订单需要登录")  // 方法级别注解
    public Result<String> createOrder(@RequestBody OrderRequest request) {
        // 业务逻辑...
        return Result.success("订单创建成功");
    }

    @GetMapping("/public")
    @RequireAuth(required = false)  // 可选认证
    public Result<String> publicEndpoint() {
        // 如果有Token则解析用户信息，没有Token也可以访问
        Long userId = AuthContext.getCurrentUserId(); // 可能为null
        return Result.success("公开接口");
    }
}
```

### 4. 生成和验证Token

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AuthService authService;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 验证用户名密码...
        
        // 生成Token
        String token = authService.generateToken(user.getId(), user.getUsername());
        
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    @Override
    public void logout(HttpServletRequest request) {
        // 登出（将Token加入黑名单）
        authService.logout(request);
    }
}
```

## 核心组件

### JwtUtil

JWT 工具类，提供 Token 的生成、解析、验证等功能。

```java
@Autowired
private JwtUtil jwtUtil;

// 生成Token
String token = jwtUtil.generateToken("123", "username");

// 解析Token
DecodedJWT decodedJWT = jwtUtil.parseToken(token);

// 验证Token
boolean isValid = jwtUtil.validateToken(token);

// 将Token加入黑名单
jwtUtil.blacklistToken(token);
```

### AuthService

认证服务，提供高级认证功能。

```java
@Autowired
private AuthService authService;

// 从HTTP请求中提取认证信息
AuthContext context = authService.extractAuthContext(request);

// 提取用户ID
Long userId = authService.extractUserId(request);

// 生成Token
String token = authService.generateToken(userId, username);

// 登出
authService.logout(request);
```

### AuthContext

认证上下文，用于在当前线程中存储和获取用户信息。

```java
// 获取当前用户ID
Long userId = AuthContext.getCurrentUserId();

// 获取当前用户名
String username = AuthContext.getCurrentUsername();

// 获取当前Token
String token = AuthContext.getCurrentToken();

// 检查是否已认证
boolean isAuthenticated = AuthContext.isAuthenticated();
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `sunshine-mall.auth.jwt.secret` | `sunshine-mall-default-secret-key` | JWT 签名密钥 |
| `sunshine-mall.auth.jwt.expiration` | `604800000` | Token 过期时间（毫秒） |
| `sunshine-mall.auth.jwt.issuer` | `sunshine-mall` | JWT 签发者 |
| `sunshine-mall.auth.jwt.header-name` | `Authorization` | HTTP Header 名称 |
| `sunshine-mall.auth.jwt.token-prefix` | `Bearer ` | Token 前缀 |
| `sunshine-mall.auth.jwt.blacklist-prefix` | `auth:blacklist:` | 黑名单缓存前缀 |
| `sunshine-mall.auth.jwt.user-cache-prefix` | `auth:user:` | 用户缓存前缀 |
| `sunshine-mall.auth.jwt.user-cache-expire` | `3600` | 用户缓存过期时间（秒） |

## 最佳实践

### 1. 安全配置

```yaml
sunshine-mall:
  auth:
    jwt:
      secret: ${JWT_SECRET:your-very-long-and-secure-secret-key}
      expiration: 86400000  # 1天，生产环境建议较短
```

### 2. 错误处理

组件会抛出 `BusinessException`，建议在全局异常处理器中统一处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        if (BusinessErrorCode.USER_NOT_LOGIN.equals(e.getErrorCode())) {
            return Result.fail(401, e.getMessage());
        }
        return Result.fail(e.getMessage());
    }
}
```

### 3. 性能优化

- Token 黑名单使用 Redis 存储，过期时间与 Token 一致
- 用户信息可以缓存，减少数据库查询
- 在网关层统一验证 Token，避免重复验证

## 注意事项

1. **密钥安全**：生产环境必须使用强密钥，建议通过环境变量配置
2. **Token 过期**：合理设置 Token 过期时间，平衡安全性和用户体验
3. **黑名单清理**：Redis 会自动清理过期的黑名单记录
4. **线程安全**：`AuthContext` 使用 `ThreadLocal`，注意在异步场景下的使用
5. **异常处理**：认证失败会抛出 `BusinessException`，需要全局异常处理器处理

## 扩展功能

### 自定义声明

```java
Map<String, Object> extraClaims = new HashMap<>();
extraClaims.put("role", "admin");
extraClaims.put("permissions", Arrays.asList("read", "write"));

String token = jwtUtil.generateToken("123", "admin", extraClaims);

// 获取自定义声明
DecodedJWT decodedJWT = jwtUtil.parseToken(token);
String role = (String) jwtUtil.getClaimFromToken(decodedJWT, "role");
```

### Token 刷新

```java
// 检查Token是否即将过期
if (authService.isTokenExpiringSoon(token)) {
    // 生成新Token
    String newToken = authService.generateToken(userId, username);
    // 返回新Token给客户端
}
```
# Gateway Service 架构设计文档

## 设计原则

### YAGNI 原则（You Aren't Gonna Need It）

**核心思想**：只实现当前需要的功能，避免过度设计。

**应用实践**：

1. **认证方案选择**
   - ✅ 采用：JWT无状态认证（满足当前需求）
   - ❌ 未采用：OAuth2.0复杂认证（暂无需求）
   - 理由：电商平台初期只需简单的用户认证，OAuth2.0过于复杂

2. **路由配置方式**
   - ✅ 采用：静态路由配置（application.yml）
   - ❌ 未采用：动态路由配置（Nacos Config）
   - 理由：服务数量有限，路由规则稳定，静态配置已满足需求
   - 扩展点：已预留Nacos Config集成接口

3. **限流熔断**
   - ✅ 采用：预留扩展点
   - ❌ 未采用：立即实现Sentinel集成
   - 理由：初期流量可控，后续根据实际压力再实现

4. **Token黑名单**
   - ✅ 采用：预留Redis扩展点
   - ❌ 未采用：立即实现
   - 理由：初期用户登出场景少，后续根据需求再实现

---

## 架构设计

### 1. 整体架构

```
┌─────────────────────────────────────────────────────┐
│                  前端应用                            │
│            (Web / Mobile / App)                     │
└──────────────────┬──────────────────────────────────┘
                   │ HTTP/HTTPS
                   ▼
┌─────────────────────────────────────────────────────┐
│              Nginx (负载均衡)                         │
└──────────────────┬──────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌───────────────┐      ┌───────────────┐
│  Gateway-1    │      │  Gateway-2    │
│  (8080)       │      │  (8080)       │
└───────┬───────┘      └───────┬───────┘
        │                      │
        └──────────┬───────────┘
                   │
        ┌──────────┴──────────┐
        │   Nacos Discovery   │
        └─────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
    ┌───▼────┐           ┌───▼────┐
    │ User   │           │Product │
    │Service │           │Service │
    └────────┘           └────────┘
        │                     │
    ┌───▼────┐           ┌───▼────┐
    │ Order  │           │  Pay   │
    │Service │           │Service │
    └────────┘           └────────┘
```

### 2. 请求处理流程

```
客户端请求
    │
    ▼
┌─────────────────┐
│  CORS过滤器     │ → 跨域预检请求处理
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ JWT认证过滤器   │ → Token验证
└────────┬────────┘
         │
    ┌────┴────┐
    │ 白名单? │
    └────┬────┘
         │
    ┌────┴────┐
    │   是    │ → 直接放行
    └────┬────┘
         │
         ▼
┌─────────────────┐
│ Token验证       │
└────────┬────────┘
         │
    ┌────┴────┐
    │ 有效?   │
    └────┬────┘
         │
    ┌────┴────┐
    │   否    │ → 返回401
    └────┬────┘
         │
         ▼
┌─────────────────┐
│ 提取用户信息    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 添加请求头      │ → X-User-Id, X-Username
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 路由转发        │ → 负载均衡到下游服务
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 下游服务        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 异常处理        │ → 统一错误响应
└────────┬────────┘
         │
         ▼
    返回响应
```

### 3. 过滤器优先级

```
优先级（数值越小越高）

-1    ┌─────────────────────┐
      │ GlobalExceptionHandler │ → 全局异常处理
      └─────────────────────┘

-100  ┌─────────────────────┐
      │ JwtAuthenticationFilter │ → JWT认证
      └─────────────────────┘

0     ┌─────────────────────┐
      │ CorsWebFilter         │ → CORS跨域
      └─────────────────────┘

默认  ┌─────────────────────┐
      │ Gateway默认过滤器     │ → 路由、重试等
      └─────────────────────┘
```

---

## 核心组件设计

### 1. JwtUtil - JWT工具类

**设计思路**：
- 单一职责：只负责JWT相关操作
- 无状态：不存储任何状态信息
- 可测试：所有方法都是纯函数

**核心方法**：
```java
generateToken(userId, username)     // 生成Token
validateToken(token)                 // 验证Token
parseToken(token)                    // 解析Token
getUserIdFromToken(token)            // 提取用户ID
getUsernameFromToken(token)          // 提取用户名
isTokenExpired(token)                // 检查过期
extractToken(authHeader)             // 提取Token
```

**配置项**：
- `jwt.secret`：JWT密钥（生产环境必须修改）
- `jwt.expiration`：Token过期时间（默认2小时）

**安全考虑**：
- 密钥配置化（不硬编码）
- Token过期检查
- 异常统一处理

### 2. JwtAuthenticationFilter - 认证过滤器

**设计思路**：
- 全局拦截：实现 `GlobalFilter`
- 高优先级：`@Order(-100)`
- Reactive编程：返回 `Mono<Void>`

**处理逻辑**：
```java
1. 检查白名单
   ├─ 是 → 直接放行
   └─ 否 → 继续验证

2. 提取Token
   ├─ 不存在 → 返回401
   └─ 存在 → 继续验证

3. 验证Token
   ├─ 无效 → 返回401
   └─ 有效 → 提取用户信息

4. 注入请求头
   ├─ X-User-Id
   └─ X-Username

5. 转发请求
```

**白名单机制**：
```java
// 配置白名单路径
private static final List<String> WHITE_LIST = Arrays.asList(
    "/api/user/login",
    "/api/user/register",
    "/actuator/health"
);

// 前缀匹配
private boolean isWhiteList(String path) {
    return WHITE_LIST.stream().anyMatch(path::startsWith);
}
```

### 3. CorsConfig - 跨域配置

**设计思路**：
- 全局配置：`CorsWebFilter`
- 宽松策略：允许所有（开发环境）
- 生产限制：需修改为具体域名

**配置项**：
```java
allowedOriginPattern("*")    // 允许所有域名
allowedMethod("*")           // 允许所有方法
allowedHeader("*")           // 允许所有请求头
allowCredentials(true)       // 允许携带认证信息
maxAge(3600L)                // 预检请求缓存1小时
exposedHeaders(...)          // 暴露自定义响应头
```

**生产环境优化**：
```yaml
# 限制允许的域名
allowed-origins:
  - https://www.sunshine-mall.com
  - https://admin.sunshine-mall.com
```

### 4. SecurityConfig - Security配置

**设计思路**：
- 禁用默认认证：使用自定义JWT认证
- 禁用CSRF：JWT无状态认证无需CSRF
- 允许所有请求：认证由过滤器处理

**配置说明**：
```java
csrf().disable()              // 禁用CSRF
httpBasic().disable()         // 禁用Basic认证
formLogin().disable()         // 禁用表单登录
logout().disable()            // 禁用登出
authorizeExchange()           // 允许所有请求
    .anyExchange().permitAll()
```

### 5. GlobalExceptionHandler - 异常处理

**设计思路**：
- 全局捕获：实现 `ErrorWebExceptionHandler`
- 高优先级：`@Order(-1)`
- 统一响应：固定JSON格式

**响应格式**：
```json
{
  "code": 500,
  "message": "错误信息",
  "timestamp": 1697520000000
}
```

**异常分类**：
```java
ResponseStatusException → 保留原状态码
其他异常 → 500 Internal Server Error
```

---

## 数据流设计

### 1. 认证Token流转

```
┌──────────┐     登录请求      ┌──────────┐
│  客户端  │ ──────────────→  │User-Service│
│          │                   │          │
│          │ ←──────────────  │          │
└──────────┘   返回Token       └──────────┘
     │
     │ 存储Token (LocalStorage/Cookie)
     │
     ▼
┌──────────┐   携带Token        ┌──────────┐
│  客户端  │ ──────────────→   │ Gateway  │
│          │  Authorization:    │          │
│          │  Bearer {token}    │          │
└──────────┘                    └──────┬───┘
                                       │
                                       ▼
                               ┌────────────┐
                               │ Token验证  │
                               └──────┬─────┘
                                      │
                                      ▼
                               ┌────────────┐
                               │ 提取用户信息│
                               └──────┬─────┘
                                      │
                                      ▼
                               ┌────────────┐
                               │ 添加请求头  │
                               │ X-User-Id  │
                               │ X-Username │
                               └──────┬─────┘
                                      │
                                      ▼
                               ┌────────────┐
                               │下游服务    │
                               └────────────┘
```

### 2. 用户信息传递

**传递方式**：HTTP请求头

```
原始请求头：
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

网关处理后：
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 10001
X-Username: testuser
```

**下游服务获取**：
```java
@GetMapping("/profile")
public Result<UserProfile> getProfile(
    @RequestHeader("X-User-Id") String userId,
    @RequestHeader("X-Username") String username
) {
    // 无需再次解析Token
    return userService.getProfile(userId);
}
```

---

## 性能优化设计

### 1. Reactive编程模型

**优势**：
- 非阻塞I/O
- 更高并发量
- 更低资源消耗

**实现**：
```java
// 传统阻塞模式（Servlet）
public void doFilter(...) {
    // 阻塞等待
    String result = callService();
}

// Reactive模式（Gateway）
public Mono<Void> filter(...) {
    // 非阻塞，立即返回
    return chain.filter(exchange);
}
```

### 2. 连接池复用

**Redis连接池**：
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

**Nacos连接复用**：
- 长连接保持
- 心跳检测
- 自动重连

### 3. 负载均衡策略

**默认策略**：RoundRobin（轮询）

**配置示例**：
```yaml
spring:
  cloud:
    loadbalancer:
      ribbon:
        enabled: false  # 使用Spring Cloud Loadbalancer
```

### 4. 重试机制

**退避策略**：指数退避

```
第1次：立即重试
第2次：等待10ms
第3次：等待20ms
第4次：等待40ms
...
最大等待：50ms
```

---

## 安全设计

### 1. 多层防护

```
第一层：网络层
  ├─ HTTPS加密传输
  ├─ 防火墙规则
  └─ DDoS防护

第二层：网关层
  ├─ JWT Token验证
  ├─ 白名单机制
  └─ 限流熔断（预留）

第三层：服务层
  ├─ 请求头验证（X-User-Id）
  ├─ 业务权限校验
  └─ 数据权限校验
```

### 2. Token安全

**密钥管理**：
- 配置化（不硬编码）
- 环境隔离（dev/test/prod）
- 定期轮换（建议）

**过期策略**：
- 默认2小时
- 支持配置调整
- 过期自动拒绝

**黑名单机制**（预留）：
```
用户登出 → 加入黑名单（Redis）
Token验证 → 检查黑名单
黑名单TTL = Token剩余有效期
```

### 3. CORS安全

**开发环境**：
```yaml
allowed-origin-pattern: "*"  # 允许所有
```

**生产环境**：
```yaml
allowed-origins:
  - https://www.sunshine-mall.com
  - https://admin.sunshine-mall.com
```

---

## 扩展性设计

### 1. 限流熔断扩展点

**Sentinel集成预留**：
```java
// 预留配置
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
      datasource:
        nacos:
          server-addr: localhost:8848
```

**实现思路**：
1. 引入Sentinel依赖
2. 配置限流规则
3. 自定义降级响应

### 2. Token黑名单扩展点

**Redis实现预留**：
```java
// 预留接口
public interface TokenBlacklistService {
    void addToBlacklist(String token);
    boolean isBlacklisted(String token);
}

// 实现类
@Service
public class RedisTokenBlacklistService implements TokenBlacklistService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void addToBlacklist(String token) {
        long ttl = jwtUtil.getExpireTime(token);
        redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
```

### 3. 动态路由扩展点

**Nacos Config集成预留**：
```java
// 预留监听器
@Component
public class DynamicRouteListener implements Listener {
    @Override
    public void receiveConfigInfo(String configInfo) {
        // 解析路由配置
        // 刷新路由表
    }
}
```

---

## 监控设计

### 1. 监控指标

**业务指标**：
- QPS（每秒请求数）
- 响应时间（P50、P95、P99）
- 错误率（4xx、5xx）
- Token验证成功率

**系统指标**：
- CPU使用率
- 内存使用率
- 网络I/O
- JVM GC

### 2. 日志设计

**日志级别**：
```
DEBUG：Token验证详情、路由转发详情
INFO：启动信息、配置加载
WARN：Token验证失败、重试触发
ERROR：异常堆栈、系统错误
```

**日志格式**：
```
[时间] [线程] [级别] [类名] - [消息]
2024-10-17 10:00:00 [reactor-http-nio-1] INFO  JwtAuthenticationFilter - Token验证通过 - userId: 10001
```

### 3. 链路追踪（预留）

**Sleuth + Zipkin集成**：
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 采样率100%
  zipkin:
    base-url: http://localhost:9411
```

---

## 总结

Gateway Service 的架构设计遵循以下核心原则：

**简洁性**：
- 只实现必需功能
- 避免过度设计
- 预留扩展点

**高性能**：
- Reactive编程模型
- 连接池复用
- 负载均衡

**高可用**：
- 多实例部署
- 重试机制
- 监控告警

**安全性**：
- JWT认证
- 多层防护
- Token黑名单（预留）

**可扩展性**：
- 限流熔断预留
- 动态路由预留
- 灰度发布预留

该设计已具备生产环境部署基础，后续可根据实际需求逐步完善高级功能。

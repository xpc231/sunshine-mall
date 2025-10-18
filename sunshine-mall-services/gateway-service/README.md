# Gateway Service - 网关服务

## 模块简介

Gateway Service 是 Sunshine-Mall 电商平台的统一入口网关，基于 Spring Cloud Gateway 实现，负责路由转发、认证鉴权、跨域处理等核心功能。

## 核心功能

### 1. 路由转发

基于 Spring Cloud Gateway 实现动态路由转发，支持负载均衡和服务发现。

**路由配置示例**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
```

**路由规则**：
- `/api/user/**` → 转发到 `user-service`
- `/api/product/**` → 转发到 `product-service`
- `/api/order/**` → 转发到 `order-service`
- `/api/pay/**` → 转发到 `pay-service`
- `/api/logistics/**` → 转发到 `logistics-service`
- `/api/ai/**` → 转发到 `ai-service`

### 2. JWT 认证鉴权

基于 JWT Token 实现无状态认证，支持用户身份验证和信息传递。

**认证流程**：
1. 客户端登录成功后获取 JWT Token
2. 后续请求携带 Token（Header: `Authorization: Bearer {token}`）
3. 网关验证 Token 有效性
4. 验证通过后提取用户信息并传递给下游服务

**白名单路径**（无需Token）：
- `/api/user/login` - 用户登录
- `/api/user/register` - 用户注册
- `/api/user/captcha` - 验证码获取
- `/actuator/health` - 健康检查
- `/actuator/info` - 服务信息

**Token 配置**：
```yaml
jwt:
  secret: sunshine-mall-secret-key-2024    # JWT密钥
  expiration: 7200000                      # 过期时间（2小时）
```

### 3. CORS 跨域支持

全局 CORS 配置，支持前端应用跨域访问。

**配置特性**：
- 允许所有域名（生产环境需限制具体域名）
- 允许所有 HTTP 方法
- 允许携带认证信息（Cookie、Token）
- 预检请求缓存 1 小时

### 4. 全局异常处理

统一异常响应格式，捕获网关层异常并记录日志。

**响应格式**：
```json
{
  "code": 401,
  "message": "Token无效或已过期",
  "timestamp": 1697520000000
}
```

### 5. 重试机制

自动重试失败请求，提升系统可用性。

**重试配置**：
- 重试次数：3次
- 重试状态：`BAD_GATEWAY`、`GATEWAY_TIMEOUT`
- 重试方法：`GET`、`POST`
- 退避策略：指数退避（首次10ms，最大50ms）

## 技术栈

- **Spring Cloud Gateway** - 路由转发
- **Spring Security** - 安全框架
- **JWT (java-jwt 4.4.0)** - Token认证
- **Nacos Discovery** - 服务发现
- **Redis** - Token黑名单（预留）
- **Spring Boot Actuator** - 监控端点

## 快速开始

### 1. 启动 Nacos

确保 Nacos Server 已启动（默认端口：8848）

### 2. 启动 Redis

确保 Redis Server 已启动（默认端口：6379）

### 3. 配置文件

编辑 `application.yml`，根据实际环境修改配置：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # Nacos地址
        namespace: dev                # 命名空间

  data:
    redis:
      host: localhost                 # Redis地址
      port: 6379

jwt:
  secret: your-secret-key             # JWT密钥（生产环境务必修改）
  expiration: 7200000                 # Token过期时间
```

### 4. 启动网关

```bash
mvn spring-boot:run
```

默认端口：`8080`

## API 使用示例

### 1. 登录获取 Token（白名单路径）

```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

响应：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "10001",
    "username": "test"
  }
}
```

### 2. 访问受保护接口

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 3. 缺少 Token 访问

```bash
curl -X GET http://localhost:8080/api/user/profile
```

响应：
```json
{
  "code": 401,
  "message": "缺少认证Token"
}
```

## 核心类说明

### 1. JwtUtil

JWT 工具类，提供 Token 生成、验证、解析功能。

**主要方法**：
- `generateToken(userId, username)` - 生成Token
- `validateToken(token)` - 验证Token有效性
- `parseToken(token)` - 解析Token
- `getUserIdFromToken(token)` - 获取用户ID
- `getUsernameFromToken(token)` - 获取用户名

### 2. JwtAuthenticationFilter

JWT 认证全局过滤器，拦截所有请求进行 Token 验证。

**核心逻辑**：
1. 白名单路径直接放行
2. 提取并验证 Token
3. 验证通过后将用户信息添加到请求头（`X-User-Id`、`X-Username`）
4. 转发请求到下游服务

**优先级**：`-100`（在其他过滤器之前执行）

### 3. CorsConfig

CORS 跨域配置，允许前端应用跨域访问。

### 4. SecurityConfig

Spring Security 配置，禁用默认认证，使用自定义 JWT 认证。

### 5. GlobalExceptionHandler

全局异常处理器，统一异常响应格式。

## 扩展功能（预留）

### 1. 限流熔断

基于 Sentinel 实现流量控制和熔断降级（暂未实现，已预留扩展点）。

**预留配置**：
```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
      datasource:
        nacos:
          server-addr: localhost:8848
```

### 2. Token 黑名单

基于 Redis 实现 Token 黑名单机制，支持强制登出（暂未实现）。

**实现思路**：
1. 用户登出时将 Token 加入黑名单
2. 认证过滤器检查 Token 是否在黑名单
3. 黑名单 TTL 设置为 Token 剩余有效期

### 3. 动态路由

基于 Nacos 配置中心实现动态路由更新（暂未实现）。

## 测试

### 1. 运行单元测试

```bash
mvn test
```

### 2. 运行集成测试

```bash
mvn verify
```

### 3. 测试覆盖场景

- ✅ 健康检查接口（白名单）
- ✅ 缺少Token访问受保护接口
- ✅ 无效Token访问受保护接口
- ✅ 有效Token访问受保护接口
- ✅ CORS预检请求
- ✅ Token生成与解析
- ✅ Token过期检查

## 监控端点

### 1. 健康检查

```bash
curl http://localhost:8080/actuator/health
```

### 2. 服务信息

```bash
curl http://localhost:8080/actuator/info
```

### 3. Prometheus 指标

```bash
curl http://localhost:8080/actuator/prometheus
```

## 部署建议

### 1. 生产环境配置

**安全配置**：
- 修改 JWT 密钥为强随机值
- 限制 CORS 允许的域名
- 启用 HTTPS
- 配置防火墙规则

**性能优化**：
- 增加网关实例数量（负载均衡）
- 调整连接池参数
- 启用 HTTP/2
- 配置缓存策略

**监控告警**：
- 接入 Prometheus + Grafana
- 配置异常告警
- 监控关键指标（QPS、响应时间、错误率）

### 2. 高可用部署

```
           ┌─────────────┐
           │   Nginx     │
           │ (负载均衡)   │
           └─────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
   ┌─────────┐         ┌─────────┐
   │ Gateway │         │ Gateway │
   │ Instance1│        │ Instance2│
   └─────────┘         └─────────┘
        │                   │
        └─────────┬─────────┘
                  │
          ┌───────┴────────┐
          │  Nacos Cluster │
          └────────────────┘
```

## 常见问题

### 1. Token 验证失败

**问题**：返回 `401 Token无效或已过期`

**解决**：
- 检查 Token 格式（必须以 `Bearer ` 开头）
- 验证 Token 是否过期
- 确认 JWT 密钥配置一致

### 2. 路由转发失败

**问题**：返回 `503 Service Unavailable`

**解决**：
- 确认下游服务已启动
- 检查 Nacos 服务注册状态
- 验证路由配置正确性

### 3. CORS 跨域问题

**问题**：浏览器提示跨域错误

**解决**：
- 检查 CORS 配置
- 确认前端请求头设置正确
- 验证预检请求是否通过

## 版本历史

### v1.0.0 (2024-10-17)

- ✅ 基础路由转发功能
- ✅ JWT 认证鉴权
- ✅ CORS 跨域支持
- ✅ 全局异常处理
- ✅ 重试机制
- ✅ 集成测试

## 贡献指南

1. 代码遵循项目统一规范
2. 使用 Lombok 简化代码
3. 编写完整的单元测试
4. 添加详细的代码注释
5. 提交前运行测试确保通过

## 许可证

Copyright © 2024 Sunshine-Mall

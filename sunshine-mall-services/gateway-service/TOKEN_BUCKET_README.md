# 令牌桶限流功能使用说明

## 功能概述

令牌桶限流改为基于 Guava RateLimiter 的本地限流实现，支持全局限流和接口级限流。

## 核心特性

1. **单机限流**：每个网关实例独立限流（不依赖Redis）
2. **响应式编程**：适配Gateway的Reactor模式，非阻塞
3. **灵活配置**：支持全局限流和接口级限流
4. **白名单机制**：支持配置白名单路径，不进行限流
5. **低依赖**：仅依赖 Guava，不需要外部服务

## 配置说明

在 `application.yml` 中配置：

```yaml
rate-limit:
  token-bucket:
    enabled: true  # 是否启用令牌桶限流
    global:
      capacity: 100      # 全局限流：令牌桶容量
      refill-rate: 10    # 全局限流：每秒填充的令牌数
    path-rules:
      # 接口级限流配置（支持Ant风格路径匹配）
      /api/seckill/**:
        capacity: 50     # 秒杀接口：令牌桶容量
        refill-rate: 5   # 秒杀接口：每秒填充的令牌数
      /api/order/create:
        capacity: 20     # 创建订单接口：令牌桶容量
        refill-rate: 2   # 创建订单接口：每秒填充的令牌数
    exclude-paths:       # 白名单路径（不进行限流）
      - /actuator/**
      - /api/user/login
      - /api/user/register
```

## 工作原理

### 令牌桶算法

1. **令牌桶容量**：桶中最多存放的令牌数
2. **令牌填充速率**：每秒向桶中添加的令牌数
3. **令牌获取**：每个请求需要获取1个令牌才能通过
4. **限流规则**：
   - 如果桶中有足够的令牌，则获取令牌并放行
   - 如果桶中令牌不足，则拒绝请求（返回429状态码）

### 执行流程

1. **请求到达** → TokenBucketFilter（Order=-1）
2. **检查白名单** → 如果在白名单中，直接放行
3. **获取限流标识** → 使用IP地址作为标识
4. **匹配限流规则** → 优先使用接口级配置，否则使用全局配置
5. **调用RateLimiter** → 使用 `tryAcquire()` 立即尝试获取令牌
6. **返回结果** → 成功放行，失败返回429

## 限流标识

当前实现使用**IP地址**作为限流标识，原因：
- 限流过滤器在认证过滤器之前执行（Order=-1）
- 此时还没有用户信息，只能使用IP地址

如果需要用户级限流，可以：
1. 在认证过滤器之后添加另一个限流过滤器
2. 或者在业务层进行用户级限流

## 响应格式

当请求被限流时，返回HTTP 429状态码和以下JSON：

```json
{
    "code": 429,
    "message": "请求过于频繁，请稍后重试",
    "timestamp": 1697520000000
}
```

## 本地缓存Key格式

RateLimiter 保存在本地缓存中，Key格式为：
```
sunshine-mall:rate-limit:token-bucket:{identifier}:{capacity}:{refillRate}
```
示例：
```
sunshine-mall:rate-limit:token-bucket:ip:192.168.1.100:50:5
```

## 性能优化

1. **RateLimiter**：Guava 提供高性能本地令牌桶实现
2. **响应式编程**：非阻塞，不会阻塞线程
3. **过期时间**：本地缓存 `expireAfterAccess(1h)`，避免内存泄漏
4. **轻量依赖**：不与外部存储交互，降低延迟与复杂度

## 注意事项

1. **单机限流**：多实例部署时，每个实例独立限流。若需分布式限流，请使用Redis版本。
2. **配置调整**：根据实际业务需求调整填充速率（refill-rate）。capacity 在 Guava 中不可精确映射，当前主要用于区分不同规则的缓存Key。
3. **监控告警**：建议监控限流触发次数，及时调整配置。

## 扩展功能

### 用户级限流

如果需要支持用户级限流，可以：
1. 创建新的限流过滤器（Order=1，在认证过滤器之后）
2. 从请求头获取用户ID
3. 使用用户ID作为限流标识

### 动态配置

可以集成Nacos Config实现动态配置：
1. 将配置移到Nacos
2. 监听配置变更
3. 动态更新限流规则

### 监控指标

可以集成Prometheus等监控系统：
1. 记录限流触发次数
2. 记录限流标识分布
3. 记录各接口的限流情况

## 故障处理

### 多实例部署

本地限流在多实例下会按实例分别限流。如需全局统一限流，请改用 Redis + Lua 的分布式实现。

### 性能问题

如果限流成为性能瓶颈，可以考虑：
1. 使用Redis Cluster提高性能
2. 使用本地缓存+Redis的混合方案
3. 调整Lua脚本优化性能

## 测试建议

1. **功能测试**：验证限流规则是否生效
2. **性能测试**：验证限流对系统性能的影响
3. **压力测试**：验证高并发场景下的限流效果
4. **一致性测试**：验证多实例部署下的限流一致性与策略是否符合预期

## 参考文档

- [Spring Cloud Gateway文档](https://spring.io/projects/spring-cloud-gateway)
- [Guava RateLimiter 文档](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/util/concurrent/RateLimiter.html)
- [令牌桶算法](https://en.wikipedia.org/wiki/Token_bucket)


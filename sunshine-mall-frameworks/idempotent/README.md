# Idempotent模块使用指南

## 模块简介

Idempotent模块为sunshine-mall电商平台提供接口幂等性保证能力，基于Redis实现分布式幂等性控制，防止重复提交，确保业务操作的唯一性。

## 核心特性

1. 声明式幂等控制：通过@Idempotent注解实现，简单易用
2. 基于Redis：利用Redis的SETNX特性实现分布式幂等
3. SpEL表达式支持：灵活配置幂等键
4. 自动过期：支持自定义过期时间，避免永久占用
5. 异常自动重试：业务异常时自动删除幂等键，允许重试

## 快速开始

### 1. 添加依赖

在业务模块的pom.xml中添加：

```xml
<dependency>
    <groupId>com.xpcjsu</groupId>
    <artifactId>sunshine-mall-framework-idempotent</artifactId>
</dependency>
```

### 2. 使用注解

在需要保证幂等性的方法上添加@Idempotent注解：

```java
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    @Idempotent(key = "#orderId")
    public void createOrder(String orderId) {
        // 业务逻辑：创建订单
        // 相同orderId在60秒内只能执行一次
    }
}
```

### 3. 处理异常

当检测到重复提交时，会抛出IdempotentException：

```java
import com.xpcjsu.sunshinemall.framework.idempotent.exception.IdempotentException;

try {
    orderService.createOrder("order-123");
} catch (IdempotentException e) {
    // 处理重复提交
    return "请勿重复提交";
}
```

## 注解参数说明

### @Idempotent注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| key | String | 必填 | 幂等键表达式（SpEL） |
| prefix | String | "idempotent" | 幂等键前缀 |
| expireTime | long | 60 | 过期时间 |
| timeUnit | TimeUnit | SECONDS | 时间单位 |
| message | String | "请勿重复提交" | 重复提交提示信息 |

### SpEL表达式示例

```java
@Idempotent(key = "#orderId")
public void method1(String orderId) {}

@Idempotent(key = "#order.orderId")
public void method2(Order order) {}

@Idempotent(key = "#user.id + ':' + #action")
public void method3(User user, String action) {}

@Idempotent(key = "'fixed-key'")
public void method4() {}
```

## 使用场景

### 1. 订单创建防重

```java
@Service
public class OrderService {
    
    @Idempotent(
        key = "#request.orderId",
        message = "订单正在创建中，请勿重复提交",
        expireTime = 300
    )
    public OrderResponse createOrder(OrderRequest request) {
        // 创建订单逻辑
        Order order = new Order();
        order.setOrderId(request.getOrderId());
        // ...
        return new OrderResponse(order);
    }
}
```

### 2. 支付请求防重

```java
@Service
public class PayService {
    
    @Idempotent(
        key = "#payRequest.orderNo + ':' + #payRequest.amount",
        prefix = "pay",
        message = "支付请求处理中，请勿重复操作",
        expireTime = 600
    )
    public PayResult pay(PayRequest payRequest) {
        // 支付逻辑
        return paymentGateway.process(payRequest);
    }
}
```

### 3. 表单提交防重

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @PostMapping("/register")
    @Idempotent(
        key = "#request.mobile",
        message = "注册请求处理中，请勿重复提交",
        expireTime = 60
    )
    public Result register(@RequestBody RegisterRequest request) {
        // 用户注册逻辑
        userService.register(request);
        return Result.success();
    }
}
```

### 4. 库存扣减防重

```java
@Service
public class InventoryService {
    
    @Idempotent(
        key = "#productId + ':' + #userId",
        prefix = "inventory:deduct",
        message = "库存扣减处理中，请稍候",
        expireTime = 30
    )
    public void deductStock(Long productId, Long userId, Integer quantity) {
        // 扣减库存逻辑
        Product product = productMapper.selectById(productId);
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);
    }
}
```

### 5. 消息发送防重

```java
@Service
public class NotificationService {
    
    @Idempotent(
        key = "#userId + ':' + #type",
        prefix = "notification",
        message = "通知发送中，请勿重复操作",
        expireTime = 120,
        timeUnit = TimeUnit.SECONDS
    )
    public void sendNotification(Long userId, String type, String content) {
        // 发送通知逻辑
        messageTemplate.send(userId, type, content);
    }
}
```

## 工作原理

### 幂等性保证流程

```
1. 用户请求 → 
2. AOP拦截@Idempotent注解 → 
3. 解析SpEL表达式生成幂等键 → 
4. Redis SETNX尝试设置幂等键 → 
   ├─ 成功 → 执行业务逻辑 → 返回结果
   └─ 失败 → 抛出IdempotentException（重复提交）
```

### 幂等键格式

```
格式：{prefix}:{key值}

示例：
- idempotent:order-12345
- pay:order-12345:999.00
- inventory:deduct:product-1:user-100
```

### Redis数据结构

```
KEY: idempotent:order-12345
VALUE: "1"
TTL: 60秒（默认）
```

### 异常处理机制

当业务方法执行失败时，自动删除幂等键，允许用户重试：

```java
try {
    return joinPoint.proceed();
} catch (Throwable e) {
    cacheManager.delete(idempotentKey);
    throw e;
}
```

## 配置最佳实践

### 1. 过期时间设置

根据业务场景合理设置过期时间：

| 场景 | 推荐过期时间 | 原因 |
|------|--------------|------|
| 订单创建 | 300秒（5分钟） | 订单创建耗时较长 |
| 支付请求 | 600秒（10分钟） | 支付流程可能较慢 |
| 表单提交 | 60秒 | 快速操作 |
| 库存扣减 | 30秒 | 高并发场景，快速释放 |
| 消息发送 | 120秒 | 中等时长 |

### 2. 幂等键设计

幂等键应包含能唯一标识业务操作的信息：

```java
订单创建：orderId
支付请求：orderNo + amount
库存扣减：productId + userId
点赞操作：userId + articleId
```

### 3. 自定义前缀

不同业务使用不同前缀，便于管理和监控：

```java
@Idempotent(key = "#orderId", prefix = "order:create")
@Idempotent(key = "#orderNo", prefix = "pay:request")
@Idempotent(key = "#productId", prefix = "inventory:deduct")
```

## 性能优化

### 1. 合理设置过期时间

避免过期时间过长导致Redis内存占用：

```java
短期操作：30-60秒
中期操作：120-300秒
长期操作：最长不超过600秒
```

### 2. 幂等键简洁化

幂等键应简洁明了，避免过长：

```java
推荐：order-123
不推荐：order-2024-01-01-12-00-00-user-123-product-456
```

### 3. 监控和告警

监控幂等异常频率，及时发现恶意攻击：

```java
单用户短时间内大量IdempotentException → 可能是恶意重复提交
```

## 常见问题

### Q1: 什么时候需要使用幂等性？

需要保证操作唯一性的场景：
- 订单创建、支付请求等写操作
- 防止表单重复提交
- 防止消息重复消费
- 防止恶意刷接口

不需要幂等性的场景：
- 查询操作（天然幂等）
- 不影响数据状态的操作

### Q2: 幂等性和分布式锁有什么区别？

| 特性 | 幂等性 | 分布式锁 |
|------|--------|----------|
| 目的 | 防止重复提交 | 保证互斥访问 |
| 使用场景 | 同一请求多次提交 | 多个请求并发竞争 |
| 失败行为 | 直接拒绝 | 等待或快速失败 |
| 实现方式 | Redis SETNX | Redis SETNX + 锁续期 |

### Q3: 如何处理幂等异常？

统一异常处理器：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IdempotentException.class)
    public Result handleIdempotentException(IdempotentException e) {
        return Result.fail(ResultCode.DUPLICATE_SUBMIT, e.getMessage());
    }
}
```

### Q4: 业务执行失败后为什么要删除幂等键？

允许用户重试。如果业务因网络抖动、超时等临时性问题失败，删除幂等键后用户可以再次提交。

### Q5: 如何自定义幂等键生成逻辑？

当前版本使用SpEL表达式已足够灵活。如需复杂逻辑，可以：

方案1：在参数对象中提供计算好的幂等键
```java
public class Request {
    public String getIdempotentKey() {
        return MD5.hash(orderId + userId + timestamp);
    }
}

@Idempotent(key = "#request.idempotentKey")
```

方案2：扩展IdempotentAspect（高级）

## 设计原则

### YAGNI原则应用

只实现核心必需功能：
- 基于Redis的幂等控制
- SpEL表达式支持
- 自动过期机制
- 异常自动重试

不实现过度设计：
- 不支持多种存储策略（Redis已足够）
- 不实现复杂的幂等键生成算法（SpEL已够用）
- 不提供幂等键管理界面（待实际需求）

### 复用现有组件

使用CacheManager：
- 统一的缓存操作接口
- 支持setIfAbsent（NX模式）
- 自动序列化和异常处理

## 未来扩展

待实际需求出现时可扩展：

1. 多级幂等：本地缓存 + Redis
2. 幂等键管理：查询、删除、监控
3. 自定义存储：支持数据库、ZooKeeper
4. 幂等日志：记录重复提交行为

遵循YAGNI原则，当前功能已满足大部分业务场景。复杂功能待真正需要时再实现。

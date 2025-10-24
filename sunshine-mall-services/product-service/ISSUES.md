# Product Service - 常见问题与解决方案

本文档列举商品服务在高并发场景下可能遇到的问题及解决方案。

## 🔴 严重问题（已解决）

### 1. 超卖问题

#### 问题描述

在高并发抢购场景下，由于"检查库存"和"扣减库存"不是原子操作，可能导致超卖。

#### 问题场景

```
库存只剩1件商品，同时有3个用户下单：

时间线：
T1: 用户A 查询库存=1，检查通过 ✓
T2: 用户B 查询库存=1，检查通过 ✓
T3: 用户C 查询库存=1，检查通过 ✓
T4: 用户A 扣减库存成功，库存=0
T5: 用户B 乐观锁冲突，重试...
T6: 用户C 乐观锁冲突，重试...
T7: 用户B 某次重试成功（版本号可能已更新），库存=-1 ❌
T8: 用户C 某次重试成功，库存=-2 ❌

结果：3个订单成功，但实际只有1件商品！
```

#### 解决方案

**双重防护机制**：

##### 1. Redis 原子预扣减（第一道防线）

```java
// 先在 Redis 中原子性扣减
Long remainingStock = cacheManager.decrement(stockCacheKey, quantity);

// 如果扣减后小于0，说明库存不足
if (remainingStock < 0) {
    // 立即回滚
    cacheManager.increment(stockCacheKey, quantity);
    throw new BusinessException("库存不足");
}
```

**优势**：
- ✅ Redis decrement 是**原子操作**，不会出现并发问题
- ✅ 性能极高（内存操作）
- ✅ 即使数据库慢，也能快速拦截超卖请求

##### 2. 数据库乐观锁（第二道防线）

```sql
UPDATE product_stock 
SET available_stock = available_stock - #{quantity}, 
    version = version + 1 
WHERE sku_id = #{skuId} 
  AND available_stock >= #{quantity}  -- 再次校验库存
  AND version = #{version}
```

**优势**：
- ✅ 数据库层面最终保障
- ✅ SQL 中再次校验库存
- ✅ 版本号机制防止并发冲突

##### 3. 失败回滚机制

任何环节失败都会回滚 Redis 库存：

```java
try {
    // 业务逻辑
} catch (Exception e) {
    // 回滚 Redis 库存
    cacheManager.increment(stockCacheKey, quantity);
    throw e;
}
```

#### 改进效果

| 场景 | 改进前 | 改进后 |
|-----|-------|-------|
| 并发1000请求抢购10件 | 可能卖出15-20件 ❌ | 严格只卖10件 ✅ |
| Redis 不可用 | 无降级 | 降级到数据库校验 ✅ |
| 网络延迟 | 容易超卖 | 双重防护 ✅ |

---

### 2. 幂等性缺失

#### 问题描述

库存操作没有幂等性控制，导致重复扣减。

#### 问题场景

```
场景1：网络超时重试
用户下单 → 扣减库存 → 网络超时 → 客户端重试 → 库存被扣2次 ❌

场景2：MQ 消息重复消费
订单服务发消息 → 消费者扣减库存 → 宕机未提交offset → 
重启后再次消费 → 库存又被扣减 ❌
```

#### 解决方案

使用 `@Idempotent` 注解：

```java
@Idempotent(key = "'stock:deduct:' + #orderId + ':' + #skuId", expireTime = 300)
public boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark)
```

**原理**：
- 基于 **Redis SETNX** 原子操作
- 使用 `orderId + skuId` 作为唯一业务键
- 第一次调用成功，后续重复调用直接拦截
- 300秒后自动过期，避免内存泄漏

**防护范围**：
- ✅ 库存扣减 (`deductStock`)
- ✅ 库存预占 (`lockStock`)
- ✅ 库存释放 (`unlockStock`)
- ✅ 确认扣减 (`confirmDeduct`)
- ✅ 退货补库存 (`returnStock`)

---

## 🟡 中等问题（已解决）

### 3. 缓存与数据库不一致

#### 问题描述

库存扣减成功后，Redis 缓存未同步更新，导致查询到过期数据。

#### 解决方案

**方案1：清除缓存（当前采用）**

```java
// 扣减成功后清除缓存
clearStockCache(skuId);

// 下次查询时重新从数据库加载
```

**方案2：同步更新 Redis（可选）**

```java
// 扣减时同步更新 Redis
cacheManager.decrement(stockCacheKey, quantity);

// 增加时同步更新 Redis
cacheManager.increment(stockCacheKey, quantity);
```

**权衡**：
- 清除缓存：简单可靠，避免数据不一致
- 同步更新：性能更好，但需处理 Redis 失败场景

---

### 4. 锁定库存超时未释放

#### 问题描述

用户下单后预占库存，但长时间未支付，库存一直被锁定。

#### 当前状态

⚠️ **未完全解决**，需要订单服务配合。

#### 解决方案

**方案1：订单超时自动释放（推荐）**

订单服务通过定时任务或延时消息：

```java
// 订单创建后15分钟
if (订单未支付) {
    // 调用库存服务释放库存
    stockService.unlockStock(skuId, quantity, orderId, "订单超时");
}
```

**方案2：库存服务主动回收**

通过定时任务扫描 `stock_log` 表：

```sql
-- 查找超过30分钟的锁定记录
SELECT * FROM stock_log 
WHERE operation_type = 3  -- 预占操作
  AND create_time < NOW() - INTERVAL 30 MINUTE
  -- 且没有对应的确认或释放记录
```

---

## 🟢 轻微问题

### 5. Redis 不可用时的降级

#### 当前处理

```java
try {
    // Redis 预扣减
} catch (Exception e) {
    // 降级到数据库检查
    log.warn("Redis 不可用，降级到数据库检查");
}
```

#### 风险

Redis 不可用时，完全依赖数据库乐观锁，高并发下可能：
- 重试次数增加
- 响应时间变长
- 用户体验下降

#### 改进建议

引入**限流机制**：

```java
// 使用 Sentinel 或 Resilience4j
@RateLimiter(name = "stockDeduct", fallbackMethod = "stockDeductFallback")
public boolean deductStock(...) {
    // 业务逻辑
}

// 降级方法
public boolean stockDeductFallback(..., Throwable e) {
    return Result.failure("系统繁忙，请稍后重试");
}
```

---

### 6. 分布式事务问题

#### 场景

订单服务和库存服务是两个独立的微服务：

```
用户下单流程：
1. 订单服务创建订单 ✅
2. 库存服务扣减库存 ✅
3. 支付服务扣款 ❌ 失败

问题：订单已创建，库存已扣，但支付失败
```

#### 当前方案

使用 **RocketMQ 事务消息** 保证最终一致性：

```
订单服务：
1. 发送事务消息（Half消息）
2. 执行本地事务（创建订单）
3. Commit/Rollback 消息

库存服务：
4. 消费确认消息
5. 扣减库存
6. 失败时回滚（通过订单ID幂等性保证）
```

---

### 7. 库存数据热点问题

#### 问题描述

热门商品的库存记录成为**热点数据**，大量请求集中在同一条记录上。

#### 解决方案（未实现）

**方案1：库存分片**

将库存拆分为多个分片：

```
原来：SKU_123 库存=1000

拆分：
SKU_123_1 库存=200
SKU_123_2 库存=200
SKU_123_3 库存=200
SKU_123_4 库存=200
SKU_123_5 库存=200
```

扣减时随机选择分片，减少单点压力。

**方案2：队列削峰**

```
请求 → 队列 → 单线程消费 → 扣减库存
```

优点：串行化处理，避免并发冲突
缺点：响应时间增加

---

## 🔵 其他注意事项

### 8. 日志审计

所有库存操作都记录在 `stock_log` 表中：

```java
recordStockLog(skuId, operationType, quantity, 
               beforeStock, afterStock, orderId, remark);
```

**用途**：
- ✅ 追溯所有库存变更
- ✅ 排查超卖问题
- ✅ 对账和审计

---

### 9. 监控告警（建议）

#### 推荐监控指标

```
1. 库存扣减失败率
   - 正常：< 1%
   - 告警：> 5%

2. 乐观锁重试次数
   - 正常：< 2次
   - 告警：> 10次

3. Redis 不可用次数
   - 告警：> 0

4. 负库存记录数
   - 告警：> 0（严重问题）
```

#### 告警脚本示例

```sql
-- 查询负库存
SELECT * FROM product_stock WHERE available_stock < 0;

-- 查询频繁重试的商品
SELECT sku_id, COUNT(*) as retry_count 
FROM stock_log 
WHERE create_time > NOW() - INTERVAL 1 HOUR
GROUP BY sku_id 
HAVING retry_count > 100;
```

---

## 总结

| 问题 | 严重程度 | 状态 | 解决方案 |
|-----|---------|------|---------|
| 超卖 | 🔴 严重 | ✅ 已解决 | Redis原子预扣减 + 数据库乐观锁 |
| 幂等性 | 🔴 严重 | ✅ 已解决 | @Idempotent 注解 |
| 缓存一致性 | 🟡 中等 | ✅ 已解决 | 清除缓存策略 |
| 锁定超时 | 🟡 中等 | ⚠️ 部分 | 需订单服务配合 |
| Redis降级 | 🟢 轻微 | ✅ 已处理 | 降级到数据库 |
| 分布式事务 | 🟡 中等 | ✅ 已规划 | RocketMQ事务消息 |
| 热点数据 | 🟢 轻微 | ⚠️ 未实现 | 库存分片（可选）|

---

**最后更新**：2025-10-21

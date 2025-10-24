# 缓存经典问题解决方案

本文档详细说明缓存穿透、缓存击穿、缓存雪崩等经典问题的解决方案。

## 1. 为什么不使用 Lua 脚本？

### 当前设计考虑

**原则**：简单场景使用 Redis 原生命令，复杂场景按需引入 Lua

#### 当前方案（推荐）

```java
// Redis DECR 本身就是原子操作
Long remainingStock = cacheManager.decrement(stockCacheKey, quantity);
if (remainingStock < 0) {
    cacheManager.increment(stockCacheKey, quantity);  // 回滚
    throw new BusinessException("库存不足");
}
```

**优势**：
- ✅ Redis `DECR` 已经是**原子操作**
- ✅ 代码简洁，易于理解和维护
- ✅ 性能足够（单次 Redis 调用）
- ✅ 无需额外的 Lua 脚本管理

#### 何时使用 Lua？

**场景 1：需要同时操作多个 Key**

```lua
-- 同时检查库存和限流
local stock = redis.call('GET', KEYS[1])
local rate_limit = redis.call('GET', KEYS[2])

if tonumber(rate_limit) > max_limit then
    return -1  -- 触发限流
end

if tonumber(stock) < quantity then
    return -2  -- 库存不足
end

redis.call('DECRBY', KEYS[1], quantity)
redis.call('INCR', KEYS[2])
return 1
```

**场景 2：复杂的条件判断**

```lua
-- 带优先级的库存扣减
if user_level == 'VIP' then
    -- VIP 用户优先扣减
else
    -- 普通用户检查库存阈值
    if stock < safe_stock then
        return -1
    end
end
```

### Lua 脚本管理器

已创建 `StockDeductScript.java`，提供：
- `deductStock()` - 基础原子扣减
- `deductStockWithRateLimit()` - 带限流的扣减

**使用时机**：
- ❌ 简单 get/set/delete - 不需要 Lua
- ✅ 多条件原子判断 - 使用 Lua
- ✅ 多 Key 联合操作 - 使用 Lua

---

## 2. Redis 与 MySQL 数据不一致

### 问题场景

#### 场景 1：Redis 成功，MySQL 失败

```
T1: Redis DECR 成功（100 → 98）
T2: MySQL UPDATE 失败（乐观锁冲突）
结果：Redis=98, MySQL=100 ❌
```

#### 场景 2：MySQL 成功，Redis 失败

```
T1: MySQL UPDATE 成功（100 → 98）
T2: Redis 宕机/网络超时
结果：MySQL=98, Redis=100 ❌
```

### 解决方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|-----|------|------|-------|
| **清除缓存** | 简单可靠 | 缓存失效后首次查询慢 | ⭐⭐⭐ |
| **延迟双删** | 保证最终一致性 | 延迟时间难以确定 | ⭐⭐⭐⭐ |
| **Canal Binlog** | 彻底解耦 | 架构复杂 | ⭐⭐⭐⭐⭐ |

#### 方案 1：清除缓存（当前采用）

```java
public boolean deductStock(...) {
    // 1. Redis 预扣减
    cacheManager.decrement(stockCacheKey, quantity);
    
    // 2. MySQL 扣减
    int updated = productStockMapper.deductStock(...);
    
    if (updated > 0) {
        // 3. 清除缓存（保证下次查询从 MySQL 加载）
        clearStockCache(skuId);  ✅
        return true;
    } else {
        // 4. 失败回滚 Redis
        cacheManager.increment(stockCacheKey, quantity);
        return false;
    }
}
```

**优点**：
- ✅ 简单可靠
- ✅ 最终一致性保证
- ✅ 易于理解和维护

**缺点**：
- ⚠️ 缓存失效后首次查询较慢

---

#### 方案 2：延迟双删（推荐）⭐⭐⭐⭐

```java
public boolean deductStock(...) {
    String cacheKey = getStockCacheKey(skuId);
    
    // 1. 第一次删除缓存
    cacheManager.delete(cacheKey);
    
    // 2. 更新数据库
    int updated = productStockMapper.deductStock(...);
    
    if (updated > 0) {
        // 3. 延迟双删（异步执行）
        cacheConsistencyHelper.delayedDeleteCache(cacheKey, 500L);
        return true;
    }
}
```

**原理**：

```
时间线：
T1: 删除缓存
T2: 更新数据库（可能耗时）
T3: [其他请求可能查询并缓存旧数据]
T4: [延迟 500ms]
T5: 再次删除缓存（清除 T3 可能产生的脏缓存）
```

**延迟时间设置**：
- 应该 > 数据库主从同步时间
- 应该 > 业务逻辑执行时间
- 通常：500ms - 1000ms

**优点**：
- ✅ 更强的一致性保证
- ✅ 解决并发写问题

**缺点**：
- ⚠️ 需要异步任务支持
- ⚠️ 延迟时间难以精确确定

---

#### 方案 3：Canal 监听 Binlog（终极方案）⭐⭐⭐⭐⭐

```
架构：
MySQL Binlog → Canal → MQ → Consumer → 更新 Redis
```

**实现步骤**：

1. **安装 Canal**
```bash
docker run -d --name canal \
  -e canal.instance.master.address=mysql:3306 \
  -e canal.instance.dbUsername=canal \
  -e canal.instance.dbPassword=canal \
  canal/canal-server:latest
```

2. **监听 Binlog**
```java
@Component
public class StockBinlogListener {
    
    @RabbitListener(queues = "canal.stock.queue")
    public void onStockChange(CanalMessage message) {
        if ("UPDATE".equals(message.getType())) {
            Long skuId = message.getData().getSkuId();
            Integer newStock = message.getData().getAvailableStock();
            
            // 同步更新 Redis
            String cacheKey = "product:stock:" + skuId;
            cacheManager.set(cacheKey, newStock, 1800L);
            
            log.info("Binlog同步更新Redis - skuId: {}, stock: {}", 
                    skuId, newStock);
        }
    }
}
```

**优点**：
- ✅ **MySQL 为主**，Redis 为从
- ✅ 彻底解耦业务代码
- ✅ 最终一致性保证
- ✅ 支持多种数据订阅

**缺点**：
- ⚠️ 架构复杂度增加
- ⚠️ 需要维护额外组件
- ⚠️ 有一定延迟

---

### 当前项目建议

**阶段 1（当前）**：清除缓存 ✅
- 简单可靠
- 适合初期快速开发

**阶段 2（优化）**：延迟双删 ⭐
- 提升一致性保证
- 代码改动小

**阶段 3（长期）**：Canal Binlog 🔥
- 适合大规模生产环境
- 需要团队评估

---

## 3. 为什么不使用 DCL 双重检查锁？

### DCL 代码示例

```java
public StockDTO getStock(Long skuId) {
    StockDTO stock = cache.get(skuId);
    
    if (stock == null) {  // 第一次检查
        synchronized (this) {  // 加锁
            stock = cache.get(skuId);  // 第二次检查
            if (stock == null) {
                stock = loadFromDB(skuId);
                cache.set(skuId, stock);
            }
        }
    }
    return stock;
}
```

### DCL 在分布式系统中的问题

#### 问题 1：只能保证单机并发 ❌

```
服务器 A：                    服务器 B：
cache miss                    cache miss
  ↓                              ↓
synchronized                   synchronized
  ↓                              ↓
查询数据库                     查询数据库
  ↓                              ↓
缓存数据                       缓存数据

结果：两个服务器都查询了数据库！
```

#### 问题 2：性能不如 Redis ❌

```
DCL 方案：
- synchronized 会阻塞其他线程
- 阻塞时间 = 数据库查询时间
- 可能造成线程堆积

Redis 方案：
- 分布式锁（SETNX）
- 性能更高
- 支持超时自动释放
```

#### 问题 3：无法防止缓存穿透 ❌

```
恶意攻击：
查询不存在的 ID（如：skuId=-1）
  ↓
缓存永远 miss
  ↓
每次都查询数据库
  ↓
数据库崩溃
```

### 正确方案

#### 方案 1：空值缓存（当前已实现）✅

```java
public StockDTO getStockBySkuId(Long skuId) {
    String cacheKey = getStockCacheKey(skuId);
    String cacheValue = cacheManager.get(cacheKey, String.class);
    
    if (StringUtils.hasText(cacheValue)) {
        // ========== 检查空值缓存 ==========
        if ("NULL".equals(cacheValue)) {
            return null;  // 数据库中不存在
        }
        // ================================
        return JSONUtil.toBean(cacheValue, StockDTO.class);
    }

    // 从数据库查询
    ProductStock stock = productStockMapper.selectOne(...);
    
    if (stock == null) {
        // ========== 设置空值缓存（防止缓存穿透）==========
        cacheManager.set(cacheKey, "NULL", 60L);  // 60秒
        return null;
        // ============================================
    }
    
    // 缓存正常数据
    cacheManager.set(cacheKey, JSONUtil.toJsonStr(stock), 1800L);
    return stock;
}
```

**优点**：
- ✅ 防止缓存穿透
- ✅ 简单有效
- ✅ 适用于大部分场景

**注意**：
- ⚠️ 空值缓存时间不宜过长（60秒左右）
- ⚠️ 避免占用过多内存

---

#### 方案 2：布隆过滤器（高级方案）🔥

```java
@Component
public class StockBloomFilter {
    
    private BloomFilter<Long> bloomFilter;
    
    @PostConstruct
    public void init() {
        // 创建布隆过滤器（预计10万个SKU，误判率0.01%）
        bloomFilter = BloomFilter.create(
            Funnels.longFunnel(),
            100000,
            0.0001
        );
        
        // 加载所有有效的 SKU ID
        List<Long> skuIds = productStockMapper.selectAllSkuIds();
        skuIds.forEach(bloomFilter::put);
    }
    
    public boolean mightExist(Long skuId) {
        return bloomFilter.mightContain(skuId);
    }
}
```

**使用**：

```java
public StockDTO getStockBySkuId(Long skuId) {
    // 先用布隆过滤器判断
    if (!stockBloomFilter.mightExist(skuId)) {
        // 一定不存在，直接返回
        return null;  ✅ 避免查询数据库
    }
    
    // 可能存在，继续查询缓存和数据库
    // ... 后续逻辑
}
```

**优点**：
- ✅ 内存占用极小
- ✅ 查询速度极快（O(k)，k为哈希函数数量）
- ✅ 适合大规模数据

**缺点**：
- ⚠️ 有误判率（可能存在的，实际不存在）
- ⚠️ 不支持删除（需要定期重建）

---

## 4. 缓存三大经典问题

### 问题 1：缓存穿透

#### 定义

查询一个**数据库中不存在**的数据，导致每次请求都打到数据库。

#### 场景

```
恶意攻击者：
for (int i = -1000000; i < 0; i--) {
    getStock(i);  // 数据库中没有负数ID
}

结果：
- 缓存永远 miss
- 数据库被打爆
- 系统崩溃
```

#### 解决方案

**方案 1：空值缓存** ✅

```java
if (stock == null) {
    // 缓存空值，60秒过期
    cacheManager.set(cacheKey, "NULL", 60L);
    return null;
}
```

**方案 2：布隆过滤器** 🔥

```java
if (!bloomFilter.mightContain(skuId)) {
    return null;  // 直接拦截
}
```

**方案 3：参数校验**

```java
if (skuId == null || skuId <= 0) {
    throw new ValidationException("无效的SKU ID");
}
```

---

### 问题 2：缓存击穿

#### 定义

一个**热点 Key** 过期，瞬间大量请求同时查询数据库。

#### 场景

```
热门商品（iPhone）缓存刚好过期：

T1: 请求1 cache miss → 查询DB
T2: 请求2 cache miss → 查询DB
T3: 请求3 cache miss → 查询DB
... (1000个请求同时查询DB)

结果：数据库瞬间压力暴增
```

#### 解决方案

**方案 1：互斥锁（推荐）** ⭐⭐⭐⭐

```java
public StockDTO getStockBySkuId(Long skuId) {
    String cacheKey = getStockCacheKey(skuId);
    String lockKey = "lock:" + cacheKey;
    
    // 1. 尝试从缓存获取
    String cacheValue = cacheManager.get(cacheKey, String.class);
    if (StringUtils.hasText(cacheValue)) {
        return JSONUtil.toBean(cacheValue, StockDTO.class);
    }
    
    // 2. 缓存未命中，尝试获取分布式锁
    boolean locked = cacheManager.setIfAbsent(lockKey, "1", 10L);
    
    if (locked) {
        try {
            // 获取锁成功，查询数据库
            ProductStock stock = productStockMapper.selectOne(...);
            
            // 缓存数据
            if (stock != null) {
                cacheManager.set(cacheKey, JSONUtil.toJsonStr(stock), 1800L);
                return BeanUtil.copyProperties(stock, StockDTO.class);
            }
        } finally {
            // 释放锁
            cacheManager.delete(lockKey);
        }
    } else {
        // 未获取到锁，等待一下再重试
        Thread.sleep(50);
        return getStockBySkuId(skuId);  // 递归重试
    }
    
    return null;
}
```

**方案 2：热点数据永不过期**

```java
// 设置非常长的过期时间
cacheManager.set(cacheKey, data, 86400L * 365);  // 1年

// 或者通过后台任务定期刷新
@Scheduled(cron = "0 */10 * * * ?")  // 每10分钟
public void refreshHotKeys() {
    List<String> hotKeys = getHotKeys();
    for (String key : hotKeys) {
        refreshCache(key);
    }
}
```

**方案 3：逻辑过期**

```java
class CacheData {
    Object data;
    LocalDateTime expireTime;  // 逻辑过期时间
}

// 查询时判断
CacheData cacheData = cache.get(key);
if (cacheData.expireTime.isBefore(LocalDateTime.now())) {
    // 已"过期"，异步刷新
    asyncRefresh(key);
}
return cacheData.data;  // 仍然返回旧数据
```

---

### 问题 3：缓存雪崩

#### 定义

**大量缓存同时过期**，导致数据库压力骤增。

#### 场景

```
系统重启：
T1: 系统启动，所有缓存为空
T2: 用户请求涌入
T3: 所有请求都查询数据库
结果：数据库崩溃

定时任务：
T1: 0点整，1万个缓存同时过期
T2: 0点01秒，大量请求打到数据库
结果：数据库压力暴增
```

#### 解决方案

**方案 1：随机过期时间** ✅

```java
// 不要这样（所有缓存1小时后同时过期）
cacheManager.set(key, data, 3600L);

// 应该这样（过期时间在 1小时 ± 5分钟之间随机）
long baseExpire = 3600L;
long randomExpire = baseExpire + ThreadLocalRandom.current().nextLong(-300, 300);
cacheManager.set(key, data, randomExpire);  ✅
```

**方案 2：缓存预热**

```java
@PostConstruct
public void warmUpCache() {
    log.info("开始缓存预热...");
    
    // 预加载热点商品
    List<Long> hotSkuIds = getHotSkuIds();
    for (Long skuId : hotSkuIds) {
        ProductStock stock = productStockMapper.selectOne(...);
        if (stock != null) {
            cacheStockInfo(skuId, stock);
        }
    }
    
    log.info("缓存预热完成，共预热 {} 个商品", hotSkuIds.size());
}
```

**方案 3：多级缓存**

```
L1: 本地缓存（Caffeine）
  ↓ miss
L2: Redis
  ↓ miss
L3: 数据库

优点：
- L1 缓存永远不会全部失效
- 减少 Redis 压力
- 更快的响应速度
```

**方案 4：限流降级**

```java
@RateLimiter(name = "getStock", fallbackMethod = "getStockFallback")
public StockDTO getStockBySkuId(Long skuId) {
    // 正常逻辑
}

public StockDTO getStockFallback(Long skuId, Throwable e) {
    // 降级逻辑：返回默认值或从数据库查询
    log.warn("触发限流降级 - skuId: {}", skuId);
    return getStockFromDB(skuId);
}
```

---

## 5. 完整解决方案总结

| 问题 | 核心原因 | 推荐方案 | 状态 |
|-----|---------|---------|------|
| **缓存穿透** | 查询不存在的数据 | 空值缓存 + 布隆过滤器 | ✅ 已实现 |
| **缓存击穿** | 热点Key过期 | 互斥锁 + 热点数据永不过期 | ⚠️ 建议添加 |
| **缓存雪崩** | 大量Key同时过期 | 随机过期时间 + 缓存预热 | ⚠️ 建议添加 |
| **数据不一致** | Redis与MySQL不同步 | 延迟双删 / Canal | ✅ 已实现 |

---

## 6. 实现建议

### 立即实现（P0）

1. ✅ **空值缓存** - 防止缓存穿透
2. ✅ **清除缓存** - 保证数据一致性
3. ⚠️ **随机过期时间** - 防止缓存雪崩

### 近期优化（P1）

1. ⚠️ **延迟双删** - 提升一致性保证
2. ⚠️ **互斥锁** - 防止缓存击穿
3. ⚠️ **布隆过滤器** - 增强穿透防护

### 长期规划（P2）

1. 📝 **Canal Binlog** - 彻底解决一致性
2. 📝 **多级缓存** - 提升性能和可用性
3. 📝 **监控告警** - 及时发现问题

---

**最后更新**：2025-10-21

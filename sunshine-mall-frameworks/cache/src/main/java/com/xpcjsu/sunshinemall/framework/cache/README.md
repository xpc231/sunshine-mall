# Cache模块使用指南

## 模块简介

Cache模块为sunshine-mall电商平台提供统一的缓存管理能力，封装Redis操作，提供简洁的API和完善的缓存策略。

## 核心组件

### 1. CacheManager - 缓存管理器

统一的Redis缓存操作接口，提供常用的缓存操作方法。

**核心功能：**
- 基础缓存操作：set、get、delete
- 批量操作：批量获取、批量删除
- 计数器：increment、decrement
- 缓存穿透防护：空值缓存
- 幂等性控制：setIfAbsent（SETNX）

**使用示例：**

```java
@Autowired
private CacheManager cacheManager;

// 基础操作
cacheManager.set("user:1", user);                    // 使用默认过期时间（30分钟）
cacheManager.set("user:1", user, 3600);              // 指定过期时间（秒）
User user = cacheManager.get("user:1", User.class);  // 获取并转换类型
cacheManager.delete("user:1");                       // 删除缓存

// 批量操作
List<String> keys = Arrays.asList("user:1", "user:2", "user:3");
List<Object> values = cacheManager.multiGet(keys);   // 批量获取
Long count = cacheManager.delete(keys);              // 批量删除

// 计数器操作
cacheManager.increment("counter:view", 1);           // 自增
cacheManager.decrement("counter:stock", 5);          // 自减

// 防缓存穿透
cacheManager.setNullValue("product:999");            // 设置空值缓存（5分钟）

// 幂等性控制
Boolean success = cacheManager.setIfAbsent("idempotent:order:123", "1", 60);  // SETNX
Boolean exists = cacheManager.hasKey("user:1");     // 检查key是否存在
```

### 2. CacheKeyBuilder - 缓存键构建工具

提供统一的缓存键构建规则，确保键名规范和可维护性。

**键格式规范：**
```
sunshine-mall:module:business:id
```

**使用示例：**

```java
// 基础构建
String key = CacheKeyBuilder.build("user", "info", userId);
// 结果：sunshine-mall:user:info:123

// 模块化构建
String key = CacheKeyBuilder.build("product", "detail", productId);
// 结果：sunshine-mall:product:detail:456

// 模式匹配（用于批量操作）
String pattern = CacheKeyBuilder.buildPattern("order", "list");
// 结果：sunshine-mall:order:list:*
```

### 3. RedisConfig - Redis配置类

配置Redis序列化方式，使用Jackson JSON实现高性能序列化。

**配置特性：**
- Key使用String序列化
- Value使用Jackson JSON序列化
- 支持任意Java对象
- 自动处理泛型

## 配置说明

### application.yml配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8      # 最大连接数
          max-idle: 8        # 最大空闲连接
          min-idle: 0        # 最小空闲连接
          max-wait: -1ms     # 最大等待时间
```

### 缓存常量配置

在 `CacheConstant` 中可配置：

```java
// 缓存键前缀
public static final String CACHE_KEY_PREFIX = "sunshine-mall";

// 默认过期时间（秒）：30分钟
public static final long DEFAULT_EXPIRE_TIME = 1800L;

// 空值缓存过期时间（秒）：5分钟
public static final long NULL_VALUE_EXPIRE_TIME = 300L;

// 永久缓存标识
public static final long PERMANENT_EXPIRE_TIME = -1L;
```

## 常见使用场景

### 1. 商品详情缓存

```java
@Service
public class ProductService {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private ProductMapper productMapper;
    
    public Product getProductDetail(Long productId) {
        // 构建缓存键
        String cacheKey = CacheKeyBuilder.build("product", "detail", productId);
        
        // 先查缓存
        Product product = cacheManager.get(cacheKey, Product.class);
        if (product != null) {
            return product;
        }
        
        // 查询数据库
        product = productMapper.selectById(productId);
        
        if (product != null) {
            // 设置缓存，1小时过期
            cacheManager.set(cacheKey, product, 3600);
        } else {
            // 设置空值缓存，防止缓存穿透
            cacheManager.setNullValue(cacheKey);
        }
        
        return product;
    }
}
```

### 2. 用户会话缓存

```java
@Service
public class UserSessionService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public void saveSession(String token, UserSession session) {
        String cacheKey = CacheKeyBuilder.build("session", "user", token);
        // 会话缓存30分钟
        cacheManager.set(cacheKey, session, 1800);
    }
    
    public UserSession getSession(String token) {
        String cacheKey = CacheKeyBuilder.build("session", "user", token);
        return cacheManager.get(cacheKey, UserSession.class);
    }
    
    public void removeSession(String token) {
        String cacheKey = CacheKeyBuilder.build("session", "user", token);
        cacheManager.delete(cacheKey);
    }
}
```

### 3. 计数器场景

```java
@Service
public class ViewCountService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public void incrementViewCount(Long productId) {
        String cacheKey = CacheKeyBuilder.build("product", "viewCount", productId);
        cacheManager.increment(cacheKey, 1);
    }
    
    public Long getViewCount(Long productId) {
        String cacheKey = CacheKeyBuilder.build("product", "viewCount", productId);
        Object count = cacheManager.get(cacheKey);
        return count != null ? Long.valueOf(count.toString()) : 0L;
    }
}
```

### 4. 幂等性控制场景

```java
@Service
public class IdempotentService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public void createOrder(String orderId) {
        String idempotentKey = "idempotent:order:" + orderId;
        
        // 尝试设置幂等键（仅当key不存在时）
        Boolean success = cacheManager.setIfAbsent(idempotentKey, "1", 60);
        
        if (!success) {
            throw new RuntimeException("请勿重复提交");
        }
        
        try {
            // 执行业务逻辑
            orderService.create(orderId);
        } catch (Exception e) {
            // 业务失败，删除幂等键，允许重试
            cacheManager.delete(idempotentKey);
            throw e;
        }
    }
}
```

### 5. 库存扣减场景

```java
@Service
public class StockService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public boolean deductStock(Long productId, Integer quantity) {
        String cacheKey = CacheKeyBuilder.build("product", "stock", productId);
        
        // 扣减库存
        Long remaining = cacheManager.decrement(cacheKey, quantity);
        
        // 库存不足
        if (remaining < 0) {
            // 回滚
            cacheManager.increment(cacheKey, quantity);
            return false;
        }
        
        return true;
    }
}
```

## 缓存策略

### 1. 过期时间设置

**热点数据：** 1-2小时
```java
cacheManager.set(key, value, 3600);  // 1小时
```

**普通数据：** 30分钟
```java
cacheManager.set(key, value);  // 使用默认过期时间
```

**会话数据：** 30分钟-1小时
```java
cacheManager.set(key, session, 1800);  // 30分钟
```

**永久数据：** 配置类数据
```java
cacheManager.set(key, config, CacheConstant.PERMANENT_EXPIRE_TIME);
```

### 2. 缓存穿透防护

对于查询结果为空的情况，设置空值缓存：

```java
Product product = productMapper.selectById(productId);
if (product == null) {
    cacheManager.setNullValue(cacheKey);  // 5分钟过期
    return null;
}
```

### 3. 缓存更新策略

**旁路缓存模式（Cache-Aside）：**

```java
// 更新数据
public void updateProduct(Product product) {
    // 1. 先更新数据库
    productMapper.updateById(product);
    
    // 2. 再删除缓存
    String cacheKey = CacheKeyBuilder.build("product", "detail", product.getId());
    cacheManager.delete(cacheKey);
}

// 查询数据
public Product getProduct(Long productId) {
    String cacheKey = CacheKeyBuilder.build("product", "detail", productId);
    
    // 1. 先查缓存
    Product product = cacheManager.get(cacheKey, Product.class);
    if (product != null) {
        return product;
    }
    
    // 2. 查数据库
    product = productMapper.selectById(productId);
    
    // 3. 写缓存
    if (product != null) {
        cacheManager.set(cacheKey, product, 3600);
    }
    
    return product;
}
```

## 最佳实践

### 1. 统一使用CacheKeyBuilder

避免硬编码缓存键，统一使用CacheKeyBuilder构建：

```java
// ❌ 不推荐
String key = "user:info:" + userId;

// ✅ 推荐
String key = CacheKeyBuilder.build("user", "info", userId);
```

### 2. 合理设置过期时间

根据业务特性设置合理的过期时间，避免缓存雪崩：

```java
// 添加随机过期时间，避免大量缓存同时失效
long expireTime = 3600 + ThreadLocalRandom.current().nextInt(300);
cacheManager.set(key, value, expireTime);
```

### 3. 类型安全

使用泛型方法获取缓存，避免类型转换异常：

```java
// ✅ 推荐：自动类型转换
Product product = cacheManager.get(key, Product.class);

// ❌ 不推荐：手动类型转换
Product product = (Product) cacheManager.get(key);
```

### 4. 异常处理策略

**CacheManager 的异常处理原则**：

**写操作失败抛异常**（影响数据一致性）
```java
try {
    cacheManager.set(key, value);        // 设置失败抛异常
    cacheManager.delete(key);            // 删除失败抛异常
    cacheManager.increment(key, 1);      // 自增失败抛异常
} catch (RuntimeException e) {
    // 必须处理异常，避免数据不一致
    log.error("缓存操作失败", e);
    // 根据业务决定是否重试或回滚
}
```

**读操作失败返回null**（不影响数据一致性）
```java
// 获取失败返回null，可降级到数据库查询
Product product = cacheManager.get(key, Product.class);
if (product == null) {
    product = productMapper.selectById(id);
    if (product != null) {
        cacheManager.set(key, product);
    }
}
```

**为什么这样设计？**
1. **写操作失败抛异常**：
   - `set` 失败可能导致DB与缓存不一致
   - `delete` 失败可能导致脏数据残留
   - `increment/decrement` 失败影响业务逻辑（如库存扣减）
   - 调用者必须知道操作失败并处理

2. **读操作失败返回null**：
   - `get` 失败不影响数据状态
   - 业务可当作缓存未命中处理
   - 降级到数据库查询，保证服务可用

### 5. 高级操作

如需Redis高级功能（如Hash、Set、Sorted Set），直接注入RedisTemplate：

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// 使用Hash操作
redisTemplate.opsForHash().put(key, field, value);

// 使用Set操作
redisTemplate.opsForSet().add(key, values);
```

## 性能优化建议

### 1. 批量操作

对于多个缓存操作，使用批量接口：

```java
// ✅ 推荐：批量获取
List<Object> values = cacheManager.multiGet(keys);

// ❌ 不推荐：循环单个获取
for (String key : keys) {
    Object value = cacheManager.get(key);
}
```

### 2. 减少网络开销

合理使用本地缓存（Caffeine）配合Redis：

```java
// 热点数据先查本地缓存，再查Redis
// 后续版本将提供多级缓存支持
```

### 3. 避免大Value

单个缓存值不要超过10KB，超大对象考虑分片存储：

```java
// 大对象分片存储
List<String> chunks = splitLargeObject(largeObject);
for (int i = 0; i < chunks.size(); i++) {
    String key = CacheKeyBuilder.build("large", "chunk", id + ":" + i);
    cacheManager.set(key, chunks.get(i));
}
```

## 依赖说明

### Maven依赖

```xml
<dependencies>
    <!-- 1. Common模块（必需，提供StringUtils等工具类） -->
    <dependency>
        <groupId>com.xpcjsu</groupId>
        <artifactId>sunshine-mall-framework-common</artifactId>
    </dependency>
    
    <!-- 2. Spring Data Redis（必需，内置Jackson序列化支持） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- 3. Redisson（可选，后续分布式锁功能） -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- 4. Caffeine（可选，后续多级缓存功能） -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

### 重要说明

1. **Jackson序列化器**：
   - Spring Data Redis默认集成了Jackson序列化器
   - 使用 `GenericJackson2JsonRedisSerializer` 处理泛型
   - 无需额外引入Jackson依赖

2. **Common模块依赖**：
   - 使用 `StringUtils.isBlank()` 方法
   - 使用Common模块的工具类，而不是Spring自带的

3. **版本管理**：
   - 版本号由父项目 `sunshine-mall-dependencies` 统一管理
   - 不需要在pom.xml中指定版本号

## 后续扩展

待实际需求出现时，可扩展以下功能：

- **多级缓存**：Caffeine本地缓存 + Redis分布式缓存
- **分布式锁**：基于Redisson实现
- **缓存注解**：@Cacheable、@CacheEvict等Spring Cache注解支持
- **布隆过滤器**：更强的缓存穿透防护
- **缓存监控**：缓存命中率统计、慢查询监控

---

**遵循YAGNI原则，当前功能已满足大部分业务场景。复杂功能待真正需要时再实现。**

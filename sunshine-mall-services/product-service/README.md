# Product Service - 商品服务

## 项目概述

商品服务是阳光商城(sunshine-mall)的核心业务模块之一，提供商品管理、分类管理、SKU管理和库存管理等功能。

## 技术栈

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Cloud 2022.0.4**
- **MyBatis-Plus 3.5.3.2** - 数据持久化
- **MySQL 8.0** - 数据存储
- **Redis 7.0** - 缓存支持
- **RocketMQ 5.1** - 消息队列
- **Nacos 2.2.3** - 服务注册与配置中心

## 核心功能模块

### 1. 商品分类管理

支持三级分类树形结构管理：

- ✅ 创建分类
- ✅ 更新分类
- ✅ 删除分类（级联检查）
- ✅ 获取分类树（支持启用/全部）
- ✅ 缓存优化（分类树缓存2小时）

**接口列表**：
```
POST   /api/category              # 创建分类
PUT    /api/category/{id}         # 更新分类
DELETE /api/category/{id}         # 删除分类
GET    /api/category/{id}         # 获取分类详情
GET    /api/category/tree         # 获取分类树（仅启用）
GET    /api/category/tree/all     # 获取分类树（所有）
GET    /api/category/list/{parentId}  # 获取子分类列表
```

### 2. 商品基础信息管理

提供商品的完整生命周期管理：

- ✅ 创建商品
- ✅ 更新商品
- ✅ 删除商品
- ✅ 查询商品详情
- ✅ 分页查询商品列表
- ✅ 商品上下架
- ✅ 浏览次数统计

**接口列表**：
```
POST   /api/product              # 创建商品
PUT    /api/product/{id}         # 更新商品
DELETE /api/product/{id}         # 删除商品
GET    /api/product/{id}         # 获取商品详情
GET    /api/product/page         # 分页查询商品
PUT    /api/product/{id}/on-shelf    # 上架商品
PUT    /api/product/{id}/off-shelf   # 下架商品
```

### 3. 商品SKU管理

支持多规格商品管理：

- ✅ 创建SKU
- ✅ 批量创建SKU
- ✅ 更新SKU
- ✅ 删除SKU
- ✅ 查询SKU详情
- ✅ 查询商品SKU列表

**接口列表**：
```
POST   /api/sku                  # 创建SKU
POST   /api/sku/batch            # 批量创建SKU
PUT    /api/sku/{id}             # 更新SKU
DELETE /api/sku/{id}             # 删除SKU
GET    /api/sku/{id}             # 获取SKU详情
GET    /api/sku/product/{productId}  # 获取商品SKU列表
GET    /api/sku/code/{skuCode}   # 根据编码获取SKU
```

### 4. 库存管理

提供完善的库存管理机制：

- ✅ 初始化库存
- ✅ 增加库存
- ✅ 扣减库存（乐观锁+重试）
- ✅ 预占库存（订单冻结库存）
- ✅ 释放库存（取消订单）
- ✅ 确认扣减（支付成功）
- ✅ 退货补库存
- ✅ 库存操作日志

**接口列表**：
```
POST   /api/stock/init           # 初始化库存
POST   /api/stock/add            # 增加库存
POST   /api/stock/deduct         # 扣减库存
POST   /api/stock/lock           # 预占库存
POST   /api/stock/unlock         # 释放库存
POST   /api/stock/confirm-deduct # 确认扣减
POST   /api/stock/return         # 退货补库存
GET    /api/stock/{skuId}        # 获取库存信息
GET    /api/stock/check          # 检查库存是否充足
```

## 数据库设计

### 表结构

1. **category** - 商品分类表
   - 支持三级分类
   - 树形结构存储
   - 支持排序

2. **product** - 商品基础信息表
   - 存储商品基本信息
   - 关联分类
   - 统计销量和浏览次数

3. **product_sku** - 商品SKU表
   - 存储规格信息（JSON格式）
   - 价格管理（销售价、原价、成本价）
   - 重量信息

4. **product_stock** - 商品库存表
   - 总库存、可用库存、锁定库存
   - 乐观锁版本号
   - 唯一约束（SKU ID）

5. **stock_log** - 库存操作日志表
   - 记录所有库存变更
   - 支持追溯
   - 关联订单

### SQL脚本

数据库初始化脚本：`sql/product_service_schema.sql`

## 技术亮点

### 1. 分布式ID生成

复用 `SnowflakeIdGenerator` 生成全局唯一ID：
- 商品ID
- SKU ID
- 分类ID
- 库存日志ID

### 2. 缓存策略

使用 `CacheManager` 实现多级缓存：

| 缓存类型 | 缓存键 | 过期时间 |
|---------|--------|---------|
| 商品详情 | product:detail:{id} | 1小时 |
| SKU详情 | product:sku:{id} | 1小时 |
| 分类树 | product:category:tree | 2小时 |
| 库存信息 | product:stock:{skuId} | 30分钟 |

**缓存更新策略**：
- 数据变更时主动清除缓存
- 查询时重建缓存
- 使用 Redis decrement 同步库存

### 3. 库存管理

#### 防超卖机制（双重防护）

**第一道防线：Redis 原子预扣减**

```java
// 在数据库操作前，先在 Redis 中原子性扣减
Long remainingStock = cacheManager.decrement(stockCacheKey, quantity);

if (remainingStock < 0) {
    // 立即回滚
    cacheManager.increment(stockCacheKey, quantity);
    throw new BusinessException("库存不足");
}
```

**优势**：
- ✅ Redis decrement 是**原子操作**，不会并发冲突
- ✅ 性能极高（内存操作）
- ✅ 即使数据库慢，也能快速拦截超卖请求

**第二道防线：数据库乐观锁**

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

**失败回滚**：

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

**效果对比**：

| 场景 | 改进前 | 改进后 |
|-----|-------|-------|
| 并发1000请求抢购10件 | 可能占15-20件 ❌ | 严格只占10件 ✅ |
| Redis 不可用 | 无降级 | 降级到数据库校验 ✅ |
| 网络延迟 | 容易超卖 | 双重防护 ✅ |

#### 乐观锁机制

使用版本号实现乐观锁，确保库存扣减的并发安全：

```sql
UPDATE product_stock 
SET available_stock = available_stock - #{quantity}, 
    version = version + 1 
WHERE sku_id = #{skuId} 
  AND available_stock >= #{quantity} 
  AND version = #{version}
```

#### 幂等性控制

**关键改进**：所有涉及订单的库存操作都使用幂等性控制，防止重复执行：

```java
@Idempotent(key = "'stock:deduct:' + #orderId + ':' + #skuId", expireTime = 300)
public boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark)
```

**幂等性保护的操作**：
- ✅ 库存扣减 - 防止重复扣减
- ✅ 库存预占 - 防止重复锁定
- ✅ 库存释放 - 防止重复释放
- ✅ 确认扣减 - 防止重复确认
- ✅ 退货补库存 - 防止重复退货

**幂等键设计**：
```
stock:deduct:{orderId}:{skuId}   # 扣减操作
stock:lock:{orderId}:{skuId}     # 预占操作
stock:unlock:{orderId}:{skuId}   # 释放操作
stock:confirm:{orderId}:{skuId}  # 确认操作
stock:return:{orderId}:{skuId}   # 退货操作
```

**过期时间**：300秒（5分钟）
- 足够长以覆盖正常的重试时间窗口
- 避免长期占用内存

#### 重试机制

库存操作失败时自动重试（最多3次），提高成功率。

#### 库存预占流程

1. **下单时**：锁定库存（available_stock → locked_stock）
2. **支付成功**：确认扣减（locked_stock → 扣减）
3. **取消订单**：释放库存（locked_stock → available_stock）

### 4. 消息通知

使用 RocketMQ 发送库存变更消息：

**主题**：`stock-change-topic`

**消息内容**：
```json
{
  "skuId": 123456,
  "operationType": 2,
  "quantity": 10,
  "beforeStock": 100,
  "afterStock": 90,
  "orderId": 789,
  "remark": "订单扣减",
  "timestamp": 1698765432000
}
```

**操作类型**：
- 1-入库
- 2-扣减
- 3-预占
- 4-释放
- 5-退货

## 配置说明

### 应用配置

`application.yml`:


### Nacos配置

服务注册与发现：
- **地址**：127.0.0.1:8848
- **命名空间**：dev
- **分组**：SUNSHINE_MALL_GROUP

## 启动步骤

### 1. 环境准备

- JDK 17
- MySQL 8.0
- Redis 7.0
- RocketMQ 5.1
- Nacos 2.2.3

### 2. 数据库初始化

```bash
mysql -u root -p < sql/product_service_schema.sql
```

### 3. 启动服务

```bash
# 编译
mvnw clean compile -pl sunshine-mall-services/product-service -am

# 运行
mvnw spring-boot:run -pl sunshine-mall-services/product-service
```

## 测试示例

### 创建商品分类

```bash
curl -X POST http://localhost:8082/api/category \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": 0,
    "name": "电子产品",
    "sortOrder": 1
  }'
```

### 创建商品

```bash
curl -X POST http://localhost:8082/api/product \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 1,
    "productName": "iPhone 15 Pro",
    "mainImage": "http://example.com/iphone15.jpg",
    "detail": "最新款iPhone"
  }'
```

### 创建SKU并初始化库存

```bash
# 1. 创建SKU
curl -X POST http://localhost:8082/api/sku \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "skuName": "iPhone 15 Pro 256GB 深空黑",
    "specMap": {"颜色":"深空黑","容量":"256GB"},
    "price": 8999.00
  }'

# 2. 初始化库存
curl -X POST "http://localhost:8082/api/stock/init?skuId=1&quantity=100"
```

### 预占库存（下单）

```bash
curl -X POST "http://localhost:8082/api/stock/lock?skuId=1&quantity=2&orderId=10001&remark=订单下单"
```

### 确认扣减（支付成功）

```bash
curl -X POST "http://localhost:8082/api/stock/confirm-deduct?skuId=1&quantity=2&orderId=10001&remark=订单支付成功"
```

## 框架模块复用

### 1. Base 模块

- ✅ `BusinessException` - 业务异常
- ✅ `ValidationException` - 参数校验异常
- ✅ `SystemException` - 系统异常

### 2. Convention 模块

- ✅ `Result` - 统一响应封装
- ✅ `GlobalExceptionHandler` - 全局异常处理

### 3. Database 模块

- ✅ MyBatis-Plus 配置
- ✅ 逻辑删除支持

### 4. Cache 模块

- ✅ `CacheManager` - 缓存管理器
  - get/set - 基础缓存操作
  - decrement - 库存同步
  - delete - 缓存清除

### 5. DistributedID 模块

- ✅ `SnowflakeIdGenerator` - 分布式ID生成器

### 5. Idempotent 模块

- ✅ `@Idempotent` - 幂等性控制注解
  - 防止库存重复扣减
  - SpEL 表达式生成业务键
  - 基于 Redis SETNX 原子操作
  - 自动过期时间管理

## 未来扩展

### ElasticSearch 集成（第二阶段）

商品搜索功能规划：

1. **全文检索**
   - 商品名称搜索
   - 关键词高亮
   - 模糊匹配

2. **多维度筛选**
   - 分类筛选
   - 价格区间
   - 品牌筛选

3. **排序支持**
   - 综合排序
   - 价格排序
   - 销量排序
   - 时间排序

4. **搜索建议**
   - 自动补全
   - 热门搜索词

## 注意事项

1. **库存安全**
   - 所有库存操作都使用乐观锁
   - 库存变更会记录操作日志
   - 支持库存追溯

2. **缓存一致性**
   - 数据变更时主动清除缓存
   - 避免缓存穿透（空值缓存）

3. **分布式ID**
   - 商品编码和SKU编码使用分布式ID生成
   - 确保全局唯一性

4. **消息可靠性**
   - 库存变更消息发送失败不影响主流程
   - 消息异步发送

## 作者

xpcjsu

## 许可证

MIT License

---

**最后更新时间**：2025-10-21

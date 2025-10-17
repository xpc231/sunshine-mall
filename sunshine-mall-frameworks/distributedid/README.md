# DistributedID模块使用指南

## 模块简介

DistributedID模块为sunshine-mall电商平台提供分布式唯一ID生成能力，基于Twitter的Snowflake（雪花）算法实现，支持高并发场景下的全局唯一ID生成。

## 核心特性

### 雪花算法优势

1. **全局唯一**：支持分布式环境，跨机器、跨数据中心生成唯一ID
2. **趋势递增**：ID按时间递增，有利于MySQL索引性能（B+树插入效率高）
3. **高性能**：本地生成，无需网络调用，单机每秒可生成400万+ID
4. **信息可读**：ID包含时间戳、机器ID等信息，便于调试和分析
5. **无依赖**：纯Java实现，无外部依赖

### ID结构（64位）

```
┌─────────┬──────────────┬─────────────┬──────────────┐
│ 1位符号 │ 41位时间戳   │ 10位机器ID  │ 12位序列号   │
│   0     │ 毫秒级时间戳 │ DC(5)+WK(5) │ 0-4095       │
└─────────┴──────────────┴─────────────┴──────────────┘

- 符号位：1位，固定为0，保证ID为正数
- 时间戳：41位，精确到毫秒，可使用约69年
- 数据中心ID：5位，支持32个数据中心（0-31）
- 工作机器ID：5位，每个数据中心支持32台机器（0-31）
- 序列号：12位，单台机器每毫秒可生成4096个ID
```

**总容量**：
- 支持1024台机器（32个数据中心 × 32台机器）
- 单机每秒可生成409.6万个ID（4096 × 1000）
- 支持到2093年（从2024年起算）

## 快速开始

### 1. 添加依赖

在业务模块的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.xpcjsu</groupId>
    <artifactId>sunshine-mall-framework-distributedid</artifactId>
</dependency>
```

### 2. 配置参数

在 `application.yml` 中配置机器标识：

```yaml
# 分布式ID配置
distributedid:
  datacenter-id: 1  # 数据中心ID（0-31）
  worker-id: 1      # 工作机器ID（0-31）
```

**配置说明**：
- `datacenter-id`：数据中心ID，不同机房使用不同值
- `worker-id`：工作机器ID，同一机房内不同机器使用不同值
- 两个参数组合确保全局唯一，范围都是0-31
- 未配置时默认都为0（适合单机开发测试）

### 3. 使用方式

#### 方式一：Spring Bean注入（推荐）

```java
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final SnowflakeIdGenerator idGenerator;
    
    public OrderService(SnowflakeIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }
    
    public void createOrder() {
        Long orderId = idGenerator.nextId();
        System.out.println("订单ID: " + orderId);
    }
}
```

#### 方式二：单例模式获取

```java
import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;

public class IdUtils {
    
    public static Long generateId() {
        SnowflakeIdGenerator generator = 
            SingletonHolder.getInstance(SnowflakeIdGenerator.class);
        return generator.nextId();
    }
}
```

## 核心API

### SnowflakeIdGenerator

**主要方法**：

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `nextId()` | 生成下一个唯一ID | Long |
| `parseId(long id)` | 解析ID信息（调试用） | String |
| `getWorkerId()` | 获取工作机器ID | long |
| `getDatacenterId()` | 获取数据中心ID | long |

**使用示例**：

```java
// 生成ID
Long id = generator.nextId();

// 解析ID（调试用）
String info = generator.parseId(id);
System.out.println(info);
// 输出：ID: 123456789, Timestamp: 1704067200000, DatacenterId: 1, WorkerId: 1, Sequence: 0

// 查看配置
long workerId = generator.getWorkerId();
long datacenterId = generator.getDatacenterId();
```

## 使用场景

### 1. 订单ID生成

```java
@Service
public class OrderService {
    
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setOrderId(idGenerator.nextId());
        order.setUserId(request.getUserId());
        // ...其他业务逻辑
        return orderMapper.insert(order);
    }
}
```

### 2. 商品ID生成

```java
@Service
public class ProductService {
    
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setProductId(idGenerator.nextId());
        product.setProductName(request.getName());
        // ...其他业务逻辑
        return productMapper.insert(product);
    }
}
```

### 3. 分布式消息ID

```java
@Component
public class MessageProducer {
    
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    
    public void sendMessage(String content) {
        Message message = new Message();
        message.setMessageId(idGenerator.nextId());
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());
        
        kafkaTemplate.send("topic", message);
    }
}
```

## 配置最佳实践

### 单机环境（开发/测试）

```yaml
# 使用默认配置即可，无需配置
# datacenter-id和worker-id默认都为0
```

### 多机房部署

```yaml
# 机房A - 服务器1
distributedid:
  datacenter-id: 0
  worker-id: 0

# 机房A - 服务器2
distributedid:
  datacenter-id: 0
  worker-id: 1

# 机房B - 服务器1
distributedid:
  datacenter-id: 1
  worker-id: 0
```

### Kubernetes部署

可以通过环境变量或ConfigMap配置：

```yaml
# ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  DISTRIBUTEDID_DATACENTER_ID: "1"
  DISTRIBUTEDID_WORKER_ID: "${POD_INDEX}"  # 使用Pod序号
```

## 性能指标

### 基准测试

- **单线程性能**：100,000个ID耗时约100ms，平均1μs/ID
- **并发性能**：10线程并发，每线程1000个ID，无重复
- **内存占用**：极小（约1KB）

### 性能对比

| 方案 | QPS | 优势 | 劣势 |
|------|-----|------|------|
| 雪花算法 | 400万+ | 本地生成，无网络开销 | 需配置机器ID |
| 数据库自增 | 1000-5000 | 简单 | 性能瓶颈，单点故障 |
| UUID | 100万+ | 无需配置 | 无序，索引性能差 |
| Redis自增 | 10万+ | 集中管理 | 依赖Redis，网络开销 |

## 常见问题

### Q1: 时钟回拨怎么办？

**问题**：服务器时间被调回，可能导致ID重复。

**解决方案**：
1. 系统会抛出异常，拒绝生成ID，避免重复
2. 生产环境建议使用NTP时间同步，禁止手动调整时间
3. 如果发生时钟回拨，等待时间追上后自动恢复

### Q2: 如何配置机器ID？

**建议方案**：

1. **手动配置**（推荐）：
   - 在配置文件中明确指定datacenter-id和worker-id
   - 通过配置中心（Nacos/Apollo）统一管理

2. **自动分配**（高级）：
   - 使用Zookeeper等分布式协调服务自动分配
   - 基于IP地址哈希计算（需确保不冲突）

3. **容器化环境**：
   - 使用Pod序号或实例序号
   - 通过环境变量注入

### Q3: 为什么不用接口设计？

**原因**：遵循YAGNI原则（You Aren't Gonna Need It）

- 当前只有一种ID生成策略（雪花算法），无需接口抽象
- 直接使用类更简单、更直观
- 如果未来需要多种策略，重构成本很低

### Q4: 与MyBatis-Plus的雪花算法有什么区别？

| 特性 | 本模块 | MyBatis-Plus |
|------|--------|--------------|
| 适用范围 | 全局，可用于任何场景 | 仅限数据库主键 |
| 配置方式 | ConfigManager，支持多种配置源 | 仅Spring配置 |
| 扩展性 | 可自定义，可复用 | 耦合在ORM层 |
| 单例管理 | SingletonHolder | MyBatis-Plus内部管理 |

**建议**：
- 数据库主键继续使用MyBatis-Plus的`IdType.ASSIGN_ID`
- 其他场景（订单号、消息ID等）使用本模块

## 设计原则

### 1. YAGNI原则

- 只实现雪花算法，不做多余抽象
- 不实现UUID、数据库自增等其他策略（Spring/MyBatis-Plus已有）
- 待真正需要时再扩展

### 2. 复用现有组件

- 使用SingletonHolder管理单例
- 使用ConfigManager读取配置
- 依赖common模块的基础能力

### 3. 高性能设计

- 本地生成，无IO开销
- synchronized保证线程安全
- 最小化对象创建

## 未来扩展

待实际需求出现时，可扩展以下功能：

- 时钟回拨优化（等待/借用未来序列号）
- 机器ID自动分配（基于Zookeeper/Redis）
- 多种ID策略支持（UUID增强、号段模式等）
- ID解析工具类（提取时间戳、机器ID等）

---

**遵循YAGNI原则，当前功能已满足大部分业务场景。复杂功能待真正需要时再实现。**

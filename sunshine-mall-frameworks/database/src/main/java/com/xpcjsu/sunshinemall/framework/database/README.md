# Database模块使用指南

## 模块简介

Database模块为sunshine-mall电商平台提供数据访问层的基础支持，集成MyBatis-Plus实现ORM映射、自动填充、分页查询和逻辑删除等核心功能。

## 核心组件

### 1. BaseEntity - 基础实体类

所有数据库实体的基类，提供统一的基础字段。

**字段说明：**

| 字段 | 类型 | 说明 | 自动填充 |
|------|------|------|----------|
| id | Long | 主键ID | 雪花算法自动生成 |
| createTime | LocalDateTime | 创建时间 | 插入时自动填充 |
| updateTime | LocalDateTime | 更新时间 | 插入/更新时自动填充 |
| delFlag | Integer | 逻辑删除标识 | 0-未删除，1-已删除 |

**使用示例：**

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private String productName;
    private BigDecimal price;
    private Integer stock;
}
```

### 2. MyMetaObjectHandler - 字段自动填充处理器

自动填充BaseEntity的时间字段，无需手动设置。

**填充规则：**
- 插入操作：自动填充 createTime 和 updateTime
- 更新操作：自动填充 updateTime

**使用示例：**

```java
// 插入数据
Product product = new Product();
product.setProductName("iPhone 15");
product.setPrice(new BigDecimal("5999"));
productMapper.insert(product);
// createTime和updateTime已自动填充，id已自动生成
```

### 3. MybatisPlusConfig - MyBatis-Plus配置

**配置功能：**
- 分页插件：支持MySQL分页查询，单页最大500条
- 防全表更新删除插件：防止误操作全表数据

**分页查询示例：**

```java
// 查询第1页，每页10条
Page<Product> page = new Page<>(1, 10);
Page<Product> result = productMapper.selectPage(page, new QueryWrapper<>());

System.out.println("总记录数：" + result.getTotal());
System.out.println("总页数：" + result.getPages());
System.out.println("当前页数据：" + result.getRecords());
```

## 主键生成策略

### 雪花算法（默认）

使用MyBatis-Plus内置雪花算法生成分布式唯一ID。

**特点：**
- 全局唯一：支持分布式环境
- 趋势递增：有利于MySQL索引性能
- 高性能：本地生成，无需网络调用
- 信息可读：包含时间戳信息

**ID结构（64位）：**
```
1位符号位 + 41位时间戳 + 10位WorkerId + 12位序列号
```

**配置说明：**

MyBatis-Plus会自动配置WorkerId，也可手动指定：

```yaml
mybatis-plus:
  global-config:
    worker-id: 1        # 工作机器ID（0-31）
    datacenter-id: 1    # 数据中心ID（0-31）
```

## 逻辑删除

### 配置说明

在 application.yml 中配置逻辑删除字段：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: delFlag       # 逻辑删除字段名
      logic-delete-value: 1             # 删除后的值
      logic-not-delete-value: 0         # 未删除的值
```

### 使用示例

```java
// 逻辑删除（UPDATE操作，设置delFlag=1）
productMapper.deleteById(1L);

// 查询时自动过滤已删除数据（WHERE delFlag=0）
Product product = productMapper.selectById(1L); // 返回null

// 如需查询包含已删除数据，使用SQL直接查询
Product deleted = productMapper.selectOne(
    new QueryWrapper<Product>()
        .eq("id", 1L)
        .apply("1=1") // 跳过逻辑删除过滤
);
```

## 常用查询示例

### 1. 基础CRUD

```java
// 插入
Product product = new Product();
product.setProductName("MacBook Pro");
productMapper.insert(product);

// 根据ID查询
Product found = productMapper.selectById(product.getId());

// 更新
product.setPrice(new BigDecimal("12999"));
productMapper.updateById(product);

// 删除（逻辑删除）
productMapper.deleteById(product.getId());
```

### 2. 条件查询

```java
// 查询价格大于5000的商品
QueryWrapper<Product> wrapper = new QueryWrapper<>();
wrapper.gt("price", 5000);
List<Product> products = productMapper.selectList(wrapper);

// 查询名称包含"iPhone"且库存大于0的商品
wrapper = new QueryWrapper<>();
wrapper.like("product_name", "iPhone")
       .gt("stock", 0);
List<Product> iphones = productMapper.selectList(wrapper);
```

### 3. 分页查询

```java
// 分页查询，按价格降序
Page<Product> page = new Page<>(1, 20);
QueryWrapper<Product> wrapper = new QueryWrapper<>();
wrapper.orderByDesc("price");
Page<Product> result = productMapper.selectPage(page, wrapper);
```

### 4. 聚合查询

```java
// 统计商品总数
Long count = productMapper.selectCount(new QueryWrapper<>());

// 查询最高价格
QueryWrapper<Product> wrapper = new QueryWrapper<>();
wrapper.select("MAX(price) as maxPrice");
Map<String, Object> map = productMapper.selectMaps(wrapper).get(0);
```

## 最佳实践

### 1. 实体类设计

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_info")
public class Order extends BaseEntity {
    
    // 使用@TableField指定数据库字段名
    @TableField("user_id")
    private Long userId;
    
    // 忽略非数据库字段
    @TableField(exist = false)
    private String userName;
    
    private String orderNo;
    private BigDecimal totalAmount;
}
```

### 2. Mapper接口定义

```java
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    // 自定义SQL方法
    @Select("SELECT * FROM product WHERE stock < #{threshold}")
    List<Product> selectLowStockProducts(@Param("threshold") Integer threshold);
}
```

### 3. 事务管理

```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    public void createOrder(Order order) {
        // 插入订单
        orderMapper.insert(order);
        
        // 扣减库存
        Product product = productMapper.selectById(order.getProductId());
        product.setStock(product.getStock() - order.getQuantity());
        productMapper.updateById(product);
        
        // 事务自动管理
    }
}
```

### 4. 性能优化建议

**批量插入：**
```java
List<Product> products = new ArrayList<>();
// ... 填充数据
productService.saveBatch(products, 1000); // 每批1000条
```

**只更新非空字段：**
```java
Product update = new Product();
update.setId(1L);
update.setPrice(new BigDecimal("6999"));
productMapper.updateById(update); // 只更新price字段
```

**使用LambdaQueryWrapper避免硬编码：**
```java
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Product::getProductName, "iPhone 15")
       .gt(Product::getPrice, 5000);
List<Product> products = productMapper.selectList(wrapper);
```

## 注意事项

1. **主键ID不要手动设置**：雪花算法会自动生成
2. **时间字段不要手动赋值**：MyMetaObjectHandler会自动填充
3. **逻辑删除后数据仍存在**：只是delFlag=1，物理数据未删除
4. **分页查询注意性能**：单页最大500条，避免一次查询过多数据
5. **防全表更新**：更新/删除操作必须带WHERE条件，否则会报错

## 依赖版本

- MyBatis-Plus：3.5.4.1
- Druid：1.2.20
- MySQL Connector：8.0.33

## 后续扩展

待实际需求出现时，可扩展以下功能：

- 分库分表支持（ShardingSphere）
- 读写分离配置
- 多数据源管理
- 自定义雪花算法（DistributedID模块）
- 数据审计功能（创建人、更新人字段）

---

**遵循YAGNI原则，当前功能已满足大部分业务场景。复杂功能待真正需要时再实现。**

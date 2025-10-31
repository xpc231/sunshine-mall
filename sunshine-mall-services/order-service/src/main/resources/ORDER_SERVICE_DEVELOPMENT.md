# 订单服务模块开发说明

本文面向开发者，记录订单服务模块的架构设计、实现现状、关键流程与注意事项。请注意：本文不包含测试代码或测试类编写指引。

## 一、架构与技术栈

- Spring Boot 3.x（jakarta 包）
- MyBatis-Plus（DAO 层与实体映射）
- Redis（缓存与幂等控制）
- RocketMQ（跨服务异步消息与最终一致性）
- OpenFeign（对接商品与库存服务）
- Nacos（配置中心，可选）
- Lombok（减少样板代码）
- 统一返回与异常框架（convention/base）

## 二、项目结构（订单服务）

```
order-service
├── controller
│   ├── CartController.java          # 已实现：购物车接口
│   └── OrderController.java         # 已实现：订单接口（创建/取消/详情/支付成功回调）
├── dto
│   └── order                        # 订单相关的请求/响应对象（已实现）
│       ├── OrderCreateRequest.java
│       ├── OrderItemRequest.java
│       ├── OrderCreateResponse.java
│       ├── OrderCancelRequest.java
│       ├── OrderDetailResponse.java
│       └── OrderPaySuccessRequest.java
├── entity                           # 订单/明细/支付/退款实体
│   ├── OrderInfo.java
│   ├── OrderItem.java
│   ├── OrderPayment.java
│   └── OrderRefund.java
├── enums
│   └── OrderStatus.java             # 订单状态枚举
├── mapper                           # MyBatis-Plus Mapper
│   ├── OrderInfoMapper.java
│   ├── OrderItemMapper.java
│   ├── OrderPaymentMapper.java
│   └── OrderRefundMapper.java
├── service
│   ├── OrderService.java            # 已实现：领域服务接口
│   ├── CartService.java
│   └── impl
│       ├── OrderServiceImpl.java    # 已实现：订单创建/取消/详情/支付成功逻辑
│       ├── CartServiceCacheDecorator.java
│       └── CartServiceImpl.java
├── client                           # OpenFeign 客户端
│   ├── ProductSkuClient.java        # 商品 SKU 查询
│   └── StockClient.java             # 库存检查/预占/释放/确认扣减
├── mq
│   └── message
│       └── OrderEventMessage.java   # 订单事件消息（CREATED/PAID/CANCELLED）
├── constant
│   └── OrderConstants.java          # MQ Topic/Tag 与业务常量
├── config
│   └── RocketMQConfig.java          # 手动创建 RocketMQTemplate（5.x）
├── state
│   ├── OrderStateMachine.java       # 状态白名单与过渡校验
│   └── TransitionContext.java       # 过渡上下文（校验标志）
└── util
    ├── JwtUtil.java
    └── OrderIdGenerator.java        # 基于雪花ID的ID/订单号生成
```

## 三、核心设计与实现要点

### 1. 分布式ID（雪花算法）
- 模块：`sunshine-mall-framework-distributedid`
- 类：`SnowflakeIdGenerator`
- 配置：`distributed-id.datacenter-id`、`distributed-id.worker-id`
- 使用：`OrderIdGenerator` 注入 `SnowflakeIdGenerator`，统一生成订单ID与订单号（`O` 前缀）

### 2. 接口幂等
- 模块：`sunshine-mall-framework-idempotent`
- 注解：`@Idempotent`（key 支持 SpEL、prefix、expireTime、message）
- 切面：`IdempotentAspect` 使用 Redis 的 SETNX 实现分布式幂等控制
- 已在创建订单方法中使用：
  - `@Idempotent(key = "'order:create:' + #userId + ':' + #request.clientToken", prefix = "idempotent", expireTime = 120)`
  - 前端需传入 `clientToken`（建议 UUID）作为幂等令牌

### 3. 订单状态机
- 类：`OrderStateMachine`
- 白名单：如 `WAIT_PAY → WAIT_SHIP → SHIPPED → COMPLETED`，以及可取消路径（`CANCELLED`）
- 过渡校验：使用 `TransitionContext` 标记业务前置条件（如支付校验、发货准备）
- 并发安全：建议为每个订单加锁避免并发状态变更（代码中在关键点使用 DB 条件 + 校验）

### 4. 数据模型与持久化
- 实体：`OrderInfo`、`OrderItem`、`OrderPayment`、`OrderRefund`
- Mapper：继承 `BaseMapper<T>`，提供基础 CRUD
- 字段映射：`@TableName`、`@TableField`，逻辑删除用 `@TableLogic`

### 5. 与商品/库存服务的对接（Feign）
- SKU 服务：根据 `skuId/skuCode` 获取 SKU 详情（`ProductSkuClient`）
- 库存服务：`checkStock`/`lockStock`/`unlockStock`/`confirmDeduct`（`StockClient`）
- 约定：统一使用 `Result<T>` 包装返回；异常走统一处理

### 6. 异步协作与最终一致性（RocketMQ）
- 常量集中定义：`OrderConstants.MQ.ORDER_EVENT_TOPIC`、`OrderConstants.MQ.Tags.CREATED/PAID/CANCELLED`
- 消息体：`OrderEventMessage`
- 生产者模板：`RocketMQTemplate`（在 `RocketMQConfig` 中手动创建，适配 5.x）

### 7. 安全与鉴权
- JWT：`JwtUtil` 提供解析/验证与 `extractToken`
- 控制器统一从 `Authorization: Bearer <token>` 解析 `userId`

## 四、已实现的订单流程

### 创建订单
1. 解析 JWT 获取 `userId`
2. 根据 `items[].skuId` 查询 SKU 详情（Feign：`ProductSkuClient`）
3. 调用库存服务 `checkStock` 验证可售数量，随后 `lockStock` 进行预占
4. 计算金额并生成订单标识（`OrderIdGenerator` 生成 ID 与订单号）
5. 使用 `@Idempotent` 保证短期幂等（`clientToken`）
6. 落库：`OrderInfo`、`OrderItem`
7. 发送订单创建事件（`ORDER_EVENT_TOPIC:created`）

### 取消订单
1. 校验订单状态允许取消（避免重复取消）
2. 释放预占库存：`unlockStock`
3. 更新订单状态为 `CANCELLED` 并发送事件（`ORDER_EVENT_TOPIC:cancelled`）

### 支付成功回调
1. 校验订单属于当前用户且状态为 `WAIT_PAY`
2. 按订单条目逐一调用 `confirmDeduct` 完成库存确认扣减
3. 更新订单状态为 `WAIT_SHIP`，记录 `paymentTime`
4. 发送支付成功事件（`ORDER_EVENT_TOPIC:paid`）

### 查询订单
- 详情：`getOrderByOrderNo(userId, orderNo)` + `listOrderItems(orderId)`

## 五、控制器与接口概览

- 基础路径：`/order/api/order`
- 已实现接口：
  - `POST /create` 创建订单（`OrderCreateRequest -> OrderCreateResponse`）
  - `POST /cancel` 取消订单（`OrderCancelRequest`）
  - `GET  /{orderNo}` 订单详情（`OrderDetailResponse`）
  - `POST /pay/success` 支付成功回调（确认扣减库存，更新状态）
- 统一认证：请求头 `Authorization: Bearer <JWT>`

## 六、配置要点

- RocketMQ：
  - `rocketmq.name-server` 指向 NameServer
  - `rocketmq.producer.group=order-service-producer-group`
  - 事件主题：`order-event-topic`；标签：`created/paid/cancelled`
- 分布式ID：`distributed-id.datacenter-id`、`distributed-id.worker-id`
- JWT：`jwt.secret`、`jwt.expiration`

## 七、常见问题与排查

- Maven 仓库权限报错（LocalRepositoryNotAccessibleException）：修复用户目录权限或通过 `-Dmaven.repo.local` 指定可写目录
- RocketMQ 模板缺失：`RocketMQConfig` 手动注入 `RocketMQTemplate`，确保 `name-server` 与 `producer.group` 有效
- Redis 幂等失败：检查 Redis 连接与键前缀/过期时间；确认客户端传入 `clientToken`
- 库存一致性：通过事件与补偿实现最终一致性；注重失败场景与重试策略

## 八、后续优化方向

- 订单创建的事务边界与补偿机制（Saga/TCC）
- 未支付自动取消（延迟队列：`ORDER_DELAY_TOPIC:timeout-cancelled`）
- 条件查询与分页、读写分离与缓存策略
- 限流与风控（下单/支付关键路径）
- 指标与监控（QPS、耗时、错误率、MQ 堆积）

---
以上为订单服务模块的开发说明。随着功能迭代，请持续更新本文档以确保与实现保持一致。
# 订单服务模块使用指南

本指南面向使用者（调用方/运维），介绍订单服务模块的功能、部署与调用方式。本文不包含测试代码或测试类。

## 一、模块概述

订单服务负责：
- 购物车增删改查（已实现）
- 订单创建、取消、详情查询、支付成功回调（已实现）
- 订单状态机（已提供基础状态与白名单校验）
- 分布式ID（雪花算法）与接口幂等支持（基于Redis）
- 与商品服务的商品/库存校验（OpenFeign 对接 Product-Service 的 /api/sku、/api/stock）
- 支付与库存的异步协作（通过 RocketMQ，实现事件通知）

## 二、环境与依赖

- JDK 17（Spring Boot 3.x 使用 jakarta 包）
- Maven 3.8+（推荐使用仓库中的 mvnw/mvnw.cmd）
- MySQL 8.x（订单库：sunshine_mall_order）
- Redis 6.x
- RocketMQ 5.x（NameServer 与 Broker）
- Nacos 2.x（可选，用于配置中心）

默认开发环境（可在 application.yml 中修改）：
- MySQL：192.168.100.128:3307
- Redis：192.168.100.128:6379
- Nacos：192.168.100.128:8848
- RocketMQ：192.168.100.128:9876

## 三、配置说明

订单服务配置文件：`sunshine-mall-services/order-service/src/main/resources/application.yml`

关键项说明：
- server.port: 8084，servlet.context-path: /order
- 数据源：指向订单库 sunshine_mall_order
- Redis：连接信息与连接池
- Nacos：server-addr、import-check
- RocketMQ：name-server 与 producer 组名（建议：order-service-producer-group）
- 分布式ID：`distributed-id.datacenter-id`、`distributed-id.worker-id`
- JWT：`jwt.secret` 与 `jwt.expiration`

示例（节选）：

```yaml
server:
  port: 8084
  servlet:
    context-path: /order

spring:
  datasource:
    url: jdbc:mysql://192.168.100.128:3307/sunshine_mall_order?... 
  data:
    redis:
      host: 192.168.100.128
      port: 6379
      password: 123321

rocketmq:
  name-server: 192.168.100.128:9876
  producer:
    group: order-service-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2

distributed-id:
  datacenter-id: 1
  worker-id: 1

jwt:
  secret: sunshine-mall-secret-key-2024
  expiration: 7200000
```

## 四、启动与构建

- Windows（推荐）：在仓库根目录执行
  - 构建：`./mvnw.cmd -pl sunshine-mall-services/order-service -am clean package`
  - 运行：`./mvnw.cmd -pl sunshine-mall-services/order-service spring-boot:run`
- Linux/Mac：使用 `./mvnw` 替代 `./mvnw.cmd`

如遇本地 Maven 仓库权限问题（LocalRepositoryNotAccessibleException），可通过以下方式之一解决：
- 以管理员身份运行终端，修复用户目录下 `~/.m2/repository` 权限
- 在命令行临时指定本地仓库：`-Dmaven.repo.local=D:\maven_repo`（确保该目录存在且可写）
- 配置 `~/.m2/settings.xml` 的 `<localRepository>` 指向可写目录

## 五、接口与调用

统一要求：
- 基础路径：`/order`
- 身份认证：所有订单与购物车接口需在请求头携带 `Authorization: Bearer <JWT>`
- 返回格式：统一使用框架 `Result<T>`，成功返回 `code=SUCCESS`、`data`；失败返回错误码与信息

### 5.1 购物车接口（已实现）

- 基础路径：`/order/api/cart`
- 接口列表：
  - POST `/items` 新增购物车条目（若SKU相同则叠加数量）
  - PUT  `/items` 更新购物车条目数量
  - DELETE `/items/{skuId}` 删除购物车条目（逻辑删除）
  - GET  `/items` 查询购物车条目列表

示例：
```bash
# 新增条目
curl -X POST "http://localhost:8084/order/api/cart/items" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"skuId": 1001, "quantity": 2}'

# 查询条目
curl -X GET "http://localhost:8084/order/api/cart/items" \
  -H "Authorization: Bearer <token>"
```

### 5.2 订单接口（已实现）

- 基础路径：`/order/api/order`
- 接口列表：
  - POST `/create` 创建订单（`OrderCreateRequest -> OrderCreateResponse`）
  - POST `/cancel` 取消订单（`OrderCancelRequest`）
  - GET  `/{orderNo}` 订单详情（`OrderDetailResponse`）
  - POST `/pay/success` 支付成功回调（确认扣减库存，更新状态）

请求示例：
```bash
# 创建订单（需幂等令牌 clientToken）
curl -X POST "http://localhost:8084/order/api/order/create" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"skuId": 1001, "quantity": 2},
      {"skuId": 1002, "quantity": 1}
    ],
    "receiverName": "张三",
    "receiverPhone": "13800001234",
    "receiverProvince": "上海",
    "receiverCity": "上海",
    "receiverDistrict": "浦东新区",
    "receiverAddress": "陆家嘴世纪大道100号",
    "note": "尽快发货",
    "clientToken": "e5a3d124-1f2b-4cce-bd3f-7c9f9a2c0abc"
  }'

# 取消订单
curl -X POST "http://localhost:8084/order/api/order/cancel" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"orderNo": "O1740000000123456789", "reason": "用户主动取消"}'

# 订单详情
curl -X GET "http://localhost:8084/order/api/order/O1740000000123456789" \
  -H "Authorization: Bearer <token>"

# 支付成功回调（由支付服务或网关触发）
curl -X POST "http://localhost:8084/order/api/order/pay/success" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"orderNo": "O1740000000123456789", "paySn": "TRADE202410010001"}'
```

响应示例：
```json
// 创建订单成功
{
  "code": "SUCCESS",
  "message": "创建成功",
  "data": {
    "orderId": 123456789,
    "orderNo": "O1740000000123456789",
    "payAmount": 88.50
  }
}

// 支付成功确认（成功）
{
  "code": "SUCCESS",
  "message": "OK",
  "data": null
}
```

注意事项：
- 创建订单需提供 `clientToken`（前端生成的 UUID），用于幂等控制；幂等窗口默认 120 秒
- 所有接口均需有效 JWT；未携带或过期的令牌将返回 `USER_NOT_LOGIN`
- 取消订单仅支持 `WAIT_PAY` 状态；重复取消会返回业务错误
- 支付成功回调在状态为 `WAIT_PAY` 时生效，会更新为 `WAIT_SHIP` 并记录 `paymentTime`

## 六、与商品服务的对接（Feign）

- 商品SKU：`product-service` 基础路径 `http://localhost:8083`，接口 `/api/sku/{id}`、`/api/sku/code/{skuCode}`
- 库存服务：`/api/stock/check?skuId=&quantity=`、`/api/stock/lock`、`/api/stock/unlock`、`/api/stock/confirm-deduct`
- 订单服务通过 OpenFeign 的 `ProductSkuClient` 与 `StockClient` 完成商品与库存校验与操作

## 七、分布式ID与幂等

- 分布式ID：订单服务通过 `OrderIdGenerator` 使用 `SnowflakeIdGenerator.nextId()` 生成分布式唯一ID；订单号规则为 `"O" + 雪花ID`。
- 幂等：在创建订单接口使用 `@Idempotent(key = "'order:create:' + #userId + ':' + #request.clientToken", prefix = "idempotent", expireTime = 120)`，依赖 Redis 的 SETNX 保障接口在过期时间内不被重复处理。

## 八、RocketMQ 集成

- 事件主题：`order-event-topic`
- 标签：`created`（订单创建）、`paid`（支付成功）、`cancelled`（订单取消）
- 生产者组：`order-service-producer-group`（需与 application.yml 保持一致）
- 说明：订单创建/取消/支付成功后会发送对应事件；如自动配置缺失，`RocketMQConfig` 会手动创建 `RocketMQTemplate`。

## 九、错误处理与返回规范

- 统一使用 `Result<T>` 返回
- 业务异常使用 `BusinessException` 与 `BusinessErrorCode`（如 `ORDER_NOT_FOUND`、`ORDER_STATUS_ERROR` 等）
- 校验错误由 Spring Validation 输出标准错误信息
- 幂等重复提交将抛出 `IdempotentException`，由全局异常处理器统一处理

## 十、安全与鉴权

- 必须携带 JWT（与网关保持一致），请求头名称：`Authorization`，前缀：`Bearer `
- 订单与购物车接口内部通过 `JwtUtil` 从令牌解析 `userId`

## 十一、排障建议

- RocketMQ 生产者组名错误导致消息无法发送：请将 `rocketmq.producer.group` 设置为 `order-service-producer-group`
- Redis 不可用导致幂等功能异常：请检查 `spring.data.redis.*` 配置与网络连通
- 数据库连接失败：检查 `spring.datasource.url`、账户与权限、时区设置

---
以上为订单服务模块的使用说明。后续如拓展接口或调整配置，请更新本文档以保持一致。
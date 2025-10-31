# 支付服务开发文档（Pay Service）

本文档面向开发与运维人员，说明支付服务的架构设计、API 接口规范、业务流程、异常处理与安全策略。文档编码统一为 UTF-8，项目风格与 product-service 保持一致，配置中 Redis 与 product-service 保持一致，依赖注入使用构造函数注入。

## 一、服务架构设计

- 技术栈与基础设施
  - Spring Boot、Spring Cloud（Nacos 注册/配置）、MyBatis-Plus、RocketMQ、Redis、MySQL
  - Sentinel 流量治理与限流（与网关/服务端联动）
  - SkyWalking 链路追踪（HTTP/MQ/DB）
  - 统一错误码与返回体使用框架 convention/base 模块
- 模块与职责
  - Controller：对外提供支付创建、查询、退款、关闭、平台回调等接口
  - Service：封装业务流程（创建交易、状态流转、退款、回调校验）
  - Repository/Mapper：读写数据库（交易、退款、渠道配置、通知日志）
  - Client：与 order-service 通讯（支付成功后通知订单）
  - MQ：支付事件投递（可选，作为回调成功的异步通知）
- 交互关系（简化说明）
  1) 用户请求支付：Pay Service 创建交易记录，生成 paySn 与支付参数（payParams），返回客户端
  2) 支付平台回调：Pay Service 验签并更新交易状态为成功/失败；成功时调用 order-service 的支付成功接口或发送 MQ 事件
  3) 退款：基于 paySn 或 orderNo 发起退款，记录退款单并更新状态；可同步或异步接收退款通知
- 配置与约定
  - Redis 与 product-service 保持一致的连接与序列化配置
  - 通过 Nacos 管理配置，敏感信息（如支付密钥）不直接写入代码或 SQL，建议使用密钥管理或环境变量占位符
  - 数据库与实体字段命名遵循下划线风格；状态字段均有枚举定义

## 二、API 接口规范

统一约定：
- BasePath：`/api/pay`
- Header：`X-User-Id` 必填，表示当前操作用户ID
- Content-Type：`application/json`
- 返回体：统一使用 `Result<T>`，其中 `code`/`message`/`data` 与框架保持一致

1) 创建支付
- POST `/create`
- Request Body：
  - `orderNo` String 必填：订单编号
  - `amount` Number 必填：支付金额（单位：元，保留两位小数）
  - `payType` Integer 必填：支付方式（1-支付宝，2-微信，3-银联）
  - `subject` String 选填：交易标题
  - `body` String 选填：交易描述
  - `clientToken` String 必填：客户端幂等令牌（前端生成 UUID 等）
- Response Data：
  - `paySn` String：支付单号（唯一）
  - `status` Integer：当前状态（0-未支付，1-支付成功，2-支付失败，3-已关闭，4-部分退款，5-全额退款）
  - `payParams` Object：用于调起支付的参数（示例：二维码URL、跳转URL、JSAPI参数等，随渠道而异）

2) 查询支付
- GET `/{paySn}`
- Response Data：交易详情（基础字段与 `pay_transaction` 表一致）

3) 关闭支付
- POST `/close`
- Request Body：`{ "paySn": "PAY2025xxxx" }`
- 逻辑：仅未支付状态可关闭；关闭后不可继续支付

4) 创建退款
- POST `/refund/create`
- Request Body：
  - `paySn` String 必填
  - `amount` Number 必填：退款金额
  - `reason` String 选填
  - `clientToken` String 必填
- Response Data：
  - `refundSn` String：退款单号（唯一）
  - `status` Integer：退款状态（0-申请中，1-成功，2-失败）

5) 查询退款
- GET `/refund/{refundSn}`
- Response Data：退款详情（基础字段与 `pay_refund` 表一致）

6) 支付平台回调（服务端）
- POST `/callback/alipay`：接收支付宝异步通知；返回纯文本 `success` 表示处理完成
- POST `/callback/wechat`：接收微信支付异步通知；返回平台期望的成功响应
- 说明：
  - 回调接口需进行签名校验与防重放处理；
  - 回调成功后更新交易状态并通知 order-service：`POST /api/order/pay/success`（携带 `orderNo` 与 `paySn`）

示例调用（PowerShell）：
```
curl -X POST "http://localhost:8080/api/pay/create" ^
  -H "Content-Type: application/json" ^
  -H "X-User-Id: 20001" ^
  --data-binary "{\"orderNo\":\"ORD202501010002\",\"amount\":288.00,\"payType\":2,\"subject\":\"订单支付\",\"clientToken\":\"PAY-ORD202501010002-uuid\"}"
```

## 三、业务流程说明

1) 创建支付流程
- 校验用户与订单号、金额、支付方式；检查交易是否已存在（幂等）
- 生成 `paySn` 与支付参数（如二维码链接、JSAPI 参数等）
- 插入 `pay_transaction` 记录（状态=未支付）并返回客户端

2) 平台回调/支付确认流程
- 验签、幂等检查（同一 `paySn` 的重复回调只更新一次）
- 更新交易状态为成功/失败，记录平台交易号与回调时间
- 支付成功：通知 order-service（HTTP）并/或发送支付成功事件（MQ）

3) 退款流程
- 校验交易状态与可退款金额；生成 `refundSn`
- 插入 `pay_refund` 记录（状态=申请中）并向平台发起退款
- 接收平台退款回调后更新状态；全额退款时同步更新支付单状态为已退款/已关闭

状态枚举（建议）：
- 支付单：`0 INIT`、`1 SUCCESS`、`2 FAIL`、`3 CLOSED`、`4 REFUND_PART`、`5 REFUND_ALL`
- 退款单：`0 APPLYING`、`1 SUCCESS`、`2 FAIL`

## 四、异常处理机制

- 统一异常：`BusinessException`、`ValidationException`，错误码来自 `BusinessErrorCode`
- 返回约定：
  - 参数问题返回 400（SYSTEM_PARAM_ERROR）
  - 业务受限/状态不允许返回 409（BUSINESS_CONFLICT 类错误码）
  - 平台回调验签失败返回 401/403（UNAUTHORIZED/ACCESS_DENIED）
  - 服务端不可用返回 503（SYSTEM_BUSY）
- 幂等策略：创建支付与创建退款均使用 `clientToken` + 用户ID 作为幂等键；回调以 `paySn/refundSn` 作为幂等键
- 失败重试：平台通知处理失败可重试（MQ 重试或定时补偿），确保最终一致性

## 五、安全策略

- 参数校验与类型约束：金额范围、订单号格式、支付方式枚举
- 身份与权限：`X-User-Id` 必填；核心接口需鉴权（网关层或服务层）
- 防重放与签名校验：平台回调验签、回调序列号与时间窗校验
- 敏感信息保护：渠道密钥、证书等不落库明文；使用环境变量或密钥管理系统，并在配置中引用占位符
- 传输安全：HTTPS；回调 IP 白名单与签名双重校验
- 流量治理：Sentinel 限流与熔断；核心路径（/create、/callback）重点保护
- 数据安全：日志脱敏（不打印完整卡号/身份证/手机号）；审计日志记录处理结果

## 六、配置建议（与 product-service 保持一致风格）

示例（仅用于文档说明，实际以 Nacos 配置为准）：
```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sunshine_pay?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${PAY_DB_PASSWORD:root}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 1  # 与 product-service 保持一致

rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
```

## 七、数据库设计与映射简述

- `pay_transaction`：支付主表，记录交易基本信息与状态流转
- `pay_refund`：退款表，记录退款申请与结果
- `pay_channel_config`：渠道配置表，按渠道（支付宝/微信/银联）存放基础配置（建议占位符）
- `pay_notify_log`：回调通知落库，便于审计与问题排查

以上表的完整建表与示例数据脚本位于 `sql/pay-service` 目录。

## 八、编码规范与项目风格

- 保持与 product-service 一致的分层、命名与异常风格
- 依赖注入统一使用构造函数注入（`@RequiredArgsConstructor` / 显式构造函数）
- Controller 入参使用 DTO，并加 `@Valid` 与约束注解；返回统一使用 `Result<T>`
- 避免过度设计：初期仅实现必要的支付/回调/退款能力；后续扩展通过迭代完成

## 九、交付物与检查

- 文档与 SQL 命名统一：`PAY_SERVICE_DEVELOPMENT.md`、`pay_service_schema.sql`、`test.sql`
- 路径：文档位于 `sunshine-mall-services/pay-service/src/main/resources`；SQL 位于 `sql/pay-service`
- 编码：UTF-8；语法：MySQL 8.x，InnoDB，utf8mb4
## 集成选择

* 采用 OpenFeign 直连物流服务完成“创建运单/查询运单”核心交互，避免过度设计，保持项目一致风格。

* 保留 MQ 仅用于现有订单事件（创建、支付、取消）的通知；物流状态回流的 MQ 订阅作为后续扩展点，当前不实施。

## 变更范围与内容

### frameworks/common

* 新增 `LogisticsClient` 与 `LogisticsClientFallbackFactory`（包名与现有 `ProductSkuClient` 保持一致风格）：

  * `@FeignClient(name="logistics-service", contextId="logisticsClient", fallbackFactory=LogisticsClientFallbackFactory.class)`

  * 方法：

    * `Result<ShipmentDTO> createShipment(CreateShipmentRequest req)` → POST `/shipments`

    * `Result<ShipmentDTO> getByOrderNo(String orderNo)` → GET `/shipments/by-order/{orderNo}`

    * `Result<ShipmentDTO> getByShipmentNo(String shipmentNo)` → GET `/shipments/{shipmentNo}`（备用）

* 在现有 `FeignClientsConfig` 中注册 fallback 工厂 bean（复用既有写法）。

### order-service

* 应用入口：`OrderServiceApplication` 已启用 `@EnableFeignClients(basePackages="...framework.common.feign.clients")`，确保包含新增 logistics 客户端所在包。

* 领域服务：在 `OrderServiceImpl` 增加“发货”流程方法 `deliverOrder(String orderNo, String deliveryCompany, String receiverName, String receiverPhone, String receiverAddress)`：

  * 校验订单状态为 `WAIT_SHIP`，并校验收件信息与配送公司。

  * 构造 `CreateShipmentRequest`（携带 `orderNo`、收件信息、配送公司等）调用 `LogisticsClient.createShipment(...)`。

  * 成功：写回订单的 `deliveryCompany`、`deliverySn`（取 `shipmentNo`）、`deliveryTime=now`，状态流转为 `SHIPPED`；发送订单 `shipped` 事件（沿用现有 `MqClient` 封装）。

  * 失败：保持 `WAIT_SHIP` 不变，按统一 `Result.failure` 返回错误码与提示（fallback 保证降级兜底，不抛业务异常）。

* 控制器：新增接口 `PUT /orders/{orderNo}/deliver`，请求体包含配送公司与收件信息，返回统一 `Result`；新增 `GET /orders/{orderNo}/shipment` 代理查询物流详情（调用 `LogisticsClient.getByOrderNo`）。

* 一致性与幂等：

  * 发货操作加 `@Idempotent`（以 `orderNo` 为幂等键）避免重复创建运单。

  * 事务：创建运单成功后再更新订单状态与发货字段，若订单更新失败需记录告警并人工处理（当前不引入分布式事务）。

### 配置与依赖

* `order-service/pom.xml`：如未引入 `spring-cloud-starter-openfeign`，补充该依赖；OkHttp、Sentinel 配置与 `product-service` 保持一致：

  * `feign.okhttp.enabled: true`

  * `feign.sentinel.enabled: true`

* 超时与重试：保持默认，避免过度配置；如需细化可按服务维度追加：`spring.cloud.openfeign.client.config.logistics-service.connectTimeout/readTimeout`。

## 与 MQ 的关系（当前策略）

* 不通过 MQ 触发创建运单，减少耦合与重试复杂性。

* 订单事件仍经 MQ 发送（已有实现）；待业务需要时，再由物流服务发布“运单状态变更”事件，订单服务订阅并更新订单状态（扩展点，当前不做）。

## 数据与接口契约

* 订单表使用既有字段：`deliveryCompany`、`deliverySn`、`deliveryTime`，不改动表结构。

* 物流服务契约复用已实现的 DTO 与统一 `Result<T>`；失败场景统一返回错误码与提示词，不抛业务异常。

## 实施步骤

1. 在 `frameworks/common` 增加 `LogisticsClient` 与 `LogisticsClientFallbackFactory`，注册到 `FeignClientsConfig`。
2. 在 `order-service` 增加发货服务方法与控制器接口；集成 `LogisticsClient` 并完善状态流转与字段回写。
3. 校验 `order-service` 是否已包含 OpenFeign 依赖与配置；必要时补齐。
4. 联调：从订单创建→支付→发货→查询物流详情，全链路验证；观察失败降级返回是否符合统一约定。

## 选择理由

* OpenFeign 同步、简单、与当前项目（common 客户端 + fallback 工厂）风格一致；避免新增 MQ 消费者、并发幂等与补偿复杂度。

* 满足核心业务路径（发货创建与查询）；后续如需状态回流或批量通知，再引入 MQ 事件订阅即可。


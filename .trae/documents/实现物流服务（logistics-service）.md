## 目标与范围
- 按照与 `product-service` 一致的项目风格，实现基础物流服务：创建运输单、更新状态、查询详情与轨迹。
- 仅落地 HTTP 接口与数据库持久化；暂不接入外部物流商与消息队列，避免过度设计。

## 模块依赖
- 调整 `logistics-service/pom.xml` 为按需依赖：
  - 引入：`spring-boot-starter-web`、`sunshine-mall-framework-database`、`mysql-connector-j`、`sunshine-mall-framework-cache`、`sunshine-mall-framework-idempotent`、`sunshine-mall-framework-distributedid`。
  - 保留父模块的：`nacos-discovery`、`nacos-config`、`bootstrap`、`actuator`、`validation`、`lombok`、`framework-base/common/convention`。
  - 移除未使用的 `rocketmq-client`（若后续需要再按需添加）。

## 配置与环境
- 新增 `src/main/resources/bootstrap.yml`：
  - 通过 Nacos `shared-configs` 引用 `shared-jdbc.yaml`、`shared-redis.yaml`、`shared-log.yaml`（与 `product-service` 保持一致）。
- 新增 `src/main/resources/application.yml`：
  - `spring.application.name: logistics-service`、`server.port`（如 `8086`）、`sunshine.db.database: logistics_service`。
  - 分布式ID：`distributed-id.datacenter-id/worker-id`。
  - 不启用 MQ：不设置 `mq.enabled` 或显式为 `false`。
- 启动类：`com.xpcjsu.sunshinemall.logistics.LogisticsServiceApplication`，标注 `@SpringBootApplication`、`@EnableDiscoveryClient`、`@MapperScan("com.xpcjsu.sunshinemall.logistics.mapper")`。

## 数据模型
- 表结构（与 MyBatis-Plus 约定一致，含逻辑删除与时间字段）：
  - `logistics_shipment`：`id`(PK)、`shipment_no`(唯一)、`order_no`(唯一)、`carrier_code/name`、`status`(枚举：CREATED/ACCEPTED/IN_TRANSIT/DELIVERED/CANCELLED/FAILED)、`tracking_code`、`receiver_*`、`sender_*`、`remark`、`create_time`、`update_time`、`is_deleted`。
  - `logistics_event`：`id`(PK)、`shipment_no`(索引)、`status`、`event_time`、`location`、`message`、`create_time`、`update_time`、`is_deleted`。
- 提交 `sql/logistics-service/logistics_service_schema.sql` 用于初始化与对齐索引命名风格。

## 业务接口
- 路由前缀：`/api/logistics`
- 创建运输单：`POST /shipments`
  - 请求：`orderNo`、`carrierCode/name`、`receiver`（姓名/电话/地址）、`senderAddress`、可选 `trackingCode`。
  - 幂等：`@Idempotent(key="#req.orderNo", prefix="logistics:ship", expireTime=120)`。
  - 返回：`Result<ShipmentDTO>`（含 `shipmentNo`）。
- 更新状态：`PUT /shipments/{shipmentNo}/status`
  - 请求：`status`、可选 `trackingCode/remark`；若 `DELIVERED`，记录送达时间。
- 追加轨迹：`POST /shipments/{shipmentNo}/events`
  - 请求：`status`、`eventTime`、`location`、`message`；可选幂等键 `clientEventId`（若提供，则 `@Idempotent(key="#req.clientEventId", prefix="logistics:event")`）。
- 查询详情：`GET /shipments/{shipmentNo}`、`GET /shipments/by-order/{orderNo}`。
- 查询轨迹：`GET /shipments/{shipmentNo}/events`（按时间倒序）。
- 返回体统一使用 `framework-convention` 的 `Result`。

## 业务实现
- 构造函数注入组件：`SnowflakeIdGenerator`、`CacheManager`、`Mapper`、`Validator`。
- 创建运输单：
  - 校验 `orderNo` 非空与唯一；生成 `shipmentNo`（雪花ID）；写库成功后写入缓存 `logistics:shipment:detail:<shipmentNo>`（TTL 5min）。
- 状态更新：
  - 校验状态流转合法（如不能从 `DELIVERED` 回到 `IN_TRANSIT`）；更新 DB 并删缓存（旁路策略）。
- 轨迹追加：
  - 插入 `logistics_event`；若为关键状态（`ACCEPTED/IN_TRANSIT/DELIVERED`），同时更新 `logistics_shipment.status`。
- 查询：
  - 先读缓存，未命中则查库并回填缓存；轨迹不做缓存或做短缓存（60s）。

## 缓存与键规范
- 缓存键：`CacheKeyBuilder.build("logistics","shipment:detail", shipmentNo)`、`CacheKeyBuilder.buildPattern("logistics","shipment:detail")`。
- 采用空值缓存防穿透（短期 `"NULL"`，30s）。
- 删除策略：更新/删除后优先删缓存，读取路径统一走缓存。

## 校验与异常
- 请求体 `@Valid`，字段约束与 `product-service` 保持一致（如 `@NotBlank`、`@Size`）。
- 异常统一抛 `BusinessException`，由全局处理器返回 `Result.failure`。

## 验证与交付
- Maven 构建：按核心服务集打包（排除无主类的模块），确保依赖与配置正确。
- 本地启动验证：通过 Nacos 读取共享配置，Redis 与 JDBC 连接成功，接口具备基本功能。
- 不编写测试用例，专注最小可用交付。

## 变更清单
- 新增：主类、Controller/Service/Mapper/Entity、`bootstrap.yml`、`application.yml`、`sql/logistics-service/logistics_service_schema.sql`。
- 修改：`logistics-service/pom.xml` 依赖结构（按需引入、移除未用 MQ）。
- 不改动其他服务与框架模块。

请确认上述方案，确认后我将按计划在代码库中落地实现并完成构建与启动验证。
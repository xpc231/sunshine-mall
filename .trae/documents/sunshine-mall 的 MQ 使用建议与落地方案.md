## 改造目标
- 在 `frameworks/common` 模块内提供统一的 RocketMQ 客户端，减少各服务重复编码。
- 兼容现有 Topic/Tag 常量与消息体，保持与 `product-service` 的风格一致、构造器注入。
- 通过 Nacos `shared-rocketmq.yaml` 进行共享配置，做到按需启用/降级不影响主链路。

## 技术选择与约束
- 依赖：使用已管理的 `rocketmq-client 4.9.6`（`sunshine-mall-dependencies/pom.xml:114-118`），不引入 `rocketmq-spring-boot-starter`。
- 配置：各服务 `bootstrap.yml` 已引用 `shared-rocketmq.yaml`（`product-service/src/main/resources/bootstrap.yml:11-13`）。
- 复用：Topic/Tag 常量直接沿用（`frameworks/common/.../MqConstant.java:12-24`、`product-service/.../ProductConstants.java:162-170`）。
- 注入：所有组件采用构造器注入；避免过度设计；先不扩展物流/ai。

## 共享配置内容（Nacos shared-rocketmq.yaml）
```yaml
rocketmq:
  name-server: 192.168.100.128:9876
  access-key: ${ROCKETMQ_ACCESS_KEY}
  secret-key: ${ROCKETMQ_SECRET_KEY}
  producer:
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    pull-interval: 0
    max-reconsume-times: 16
    consume-thread-min: 10
    consume-thread-max: 20
  message:
    trace-enabled: true
    charset: UTF-8
mq:
  enabled: true
```
- 说明：各服务保留自身的 `rocketmq.producer.group`（如 `order-service/src/main/resources/application.yml:18-22`）。

## common 模块内的类设计
- `com.xpcjsu.sunshinemall.framework.common.mq.MqClient`
  - 同步发送：`sendSync(String topic, String tag, Object payload, String key, Map<String,String> headers)`
  - 异步发送：`sendAsync(...)`
  - 单向发送：`sendOneWay(...)`
  - 延迟发送：`sendDelaySync(String topic, String tag, Object payload, int delayLevel, String key, Map<String,String> headers)`
- `com.xpcjsu.sunshinemall.framework.common.mq.RocketMqClient`（实现）
  - 基于 `DefaultMQProducer`；启动/关闭生命周期托管；统一 JSON 序列化（复用 Spring `ObjectMapper`）。
  - 读取 `rocketmq.*` 与 `rocketmq.producer.group`；支持 `mq.enabled=false` 时降级为 no-op。
- `com.xpcjsu.sunshinemall.framework.common.mq.MqProperties`
  - `@ConfigurationProperties(prefix = "rocketmq")` 绑定 nameServer、producer/consumer 通用项；同时 `@Value` 读取当前服务的 `rocketmq.producer.group`。
- `com.xpcjsu.sunshinemall.framework.common.mq.MqMessageHeaders`
  - 统一可选头：`traceId`、`producerApp`、`bizKey` 等；方法辅助构建。
- 复用现有常量与消息体
  - 订单：`MqConstant.Order.*`（`frameworks/common/.../MqConstant.java:12-24`）
  - 支付：`PaymentEventMessage`（`pay-service/.../PaymentEventMessage.java:21-68`）
  - 商品：`ProductConstants.MQ.*`（`product-service/.../ProductConstants.java:162-170`）

## 使用方式（服务侧改造示例）
- 商品服务库存广播：
  - 旧：`StockChangeProducer.syncSend(...)`（已禁用，`product-service/.../StockChangeProducer.java:3-4`）
  - 新：注入 `MqClient` 并调用 `sendSync(ProductConstants.MQ.STOCK_CHANGE_TOPIC, tag, message, key, headers)`。
- 支付成功事件：
  - `pay-service` 在支付完成处注入 `MqClient`，发布 `PaymentEventMessage` 到 `order-event-topic`，Tag=`paid`。
- 订单延迟取消：
  - `order-service` 下单时调用 `sendDelaySync(MqConstant.Order.TOPIC_DELAY, TIMEOUT_CANCELLED, msg, level, key, headers)`，替换定时扫描。

## 幂等与失败处理策略
- 发送端：重试次数来自共享配置；返回结果封装 `MqSendResult`（含 msgId）。
- 消费端（后续迭代）：提供 `AbstractJsonMessageListener<T>` 基类，统一反序列化、异常处理与幂等钩子；当前迭代仅提供发送端以减少改动面。

## 改造步骤
1. 在 `frameworks/common` 增加上述 `MqClient` 接口与 `RocketMqClient` 实现、`MqProperties` 与 `MqMessageHeaders`。
2. 在 Nacos 补齐/创建 `shared-rocketmq.yaml`，并确保所有服务已在 `bootstrap.yml` 引用（已存在）。
3. 在 `order-service` 与 `pay-service`、`product-service` 中：
   - 删除/保留原 `RocketMQConfig` 注解禁用状态不变；替换调用为注入 `MqClient` 的统一入口。
   - 保留各自的 `rocketmq.producer.group`。
4. 灰度放量
   - 先启用“支付成功→订单更新”“订单事件广播”“库存广播”等低风险发送；观察堆积与失败率。
   - 再替换“订单超时取消”为延迟消息。

## 验收标准
- 发送端无重复编码，服务仅关心业务消息体与 Topic/Tag。
- 主链路不受影响；`mq.enabled=false` 时自动降级不抛错。
- 指标与日志可定位：发送失败可重试/报警，返回 msgId 便于排查。

确认后，我将按以上方案在 common 模块落地实现，并逐服务替换调用。
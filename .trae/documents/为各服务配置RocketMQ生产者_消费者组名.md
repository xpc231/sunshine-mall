## 目标
- 在参与MQ的服务配置中补充`rocketmq.producer.group`与`rocketmq.consumer.group`，保证组名唯一、符合项目风格，并与`shared-rocketmq.yaml`协同工作。

## 范围
- 服务：`pay-service`、`order-service`、`stock-service`（`product-service`当前不使用MQ，可不加以避免过度设计）。
- 配置来源：优先在 Nacos 服务私有配置（如`pay-service.yaml`、`order-service.yaml`、`stock-service.yaml`）中添加；也可临时写入各服务的`application.yml`再迁移到Nacos。

## 配置示例（建议组名规范：`<service>-pg`/`<service>-cg`）
- pay-service（生产者为支付事件）：
```
rocketmq:
  producer:
    group: pay-service-pg
  consumer:
    group: pay-service-cg
```
- order-service（生产者为延迟取消；消费者为订单超时取消监听）：
```
rocketmq:
  producer:
    group: order-service-pg
  consumer:
    group: order-service-cg
```
- stock-service（生产者为库存变更事件；消费者预留）：
```
rocketmq:
  producer:
    group: stock-service-pg
  consumer:
    group: stock-service-cg
```

## 与现有代码的对应
- 生产者组：`MqAutoConfiguration`读取`rocketmq.producer.group`，用于`DefaultMQProducer`初始化。
- 消费者组：当前消费者代码大多禁用（注释），组名先配置以便后续启用；若启用`@RocketMQMessageListener`或自建`DefaultMQPushConsumer`，统一从`rocketmq.consumer.group`读取。

## 兼容说明
- `name-server`、发送超时/重试等仍在`shared-rocketmq.yaml`中统一管理；组名必须在服务私有配置中设置，避免多个服务共享同一组导致消费冲突。
- 网关与用户服务不参与MQ，无需添加组名，避免过度设计。

## 验证
- 添加配置后，分别启动或打包`pay-service`、`order-service`、`stock-service`，观察`RocketMQ producer started: group=...`日志；如暂未启动消费者，配置不会产生副作用。

确认后我将把以上配置添加到对应服务配置文件（或提供待发布的Nacos服务私有配置片段），并进行编译验证。
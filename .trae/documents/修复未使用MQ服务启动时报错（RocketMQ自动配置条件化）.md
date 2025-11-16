## 问题分析
- 报错来源：`MqAutoConfiguration` 无条件创建 `rocketMqClient`，并强制解析 `rocketmq.name-server`、`rocketmq.producer.group`，当未配置这些属性时抛出异常。
- 代码位置：
  - 自动配置导入：`sunshine-mall-frameworks/common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1`
  - Bean 创建：`sunshine-mall-frameworks/common/src/main/java/com/xpcjsu/sunshinemall/framework/common/mq/MqAutoConfiguration.java:33-45`
  - 默认启用标记：方法参数 `@Value("${mq.enabled:true}")` 导致即使未使用 MQ 也尝试创建 Bean。
- 影响范围：不使用 MQ 的服务（如网关）在未配置 `rocketmq.*` 时也会加载自动配置并报错。

## 改造方案
- 为自动配置增加条件：在 `MqAutoConfiguration` 类或 `rocketMqClient` 方法上添加 `@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true", matchIfMissing = false)`，仅当明确开启时才创建 MQ Bean。
- 将 `@Value("${mq.enabled:true}")` 的默认值改为 `false` 或直接依赖条件注解控制启用，避免默认开启。
- 保持现有 `RocketMqClient` 的 `enabled` 开关语义（`init()` 已检查 `enabled`，但需要先避免属性解析阶段报错）。

## 配置调整
- 需要 MQ 的服务在自身配置或 Nacos 共享配置中显式添加：
  - `mq.enabled: true`
  - `rocketmq.name-server: <NameServer地址>`
  - `rocketmq.producer.group: <Producer组名>`
- 不使用 MQ 的服务不要设置 `mq.enabled: true`；如引用了 `shared-rocketmq.yaml`，建议移除该共享配置以减少不必要的属性。
- 现状参考：
  - 网关未引用 `shared-rocketmq.yaml`，按上述改造后将不再报错（`sunshine-mall-services/gateway-service/src/main/resources/bootstrap.yaml:9-12`）。
  - 多个服务通过 Nacos 引入 `shared-rocketmq.yaml`（如 `pay-service`、`order-service`、`user-service` 的 `bootstrap.yaml`），需在共享或服务级配置中补充 `mq.enabled: true` 才会启用。

## 验证步骤
- 启动不使用 MQ 的服务（如网关、用户），在未配置 `rocketmq.*` 的情况下不再出现 `BeanCreationException`。
- 启动使用 MQ 的服务（如支付/订单/库存），在设置 `mq.enabled: true` 且正确配置 `rocketmq.*` 后，`RocketMqClient` 成功初始化：
  - 参考使用点：`pay-service/src/main/java/com/xpcjsu/sunshinemall/pay/service/impl/PayServiceImpl.java:341-347`（`mqClient.sendSync(...)`）。

## 备选方案（如需）
- 提供 `NoopMqClient` 空实现，并在 `mq.enabled != true` 或配置缺失时返回空实现以消除业务端判空需要；为避免过度设计，当前计划不落地该方案。请确认是否需要。
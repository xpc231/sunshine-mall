## 问题与原因
- 启动报错：缺少 `SnowflakeIdGenerator` Bean，被 `logistics-service` 的 `LogisticsIdGenerator` 构造器注入所需。
- 根因：项目使用 Spring Boot 3.1.5（sunshine-mall-dependencies/pom.xml:20），而 `framework-distributedid` 仍使用旧版 `spring.factories` 机制（META-INF/spring.factories），导致自动配置未生效，Bean 未注册。

## 修复方案
- 在 `sunshine-mall-frameworks/distributedid` 增加 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，内容为：
  - `com.xpcjsu.sunshinemall.framework.distributedid.config.DistributedIdConfig`
- 保持 `DistributedIdConfig` 与现有实现不变（`@Bean SnowflakeIdGenerator` + `@ConfigurationProperties`），不做额外改动，避免过度设计。
- 不修改业务服务代码；物流服务现有 `application.yml` 已配置 `distributed-id`，无需调整。

## 验证步骤
- 仅构建 `logistics-service` 及其依赖模块并启动验证：
  - `./mvnw -q -DskipTests -pl sunshine-mall-services/logistics-service -am clean package`
- 启动 `logistics-service`，确认 `SnowflakeIdGenerator` Bean注入正常，应用上下文加载成功。

## 备选临时方案（如需快速绕过）
- 在 `LogisticsServiceApplication` 上添加 `@Import(DistributedIdConfig.class)` 强制引入配置，但不推荐；统一在框架层修复更稳妥。

确认后我将按上述方案新增 AutoConfiguration.imports 文件并完成构建与启动验证。
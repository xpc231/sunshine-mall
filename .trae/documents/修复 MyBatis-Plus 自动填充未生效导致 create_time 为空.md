## 问题与根因

* 异常：`SQLIntegrityConstraintViolationException: Column 'create_time' cannot be null`。

* 根因：`framework-database` 的自动填充处理器 `MyMetaObjectHandler` 未随 Boot 3 自动配置加载；当前模块没有 `AutoConfiguration.imports` 注册，导致 Bean 未进入容器，插入时未填充 `createTime/updateTime`。

## 修复方案

* 在 `framework-database` 新增自动配置：

  * 创建 `DatabaseAutoConfiguration`，`@Configuration`，提供 `@Bean MyMetaObjectHandler`。

  * 在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册：

    * `com.xpcjsu.sunshinemall.framework.database.config.MybatisPlusConfig`

    * `com.xpcjsu.sunshinemall.framework.database.config.DatabaseAutoConfiguration`

* 保持最小改动，不变更业务代码与既有组件结构，避免过度设计。

* 顺便检查其他字段是否会出现类似错误。

## 验证

* 重新构建并运行 `logistics-service`，通过创建运输单接口插入数据，观察 `create_time/update_time` 自动填充成功（或由表默认值兜底）。

* 若端口占用，调整 `server.port` 后重试。

确认后我将按上述方案实现并完成验证。

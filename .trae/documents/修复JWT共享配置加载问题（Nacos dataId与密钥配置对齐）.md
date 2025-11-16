## 问题定位
- 文件存在：`hmall.jks` 位于 `common` 资源目录，可通过 `classpath:hmall.jks` 访问。
- 配置未加载：两个服务的 `bootstrap.yaml` 引用的是 `shared_jwt.yaml`（下划线），而实际 Nacos 中常用约定是 `shared-jwt.yaml`（连字符）。dataId 名称不一致会导致 `hm.jwt.*` 未注入，出现 `JwtProperties` 字段为 `null` 的错误。
- 兼容处理：我们已对密钥密码做了空值兼容，但若 `location/alias` 未加载，仍会失败。

## 修复方案
- 统一 dataId 名称
  - 方案A（推荐）：将网关与用户服务的 `bootstrap.yaml` 中 `shared_jwt.yaml` 改为实际的 Nacos dataId（推荐 `shared-jwt.yaml`，与现有 `shared-log.yaml`、`shared-redis.yaml` 风格一致）。
    - 网关：`sunshine-mall-services/gateway-service/src/main/resources/bootstrap.yaml:9-12`
    - 用户：`sunshine-mall-services/user-service/src/main/resources/bootstrap.yaml:8-14`
  - 方案B：在 Nacos 中把你的 JWT 共享配置 dataId 改名/新增为 `shared_jwt.yaml`，与当前 `bootstrap.yaml` 一致。
- 保持统一键前缀：`hm.jwt`（与属性类 `JwtProperties` 注解 `prefix = "hm.jwt"` 匹配），避免前缀变更。
- 配置示例（Nacos dataId: `shared-jwt.yaml`）
  ```yaml
  hm:
    jwt:
      location: classpath:hmall.jks
      alias: hmall
      password: hmall123  # 若JKS无密码，可留空或删除该项
      tokenTTL: 30m
  ```
- 密钥与别名
  - 确认 `alias: hmall` 与 JKS 文件中条目别名一致。
  - 若 JKS 存储密码为空：我们已支持空密码（自动用空字符数组），无需强制填写。

## 验证步骤
- 更新 dataId 一致性后，重启网关与用户服务。
- 访问用户登录，检查是否成功返回 Token；访问受保护路由，网关验证通过并透传 `user-info`。
- 如仍异常，查看是否为别名或密码不匹配（会报 `无法读取密钥` 等），按实际JKS条目调整 `alias/password`。

## 后续优化（可选）
- 网关本地 `JwtProperties` 与 `common` 的 `JwtProperties` 重复，建议后续移除网关本地类，仅使用公共配置，减少歧义。
- 恢复用户服务的 `SecurityConfig` 为有效的 `PasswordEncoder` Bean（目前文件被注释包裹），保证注册/登录密码加密一致。

请确认采用方案A还是方案B，我将据此更新对应 `bootstrap.yaml` 或给出Nacos端dataId改名建议，并进行重启验证。
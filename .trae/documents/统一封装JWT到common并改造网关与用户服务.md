## 现状与差异
- 网关：使用 Hutool JWT + RS256，类 `JwtTool` 负责生成与解析；`AuthGlobalFilter` 从 `Authorization` 读取 Bearer，调用解析。
  - 参考：`gateway-service/.../AuthGlobalFilter.filter` f:\ideaCode\sunshine-mall\sunshine-mall-services\gateway-service\src\main\java\com\xpcjsu\sunshinemall\gateway\filter\AuthGlobalFilter.java:31-67
  - 参考：`gateway-service/.../JwtTool` f:\ideaCode\sunshine-mall\sunshine-mall-services\gateway-service\src\main\java\com\xpcjsu\sunshinemall\gateway\util\JwtTool.java:15-81
  - 参考：`gateway-service/.../SecurityConfig.keyPair` f:\ideaCode\sunshine-mall\sunshine-mall-services\gateway-service\src\main\java\com\xpcjsu\sunshinemall\gateway\config\SecurityConfig.java:21-32
  - 参考：`gateway-service/.../JwtProperties(hm.jwt)` f:\ideaCode\sunshine-mall\sunshine-mall-services\gateway-service\src\main\java\com\xpcjsu\sunshinemall\gateway\config\JwtProperties.java:9-15
- 用户服务：使用 Auth0 `java-jwt` + HMAC256 生成 Token；`JwtConfig` 提供 `jwt.secret` 与过期时间；登录后返回 Token。
  - 参考：`user-service/.../UserServiceImpl.login` f:\ideaCode\sunshine-mall\sunshine-mall-services\user-service\src\main\java\com\xpcjsu\sunshinemall\user\service\impl\UserServiceImpl.java:49-74
  - 参考：`user-service/.../UserServiceImpl.generateToken` f:\ideaCode\sunshine-mall\sunshine-mall-services\user-service\src\main\java\com\xpcjsu\sunshinemall\user\service\impl\UserServiceImpl.java:248-267
  - 参考：`user-service/.../JwtConfig` f:\ideaCode\sunshine-mall\sunshine-mall-services\user-service\src\main\java\com\xpcjsu\sunshinemall\user\config\JwtConfig.java:13-27

## 目标
- 将 JWT 能力统一封装到 `frameworks/common` 模块：提供标准化的“生成Token接口”和“验证Token接口”。
- 用户登录时调用“生成接口”；网关校验时调用“验证接口”。
- 统一算法为 RS256（非对称），复用网关现有 `hm.jwt` 配置与密钥库，保持项目风格一致、减少重复代码。

## 设计与接口
- 新增 `common` 组件：`com.xpcjsu.sunshinemall.framework.common.jwt.JwtTool`
  - 方法：
    - `String createToken(Long userId, Duration ttl)`
    - `Long parseToken(String token)`
  - 行为：与网关当前 `JwtTool` 一致，载荷仅包含 `user`（用户ID），过期时间由入参或配置提供。
- 新增 `common` 配置：`JwtProperties`（prefix=`hm.jwt`）与自动配置，提供 `KeyPair` Bean 与 `JwtTool` Bean；构造器注入。
- 依赖注入：服务层通过构造器注入 `JwtTool`，不直接依赖具体库。

## 改造内容
- 网关服务：
  - 替换为注入 `common` 的 `JwtTool`，删除或停用网关本地 `JwtTool` 与 `JwtProperties` 的重复类；`AuthGlobalFilter` 保持调用不变（`parseToken`）。
  - 如网关仍需 `PasswordEncoder`，保留现有 `SecurityConfig` 中密码编码 Bean。
- 用户服务：
  - 在 `UserServiceImpl.login(...)` 中用 `jwtTool.createToken(userId, jwtProps.getTokenTTL())` 替换 `generateToken(...)`；删除 `Auth0 java-jwt` 依赖与 `JwtConfig`（对称密钥配置不再需要）。
  - 保留黑名单逻辑（登出时写入缓存），后续可在网关校验时增加黑名单判断（可选，避免过度设计）。
- 下游服务：继续通过 `UserInfoInterceptor` 读取网关透传 `user-info`，无需解析 JWT。

## 配置方案
- 使用 Nacos 共享配置或在各服务 `bootstrap.yml` 统一引用 `hm.jwt`：
  - `hm.jwt.location`、`hm.jwt.password`、`hm.jwt.alias`、`hm.jwt.token-ttl`（保持与网关一致）。
- 用户服务需能读取同一密钥库（生成端需要私钥），网关读取公私钥用于验证。

## 兼容与风险
- 算法切换：用户服务从 HMAC256 切换到 RS256；历史 Token 将失效，需让用户重新登录。
- 安全性提升：非对称密钥避免在多服务间分发对称密钥，符合最佳实践。
- 逐步迁移（可选）：如需平滑过渡，可在网关临时支持双验证（先验 RS256，再验 HMAC256），直到旧 Token 过期。

## 验证步骤
- 配置 `hm.jwt` 在 Nacos/配置中心后，启动用户服务与网关。
- 登录获取 Token（用户服务），网关访问受保护路由时校验通过并透传 `user-info`。
- 校验过期与无效 Token：网关返回 `401`（参考 `AuthGlobalFilter`）。

## 预期变更的代码位置
- 网关：`AuthGlobalFilter.parseToken(...)` 保持调用一致，只更换为 `common` 的 Bean。
  - 参考：f:\ideaCode\sunshine-mall\sunshine-mall-services\gateway-service\src\main\java\com\xpcjsu\sunshinemall\gateway\filter\AuthGlobalFilter.java:52-66
- 用户服务：替换 `generateToken(...)` 的调用点与实现。
  - 参考：f:\ideaCode\sunshine-mall\sunshine-mall-services\user-service\src\main\java\com\xpcjsu\sunshinemall\user\service\impl\UserServiceImpl.java:65-74, 248-267

请确认以上方案，确认后我将按上述计划实施改造并进行验证。
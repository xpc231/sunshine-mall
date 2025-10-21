# User Service 开发文档

## 一、开发历程概述

本文档记录了 User Service 模块从零开发到完成的全过程，包括遇到的技术难题、解决方案及经验总结。

---

## 二、项目架构设计

### 1. 分层架构

```
user-service/
├── controller/          # 控制层 - REST API 接口
├── service/             # 业务层 - 核心业务逻辑
│   └── impl/           # 业务实现
├── mapper/              # 持久层 - MyBatis-Plus Mapper
├── entity/              # 实体层 - 数据库映射对象
├── dto/                 # 数据传输对象
├── config/              # 配置类
└── UserServiceApplication.java  # 启动类
```

### 2. 技术选型

| 技术组件 | 版本 | 用途 |
|---------|------|------|
| Spring Boot | 3.1.5 | 基础框架 |
| MyBatis-Plus | 3.5.x | ORM 框架 |
| BCrypt | Spring Security Crypto | 密码加密 |
| JWT | java-jwt 4.4.0 | 令牌认证 |
| Redis | Spring Data Redis | 缓存与黑名单 |
| Druid | 1.2.x | 数据库连接池 |
| Nacos | 2.2.x | 服务注册与配置中心 |

### 3. 核心框架依赖

复用项目 `sunshine-mall-frameworks` 提供的基础能力：

- **base**: Spring 上下文管理、单例模式、异常体系
- **common**: 工具类集合
- **convention**: 统一响应格式、错误码定义
- **database**: BaseEntity、MyBatis-Plus 配置
- **cache**: Redis 配置、CacheManager 封装
- **distributedid**: 分布式 ID 生成（预留）
- **idempotent**: 幂等性控制注解

---

## 三、开发过程与问题解决

### 阶段一：项目初始化与基础搭建

#### 1.1 创建模块结构

**操作**：在 `sunshine-mall-services` 下创建 `user-service` 子模块

**关键配置**：

pom.xml 依赖管理

```xml
<parent>
    <groupId>com.xpcjsu</groupId>
    <artifactId>sunshine-mall-services</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>
```

**问题**: 编译时提示找不到 Lombok 注解

**原因**: `sunshine-mall-services` 父 POM 中缺少 Lombok 依赖声明

**解决方案**:

在 `sunshine-mall-services/pom.xml` 中添加：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

**经验总结**:
- Maven 多模块项目中，公共依赖应在父 POM 统一管理
- Lombok 作为编译期依赖，scope 设置为 `provided`

---

#### 1.2 Lombok 版本兼容性问题

**问题**: 编译时报错 `JCTree$JCImport does not have member field 'qualid'`

**错误日志**:

```
Fatal error compiling: java.lang.NoSuchFieldError: 
Class com.sun.tools.javac.tree.JCTree$JCImport does not have member field 'qualid'
```

**原因**: Lombok 1.18.20 不支持 JDK 17

**解决方案**:

修改 `sunshine-mall-dependencies/pom.xml`：

```xml
<lombok.version>1.18.30</lombok.version>
```

**经验总结**:
- JDK 17 需要 Lombok 1.18.30+
- 升级 JDK 版本时必须检查第三方库兼容性
- 版本管理应在顶层 dependencies 模块统一控制

---

### 阶段二：API 规范与路径问题

#### 2.1 API 路径不符合规范

**问题**: 初始设计中 Controller 使用 `@RequestMapping("/user")`

**不符合规范**: 微服务 API 应统一使用 `/api` 前缀

**解决方案**:

修改 [`UserController`](f:\ideaCode\sunshine-mall\sunshine-mall-services\user-service\src\main\java\com\xpcjsu\sunshinemall\user\controller\UserController.java)：

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    // ...
}
```

**经验总结**:
- RESTful API 设计应遵循团队规范
- 统一前缀便于网关路由和权限控制

---

#### 2.2 更新接口路径参数绑定问题

**问题**: `PUT /api/user/{id}` 接口提示"用户ID不能为空"

**原因**: 路径参数 `{id}` 未绑定到方法参数，导致 DTO 中 id 为 null

**错误代码**:

```java
@PutMapping("/{id}")
public Result<Void> updateUser(@Valid @RequestBody UserDTO userDTO) {
    // userDTO.getId() 为 null
}
```

**解决方案**:

```java
@PutMapping("/{id}")
public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
    userDTO.setId(id);
    boolean success = userService.updateUser(userDTO);
    return success ? Result.success(null, "更新成功") 
                  : Result.failure("UPDATE_FAILED", "更新失败");
}
```

**经验总结**:
- RESTful 风格接口的路径参数需要 `@PathVariable` 注解
- URL 中的资源 ID 应显式绑定到方法参数

---

### 阶段三：安全机制实现

#### 3.1 密码加密算法选择

**初始方案**: 使用 Hutool 的 MD5 加密

```java
String encryptedPassword = DigestUtil.md5Hex(password);
```

**问题**: MD5 已被证明不安全，存在彩虹表攻击风险

**改进方案**: 使用 BCrypt 加密

**实现步骤**:

1. 创建 SecurityConfig 配置类：

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

2. 修改 UserServiceImpl：

```java
// 注册时加密密码
user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

// 登录时验证密码
if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
    throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户名或密码错误");
}
```

**关键问题**: 切换加密算法后，已存在用户无法登录

**原因**: 数据库中旧用户密码仍是 MD5 格式，BCrypt 无法验证

**解决方案**:

方案一（开发环境）：清空用户表，重新注册

```sql
TRUNCATE TABLE t_user;
```

方案二（生产环境）：实现双重验证机制（先 BCrypt，失败则尝试 MD5，成功后自动升级）

**经验总结**:
- 密码加密应选择 BCrypt、Argon2 等安全算法
- 加密算法变更需要数据迁移方案
- BCrypt 自带盐值，无需手动管理

---

#### 3.2 JWT 配置管理问题

**初始方案**: 使用 `@Value` 注解读取配置

```java
@Value("${jwt.secret}")
private String jwtSecret;

@Value("${jwt.expiration}")
private Long jwtExpiration;
```

**问题**: 
- 配置分散在多个字段
- 缺乏类型安全和默认值支持
- 不符合 Spring Boot 配置规范

**改进方案**: 创建 JwtConfig 配置类

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private Long expiration;
}
```

**使用方式**:

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final JwtConfig jwtConfig;
    
    private String generateToken(String userId, String username) {
        return JWT.create()
            .withExpiresAt(new Date(now.getTime() + jwtConfig.getExpiration()))
            .sign(Algorithm.HMAC256(jwtConfig.getSecret()));
    }
}
```

**经验总结**:
- 使用 `@ConfigurationProperties` 替代 `@Value`
- 配置类提供更好的类型安全和可维护性
- Lombok 的 `@Data` 简化 getter/setter

---

### 阶段四：数据持久化与缓存

#### 4.1 MyBatis-Plus 配置问题

**问题**: 启动时提示找不到 Mapper XML 文件

**错误配置**:

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

**原因**: 项目使用 MyBatis-Plus BaseMapper，不需要 XML 映射文件

**解决方案**:

删除 `mapper-locations` 配置，保留其他配置：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**经验总结**:
- MyBatis-Plus 提供 BaseMapper，无需 XML 即可完成 CRUD
- 复杂查询可使用 LambdaQueryWrapper
- 逻辑删除配置在 global-config 中统一管理

---

#### 4.2 Redis 序列化问题

**问题**: 缓存 User 对象时报错 `Cannot serialize java.time.LocalDateTime`

**原因**: `GenericJackson2JsonRedisSerializer` 默认不支持 Java 8 时间类型

**解决步骤**:

1. 添加 Jackson 时间模块依赖（cache 模块 pom.xml）：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

2. 配置 RedisConfig：

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置支持 Java 8 时间类型的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册 Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

**编译错误**: `JavaTimeModule javaTimeModule = new JavaTimeModule;`

**原因**: 构造函数缺少括号

**修复**: `JavaTimeModule javaTimeModule = new JavaTimeModule();`

**经验总结**:
- Jackson 序列化 Java 8 时间类型需要 `jackson-datatype-jsr310` 模块
- 时间格式化统一使用 `yyyy-MM-dd HH:mm:ss`
- 禁用时间戳序列化，使用可读字符串

---

### 阶段五：业务功能完善

#### 5.1 幂等性控制

**需求**: 防止用户重复注册

**实现方案**: 使用框架提供的 `@Idempotent` 注解

```java
@PostMapping("/register")
@Idempotent(key = "#userDTO.username", expireTime = 60, message = "请勿重复注册")
public Result<Long> createUser(@Valid @RequestBody UserDTO userDTO) {
    Long userId = userService.createUser(userDTO);
    return Result.success(userId, "注册成功");
}
```

**原理**:
- 基于 Redis SETNX 实现
- SpEL 表达式动态生成幂等键
- 60 秒内重复请求直接拦截

**经验总结**:
- 框架提供的幂等性注解简化业务代码
- 幂等键设计应基于业务唯一标识
- 过期时间根据业务场景合理设置

---

#### 5.2 缓存策略

**实现**: 用户查询优先从缓存获取

```java
@Override
public UserDTO getUserById(Long id) {
    // 先从缓存获取
    String cacheKey = USER_CACHE_PREFIX + id;
    User cachedUser = cacheManager.get(cacheKey, User.class);
    
    if (cachedUser != null) {
        return convertToDTO(cachedUser);
    }

    // 从数据库查询
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在");
    }

    // 缓存用户信息
    cacheManager.set(cacheKey, user, USER_CACHE_EXPIRE);
    return convertToDTO(user);
}
```

**缓存清除策略**:

```java
@Override
public boolean updateUser(UserDTO userDTO) {
    // 更新数据库
    int rows = userMapper.updateById(user);
    
    // 清除缓存
    if (rows > 0) {
        String cacheKey = USER_CACHE_PREFIX + user.getId();
        cacheManager.delete(cacheKey);
    }
    
    return rows > 0;
}
```

**经验总结**:
- 读多写少场景优先使用缓存
- 更新/删除操作必须清除缓存
- 缓存过期时间根据数据变更频率设置

---

#### 5.3 Token 黑名单机制

**需求**: 用户登出后 Token 立即失效

**实现**:

```java
@Override
public void logout(String token) {
    // 将 token 加入黑名单
    String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
    Long expireTime = jwtConfig.getExpiration() / 1000;
    cacheManager.set(blacklistKey, "1", expireTime);
    
    log.info("Token已加入黑名单");
}
```

**网关验证逻辑** (预留接口)：

```java
// 验证 Token 是否在黑名单中
String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
if (cacheManager.get(blacklistKey) != null) {
    throw new UnauthorizedException("Token 已失效");
}
```

**经验总结**:
- Token 无法撤销，需要黑名单机制补充
- 黑名单过期时间与 Token 一致
- 网关层统一验证黑名单

---

### 阶段六：参数验证与异常处理

#### 6.1 Jakarta Validation 迁移

**问题**: Spring Boot 3.x 使用 Jakarta 命名空间

**错误示例**:

```java
import javax.validation.constraints.NotBlank;  // 错误
```

**正确写法**:

```java
import jakarta.validation.constraints.NotBlank;  // 正确
```

**DTO 验证示例**:

```java
@Data
public class UserDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为 6-20 位")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

**经验总结**:
- Spring Boot 3.x 全面迁移到 Jakarta EE 9+
- 所有 `javax.*` 包需要替换为 `jakarta.*`
- 使用 `@Valid` 触发参数校验

---

#### 6.2 全局异常处理

**依赖**: 复用 `sunshine-mall-framework-convention` 提供的全局异常处理

**自定义业务异常**:

```java
// 用户不存在
throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在");

// 用户名已存在
throw new BusinessException("USER_ALREADY_EXISTS", "用户名已存在");

// 账号被禁用
throw new BusinessException(BusinessErrorCode.USER_ACCESS_DENIED, "账号已被禁用");
```

**统一响应格式**:

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null,
  "timestamp": 1760958857839,
  "success": false,
  "failure": true
}
```

**经验总结**:
- 使用框架统一异常体系
- 业务异常使用 BusinessException
- 错误码在 convention 模块统一定义

---

## 四、依赖管理总结

### 关键依赖清单

| 依赖 | 版本 | 作用 | 问题与解决 |
|------|------|------|-----------|
| Lombok | 1.18.30 | 简化代码 | 1.18.20 不支持 JDK 17，升级到 1.18.30 |
| BCrypt | Spring Security Crypto | 密码加密 | 替代不安全的 MD5 |
| java-jwt | 4.4.0 | JWT 认证 | 无问题 |
| MyBatis-Plus | 3.5.x | ORM 框架 | 删除无用的 mapper-locations 配置 |
| jackson-datatype-jsr310 | - | Java 8 时间序列化 | 初始缺失，导致 LocalDateTime 序列化失败 |
| Druid | 1.2.x | 连接池 | 无问题 |

### 父模块继承依赖

从 `sunshine-mall-services` 继承：

- spring-boot-starter-web
- spring-cloud-starter-alibaba-nacos-discovery
- spring-cloud-starter-alibaba-nacos-config
- spring-boot-starter-test
- lombok (修复后添加)

---

## 五、代码规范与最佳实践

### 1. 分层职责

- **Controller**: 仅负责接收请求、参数验证、调用 Service
- **Service**: 核心业务逻辑、事务控制
- **Mapper**: 数据库访问，使用 MyBatis-Plus BaseMapper
- **DTO**: 数据传输对象，包含验证注解
- **Entity**: 数据库实体，继承 BaseEntity

### 2. 命名规范

- Controller 方法名：动词 + 名词（getUserById、createUser）
- Service 方法名：业务动作（login、logout、updateUser）
- Mapper 方法名：符合 MyBatis-Plus 规范（selectById、insert）
- 常量命名：大写下划线分隔（USER_CACHE_PREFIX）

### 3. 日志规范

```java
log.info("用户登录成功 - userId: {}, username: {}", user.getId(), user.getUsername());
log.error("生成JWT Token失败", e);
log.debug("缓存命中 - key: {}", cacheKey);
```

### 4. 异常处理

- 使用框架提供的 BusinessException
- 避免捕获异常后吞掉，应向上抛出或记录日志
- 事务方法添加 `@Transactional(rollbackFor = Exception.class)`

---

## 六、测试策略

### 1. 单元测试（待补充）

- Controller 层：MockMvc 测试
- Service 层：Mock Mapper 测试业务逻辑
- Mapper 层：Spring Boot Test 集成测试

### 2. 接口测试

使用 Postman/Apifox 测试所有 REST API：

- 正常场景
- 异常场景（参数缺失、格式错误）
- 边界条件（用户名重复、密码错误）

### 3. 性能测试

- JMeter 压测登录接口
- Redis 缓存命中率统计
- 数据库慢查询分析

---

## 七、待优化项

### 1. 功能增强

- [ ] 手机号验证码登录
- [ ] 第三方登录（微信、QQ）
- [ ] 用户权限角色管理
- [ ] 登录失败次数限制与自动锁定
- [ ] 密码强度校验
- [ ] Token 刷新机制

### 2. 性能优化

- [ ] 分页查询用户列表
- [ ] 查询结果分页缓存
- [ ] 多级缓存（本地缓存 + Redis）
- [ ] 异步日志记录

### 3. 安全加固

- [ ] 接口限流
- [ ] SQL 注入防护（MyBatis-Plus 已提供）
- [ ] XSS 攻击防护
- [ ] CSRF 防护
- [ ] 敏感信息脱敏

### 4. 监控与运维

- [ ] 集成 Spring Boot Actuator
- [ ] Prometheus + Grafana 监控
- [ ] SkyWalking 链路追踪
- [ ] ELK 日志分析

---

## 八、技术难点总结

### 难点 1: 密码加密算法迁移

**挑战**: MD5 → BCrypt 切换导致已有用户无法登录

**解决**:
- 开发环境：清空数据重建
- 生产环境：双重验证 + 自动升级机制

**收获**: 密码安全至关重要，设计之初应选择安全算法

---

### 难点 2: Redis 序列化 Java 8 时间类型

**挑战**: LocalDateTime 无法被 GenericJackson2JsonRedisSerializer 序列化

**解决**:
1. 添加 `jackson-datatype-jsr310` 依赖
2. 配置 ObjectMapper 注册 JavaTimeModule
3. 自定义时间格式化规则

**收获**: 
- Jackson 扩展模块机制
- Redis 序列化配置优化

---

### 难点 3: Lombok 版本兼容性

**挑战**: JDK 17 与 Lombok 1.18.20 不兼容

**解决**: 升级 Lombok 到 1.18.30

**收获**: 
- 版本升级需要全面兼容性测试
- 顶层依赖管理模块统一控制版本

---

### 难点 4: RESTful 路径参数绑定

**挑战**: `PUT /api/user/{id}` 的 id 未传递到 DTO

**解决**: 使用 `@PathVariable` 绑定路径参数

**收获**: 
- RESTful 设计规范
- Spring MVC 参数绑定机制

---

## 九、开发经验沉淀

### 1. 架构设计

- 优先复用框架层能力，避免重复造轮子
- 配置类集中管理配置项，避免 @Value 分散
- 分层架构严格职责分离

### 2. 代码质量

- 使用 Lombok 简化样板代码
- 参数验证使用 Jakarta Validation
- 异常处理统一使用框架异常体系

### 3. 性能优化

- 缓存优先策略降低数据库压力
- 连接池合理配置
- 索引优化提升查询性能

### 4. 安全防护

- 密码加密使用 BCrypt
- JWT 令牌 + 黑名单机制
- 幂等性控制防重复提交

---

## 十、参考资料

- Spring Boot 3.x 官方文档
- MyBatis-Plus 官方文档
- JWT 规范 RFC 7519
- BCrypt 算法原理
- Redis 最佳实践
- RESTful API 设计规范

---

## 十一、总结

User Service 开发过程中遇到了 Lombok 兼容性、密码加密迁移、Redis 序列化等多个技术难题，通过查阅文档、分析错误日志、调整技术方案，最终成功解决所有问题。

核心收获：

1. Maven 多模块依赖管理机制
2. Spring Boot 3.x Jakarta 命名空间迁移
3. BCrypt 密码加密最佳实践
4. Redis 序列化自定义配置
5. RESTful API 设计规范
6. 框架层能力复用思想

这些经验将为后续微服务开发（商品、订单、支付等）提供宝贵参考。

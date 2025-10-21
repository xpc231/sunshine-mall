# User Service 使用文档

## 一、服务概述

User Service 是 Sunshine Mall 微服务架构中的用户服务模块，负责用户管理、认证授权和会员体系功能。

### 核心功能

1. 用户注册与登录
2. JWT 令牌认证
3. 用户信息管理（增删改查）
4. BCrypt 密码加密
5. Redis 缓存支持
6. 幂等性控制

### 技术栈

- JDK 17
- Spring Boot 3.1.5
- Spring Cloud 2022.0.4
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 7.0
- Nacos 2.2.x
- JWT (java-jwt)
- BCrypt 密码加密

---

## 二、快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.9.4+
- MySQL 8.0+
- Redis 7.0+
- Nacos 2.2.x+

### 2. 数据库准备

创建数据库：

```sql
CREATE DATABASE sunshine_mall_user 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;
```

创建用户表：

```sql
USE sunshine_mall_user;

CREATE TABLE t_user (
  id BIGINT PRIMARY KEY COMMENT '用户ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  real_name VARCHAR(50) COMMENT '真实姓名',
  phone VARCHAR(20) UNIQUE COMMENT '手机号',
  email VARCHAR(100) COMMENT '邮箱',
  gender TINYINT DEFAULT 0 COMMENT '性别：0-女，1-男',
  avatar VARCHAR(255) COMMENT '头像URL',
  status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  last_login_time DATETIME COMMENT '最后登录时间',
  login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
  lock_time DATETIME COMMENT '锁定时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  del_flag TINYINT DEFAULT 0 COMMENT '删除标识：0-未删除，1-已删除',
  INDEX idx_username (username),
  INDEX idx_phone (phone),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 3. 配置文件

修改 `application.yml`：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sunshine_mall_user
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

  cloud:
    nacos:
      server-addr: localhost:8848

jwt:
  secret: your-secret-key-here
  expiration: 7200000  # 2小时
```

### 4. 启动服务

```bash
# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run
```

或直接运行主类：`UserServiceApplication`

---

## 三、API 接口文档

### 基础信息

- **Base URL**: `http://localhost:8081/api/user`
- **Content-Type**: `application/json`
- **认证方式**: JWT Bearer Token（除注册和登录接口外）

### 1. 用户注册

**接口**: `POST /api/user/register`

**请求体**:

```json
{
  "username": "testUser01",
  "password": "123456",
  "email": "test@example.com",
  "phone": "13800138000",
  "realName": "张三",
  "gender": 1
}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "注册成功",
  "data": 1760958856946,
  "timestamp": 1760958856946,
  "success": true,
  "failure": false
}
```

**说明**:
- 用户名不能重复
- 密码使用 BCrypt 加密
- 支持幂等性控制，60秒内重复注册会被拦截

---

### 2. 用户登录

**接口**: `POST /api/user/login`

**请求体**:

```json
{
  "username": "testUser01",
  "password": "123456"
}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "登录成功",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "userId": 1760958856946,
    "username": "testUser01",
    "realName": "张三"
  },
  "timestamp": 1760958857839,
  "success": true,
  "failure": false
}
```

**说明**:
- 密码验证使用 BCrypt
- 成功后返回 JWT Token
- Token 有效期 2 小时

---

### 3. 用户登出

**接口**: `POST /api/user/logout`

**请求头**:

```
Authorization: Bearer {token}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "登出成功",
  "data": null,
  "timestamp": 1760958858123,
  "success": true,
  "failure": false
}
```

**说明**:
- Token 会被加入黑名单
- 黑名单在 Redis 中存储，过期时间与 Token 一致

---

### 4. 查询用户信息（按ID）

**接口**: `GET /api/user/{id}`

**请求头**:

```
Authorization: Bearer {token}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 1760958856946,
    "username": "testUser01",
    "realName": "张三",
    "email": "test@example.com",
    "phone": "13800138000",
    "gender": 1,
    "status": 1,
    "createTime": "2025-10-20 19:00:56",
    "updateTime": "2025-10-20 19:00:56"
  },
  "success": true
}
```

---

### 5. 查询用户信息（按用户名）

**接口**: `GET /api/user/username/{username}`

**请求头**:

```
Authorization: Bearer {token}
```

**响应示例**: 同上

---

### 6. 查询所有用户

**接口**: `GET /api/user/list`

**请求头**:

```
Authorization: Bearer {token}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": [
    {
      "id": 1760958856946,
      "username": "testUser01",
      "realName": "张三"
    }
  ],
  "success": true
}
```

---

### 7. 更新用户信息

**接口**: `PUT /api/user/{id}`

**请求头**:

```
Authorization: Bearer {token}
```

**请求体**:

```json
{
  "username": "testUser02",
  "email": "new@example.com",
  "realName": "李四"
}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "更新成功",
  "data": null,
  "success": true
}
```

**说明**:
- URL 中的 id 会自动设置到 DTO 中
- 如果需要修改密码，传入 password 字段会自动加密
- 更新成功后会清除缓存

---

### 8. 删除用户（逻辑删除）

**接口**: `DELETE /api/user/{id}`

**请求头**:

```
Authorization: Bearer {token}
```

**响应示例**:

```json
{
  "code": "SUCCESS",
  "message": "删除成功",
  "data": null,
  "success": true
}
```

**说明**:
- 逻辑删除，只修改 del_flag 字段
- 删除后会清除缓存

---

## 四、错误码说明

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| SUCCESS | 操作成功 | 200 |
| USER_ALREADY_EXISTS | 用户名已存在 | 400 |
| PHONE_ALREADY_EXISTS | 手机号已被注册 | 400 |
| USER_NOT_FOUND | 用户名或密码错误 / 用户不存在 | 404 |
| USER_ACCESS_DENIED | 账号已被禁用 | 403 |
| SYSTEM_PARAM_ERROR | 参数错误 | 400 |
| UPDATE_FAILED | 更新失败 | 500 |
| DELETE_FAILED | 删除失败 | 500 |

---

## 五、安全机制

### 1. 密码加密

- 使用 BCrypt 算法加密密码
- 强度因子：默认 10
- 密码不可逆，无法从加密值还原原文

### 2. JWT 认证

- 使用 HMAC256 算法签名
- Payload 包含：userId、username
- Token 有效期：2 小时
- 黑名单机制：登出后 Token 失效

### 3. 缓存策略

- 用户信息缓存 1 小时
- 更新/删除操作自动清除缓存
- Token 黑名单存储在 Redis

### 4. 幂等性控制

- 注册接口 60 秒内防重复提交
- 基于用户名生成幂等键
- 使用 Redis SETNX 实现

---

## 六、性能优化

### 1. 缓存机制

用户信息查询优先从 Redis 获取，降低数据库压力

### 2. 连接池

- Druid 数据库连接池
- 初始连接数：5
- 最大连接数：20
- Redis Lettuce 连接池

### 3. 索引优化

- username、phone 字段建立唯一索引
- status 字段建立普通索引

---

## 七、监控与日志

### 1. 日志级别

```yaml
logging:
  level:
    com.xpcjsu.sunshinemall: debug
    org.springframework: info
```

### 2. 关键日志

- 用户登录成功/失败
- Token 生成与加入黑名单
- 用户创建/更新/删除
- 缓存命中情况

### 3. SQL 日志

开发环境打印 SQL 语句，生产环境建议关闭

---

## 八、常见问题

### Q1: 登录失败，提示"用户名或密码错误"

**原因**: 数据库中存在旧的 MD5 密码

**解决**: 清空用户表重新注册，或使用 BCrypt 更新现有密码

### Q2: Redis 连接失败

**检查项**:
1. Redis 服务是否启动
2. 配置文件中的 host、port、password 是否正确
3. 防火墙是否开放端口

### Q3: 更新用户信息提示"用户ID不能为空"

**原因**: URL 路径参数未正确绑定

**解决**: 确保使用 `PUT /api/user/{id}` 格式，Controller 已自动绑定

### Q4: 注册提示"请勿重复注册"

**原因**: 60 秒内重复提交了相同用户名的注册请求

**解决**: 等待 60 秒后重试，或使用不同的用户名

---

## 九、开发环境调试

### 1. 本地启动

```bash
# 确保 MySQL、Redis、Nacos 已启动
mvn clean compile
mvn spring-boot:run
```

### 2. 接口测试

推荐使用 Postman、Apifox 等工具测试接口

### 3. 查看日志

```bash
# 实时查看日志
tail -f logs/user-service.log
```

---

## 十、生产部署建议

### 1. 配置优化

```yaml
# 关闭 SQL 日志
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

# 调整日志级别
logging:
  level:
    com.xpcjsu.sunshinemall: info
```

### 2. 连接池调优

根据实际并发量调整 Druid 和 Redis 连接池参数

### 3. JWT 密钥安全

生产环境使用强随机密钥，定期轮换

### 4. 监控告警

集成 Spring Boot Actuator + Prometheus + Grafana

---

## 十一、联系方式

- **项目地址**: https://github.com/xpcjsu/sunshine-mall
- **问题反馈**: 提交 GitHub Issue
- **技术支持**: sunshine-mall@xpcjsu.com

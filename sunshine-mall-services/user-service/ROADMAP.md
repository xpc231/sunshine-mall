# User Service 功能扩展规划与业务关联文档

## 一、当前服务能力概览

### 核心功能

1. 用户注册与登录认证
2. 用户信息管理（增删改查）
3. JWT Token 生成与黑名单管理
4. BCrypt 密码加密
5. 用户信息缓存
6. 注册幂等性控制

### 技术栈

JDK 17、Spring Boot 3.1.5、MyBatis-Plus、Redis 7.0、MySQL 8.0、JWT、BCrypt

---

## 二、功能扩展规划

### 阶段一：安全增强（优先级：P0）

#### 1.1 多因素认证（MFA）

**功能描述**：
- 支持手机验证码二次验证
- 支持邮箱验证码验证
- 支持 TOTP（Time-based One-Time Password）动态口令

**技术方案**：
- 集成短信服务（阿里云 SMS / 腾讯云 SMS）
- 集成邮件服务（Spring Mail）
- TOTP 使用 Google Authenticator 标准

**业务关联**：
- 订单服务：高额订单支付前二次验证
- 支付服务：支付操作前验证
- 会员服务：积分兑换、等级升级验证

**数据库变更**：
```sql
ALTER TABLE t_user ADD COLUMN mfa_enabled TINYINT DEFAULT 0 COMMENT '是否启用多因素认证';
ALTER TABLE t_user ADD COLUMN mfa_secret VARCHAR(100) COMMENT 'TOTP密钥';
ALTER TABLE t_user ADD COLUMN phone_verified TINYINT DEFAULT 0 COMMENT '手机号是否验证';
ALTER TABLE t_user ADD COLUMN email_verified TINYINT DEFAULT 0 COMMENT '邮箱是否验证';
```

---

#### 1.2 登录安全策略

**功能描述**：
- 登录失败次数限制（5次失败锁定30分钟）
- 异常登录检测（异地登录、设备变更）
- 登录日志记录与审计
- 密码强度校验
- 密码定期更换提醒

**技术方案**：
- Redis 记录登录失败次数
- IP 地址库识别登录地理位置
- UserAgent 解析识别设备类型
- 异步任务记录登录日志到 ES

**业务关联**：
- 风控服务：异常行为检测
- 通知服务：异常登录告警
- 日志服务：审计追踪

**数据库变更**：
```sql
CREATE TABLE t_user_login_log (
  id BIGINT PRIMARY KEY COMMENT '日志ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  login_time DATETIME NOT NULL COMMENT '登录时间',
  login_ip VARCHAR(50) COMMENT '登录IP',
  login_location VARCHAR(100) COMMENT '登录地点',
  device_type VARCHAR(50) COMMENT '设备类型',
  browser VARCHAR(50) COMMENT '浏览器',
  status TINYINT COMMENT '登录状态：1-成功，0-失败',
  fail_reason VARCHAR(200) COMMENT '失败原因',
  INDEX idx_user_id (user_id),
  INDEX idx_login_time (login_time)
) ENGINE=InnoDB COMMENT='用户登录日志表';
```

---

#### 1.3 第三方登录集成

**功能描述**：
- 微信登录
- QQ 登录
- 支付宝登录
- 微博登录

**技术方案**：
- OAuth 2.0 授权流程
- 第三方用户信息映射到本地用户
- 支持账号绑定与解绑

**业务关联**：
- 商品服务：社交分享
- 订单服务：微信快捷下单
- 营销服务：社交裂变推广

**数据库变更**：
```sql
CREATE TABLE t_user_oauth (
  id BIGINT PRIMARY KEY COMMENT 'OAuth ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  oauth_type VARCHAR(20) NOT NULL COMMENT 'OAuth类型：wechat/qq/alipay/weibo',
  oauth_id VARCHAR(100) NOT NULL COMMENT '第三方用户ID',
  oauth_name VARCHAR(100) COMMENT '第三方用户名',
  oauth_avatar VARCHAR(255) COMMENT '第三方头像',
  bind_time DATETIME NOT NULL COMMENT '绑定时间',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间',
  UNIQUE KEY uk_oauth (oauth_type, oauth_id),
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='第三方登录绑定表';
```

---

### 阶段二：用户体系完善（优先级：P1）

#### 2.1 用户角色与权限管理（RBAC）

**功能描述**：
- 角色管理（超级管理员、运营人员、客服、普通用户）
- 权限管理（功能权限、数据权限）
- 角色-权限动态绑定
- 用户-角色多对多关联

**技术方案**：
- RBAC（Role-Based Access Control）模型
- Spring Security 集成
- 权限缓存优化（Redis）
- 动态权限加载

**业务关联**：
- 后台管理服务：管理员权限控制
- 商品服务：商品上下架权限
- 订单服务：订单处理权限
- 数据统计服务：数据查看权限

**数据库变更**：
```sql
CREATE TABLE t_role (
  id BIGINT PRIMARY KEY COMMENT '角色ID',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
  description VARCHAR(200) COMMENT '角色描述',
  status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间',
  del_flag TINYINT DEFAULT 0 COMMENT '删除标识'
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE t_permission (
  id BIGINT PRIMARY KEY COMMENT '权限ID',
  permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
  permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
  permission_type TINYINT COMMENT '权限类型：1-菜单，2-按钮，3-接口',
  parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
  path VARCHAR(200) COMMENT '路径',
  status TINYINT DEFAULT 1 COMMENT '状态',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='权限表';

CREATE TABLE t_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (user_id, role_id),
  INDEX idx_role_id (role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE t_role_permission (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (role_id, permission_id),
  INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB COMMENT='角色权限关联表';
```

---

#### 2.2 用户等级与成长体系

**功能描述**：
- 用户等级划分（青铜、白银、黄金、铂金、钻石）
- 经验值积累规则
- 等级权益配置
- 升级任务系统

**技术方案**：
- 规则引擎计算经验值
- 定时任务检查升级条件
- 事件驱动更新经验值

**业务关联**：
- 订单服务：购物获得经验值
- 评价服务：评价获得经验值
- 营销服务：等级专属优惠
- 积分服务：等级积分倍率

**数据库变更**：
```sql
ALTER TABLE t_user ADD COLUMN user_level INT DEFAULT 1 COMMENT '用户等级';
ALTER TABLE t_user ADD COLUMN experience INT DEFAULT 0 COMMENT '经验值';
ALTER TABLE t_user ADD COLUMN next_level_exp INT COMMENT '下一等级所需经验值';

CREATE TABLE t_user_level_config (
  level INT PRIMARY KEY COMMENT '等级',
  level_name VARCHAR(50) NOT NULL COMMENT '等级名称',
  required_exp INT NOT NULL COMMENT '所需经验值',
  benefits JSON COMMENT '等级权益（JSON格式）',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户等级配置表';

CREATE TABLE t_user_experience_log (
  id BIGINT PRIMARY KEY COMMENT '日志ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  exp_change INT NOT NULL COMMENT '经验值变化',
  change_type VARCHAR(50) COMMENT '变化类型：order/review/task',
  relation_id BIGINT COMMENT '关联业务ID',
  description VARCHAR(200) COMMENT '描述',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  INDEX idx_user_id (user_id),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='用户经验值变动日志';
```

---

#### 2.3 用户画像与标签体系

**功能描述**：
- 用户属性标签（年龄、性别、地域、职业）
- 用户行为标签（购买偏好、浏览习惯、价格敏感度）
- 用户价值标签（RFM模型：最近购买、购买频次、购买金额）
- 自动打标与手动打标

**技术方案**：
- 大数据分析平台（Spark / Flink）
- 机器学习算法（聚类、分类）
- 实时标签更新（Kafka + Redis）

**业务关联**：
- 推荐服务：个性化推荐
- 营销服务：精准营销
- 客服服务：智能客服
- 数据分析服务：用户洞察

**数据库变更**：
```sql
CREATE TABLE t_user_tag (
  id BIGINT PRIMARY KEY COMMENT '标签ID',
  tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
  tag_code VARCHAR(50) NOT NULL UNIQUE COMMENT '标签编码',
  tag_category VARCHAR(50) COMMENT '标签分类',
  description VARCHAR(200) COMMENT '标签描述',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户标签表';

CREATE TABLE t_user_tag_relation (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  tag_value VARCHAR(100) COMMENT '标签值',
  tag_source VARCHAR(50) COMMENT '标签来源：auto/manual',
  create_time DATETIME NOT NULL COMMENT '创建时间',
  update_time DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (user_id, tag_id),
  INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB COMMENT='用户标签关联表';
```

---

### 阶段三：高级功能（优先级：P2）

#### 3.1 用户隐私与数据脱敏

**功能描述**：
- 个人信息导出（符合 GDPR）
- 账号注销（数据清除）
- 敏感信息脱敏展示
- 隐私设置管理

**技术方案**：
- 使用框架提供的 StringUtils 脱敏工具
- 数据导出异步任务
- 账号注销延迟删除（30天冷静期）

**业务关联**：
- 所有服务：数据脱敏展示
- 日志服务：脱敏日志记录
- 数据分析服务：匿名化数据分析

---

#### 3.2 用户行为分析

**功能描述**：
- 用户访问轨迹记录
- 用户行为漏斗分析
- 用户留存率分析
- 用户活跃度分析

**技术方案**：
- 埋点 SDK（前端/移动端）
- 实时数据流处理（Flink）
- 数据仓库存储（ClickHouse / Hive）
- 可视化报表（Grafana / DataV）

**业务关联**：
- 商品服务：商品浏览行为
- 订单服务：购物流程漏斗
- 营销服务：活动效果分析
- 推荐服务：协同过滤推荐

---

#### 3.3 用户反馈与投诉管理

**功能描述**：
- 用户反馈提交
- 投诉工单管理
- 反馈分类与优先级
- 反馈处理流程

**技术方案**：
- 工单系统（自研 / Zendesk）
- 自动分配客服
- NLP 情感分析

**业务关联**：
- 客服服务：工单处理
- 订单服务：订单投诉
- 商品服务：商品质量反馈
- 物流服务：物流投诉

---

## 三、业务关联分析

### 3.1 核心关联服务

#### 与订单服务（Order Service）的关联

**当前关联**：
- 订单创建时需要验证用户身份（JWT Token）
- 订单查询需要用户ID

**扩展关联**：
- 用户等级影响订单优惠
- 用户标签影响推荐商品
- 用户地址管理（收货地址）
- 用户订单行为反馈到用户画像
- 高额订单需要二次验证（MFA）

**接口依赖**：
```
用户服务 → 订单服务
- POST /api/order/create (携带用户Token)
- GET /api/order/{id} (验证用户权限)

订单服务 → 用户服务
- GET /api/user/{userId} (获取用户信息)
- GET /api/user/address/{userId} (获取收货地址)
```

**数据流转**：
```
用户下单 → 验证用户Token → 获取用户等级 → 计算折扣 → 创建订单 → 更新用户经验值
```

---

#### 与商品服务（Product Service）的关联

**当前关联**：
- 浏览商品无需登录
- 收藏商品需要用户ID
- 评价商品需要用户身份验证

**扩展关联**：
- 用户浏览历史记录
- 用户收藏夹管理
- 用户购物车（可能独立为 Cart Service）
- 基于用户画像的个性化推荐
- 用户评价反馈到信誉体系

**接口依赖**：
```
用户服务 → 商品服务
- GET /api/product/recommend (基于用户标签推荐)

商品服务 → 用户服务
- GET /api/user/{userId}/tags (获取用户标签)
- POST /api/user/behavior/log (记录浏览行为)
```

**数据流转**：
```
用户浏览商品 → 记录行为日志 → 更新用户标签 → 个性化推荐 → 提升转化率
```

---

#### 与支付服务（Pay Service）的关联

**当前关联**：
- 支付操作需要验证用户身份
- 支付成功后通知用户

**扩展关联**：
- 高额支付需要 MFA 验证
- 支付方式绑定（微信、支付宝）
- 支付密码管理
- 支付行为风控检测
- 支付成功积分奖励

**接口依赖**：
```
支付服务 → 用户服务
- POST /api/user/verify/mfa (多因素认证)
- GET /api/user/{userId}/risk-level (风险等级)

用户服务 → 支付服务
- GET /api/pay/method/{userId} (获取支付方式)
```

**数据流转**：
```
发起支付 → MFA验证 → 风控检测 → 执行支付 → 更新用户积分/经验值 → 记录支付行为
```

---

#### 与营销服务（Marketing Service）的关联

**当前关联**：
- 优惠券发放需要用户ID
- 活动参与需要用户身份

**扩展关联**：
- 基于用户等级的专属优惠
- 基于用户标签的精准营销
- 用户邀请码系统
- 积分兑换优惠券
- 会员专属活动

**接口依赖**：
```
营销服务 → 用户服务
- GET /api/user/{userId}/level (获取用户等级)
- GET /api/user/tags/filter (按标签筛选用户)
- POST /api/user/invite-code (生成邀请码)

用户服务 → 营销服务
- GET /api/marketing/coupon/available (可用优惠券)
```

**数据流转**：
```
营销活动创建 → 筛选目标用户（按标签/等级） → 发送通知 → 用户领取优惠券 → 使用优惠券 → 记录营销效果
```

---

#### 与会员服务（Member Service）的关联

**当前关联**：
- 会员开通需要用户ID
- 会员权益查询

**扩展关联**：
- 会员等级体系（可能与用户等级独立）
- 会员积分系统
- 会员专属客服
- 会员生日特权
- 会员续费提醒

**接口依赖**：
```
会员服务 → 用户服务
- GET /api/user/{userId} (获取用户基本信息)
- PUT /api/user/{userId}/member-level (更新会员等级)

用户服务 → 会员服务
- GET /api/member/{userId}/info (获取会员信息)
- GET /api/member/{userId}/points (获取积分余额)
```

**数据流转**：
```
开通会员 → 更新用户会员等级 → 获得会员权益 → 积分累积 → 积分兑换 → 会员续费
```

---

#### 与物流服务（Logistics Service）的关联

**当前关联**：
- 物流信息推送给用户
- 用户查询物流状态

**扩展关联**：
- 用户默认收货地址管理
- 用户收货地址库
- 物流异常投诉
- 物流评价反馈

**接口依赖**：
```
物流服务 → 用户服务
- GET /api/user/{userId}/address/default (获取默认地址)
- POST /api/user/feedback (物流投诉反馈)

用户服务 → 物流服务
- GET /api/logistics/track/{orderId} (查询物流)
```

**数据流转**：
```
订单发货 → 获取用户收货地址 → 物流追踪 → 推送物流状态 → 用户确认收货 → 物流评价
```

---

#### 与客服服务（Customer Service）的关联

**当前关联**：
- 在线客服需要用户身份
- 工单系统用户信息

**扩展关联**：
- 基于用户等级的客服优先级
- 用户历史咨询记录
- 智能客服机器人（基于用户画像）
- VIP 用户专属客服

**接口依赖**：
```
客服服务 → 用户服务
- GET /api/user/{userId}/profile (获取用户画像)
- GET /api/user/{userId}/order-history (订单历史)

用户服务 → 客服服务
- POST /api/cs/session/create (创建客服会话)
- GET /api/cs/session/{userId}/history (咨询历史)
```

---

#### 与通知服务（Notification Service）的关联

**当前关联**：
- 登录成功通知
- 密码修改通知

**扩展关联**：
- 站内信通知
- 短信通知（验证码、营销）
- 邮件通知（账单、活动）
- Push 推送（App）
- 微信公众号/小程序消息

**接口依赖**：
```
通知服务 → 用户服务
- GET /api/user/{userId}/notification-settings (通知偏好设置)

用户服务 → 通知服务
- POST /api/notify/sms (发送短信)
- POST /api/notify/email (发送邮件)
- POST /api/notify/push (推送通知)
```

**数据流转**：
```
业务事件触发 → 获取用户通知偏好 → 选择通知渠道 → 发送通知 → 记录通知日志 → 用户已读确认
```

---

### 3.2 支撑服务关联

#### 与网关服务（Gateway Service）的关联

**当前关联**：
- JWT Token 生成在 User Service
- Token 验证应在 Gateway

**优化建议**：
- Gateway 统一 Token 验证
- Gateway 统一黑名单检查
- Gateway 统一限流策略
- Gateway 提取用户信息传递给下游服务

**数据流转**：
```
用户请求 → Gateway 验证 Token → 检查黑名单 → 提取用户ID → 添加到Header → 转发到服务
```

---

#### 与搜索服务（Search Service）的关联

**扩展关联**：
- 基于用户历史搜索的智能提示
- 基于用户标签的搜索结果排序
- 用户搜索行为分析

**接口依赖**：
```
搜索服务 → 用户服务
- GET /api/user/{userId}/search-history (搜索历史)
- GET /api/user/{userId}/preferences (用户偏好)
```

---

#### 与数据分析服务（Analytics Service）的关联

**扩展关联**：
- 用户行为数据采集
- 用户留存率分析
- 用户价值分析（RFM）
- 用户画像生成

**数据流转**：
```
用户行为埋点 → Kafka → Flink实时处理 → 更新用户画像 → Redis缓存 → 实时推荐
```

---

## 四、系统架构演进路线

### 当前架构：单体用户服务

```
用户服务（User Service）
├── 用户管理
├── 认证授权
├── 信息查询
└── 缓存管理
```

### 演进阶段一：职责分离

```
用户核心服务（User Core Service）
├── 用户基本信息管理
├── 密码管理
└── 账号状态管理

认证服务（Auth Service）
├── 登录认证
├── Token 管理
├── OAuth 第三方登录
└── MFA 多因素认证

权限服务（Permission Service）
├── RBAC 权限管理
├── 角色管理
└── 权限校验
```

### 演进阶段二：微服务拆分

```
用户中心（User Center）
├── 用户核心服务（User Core）
├── 认证服务（Auth Service）
├── 权限服务（Permission Service）
├── 会员服务（Member Service）
├── 积分服务（Points Service）
└── 用户画像服务（User Profile Service）
```

---

## 五、技术债务与优化方向

### 5.1 性能优化

1. **缓存策略优化**
   - 多级缓存（本地缓存 + Redis）
   - 缓存预热
   - 缓存更新策略优化

2. **数据库优化**
   - 读写分离
   - 分库分表（用户量达到千万级）
   - 索引优化

3. **异步处理**
   - 登录日志异步记录
   - 用户行为异步分析
   - 通知异步发送

### 5.2 安全加固

1. **Token 安全**
   - Token 刷新机制
   - Token 加密传输
   - Token 定期轮换

2. **数据安全**
   - 敏感信息加密存储
   - 数据库字段级加密
   - 传输层 HTTPS

3. **接口安全**
   - 接口签名验证
   - 防重放攻击
   - SQL 注入防护

### 5.3 可观测性

1. **日志体系**
   - 结构化日志
   - 分布式链路追踪（SkyWalking）
   - 日志聚合（ELK）

2. **监控告警**
   - Prometheus + Grafana
   - 业务指标监控（登录成功率、注册转化率）
   - 性能指标监控（响应时间、吞吐量）

3. **审计合规**
   - 操作审计日志
   - 数据变更记录
   - 合规性报告

---

## 六、实施优先级矩阵

### 紧急且重要（立即实施）

- 登录安全策略（登录失败锁定、异常登录检测）
- 密码强度校验
- 敏感信息脱敏
- 缓存穿透防护

### 重要不紧急（近期规划）

- 多因素认证（MFA）
- 第三方登录集成
- 用户等级体系
- RBAC 权限管理
- 用户地址管理

### 紧急不重要（资源允许时）

- 登录日志记录
- 用户反馈系统
- 分页查询优化

### 不紧急不重要（长期规划）

- 用户画像体系
- 行为分析系统
- 机器学习推荐

---

## 七、风险评估与应对

### 7.1 技术风险

**风险点**：用户数据丢失

**应对措施**：
- 数据库主从备份
- 定期数据备份
- 灾难恢复演练

**风险点**：高并发登录压力

**应对措施**：
- 限流策略（Sentinel）
- 缓存预热
- 水平扩展

### 7.2 业务风险

**风险点**：用户隐私泄露

**应对措施**：
- 数据脱敏
- 访问权限控制
- 审计日志

**风险点**：账号安全（盗号、撞库）

**应对措施**：
- MFA 强制启用
- 异常登录检测
- 密码定期更换

---

## 八、总结

### 核心扩展方向

1. **安全增强**：MFA、登录安全、第三方登录
2. **用户体系**：等级体系、权限管理、用户画像
3. **业务关联**：与订单、商品、支付、营销等服务深度集成

### 关键业务关联

- **订单服务**：身份验证、等级优惠、地址管理
- **商品服务**：浏览历史、收藏管理、个性化推荐
- **支付服务**：MFA 验证、风控检测、支付方式管理
- **营销服务**：精准营销、会员权益、邀请码系统
- **会员服务**：会员等级、积分体系、专属权益

### 演进路线

**短期**（1-3个月）：安全加固、基础功能完善  
**中期**（3-6个月）：用户体系建设、业务深度集成  
**长期**（6-12个月）：微服务拆分、智能化升级

---

**文档版本**：v1.0  
**更新时间**：2025-10-20  
**维护团队**：User Service Team

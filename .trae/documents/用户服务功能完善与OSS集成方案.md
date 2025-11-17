## 现状与定位

* 用户服务已具备用户名密码登录、JWT发放与登出黑名单、基础CRUD，未实现短信登录、自动登录（刷新）、每日签到、收货地址管理、头像上传。

* 订单服务内嵌收货地址字段，未有独立地址域；存在`comment_time`字段但无评价系统。

* 商品服务使用图片URL字段，未集成对象存储与上传能力。

## 归属建议

* 每日签到：归属用户服务（用户行为与积分/成长值关联）。

* 短信登录：归属用户服务（认证入口的一种）。

* 自动登录（刷新Token/记住我）：归属用户服务（令牌生命周期管理）。

* 收货地址管理：归属用户服务（用户私域数据，订单仅复制快照）。

* 评价功能：归属商品服务（按商品维度聚合与展示，写入需订单校验）。

* 图片与头像：商品图片归商品服务、用户头像归用户服务；统一使用阿里云OSS。

## 每日签到（用户服务）

基于redis的bitmap实现。

* 数据模型：`user_sign_in`（userId, signDate, continueDays, rewardPoints, createTime）。日粒度唯一约束（userId+signDate）。

* 缓存：`sign:streak:{userId}`记录连续天数，过期与DB回填一致。

* 接口：

  * `POST /api/user/sign-in`（当日签到，幂等；返回连续天数与奖励）。

  * `GET /api/user/sign-in/stats`（最近30天签到日历与连续天数）。

* 规则：同日仅一次；出现断签重置连续计数；奖励简单积分增量，避免过度设计。

## 短信登录（用户服务）

暂时不考虑接入第三方生成短信服务，在本地生成短信打印到控制台即可。

* 验证码生成与校验：

  * `POST /api/user/login/sms/send`（入参手机号；生成6位验证码，`redis`存`login:sms:{phone}`TTL 5分钟；滑动窗口限频5次/小时）。

  * `POST /api/user/login/sms/verify`（手机号+验证码；校验通过后发放JWT）。

* 设备策略：默认多设备；若需单设备可在`redis`维护用户当前有效设备标识。

* 供应商适配：预留`SmsSender`接口，开发环境使用日志模拟；正式环境对接阿里云短信即可，无需改变控制器。

## 自动登录与刷新（用户服务）

Refresh Token存储在LocalStorage和redis中。

* 令牌形态：`accessToken`（TTL短，如30分钟）+`refreshToken`（TTL长，如7天）。

* 存储：`redis`维护`refresh:{userId}:{rtId}`与黑名单键；登出时将`accessToken`加入黑名单。

* 接口：

  * `POST /api/user/token/refresh`（入参refreshToken；返回新accessToken与新refreshToken，旧RT失效）。

  * 登录接口增加`rememberMe`布尔参数，决定refreshToken TTL策略。

* 网关：沿用现有JWT校验；对`/token/refresh`走白名单。

## 收货地址管理（用户服务）

* 数据模型：`user_address`（id, userId, name, phone, province, city, district, address, isDefault, createTime, updateTime）。

* 接口：

  * `POST /api/user/addresses`（新增，默认地址互斥）。

  * `PUT /api/user/addresses/{id}`（更新）。

  * `DELETE /api/user/addresses/{id}`（删除）。

  * `GET /api/user/addresses`（列表，含默认）。

* 订单对接：订单创建支持`addressId`入参；订单服务拉取地址并复制为快照写入订单，避免后续用户改动影响历史订单。

## 评价功能（商品服务）

* 数据模型：`product_comment`（id, productId, skuId?, orderNo, userId, score, content, images\[], createTime, auditStatus）。

* 约束：同用户对同订单的同商品只允许一次评价（唯一约束）。

* 接口：

  * `POST /api/product/{productId}/comments`（校验用户是否购买该商品：通过Feign调用订单服务验证订单状态/明细；成功后写入评论并更新商品评价计数与平均分）。

  * `GET /api/product/{productId}/comments`（分页列表）。

* 展示：商品详情页聚合评价统计；用户服务可增加“我的评价”视图但数据源仍来自商品服务。

## 阿里云OSS集成

* 依赖：在商品服务与用户服务分别引入阿里云OSS SDK；配置参考`product`服务风格，放入Nacos共享配置（`oss.endpoint/bucket/accessKey/secretKey`）。

* 上传策略：

  * 方案A（服务端直传，简单）：`MultipartFile`上传到服务，再由后端`PutObject`到OSS，返回URL；限制文件大小与类型。

  * 方案B（前端直传，性能好）：后端提供STS临时凭证与`policy`签名，前端直传OSS；后端仅校验与记录URL。

* 接口：

  * 用户服务：`POST /api/user/avatar/upload`（A方案）或`GET /api/user/avatar/upload/credential`（B方案）；字段`avatar`写回用户。

  * 商品服务：`POST /api/product/images/upload`与`GET /api/product/images/upload/credential`；更新`mainImage/subImages`。

* Key命名：`user/avatar/{userId}/{yyyyMMdd}/{uuid}.jpg`、`product/images/{productId}/{yyyyMMdd}/{uuid}.jpg`。

## 安全与一致性

* 统一从网关透传`userId`，服务内部通过`UserContext`获取。

* 构造函数依赖注入；Redis序列化与配置与`product`服务一致；响应统一使用`Result<T>`。

* 验证码、刷新、上传等接口加入限流与尺寸校验；避免过度设计，不引入复杂风控。

## 最小改动清单（按服务）

* 用户服务：新增签到、短信登录、刷新令牌、地址管理、头像上传接口与对应实体/Mapper/Service；JWT与Redis沿用现有配置。

* 商品服务：新增评论实体与接口、图片上传与凭证接口；对接订单服务校验购买。

* 网关：开放短信发送/校验与令牌刷新接口；其余按既有鉴权。

请确认以上归属与最小实现范围，我将按此方案开始分阶段落地（优先用户服务：地址、短信登录、刷新；随后签到；

商品服务：评论与OSS先暂时不实现，先实现完用户，然后再做决定。

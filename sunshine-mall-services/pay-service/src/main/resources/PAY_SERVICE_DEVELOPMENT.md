# 支付服务开发文档（Pay Service）

本文档面向开发与运维人员，说明支付服务的接口规范、业务流程、异常与安全、版本变更与兼容性。遵循项目工作空间规则：统一使用中文、避免过度设计、保持与 product-service 风格一致、Redis 配置保持一致、依赖注入使用构造函数注入。

## 一、通用约定

- 服务上下文路径：`/pay`（对应 application.yml 中 server.servlet.context-path）
- API 基础路径：`/api/pay`
- 统一请求头：`userId`（必填，Long），由网关或上游服务注入
- 统一返回体：`Result<T>`
  - 字段：`code`（字符串，成功为"0"）、`message`、`data`、`timestamp`
  - 成功：`code = "0"`，`message = "操作成功"`
  - 失败：`code = BusinessErrorCode.*`（字符串），`message = 默认或具体错误信息`
- 内容类型：除渠道回调外，均为 `application/json`
- 说明中的完整 URL 采用本地示例：`http://localhost:8085/pay/...`，网关转发后可能不带 `/pay` 上下文，请以实际路由为准。

## 二、API 接口详情（当前已实现）

1) 创建支付
- 方法与路径：`POST /pay/api/pay/create`
- 请求头：`userId: Long`（必填）
- 请求体（JSON）：
  - `orderNo` String 必填：订单编号
  - `amount` Number 必填：支付金额（单位：元，>0）
  - `payType` Integer 必填：支付方式（1-支付宝，2-微信；其他值返回模拟链接）
  - `subject` String 选填：交易标题
  - `body` String 选填：交易描述
  - `clientToken` String 必填：幂等令牌（前端生成 UUID 等）
- 返回体（Result<PayCreateResponse>）：
  - `data.payId` Long：支付记录ID
  - `data.payNo` String：支付单号（以 P 开头）
  - `data.payUrl` String：二维码或唤起链接（未配置密钥时返回模拟链接）
- 可能错误：
  - `USER_NOT_LOGIN` 用户未登录（缺少/非法 userId）
  - `SYSTEM_PARAM_ERROR` 参数非法（金额<=0、缺少必要字段等）
- 调用示例（PowerShell）：
```
curl -X POST "http://localhost:8085/pay/api/pay/create" ^
  -H "Content-Type: application/json" ^
  -H "userId: 20001" ^
  --data-binary "{\"orderNo\":\"ORD202501010002\",\"amount\":288.00,\"payType\":1,\"subject\":\"订单支付\",\"clientToken\":\"PAY-ORD202501010002-uuid\"}"
```

2) 查询支付
- 方法与路径：`GET /pay/api/pay/{payNo}`
- 路径变量：`payNo` String 必填（以 P 开头）
- 返回体（Result<PayQueryResponse>）：
  - `data.payId` Long
  - `data.payNo` String
  - `data.status` Integer：见“支付状态枚举”
  - `data.channelOrderNo` String：渠道订单号（存在时返回）
- 可能错误：
  - `SYSTEM_PARAM_ERROR` 参数非法（payNo 为空）
  - `PAYMENT_FAILED` 支付记录不存在
- 示例：
```
curl -X GET "http://localhost:8085/pay/api/pay/P1234567890"
```

3) 模拟支付成功回调（联调用）
- 方法与路径：`POST /pay/api/pay/mock/callback`
- 请求头：`userId: Long`（必填）
- 请求体（JSON）：
  - `payNo` String 必填
  - `channelTradeNo` String 选填：模拟渠道交易号
- 返回体（Result<Boolean>）：
  - `data = true` 表示处理成功并已通知订单服务
- 可能错误：
  - `USER_NOT_LOGIN` 用户未登录
  - `SYSTEM_PARAM_ERROR` 参数非法（payNo 为空）
  - `PAYMENT_FAILED` 支付记录不存在
- 示例：
```
curl -X POST "http://localhost:8085/pay/api/pay/mock/callback" ^
  -H "Content-Type: application/json" ^
  -H "userId: 20001" ^
  --data-binary "{\"payNo\":\"P1234567890\",\"channelTradeNo\":\"ALIPAY-20250101-0001\"}"
```

4) 支付宝渠道回调（异步通知）
- 方法与路径：`POST /pay/api/pay/notify/alipay`
- 内容类型：`application/x-www-form-urlencoded`
- 表单参数（最小集）：
  - `out_trade_no` String 必填：我们的支付单号（payNo）
  - `trade_no` String 选填：支付宝交易号
  - `trade_status` String 选填：`TRADE_SUCCESS` 等
- 返回：纯文本 `success` 或 `failure`（符合支付宝回调协议）
- 说明：当前为占位实现，验签通过逻辑待密钥与 SDK 接入后启用；记录通知日志并更新支付状态为成功后，调用订单服务的“支付成功”接口。
- 示例：
```
curl -X POST "http://localhost:8085/pay/api/pay/notify/alipay" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "out_trade_no=P1234567890" -d "trade_status=TRADE_SUCCESS" -d "trade_no=202501010001"
```

5) 微信渠道回调（异步通知，V3 占位）
- 方法与路径：`POST /pay/api/pay/notify/wechat`
- 请求头（占位）：`Wechatpay-Serial`、`Wechatpay-Signature`、`Wechatpay-Timestamp`、`Wechatpay-Nonce`
- 请求体（JSON 最小占位）：
  - 顶层或 `resource` 中包含 `out_trade_no`、`transaction_id`
- 返回：`success` 或 `failure`（text/plain）
- 说明：当前为占位实现，未真实验签与解密；从明文 JSON 中提取 `out_trade_no` 与 `transaction_id` 用于联调。
- 示例：
```
curl -X POST "http://localhost:8085/pay/api/pay/notify/wechat" ^
  -H "Content-Type: application/json" ^
  -H "Wechatpay-Timestamp: 1733044800" ^
  -H "Wechatpay-Nonce: abc123" ^
  --data-binary "{\"out_trade_no\":\"P1234567890\",\"transaction_id\":\"4200000-202501010001\"}"
```

6) 创建退款
- 方法与路径：`POST /pay/api/pay/refund/create`
- 请求头：`userId: Long`（必填）
- 请求体（JSON）：
  - `orderNo` String 必填：订单编号
  - `amount` Number 必填：退款金额（>0）
  - `reason` String 选填
- 返回体（Result<RefundCreateResponse>）：
  - `data.refundId` Long
  - `data.refundNo` String（以 R 开头）
  - `data.status` Integer：`0-申请中`、`1-成功`、`2-失败`
- 可能错误：
  - `USER_NOT_LOGIN` 用户未登录
  - `SYSTEM_PARAM_ERROR` 参数非法
  - `PAYMENT_FAILED` 订单未支付或支付记录缺失
- 示例：
```
curl -X POST "http://localhost:8085/pay/api/pay/refund/create" ^
  -H "Content-Type: application/json" ^
  -H "userId: 20001" ^
  --data-binary "{\"orderNo\":\"ORD202501010002\",\"amount\":10.00,\"reason\":\"用户申请退款\"}"
```

## 三、状态枚举与错误码

- 支付状态（PayStatus）：`0 INIT`、`1 SUCCESS`、`2 FAIL`、`3 CLOSED`、`4 REFUND_PART`、`5 REFUND_ALL`
- 退款状态（RefundStatus）：`0 APPLYING`、`1 SUCCESS`、`2 FAIL`
- 常用错误码（取自 BusinessErrorCode）：
  - `USER_NOT_LOGIN` 用户未登录
  - `SYSTEM_PARAM_ERROR` 参数错误
  - `PAYMENT_FAILED` 支付失败/记录不存在/订单未支付
  - `SYSTEM_BUSY` 系统繁忙
  - `SYSTEM_ERROR` 系统异常

## 四、注意事项（联调与生产）

- 上下文路径：本服务默认 `/pay`，通过网关转发后可能省略，请以网关暴露路径为准。
- 幂等控制：
  - 创建支付：`userId + clientToken`（注解 Idempotent 实现）
  - 创建退款：`userId + orderNo`
  - 渠道回调：支付宝以 `out_trade_no`，微信以 `timestamp + nonce`
- 回调公网可达：`notifyUrl` 必须可公网访问；本地开发可使用内网穿透（如 natapp、frp）。
- 验签与解密：当前为占位实现；真实环境需对支付宝/微信回调进行签名校验、证书校验与报文解密。
- 日志与审计：所有回调均记录到 `pay_notify_log`，用于问题排查与审计。
- 避免过度设计：未配置密钥时返回模拟二维码链接，待后续迭代接入官方 SDK。

## 五、版本变更记录

- v1.0.0（初始）
  - 实现：创建支付、查询支付、模拟支付回调、创建退款、渠道占位（支付宝/微信）
  - 返回体：统一使用 Result<T>
- v1.1.0（MVC 重构与回调优化）
  - 重构：`PayCallbackController` 严格 MVC，业务逻辑迁移到 `PayCallbackService`
  - 新增：微信回调幂等（按 `timestamp + nonce`），日志记录与订单通知完善
  - 路径调整：回调路径统一为 `/api/pay/notify/{channel}`（原文档中的 `/callback/*` 已废弃）
  - 统一请求头：由 `X-User-Id` 调整为 `userId`
- v1.1.1（缺陷修复与配置补充）
  - 修复：支付宝回调在查询支付记录为空时的 NPE 问题
  - 配置：在 `application.yml` 中补充支付宝/微信占位配置与 `notifyUrl`

## 六、兼容性说明

- 路由兼容：如果通过网关暴露路径为 `/api/pay/...`（不含 `/pay`），请以实际路径为准；服务内仍按 `/pay/api/pay/...`。
- 请求头兼容：历史文档使用 `X-User-Id`，现统一为 `userId`；如网关仍注入 `X-User-Id`，请在网关完成映射。
- 回调兼容：当前为占位实现，未进行真实验签与解密；上线前需替换为 SDK 实现并完成证书与密钥配置。

## 七、快速联调指引

- 开发流程建议：先创建支付得到 `payNo` → 浏览器扫码（模拟链接）→ 触发模拟或渠道回调 → 查询支付状态 → 如需测试退款，先走支付成功流程再创建退款。
- 推荐测试工具：Postman / VSCode REST Client / curl（Windows 终端建议使用 PowerShell）。
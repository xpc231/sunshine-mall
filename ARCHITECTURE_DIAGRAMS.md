# Sunshine Mall 架构图（技术栈与系统应用分层）

说明：本文件提供两套统一风格的架构图，采用 Mermaid 绘制，便于在 IDE/Markdown 预览中直接查看与维护。遵循项目工作区规则：全中文表达、避免过度设计、暂不考虑物流/AI服务的实现细节、风格与 product-service 保持一致、Redis 配置与 product-service 一致、依赖注入采用构造函数注入。

目录：
- 一、微服务架构技术栈图（组件与关系、通信机制、运维体系）
- 二、系统应用架构图（分层设计、关键技术选型、高可用与安全）
- 三、设计要点与架构优势
- 四、查看与维护建议

---

## 一、微服务架构技术栈图

```mermaid
flowchart LR
    %% 终端入口
    subgraph Clients[用户终端]
      PC[Web/PC]:::client --> NG[Nginx/WAF]:::infra
      IOS[iOS]:::client --> NG
      AND[Android]:::client --> NG
      H5[H5/小程序]:::client --> NG
      ADM[Admin/运营]:::client --> NG
    end

    %% 网关层
    NG --> GW[Spring Cloud Gateway\n统一认证/路由/灰度/限流]:::gateway

    %% 注册与配置中心
    GW -- 服务发现 --> NAC[Nacos 集群\n注册中心/配置中心]:::core

    %% 业务微服务（参考已实现模块）
    subgraph Services[业务微服务集群]
      USR[user-service]:::svc
      PROD[product-service]:::svc
      ORD[order-service]:::svc
      PAY[pay-service]:::svc
    end

    %% 服务注册与配置拉取
    USR -- 注册/配置 --> NAC
    PROD -- 注册/配置 --> NAC
    ORD -- 注册/配置 --> NAC
    PAY -- 注册/配置 --> NAC
    GW -- 路由表/动态规则 --> NAC

    %% 服务间通信
    ORD -- REST/Feign --> PROD
    ORD -- REST/Feign --> USR
    PAY -- REST/Feign --> ORD
    ORD -- 异步事件 --> MQ[RocketMQ 集群]:::core
    PAY -- 支付结果事件 --> MQ

    %% 数据与缓存
    subgraph DataStores[数据存储与缓存]
      RDS[(Redis 集群\n与product-service配置一致)]:::store
      DB[(MySQL 实例\n按服务独立库)]:::store
    end

    USR --- DB
    PROD --- DB
    ORD  --- DB
    PAY  --- DB
    USR --- RDS
    PROD --- RDS
    ORD  --- RDS

    %% 可观测与运维
    subgraph Observability[运维支撑体系]
      SW[SkyWalking OAP/UI\n链路追踪]:::ops
      SEN[Sentinel\n流控/熔断/降级]:::ops
      LOG[统一日志/Slf4j]:::ops
    end

    USR -- Tracing --> SW
    PROD -- Tracing --> SW
    ORD  -- Tracing --> SW
    PAY  -- Tracing --> SW
    USR -- 流控/熔断 --> SEN
    PROD -- 流控/熔断 --> SEN
    ORD  -- 流控/熔断 --> SEN
    PAY  -- 流控/熔断 --> SEN

    %% 持续交付与部署
    subgraph Delivery[持续交付与部署]
      MVN[Maven 构建]:::infra
      JK[Jenkins CI/CD]:::infra
      DK[Docker 镜像]:::infra
      K8S[Kubernetes 部署/扩缩容/滚动升级]:::infra
    end

    MVN --> JK --> DK --> K8S
    GW --- K8S
    USR --- K8S
    PROD --- K8S
    ORD  --- K8S
    PAY  --- K8S

    classDef client fill:#fef3c7,stroke:#f59e0b,stroke-width:1px;
    classDef infra fill:#e5e7eb,stroke:#374151,stroke-width:1px;
    classDef gateway fill:#dbeafe,stroke:#1d4ed8,stroke-width:1px;
    classDef core fill:#e0e7ff,stroke:#4f46e5,stroke-width:1px;
    classDef svc fill:#dcfce7,stroke:#16a34a,stroke-width:1px;
    classDef store fill:#fde68a,stroke:#ca8a04,stroke-width:1px;
    classDef ops fill:#fee2e2,stroke:#dc2626,stroke-width:1px;
```

要点：
- 服务注册与发现、配置中心：Nacos（集群），网关与微服务统一从 Nacos 拉取配置、参与注册。
- 服务通信机制：内部同步走 REST + OpenFeign；异步使用 RocketMQ；暂不使用 gRPC（如需可扩展）。
- 运维支撑：SkyWalking 链路追踪，Sentinel 流控/熔断，统一日志；满足观测与稳定性要求。
- 缓存与数据：Redis 与 product-service 配置一致；MySQL 按服务独立库，后续可演进主从与分库分表。
- 交付与部署：Maven 构建、Jenkins流水线、Docker 镜像、Kubernetes 部署（多副本、滚动升级、弹性扩缩容）。

---

## 二、系统应用架构图（分层）

```mermaid
flowchart TB
    %% 应用端层
    subgraph L1[应用端层]
      PC[Web/PC]:::client
      IOS[iOS]:::client
      AND[Android]:::client
      H5[H5/小程序]:::client
      ADM[Admin/运营后台]:::client
    end

    %% 安全入口与网关
    PC --> NG[Nginx/WAF]:::infra
    IOS --> NG
    AND --> NG
    H5 --> NG
    ADM --> NG
    NG --> GW[Spring Cloud Gateway\n统一认证/鉴权/路由/限流]:::gateway

    %% 应用架构层（微服务划分）
    subgraph L2[应用架构层]
      USR[user-service\n用户与权限]:::svc
      PROD[product-service\n商品/类目/库存接口]:::svc
      ORD[order-service\n订单/购物车]:::svc
      PAY[pay-service\n支付对接]:::svc
    end
    GW --> USR
    GW --> PROD
    GW --> ORD
    GW --> PAY

    %% 中间件层
    subgraph L3[中间件层]
      NAC[Nacos\n注册/配置中心]:::core
      MQ[RocketMQ\n消息队列]:::core
      RDS[(Redis\n缓存/会话/热点数据)]:::store
      SEN[Sentinel\n流控/熔断/降级]:::ops
      SW[SkyWalking\n链路追踪]:::ops
    end

    USR -- 注册/配置 --> NAC
    PROD -- 注册/配置 --> NAC
    ORD -- 注册/配置 --> NAC
    PAY -- 注册/配置 --> NAC

    ORD -- REST/Feign --> PROD
    ORD -- REST/Feign --> USR
    PAY -- REST/Feign --> ORD
    ORD -- 异步事件 --> MQ

    USR --- RDS
    PROD --- RDS
    ORD  --- RDS

    USR -- Tracing --> SW
    PROD -- Tracing --> SW
    ORD  -- Tracing --> SW
    PAY  -- Tracing --> SW

    USR -- 保护 --> SEN
    PROD -- 保护 --> SEN
    ORD  -- 保护 --> SEN
    PAY  -- 保护 --> SEN

    %% 数据存储层
    subgraph L4[数据存储层]
      DB1[(MySQL-User)]:::store
      DB2[(MySQL-Product)]:::store
      DB3[(MySQL-Order)]:::store
      DB4[(MySQL-Pay)]:::store
    end

    USR --- DB1
    PROD --- DB2
    ORD  --- DB3
    PAY  --- DB4

    %% 高可用/容错/扩展性提示
    classDef client fill:#fef3c7,stroke:#f59e0b,stroke-width:1px;
    classDef infra fill:#e5e7eb,stroke:#374151,stroke-width:1px;
    classDef gateway fill:#dbeafe,stroke:#1d4ed8,stroke-width:1px;
    classDef core fill:#e0e7ff,stroke:#4f46e5,stroke-width:1px;
    classDef svc fill:#dcfce7,stroke:#16a34a,stroke-width:1px;
    classDef store fill:#fde68a,stroke:#ca8a04,stroke-width:1px;
    classDef ops fill:#fee2e2,stroke:#dc2626,stroke-width:1px;
```

关键技术选型与设计：
- 网关：Spring Cloud Gateway（统一认证在网关，服务内去除JWT），支持灰度、限流与黑白名单。
- 注册/配置：Nacos 集群，统一配置下发（如 Redis 连接、DB 连接等），服务按构造函数注入依赖。
- 通信机制：REST + OpenFeign 为主；RocketMQ 作为异步事件通道；暂不使用 gRPC。
- 缓存：Redis（与 product-service 配置一致），作为热点数据与会话存储；支持穿透/雪崩/击穿防护（参考 product-service 缓存策略）。
- 数据：MySQL 按服务独立库；可平滑演进主从、读写分离与分库分表（不做过度设计）。
- 稳定性：Sentinel（流控/熔断/降级）；幂等（使用 idempotent 框架）；统一日志与链路追踪（SkyWalking）。
- 部署与运维：Jenkins + Maven 构建，Docker 镜像，Kubernetes 部署；多副本、滚动升级、健康检查与自动重启。
- 安全防护：Nginx/WAF 前置；网关鉴权、参数校验与限流；内部服务最小权限与接口黑白名单；敏感数据脱敏日志。

---

## 三、设计要点与架构优势
- 一致的技术栈：Spring Boot + Spring Cloud（Gateway、OpenFeign、Nacos）+ Redis + RocketMQ + MySQL + SkyWalking + Sentinel。
- 清晰的服务边界：user/product/order/pay 独立自治，网关统一入口，降低耦合。
- 可靠的通信与事件驱动：同步 REST 保证简单直观；异步 MQ 保证解耦与削峰。
- 可观测与稳定性：SkyWalking 端到端追踪、统一日志；Sentinel 的限流熔断，结合幂等与重试策略提升韧性。
- 持续交付与弹性扩展：Jenkins 流水线、Docker 化、K8s 部署，多副本与滚动升级确保高可用。
- 简洁而不失扩展：当前不引入 gRPC/复杂分库；保留演进空间，避免过度设计。

---

## 四、查看与维护建议
- 在 VS Code/IntelliJ 安装 Mermaid 插件或开启 Markdown 预览，即可渲染本图。
- 如需扩展到更多微服务（如物流/AI），建议以“可选层”或“预留节点”标注，避免影响当前实现与测试。
- Redis、Nacos、RocketMQ、MySQL 的集群地址与参数统一由 Nacos 配置中心管理，保持与 product-service 的风格与配置一致。
- 文档与代码演进时，优先复用已完成组件与通用框架（convention、idempotent、cache、database等），减少重复造轮子。
```
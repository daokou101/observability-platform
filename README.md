# 微服务可观测性平台 (Observability Platform)

基于 Spring Cloud Alibaba 的微服务可观测性管理平台，提供全链路追踪、动态采样、Sentinel 流控、灰度路由等能力。

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Nginx (Frontend)                         │
│                     Vue 3 + Element Plus                        │
├─────────────────────────────────────────────────────────────────┤
│                Spring Cloud Gateway (8080)                      │
│  ┌─────────────┐ ┌──────────────┐ ┌──────────────────────────┐  │
│  │ TraceId     │ │ Sampling     │ │ Grayscale                │  │
│  │ GlobalFilter│ │ Log Filter   │ │ GlobalFilter             │  │
│  └─────────────┘ └──────┬───────┘ └──────────────────────────┘  │
│                         │                                        │
│                    RabbitMQ                                      │
├─────────────────────────┴───────────────────────────────────────┤
│              Platform Service (8081)                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │ 日志中心  │ │ 动态路由  │ │ Sentinel │ │   灰度路由       │   │
│  │          │ │ 管理     │ │ 流控管理  │ │   管理           │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌────────────────────────────────┐   │
│  │ 服务发现  │ │ 告警管理  │ │   定时告警检查 (@Scheduled)    │   │
│  └──────────┘ └──────────┘ └────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│  Nacos         Redis        RabbitMQ         MySQL              │
│ (服务发现+     (缓存+       (异步日志         (持久化)           │
│  配置中心)     采样控制)    消息队列)                            │
└─────────────────────────────────────────────────────────────────┘
```

## 核心技术栈

| 组件 | 技术 |
|------|------|
| 服务网关 | Spring Cloud Gateway 4.x |
| 注册/配置中心 | Nacos 2.3.x |
| 限流降级 | Sentinel 1.8.x + Nacos push 模式 |
| 数据持久化 | MySQL 8.0 + MyBatis-Plus 3.5 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3.12 |
| 前端 | Vue 3 + Element Plus + ECharts |
| Java 版本 | 17 |

## 核心功能

### 1. Trace ID 全链路追踪

Gateway 入口为每个请求生成全局唯一 Trace ID（UUID），通过请求头 `X-Trace-Id` 向下游传递，响应头也携带该 ID，实现请求链路的端到端关联。

**面试亮点**: 自定义 GlobalFilter 在 Gateway 入口注入，无侵入式全链路追踪。

### 2. 动态日志采样

- 按服务名配置采样率（0-100%），存储在 MySQL 并通过 Redis 缓存
- Gateway 过滤器根据采样率决定是否采集日志
- 采集的日志通过 RabbitMQ 异步写入 MySQL（按月分区表）
- 采样率可在管理页面动态调整，无需重启

**面试亮点**: 分库分表思想（按月分区）、MQ 异步削峰、采样控制避免日志淹没。

### 3. 动态路由管理

基于 Nacos 配置中心和 Spring Cloud Gateway 的 `RouteDefinitionWriter`，支持可视化创建/编辑/启停路由规则。

**面试亮点**: 路由的热加载机制，`RouteDefinitionWriter` + `RefreshRoutesEvent` 事件驱动。

### 4. Sentinel 流控规则管理

流控规则存储在 MySQL，支持 QPS/线程数阈值、直接/关联/链路模式、快速失败/Warm Up/排队等待效果。规则变更通过 `FlowRuleManager.loadRules()` 实时同步到 Sentinel 内存。

**面试亮点**: Sentinel 核心概念（资源、阈值、流控模式、流控效果）、规则动态下发。

### 5. 灰度路由

支持三种灰度策略：
- **用户哈希**: `X-User-Id` 的 hash 值模 100，落在配置区间内即路由到灰度版本
- **IP 范围**: 按客户端 IP 前缀匹配
- **请求头**: 按请求头精确匹配

规则存储在 MySQL，Gateway 通过 Redis 读取规则，匹配时在请求中添加 `X-Gray-Version` 头。

**面试亮点**: 灰度发布策略实现、Gateway 自定义负载均衡配合。

### 6. 告警管理

支持基于错误数、错误率、平均耗时的告警规则，`@Scheduled` 定时检查，阈值超限自动记录告警日志。

### 7. 服务发现可视化

通过 Nacos OpenAPI (`NamingService`) 查询注册的服务列表、实例健康状态、元数据。

### 8. 日志中心

按服务名、Trace ID、状态码过滤查询网关日志，支持分页。

## 快速启动

### 前置条件

- Docker & Docker Compose
- JDK 17+ (仅本地开发)
- Node.js 20+ (仅前端开发)
- Maven 3.9+ (仅本地构建)

### Docker 一键启动

```bash
# 克隆项目
git clone https://github.com/daokou101/observability-platform.git
cd observability-platform

# 启动所有服务
docker compose up --build

# 访问前端
open http://localhost

# 访问 Nacos 控制台
open http://localhost:8848/nacos
```

### 本地开发

```bash
# 1. 启动依赖服务
docker compose up -d mysql redis rabbitmq nacos

# 2. 构建后端
mvn clean install -DskipTests

# 3. 启动 platform-service
mvn spring-boot:run -pl platform-service

# 4. 启动 gateway
mvn spring-boot:run -pl gateway

# 5. 启动前端
cd frontend
npm install
npm run dev
```

## 服务端口

| 服务 | 端口 | Docker 内地址 |
|------|------|---------------|
| 前端 (Nginx) | 80 | frontend |
| Gateway | 8080 | gateway |
| Platform Service | 8081 | platform-service |
| Nacos | 8848 | nacos:8848 |
| MySQL | 3308 | mysql:3306 |
| Redis | 6381 | redis:6379 |
| RabbitMQ | 5673 (AMQP) / 15673 (管理) | rabbitmq:5672 |

## 项目结构

```
observability-platform/
├── common/                    # 公共模块
│   └── src/main/java/com/obs/platform/common/
│       ├── api/              # Result, ResultCode, PageResult
│       ├── constant/         # TraceConstant
│       └── exception/        # GlobalExceptionHandler, BusinessException
├── gateway/                  # 网关模块
│   └── src/main/java/com/obs/platform/
│       ├── filter/           # TraceIdGlobalFilter, SamplingLogFilter, GrayscaleGlobalFilter
│       └── config/           # NacosDynamicRouteConfig
├── platform-service/         # 后端服务
│   └── src/main/java/com/obs/platform/
│       ├── controller/       # 7 个 REST 控制器
│       ├── service/          # 业务逻辑层
│       ├── mapper/           # 7 个 MyBatis Mapper
│       ├── entity/           # 7 个数据实体
│       ├── config/           # RabbitMQ, Redis, MyBatis-Plus 配置
│       └── mq/               # RabbitMQ 消费者
├── frontend/                 # Vue 3 前端
│   └── src/
│       ├── views/            # 8 个页面组件
│       ├── api/              # Axios 封装
│       └── router/           # 路由配置
├── sql/                      # 初始化 SQL
├── docker-compose.yml
└── pom.xml                   # 父 POM
```

## 面试考点

### 核心原理
- Spring Cloud Gateway 的请求/响应生命周期和 Filter 链
- Nacos 服务注册发现机制（心跳、健康检查、保护阈值）
- Sentinel 流控原理（滑动窗口、Leaky Bucket、Warm Up）
- 灰度路由的负载均衡配合（Metadata + LoadBalancer）
- MQ 异步解耦在日志采集中的应用

### 常见问题
**Q: 为什么不用 ELK 做日志？**
A: MySQL 按月分区+定期清理满足中小规模场景，减少基础设施复杂度。ES 更适合全文搜索场景。

**Q: Trace ID 如何跨服务传递？**
A: Gateway 入口生成 UUID 通过 HTTP Header `X-Trace-Id` 透传，RestTemplate/Feign 通过拦截器自动传递。

**Q: Sentinel 规则如何动态生效？**
A: 规则存储在 MySQL，变更时通过 `FlowRuleManager.loadRules()` 同步到 Sentinel 内存。生产环境通过 Nacos 配置中心 push 模式下发。

## 开发规划

- [x] 项目骨架和公共模块
- [x] 5 个后端实体 + 5 个 Mapper + 5 个 Service + 5 个 Controller
- [x] 前端 8 个管理页面
- [x] Docker Compose 7 容器编排
- [x] Sentinel 流控规则管理
- [x] 灰度路由功能
- [x] 告警管理
- [x] 仪表盘 ECharts 图表
- [x] 单元测试
- [ ] GitHub Actions CI
- [ ] 更多 Sentinel 规则类型（降级、热点、系统）

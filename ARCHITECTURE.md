# AI 智能面试刷题平台 — 架构文档（Java 核心 + 现代化增强）

## 项目概览

用户上传学习资料，AI 生成试卷/面试问题，自动评分解析，错题复习，语音面试模拟。

架构目标：在保留 Java 企业级核心技术体系的基础上，引入可控的现代化能力（异步事件驱动、可观测性、AI 评测治理、云原生交付），实现“稳定可维护 + 持续演进”。

## 架构原则

- 核心业务稳定优先：经典 Java 分层与事务模型不变
- 新能力渐进引入：先可用，再增强，不一次性重构
- AI 能力平台化：统一调用、统一评测、统一审计
- 全链路可观测：接口、任务、模型调用均可追踪
- 成本与质量平衡：缓存 + 模型路由 + 回归评测

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Next.js 15 + TypeScript + Tailwind CSS |
| 网关/反向代理 | Nginx |
| 后端核心 | Spring Boot 3.x + Spring MVC + Spring Validation |
| 持久层 | MyBatis + MyBatis XML + PageHelper |
| 安全 | Spring Security + JWT + RBAC |
| 关系型数据库 | MySQL 8.x |
| 向量检索 | Milvus（主）+ 可选 pgvector（演进） |
| 缓存/会话 | Redis + Spring Session |
| 任务与异步 | Spring Task / Quartz + RabbitMQ（增强） |
| LLM 接入 | OpenAI（主）/ DeepSeek（备）/ Qwen-VL（图像），统一 LLM Gateway 封装 |
| 韧性治理 | Resilience4j（重试/熔断/限流/隔离） |
| API 标准化 | OpenAPI 3 + Problem Details（RFC 7807） |
| 可观测性 | Micrometer + Prometheus + Grafana + OpenTelemetry |
| 构建与测试 | Maven + JUnit 5 + Mockito + Spring Boot Test + Testcontainers |
| 迁移与运维 | Flyway + Logback + Actuator |
| 部署 | Docker Compose（起步）→ Kubernetes（增强） |

## 后端架构风格（保留传统核心）

采用“传统分层 + 端口适配器思想”：

- Controller：入参校验、鉴权、统一响应
- Application Service：用例编排、事务边界
- Domain：领域对象与规则
- Repository/Mapper：MyBatis XML 数据持久化
- Integration Adapter：LLM、Milvus、对象存储、MQ 等外部系统

说明：

- 编码与协作保持传统 Java 工程习惯
- 外部能力通过适配器接入，避免污染领域层

## 模块划分

### 1. 资料管理模块
- 上传 PDF/Word/MD/图片
- 文档解析（异步任务）
- 分块 + 向量化（Milvus）
- 资料列表 / 删除 / 预览
- 解析状态机（待处理/处理中/成功/失败）

### 2. 刷题引擎模块
- 用户选择资料范围 + 出题策略（实时/缓存）
- AI 生成题目（单选/多选/简答/编程）
- 用户作答
- AI 评分 + 逐题解析
- 错题自动入库 + 回看复习 + 标记掌握
- 评分输出 Schema 校验 + 规则二次校验

### 3. 面试模拟模块
- 岗位/技术栈/难度设定
- WebSocket 面试会话
- AI 面试官流式提问
- 文本/语音交互
- 结束后异步生成综合报告

### 4. 学习分析模块
- 刷题趋势统计（日/周/月）
- 薄弱知识点识别
- 学习建议生成
- 报告快照归档（可追溯）

### 5. 错题本模块
- 按知识点/时间筛选
- 错题回看重做
- 掌握状态管理
- 反复错题统计

### 6. 用户与权限模块
- 登录/注册
- RBAC 权限模型（用户/管理员）
- JWT + Refresh Token
- 审计日志

## 数据库核心表

```
users
roles
user_roles
materials
material_chunks
questions
quiz_records
wrong_book
knowledge_points
question_kp_rel
interview_sessions
interview_messages
learning_reports
llm_call_logs
async_task_records
idempotency_records
```

补充说明：

- `llm_call_logs`：记录模型、token、耗时、错误码、重试次数
- `async_task_records`：任务状态、重试、失败原因
- `idempotency_records`：关键写操作幂等控制

## Prompt 与 AI 治理体系

```
prompts/
├── quiz-generate.st
├── quiz-evaluate.st
├── interview-interviewer.st
└── interview-report.st
```

治理策略：

- Prompt 版本管理（灰度切换）
- 固定 JSON Schema 输出
- 建立标准评测集（题目质量/评分一致性/解释质量）
- 每次 Prompt 或模型切换触发自动回归

## 核心流程

### 刷题闭环
上传资料 → 异步解析 → 分块向量化 → 选择范围/策略 → AI 出题 → 作答 → AI 评分解析 → 错题入库 → 复习

### 面试闭环
设定岗位 → WebSocket 会话 → AI 连续提问 → 用户回答 → 流式互动 → 结束 → 异步生成报告

## API 与契约规范

- RESTful API + OpenAPI 3 文档自动生成
- 统一响应封装（成功/失败）
- 错误响应采用 Problem Details（RFC 7807）
- WebSocket 消息体标准化（事件类型 + traceId + payload）

## 韧性与稳定性设计

- Resilience4j：
  - Timeout：限制外部 LLM/向量检索超时
  - Retry：可控重试（指数退避）
  - Circuit Breaker：故障熔断
  - RateLimiter/Bulkhead：限流与隔离
- 幂等：提交答案、任务回调、报告生成引入幂等键
- 降级：主模型不可用时自动切换备用模型

## 可观测性设计

- Metrics：QPS、RT、错误率、任务堆积、模型调用成功率
- Tracing：HTTP → Service → DB/Redis/MQ/LLM 全链路 Trace
- Logging：结构化日志（traceId、userId、sessionId）
- 告警：任务失败率、模型超时率、WebSocket 异常断连率

## 后端工程结构建议（Maven 多模块）

```
backend/
├── pom.xml
├── interview-api
├── interview-application
├── interview-domain
├── interview-infrastructure
├── interview-ai-gateway
└── interview-common
```

建议职责：

- `interview-ai-gateway`：统一模型路由、Prompt 装配、输出校验、调用日志
- `interview-infrastructure`：MyBatis、Redis、MQ、Milvus 等外部实现

## 部署演进路线

### 阶段 A（MVP，2-4 周）
- 单体部署：Nginx + Spring Boot + MySQL + Redis + Milvus
- 完成：资料管理、刷题、错题本、基础鉴权
- 引入：基础缓存、基础调用日志

### 阶段 B（增强，4-8 周）
- 引入 RabbitMQ 处理重任务
- 完成面试模拟与学习分析
- 上线 Prompt 版本管理与评测回归
- 接入 Resilience4j

### 阶段 C（生产化，持续）
- 容器化与 K8s 部署
- OpenTelemetry + Prometheus + Grafana 全量观测
- 模型路由优化（成本优先/质量优先策略）
- 灰度发布与回滚机制

## 可行性结论

- 高可行：核心技术均为成熟 Java 生态能力
- 可扩展：通过 Adapter/Gateway 设计承接 AI 能力快速演进
- 可运营：具备日志、指标、追踪、评测与成本治理基础
- 可控风险：采用分阶段落地，避免一次性高风险改造

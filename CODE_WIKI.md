# AI 智能面试刷题平台 - Code Wiki

> **版本**: 0.1.0-SNAPSHOT
> **更新日期**: 2026-05-13
> **项目地址**: /Users/liuyongze/Documents/New project

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈总览](#2-技术栈总览)
3. [架构设计](#3-架构设计)
4. [后端模块详解](#4-后端模块详解)
5. [前端结构](#5-前端结构)
6. [数据库设计](#6-数据库设计)
7. [核心功能实现](#7-核心功能实现)
8. [API接口文档](#8-api接口文档)
9. [依赖关系](#9-依赖关系)
10. [项目运行指南](#10-项目运行指南)
11. [项目文件索引](#11-项目文件索引)

---

## 1. 项目概述

### 1.1 项目简介

**AI Interview Platform** 是一个全栈智能面试刷题平台，采用前后端分离架构。用户可以上传学习资料，AI 自动解析并生成面试题目，支持多种题型（单选、多选、简答、编程），提供 AI 评分解析、错题复习和面试模拟等功能。

### 1.2 核心功能模块

| 模块 | 功能描述 |
|------|----------|
| **资料管理** | 上传 PDF/Word/MD/TXT/CSV/JSON/图片，异步解析，分块向量化 |
| **刷题引擎** | AI 生成题目，用户作答，AI 评分解析，错题自动入库 |
| **面试模拟** | WebSocket 实时会话，AI 面试官流式提问，生成面试报告 |
| **错题本** | 错题记录，掌握状态管理，反复错题统计 |
| **用户系统** | 登录注册，JWT 认证，多厂商 LLM 设置 |
| **AI 评测** | Prompt/模型质量回归测试，Eval Framework |

### 1.3 项目统计

| 指标 | 数值 |
|------|------|
| 后端模块数 | 6 个 Maven 模块 |
| 前端页面数 | 12 个主要页面 |
| 核心服务类 | 20+ 个 |
| 数据库表 | 16 张核心表 |
| Flyway 迁移 | 12 个版本 |

---

## 2. 技术栈总览

### 2.1 后端技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| **核心框架** | Spring Boot | 3.3.5 |
| **编程语言** | Java | 21 |
| **持久层** | MyBatis + XML | 3.0.4 |
| **数据库** | MySQL | 8.4 |
| **缓存** | Redis | 7.2 |
| **向量数据库** | Milvus | 2.4 |
| **消息队列** | RabbitMQ | 3.13 |
| **安全** | Spring Security + JWT | - |
| **韧性治理** | Resilience4j | 2.2.0 |
| **可观测性** | Micrometer + Prometheus + Grafana | - |
| **链路追踪** | OpenTelemetry + Zipkin | 1.43.0 |
| **数据库迁移** | Flyway | - |
| **构建工具** | Maven | 3.9+ |

### 2.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Next.js** | 15.x | React 全栈框架（App Router） |
| **React** | 19.x | UI 库 |
| **TypeScript** | 5.7.x | 类型安全 |
| **Tailwind CSS** | 3.4.x | 样式框架 |
| **Framer Motion** | 12.x | 动画库 |
| **Zustand** | 5.x | 状态管理 |
| **Lucide React** | 0.468.x | 图标库 |
| **Playwright** | 1.59.x | E2E 测试 |

### 2.3 基础设施

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| MySQL | mysql:8.4 | 3306 | 关系数据库 |
| Redis | redis:7.2-alpine | 6379 | 缓存/会话/Token 黑名单 |
| Milvus | milvusdb/milvus:v2.4.23 | 19530 | 向量检索 |
| etcd | quay.io/coreos/etcd:v3.5.18 | 2379 | Milvus 元数据存储 |
| MinIO | minio/minio | 9000 | Milvus 对象存储 |
| RabbitMQ | rabbitmq:3.13-management | 5672/15672 | 消息队列 |
| Prometheus | prom/prometheus:v2.55.0 | 9090 | 指标收集 |
| Grafana | grafana/grafana:11.3.0 | 3001 | 可视化面板 |
| Zipkin | openzipkin/zipkin:3 | 9411 | 链路追踪 |

---

## 3. 架构设计

### 3.1 整体架构风格

项目采用**传统分层架构 + 端口适配器模式**的混合架构：

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Next.js 15)                         │
│                   127.0.0.1:3000 → /api/* 代理到后端             │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP / WebSocket
┌──────────────────────────────▼──────────────────────────────────┐
│                   interview-api (Web / 入口层)                   │
│              Controller / Security / JWT / WebSocket            │
├─────────────────────────────────────────────────────────────────┤
│               interview-application (应用层)                      │
│                  Application Services / DTO / Ports             │
├─────────────────────────────────────────────────────────────────┤
│                   interview-domain (领域层)                       │
│                   Domain Models / Repository 接口                 │
├─────────────────────────────────────────────────────────────────┤
│              interview-infrastructure (基础设施层)                 │
│              RepositoryImpl / MyBatis Mapper / PO              │
├─────────────────────────────────────────────────────────────────┤
│                interview-ai-gateway (AI 网关层)                   │
│              LlmGateway / OpenAI / DeepSeek / MultiVendor        │
├─────────────────────────────────────────────────────────────────┤
│                   interview-common (通用层)                       │
│                   ApiResponse / Exceptions / Utils               │
└──────────────────────────────┬──────────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
      ┌──────────┐      ┌──────────┐      ┌──────────┐
      │  MySQL   │      │  Redis   │      │  Milvus  │
      │  :3306   │      │  :6379   │      │  :19530  │
      └──────────┘      └──────────┘      └──────────┘
            │
            ▼
      ┌──────────┐
      │ RabbitMQ │
      │  :5672   │
      └──────────┘
```

### 3.2 请求处理流程

```
HTTP 请求
  → JwtAuthenticationFilter (Token 解析与认证)
    → Controller (入参校验、统一响应)
      → ApplicationService (用例编排、事务边界)
        → Domain Repository (接口)
          → Infrastructure RepositoryImpl (MyBatis Mapper)
            → MySQL / Redis / Milvus
```

### 3.3 异步任务流程

```
上传资料
  → MaterialApplicationService.uploadAndCreateParseTask()
    → 创建 Material 记录 (MySQL)
    → 创建 AsyncTaskRecord (状态=PENDING, taskNo=PARSE-xxx)
  → RabbitMQ (已配置) / Scheduler 扫描 PENDING 任务
    → MaterialParseTaskListener / MaterialParseTaskProcessor
      → 解析文件内容 → 分块 → 向量化 (Milvus)
      → 更新 AsyncTaskRecord (状态=SUCCESS/FAILED)
```

### 3.4 架构设计原则

| 原则 | 说明 |
|------|------|
| **核心业务稳定优先** | 经典 Java 分层与事务模型不变 |
| **新能力渐进引入** | 先可用，再增强，不一次性重构 |
| **AI 能力平台化** | 统一调用、统一评测、统一审计 |
| **全链路可观测** | 接口、任务、模型调用均可追踪 |
| **成本与质量平衡** | 缓存 + 模型路由 + 回归评测 |

---

## 4. 后端模块详解

### 4.1 模块概览

```
backend/
├── pom.xml                    # 父 POM，管理版本依赖
├── interview-common/          # 通用层：无业务依赖的工具类
├── interview-domain/          # 领域层：纯 Java records + 接口
├── interview-application/     # 应用层：用例编排、DTO、Port 接口
├── interview-infrastructure/  # 基础设施层：Repository 实现、MyBatis
├── interview-ai-gateway/      # AI 网关层：LLM 统一封装
└── interview-api/             # API 层：Controller、Security、WebSocket
```

### 4.2 interview-common（通用层）

**职责**：全局共享工具、API 响应封装、异常定义、常量定义。

| 类名 | 描述 |
|------|------|
| `ApiResponse<T>` | 统一响应 record，包含 `success`/`message`/`data` 字段 |
| `ResourceNotFoundException` | 资源未找到异常 |
| `UnauthorizedException` | 未授权异常 |
| `TaskConstants` | 任务类型常量（如 `TASK_TYPE_PARSE`、`STATUS_PENDING` 等） |
| `LlmProviderNormalizer` | LLM 提供商名称标准化工具 |

**依赖**：仅 `spring-context`、`spring-web`、`jakarta.annotation-api`，无业务依赖。

### 4.3 interview-domain（领域层）

**职责**：核心领域模型和仓储接口。纯 Java records + 接口，零外部依赖。

#### 4.3.1 领域模型

| 模型 | 类型 | 描述 |
|------|------|------|
| `User` | record | 用户模型 |
| `Material` | record | 学习资料 |
| `MaterialChunk` | record | 资料分块 |
| `MaterialChunkDraft` | record | 分块草稿 |
| `AsyncTaskRecord` | record | 异步任务记录 |
| `Question` | record | 题目 |
| `InterviewSession` | record | 面试会话 |
| `WrongBook` | record | 错题本 |
| `UserLlmSettings` | record | 用户 LLM 设置 |
| `PromptTemplate` | record | Prompt 模板 |
| `EvalRun` | record | 评测运行 |
| `EvalCase` | record | 评测用例 |
| `EvalResult` | record | 评测结果 |

#### 4.3.2 仓储接口

| 接口 | 关键方法 |
|------|----------|
| `UserRepository` | `findByUsername`、`findById`、`save` |
| `MaterialRepository` | `save`、`findById`、`findAllByUserId`、`deleteById` |
| `MaterialChunkRepository` | `save`、`findByMaterialId`、`deleteByMaterialId` |
| `AsyncTaskRecordRepository` | `create`、`findByTaskNo`、`findByBizId`、`findPendingTasks`、`updateStatus`、`updateProgress` |
| `QuestionRepository` | `save`、`findById`、`findByUserId`、`findByMaterialIds` |
| `InterviewSessionRepository` | `save`、`findById`、`findActiveByUserId`、`updateStatus` |
| `WrongBookRepository` | `save`、`findById`、`findByUserId`、`findByUserIdAndMasteryStatus`、`deleteById` |
| `UserLlmSettingsRepository` | `findByUserId`、`save`、`upsert` |
| `PromptTemplateRepository` | `findByNameAndVersion`、`findActiveByName` |
| `EvalRunRepository` | `save`、`findByRunKey`、`findAll` |
| `EvalCaseRepository` | `save`、`findByRunKey` |
| `EvalResultRepository` | `save`、`findByRunKey` |

**依赖**：零外部依赖，纯 Java。

### 4.4 interview-application（应用层）

**职责**：用例编排、事务边界、DTO 定义、Port 接口定义。

#### 4.4.1 应用服务

| 服务类 | 描述 |
|--------|------|
| `AuthApplicationService` | 认证：登录、注册、刷新 Token、验证码集成、登录尝试追踪 |
| `MaterialApplicationService` | 资料用例：上传、创建解析任务、按用户查询列表、删除、重试解析 |
| `AsyncTaskApplicationService` | 异步任务用例：按 taskNo 查询 |
| `ChatApplicationService` | 与 AI 聊天，发送消息获取响应 |
| `QuizApplicationService` | 题目生成：从资料生成题目、查询最近题目 |
| `WrongBookApplicationService` | 错题本管理：添加、列表、更新掌握状态、删除 |
| `EvalService` | AI 评测框架：运行评测、列表运行、列表用例 |
| `UserLlmSettingsApplicationService` | 用户 LLM 设置：获取、更新 |
| `MaterialRagApplicationService` | RAG（检索增强生成）：从 Milvus 检索相关分块 |
| `MaterialParseTaskProcessor` | 处理资料解析任务：解析文件、分块、向量化 |
| `TokenService` | Token 生成和解析接口 |
| `TokenBlacklistService` | JWT Token 黑名单管理（Redis） |
| `CaptchaService` | 验证码生成 |
| `LoginAttemptService` | 登录尝试追踪和锁定 |
| `PromptTemplateService` | Prompt 模板加载和渲染 |

#### 4.4.2 DTO 定义

| DTO | 描述 |
|-----|------|
| `LoginCommand` | 登录请求 |
| `LoginResult` | 登录响应（token、refreshToken） |
| `RegisterCommand` | 注册请求 |
| `RefreshTokenCommand` | 刷新请求 |
| `LogoutCommand` | 登出请求 |
| `CaptchaResponse` | 验证码响应 |
| `GenerateQuizCommand` | 生成题目请求 |
| `GeneratedQuizResult` | 生成题目响应 |
| `UploadMaterialResult` | 上传响应 |
| `ChatRequest` | 聊天请求 |
| `ChatResponse` | 聊天响应 |
| `CreateSessionRequest` | 创建会话请求 |
| `AddWrongBookCommand` | 添加错题请求 |
| `UpdateMasteryCommand` | 更新掌握状态请求 |
| `WrongBookItem` | 错题条目视图 |
| `UpdateUserLlmSettingsCommand` | 更新 LLM 设置请求 |
| `UserLlmSettingsView` | 用户 LLM 设置视图 |

#### 4.4.3 Port 接口（外部能力抽象）

| 接口 | 描述 |
|------|------|
| `LlmGateway` | LLM 调用抽象：`chat(Long userId, String prompt)` |
| `AsyncTaskDispatcher` | 异步任务分发抽象 |
| `VectorStoreGateway` | 向量存储抽象：`upsert`、`search`、`delete` |

#### 4.4.4 quiz 子包

| 类 | 描述 |
|----|------|
| `QuizPromptBuilder` | 构建题目生成的 Prompt |
| `QuizGenerationPolicy` | 题目生成策略 |
| `QuizFallbackQuestionFactory` | 回退题目工厂 |
| `StructuredQuizPayload` | 结构化题目负载 |
| `StructuredQuizPayloadParser` | 解析 LLM 返回的结构化数据 |
| `QuestionDraft` | 题目草稿 |

**依赖**：`interview-domain` + `spring-context` + `spring-tx` + `spring-security-crypto` + `jakarta.validation-api`

### 4.5 interview-api（API 层）

**职责**：HTTP 入口、安全配置、JWT 认证、WebSocket、请求路由、定时任务。

#### 4.5.1 控制器

| 控制器 | 基础路径 | 描述 |
|--------|----------|------|
| `AuthController` | `/api/v1/auth` | 登录、注册、验证码、刷新 Token、登出 |
| `MaterialController` | `/api/v1/materials` | 上传（multipart）、列表、删除、重试解析 |
| `QuizController` | `/api/v1/quizzes` | 生成题目、最近题目 |
| `InterviewController` | `/api/v1/interviews` | 创建/获取/恢复面试会话 |
| `ChatController` | `/api/v1/chat` | AI 聊天 |
| `WrongBookController` | `/api/v1/wrong-books` | 添加、列表、更新掌握状态、删除错题 |
| `EvalController` | `/api/v1/eval` | 触发评测、列表运行、列表用例 |
| `AsyncTaskController` | `/api/v1/async-tasks` | 查询异步任务状态 |
| `UserLlmSettingsController` | `/api/v1/llm/settings` | 获取/更新用户 LLM 设置 |
| `HealthController` | `/api/v1/health` | 健康检查 |

#### 4.5.2 配置类

| 类 | 描述 |
|----|------|
| `InterviewApiApplication` | Spring Boot 入口，`@MapperScan` |
| `SecurityConfig` | Spring Security：无状态会话、JWT 过滤器、路径权限 |
| `JwtAuthenticationFilter` | JWT Token 解析过滤器，从 `Authorization: Bearer` 提取用户 |
| `JwtTokenService` | JWT Token 生成和解析（实现 `TokenService`） |
| `InterviewWebSocketConfig` | 面试会话 WebSocket 配置 |
| `JwtWebSocketHandshakeInterceptor` | WebSocket 握手 JWT 认证拦截器 |
| `InterviewWebSocketHandler` | 实时面试聊天 WebSocket 处理器 |
| `WebMvcConfig` | Web MVC 配置、API 版本拦截器 |
| `ApiVersionInterceptor` | API 版本校验拦截器 |
| `OpenApiConfig` | OpenAPI/Swagger 配置 |
| `Resilience4jMetricsConfig` | Resilience4j + Micrometer 指标配置 |
| `TracingConfig` | OpenTelemetry 追踪配置 |
| `ApiExceptionHandler` | 全局异常处理器，统一错误响应 |

#### 4.5.3 异步/消息队列

| 类 | 描述 |
|----|------|
| `AsyncTaskRabbitMqConfig` | RabbitMQ 队列/交换机配置 |
| `MaterialParseTaskListener` | 资料解析任务 RabbitMQ 消费者 |
| `RabbitMqAsyncTaskDispatcher` | RabbitMQ 异步任务分发器实现 |
| `NoopAsyncTaskDispatcher` | 空操作分发器（RabbitMQ 未配置时的回退） |
| `AsyncTaskScheduler` | 扫描 PENDING 任务并分发的调度器 |

#### 4.5.4 定时任务

| 类 | 描述 |
|----|------|
| `AsyncTaskScheduler` | 定期扫描并分发 PENDING 异步任务 |
| `QuestionPrewarmScheduler` | 题目缓存预热 |
| `QuestionReviewScheduler` | 审核生成题目质量 |

#### 4.5.5 安全

| 类 | 描述 |
|----|------|
| `JwtAuthenticationFilter` | 提取 Bearer Token、解析 userId、设置 SecurityContext |
| `CurrentUser` | 工具类：从 SecurityContext 获取当前认证用户 ID |

**依赖**：所有 5 个子模块 + Spring Boot starters + `jjwt` + `mysql-connector-j` + `springdoc-openapi` + `resilience4j` + `micrometer-tracing` + `rabbitmq-client`

### 4.6 interview-infrastructure（基础设施层）

**职责**：领域仓储接口的具体实现、MyBatis Mapper 和 XML 映射、外部系统网关。

#### 4.6.1 持久化

| PO 类 | 描述 |
|-------|------|
| `UserPO` | 用户持久化对象 |
| `MaterialPO` | 资料持久化对象 |
| `MaterialChunkPO` | 资料分块持久化对象 |
| `AsyncTaskRecordPO` | 异步任务持久化对象 |
| `QuestionPO` | 题目持久化对象 |
| `InterviewSessionPO` | 面试会话持久化对象 |
| `WrongBookPO` | 错题本持久化对象 |
| `UserLlmSettingsPO` | 用户 LLM 设置持久化对象 |
| `PromptTemplatePO` | Prompt 模板持久化对象 |
| `EvalRunPO` | 评测运行持久化对象 |
| `EvalCasePO` | 评测用例持久化对象 |
| `EvalResultPO` | 评测结果持久化对象 |

#### 4.6.2 仓储实现

| 实现类 | 描述 |
|--------|------|
| `UserRepositoryImpl` | 用户仓储实现 |
| `MaterialRepositoryImpl` | 资料仓储实现 |
| `MaterialChunkRepositoryImpl` | 资料分块仓储实现 |
| `AsyncTaskRecordRepositoryImpl` | 异步任务仓储实现 |
| `QuestionRepositoryImpl` | 题目仓储实现 |
| `InterviewSessionRepositoryImpl` | 面试会话仓储实现 |
| `WrongBookRepositoryImpl` | 错题本仓储实现 |
| `UserLlmSettingsRepositoryImpl` | 用户 LLM 设置仓储实现 |
| `PromptTemplateRepositoryImpl` | Prompt 模板仓储实现 |
| `EvalRunRepositoryImpl` | 评测运行仓储实现 |
| `EvalCaseRepositoryImpl` | 评测用例仓储实现 |
| `EvalResultRepositoryImpl` | 评测结果仓储实现 |

#### 4.6.3 MyBatis Mapper XML

| 文件 | 描述 |
|------|------|
| `UserMapper.xml` | 用户 SQL |
| `MaterialMapper.xml` | 资料 SQL |
| `MaterialChunkMapper.xml` | 资料分块 SQL |
| `AsyncTaskRecordMapper.xml` | 异步任务 SQL |
| `QuestionMapper.xml` | 题目 SQL |
| `InterviewSessionMapper.xml` | 面试会话 SQL |
| `WrongBookMapper.xml` | 错题本 SQL |
| `UserLlmSettingsMapper.xml` | 用户 LLM 设置 SQL |
| `PromptTemplateMapper.xml` | Prompt 模板 SQL |
| `EvalRunMapper.xml` | 评测运行 SQL |
| `EvalCaseMapper.xml` | 评测用例 SQL |
| `EvalResultMapper.xml` | 评测结果 SQL |

#### 4.6.4 外部网关

| 类 | 描述 |
|----|------|
| `MilvusVectorStoreGateway` | Milvus 向量存储网关实现（实现 `VectorStoreGateway`） |
| `ApiKeyCryptoService` | API Key 加密/解密服务 |

**依赖**：`interview-domain` + `mybatis-spring-boot-starter` + `spring-boot-starter-data-redis` + `milvus-sdk-java`

### 4.7 interview-ai-gateway（AI 网关层）

**职责**：统一 LLM 调用抽象、多厂商模型路由、Prompt 装配、输出校验。

| 类 | 描述 |
|----|------|
| `LlmGateway` | 接口：`chat(Long userId, String prompt)` |
| `OpenAiLlmGateway` | OpenAI 兼容 API 实现（使用 `RestClient`） |
| `DeepSeekLlmGateway` | DeepSeek API 实现 |
| `MultiVendorLlmGateway` | 多厂商路由器 |
| `NoopLlmGateway` | 桩实现：开发测试返回占位消息 |

#### 4.7.1 MultiVendorLlmGateway 特性

- **支持提供商**：
  - OpenAI 兼容格式（DeepSeek、Qwen 等）
  - Anthropic/Claude API
- **配置优先级**：
  1. 用户 LLM 设置（UserLlmSettings）
  2. 系统默认值（application.yml）
  3. 本地 Claude settings 文件
- **功能特性**：
  - 自动提供商检测
  - 重试机制（指数退避）
  - OpenAI 流式响应支持
  - Anthropic 流式响应支持

**依赖**：`spring-context` + `spring-web`（RestClient）

---

## 5. 前端结构

### 5.1 目录结构

```
frontend/
├── app/                      # Next.js App Router 页面
│   ├── page.tsx              # 首页/重定向
│   ├── layout.tsx             # 根布局
│   ├── globals.css           # 全局样式
│   ├── home/page.tsx         # 首页仪表盘
│   ├── login/page.tsx         # 登录页
│   ├── register/page.tsx      # 注册页
│   ├── ai-interviewer/page.tsx    # AI 面试官
│   ├── ai-qa/page.tsx             # AI 问答
│   ├── ai-test/page.tsx           # AI 测试
│   ├── question-bank/page.tsx     # 题库
│   ├── wrong-answers/page.tsx     # 错题本
│   ├── knowledge-base/page.tsx    # 知识库
│   ├── interview-tips/page.tsx    # 面试技巧
│   └── settings/page.tsx          # 用户设置
├── components/               # 组件
│   ├── dashboard/            # 仪表盘组件
│   │   ├── InsightPanel.tsx
│   │   ├── MaterialPanel.tsx
│   │   ├── OverviewGrid.tsx
│   │   ├── QuestionComposer.tsx
│   │   ├── StatTile.tsx
│   │   └── TaskPanel.tsx
│   ├── layout/               # 布局组件
│   │   ├── ParticleField.tsx
│   │   ├── Sidebar.tsx
│   │   └── TopBar.tsx
│   ├── new-ui/               # 新 UI 系统
│   │   ├── AnimatedCounter.tsx
│   │   ├── AppChrome.tsx
│   │   ├── PageHero.tsx
│   │   ├── ToastProvider.tsx
│   │   ├── cards.tsx
│   │   └── nav-config.ts
│   ├── theme/                # 主题系统
│   │   ├── ThemeProvider.tsx
│   │   └── ThemeToggle.tsx
│   └── ui/                   # 基础 UI 组件
│       ├── EmptyState.tsx
│       ├── Skeleton.tsx
│       ├── button.tsx
│       ├── error-boundary.tsx
│       ├── loading-overlay.tsx
│       └── progress.tsx
├── lib/                      # 工具函数
│   ├── hooks/
│   │   ├── useMaterials.ts   # 资料操作 Hook
│   │   └── useTasks.ts       # 任务操作 Hook
│   ├── dashboard-data.ts     # 仪表盘数据获取
│   ├── dashboard-format.ts   # 仪表盘数据格式化
│   ├── fetch-with-retry.ts    # 带重试的请求
│   ├── interview-api.ts      # 后端 API 客户端
│   └── utils.ts              # 通用工具
├── stores/                   # 状态管理
│   └── useDashboardStore.ts  # Zustand 仪表盘状态
├── types/                    # TypeScript 类型
│   └── dashboard.ts
├── docs/                     # 文档
│   └── ui-overhaul-plan.md    # UI 重构计划
├── artifacts/                # 设计截图
├── tests/                    # 测试
│   └── e2e/
│       └── critical-flows.spec.ts
├── next.config.ts            # Next.js 配置（API 代理）
├── tailwind.config.ts        # Tailwind 配置
├── package.json              # 依赖
└── playwright.config.ts      # Playwright 配置
```

### 5.2 核心组件说明

| 组件/函数 | 描述 |
|----------|------|
| `AppChrome` | 主应用外壳，包含侧边栏和内容区 |
| `Sidebar` | 导航侧边栏 |
| `TopBar` | 顶部导航栏，包含用户信息和主题切换 |
| `ParticleField` | 背景粒子动画装饰 |
| `StatTile` | 统计卡片：图标 + 标签 + 数值 + 备注 |
| `MaterialPanel` | 资料列表面板 |
| `QuestionComposer` | 题目组合面板 |
| `TaskPanel` | 异步任务队列面板 |
| `InsightPanel` | 学习洞察面板 |
| `OverviewGrid` | 仪表盘概览网格 |
| `ThemeProvider` | 主题提供者（亮色/暗色模式） |
| `ThemeToggle` | 主题切换按钮 |
| `AnimatedCounter` | 动画数字计数器 |
| `PageHero` | 页面英雄区域组件 |
| `ToastProvider` | Toast 通知提供者 |
| `useMaterials` | 资料操作自定义 Hook |
| `useTasks` | 任务操作自定义 Hook |
| `useDashboardStore` | Zustand 仪表盘状态 |

### 5.3 状态管理

| 方式 | 用途 |
|------|------|
| **Zustand** | 全局仪表盘状态（`useDashboardStore`） |
| **localStorage** | 存储 `interview_token` 和 `interview_refresh_token` |
| **React Hooks** | 自定义 Hook 封装 API 调用和本地状态 |

### 5.4 API 代理配置

`next.config.ts` 配置重写规则，将 `/api/*` 请求代理到后端 `http://127.0.0.1:8080/api/*`，可通过 `BACKEND_API_URL` 环境变量覆盖。

---

## 6. 数据库设计

### 6.1 核心表清单

| # | 表名 | 描述 | 代码中引用 |
|---|------|------|------------|
| 1 | `users` | 用户表 | ✅ |
| 2 | `roles` | 角色表 | ✅ |
| 3 | `user_roles` | 用户角色关联表 | ✅ |
| 4 | `materials` | 学习资料表 | ✅ |
| 5 | `material_chunks` | 资料分块表 | ✅ |
| 6 | `questions` | 题目表 | ✅ |
| 7 | `quiz_records` | 答题记录表 | 规划中 |
| 8 | `wrong_book` | 错题本表 | ✅ |
| 9 | `knowledge_points` | 知识点表 | ✅ |
| 10 | `question_kp_rel` | 题目知识点关联表 | ✅ |
| 11 | `interview_sessions` | 面试会话表 | ✅ |
| 12 | `interview_messages` | 面试消息表 | ✅ |
| 13 | `learning_reports` | 学习报告表 | ✅ |
| 14 | `async_task_records` | 异步任务记录表 | ✅ |
| 15 | `llm_call_logs` | LLM 调用日志表 | 规划中 |
| 16 | `idempotency_records` | 幂等记录表 | ✅ |
| 17 | `user_llm_settings` | 用户 LLM 设置表 | ✅ |
| 18 | `prompt_templates` | Prompt 模板表 | ✅ |
| 19 | `eval_runs` | 评测运行表 | ✅ |
| 20 | `eval_cases` | 评测用例表 | ✅ |
| 21 | `eval_results` | 评测结果表 | ✅ |

### 6.2 Flyway 迁移版本

| 迁移 | 描述 |
|------|------|
| `V1__init.sql` | 初始schema：用户、资料、题目、错题本 |
| `V2__add_material_analysis_text.sql` | 添加资料分析文本字段 |
| `V3__optimize_questions_table.sql` | 优化题目表结构 |
| `V4__add_async_task_progress.sql` | 添加异步任务进度追踪 |
| `V5__align_async_task_record_columns.sql` | 对齐异步任务记录列 |
| `V6__add_interview_session.sql` | 添加面试会话表 |
| `V6_1__add_question_quality_metadata.sql` | 添加题目质量元数据 |
| `V7__add_error_detail_to_async_task_records.sql` | 添加异步任务错误详情 |
| `V8__add_user_llm_settings.sql` | 添加用户 LLM 设置表 |
| `V9__add_biz_type_to_async_task_records.sql` | 添加异步任务业务类型 |
| `V10__expand_user_llm_settings_api_key_length.sql` | 扩展 API Key 长度 |
| `V11__add_prompt_templates.sql` | 添加 Prompt 模板表 |
| `V12__add_eval_framework.sql` | 添加评测框架表 |

### 6.3 核心表结构

#### 6.3.1 users（用户表）

| 字段 | 类型 | 描述 |
|------|------|------|
| `id` | BIGINT UNSIGNED PK | 自增主键 |
| `username` | VARCHAR(64) UK | 用户名 |
| `email` | VARCHAR(128) UK | 邮箱 |
| `phone` | VARCHAR(32) UK | 手机号 |
| `password_hash` | VARCHAR(255) | BCrypt 密码哈希 |
| `display_name` | VARCHAR(100) | 显示名称 |
| `avatar_url` | VARCHAR(512) | 头像 URL |
| `status` | TINYINT UNSIGNED | 1=激活，0=禁用 |
| `last_login_at` | DATETIME | 最后登录时间 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

#### 6.3.2 materials（资料表）

| 字段 | 类型 | 描述 |
|------|------|------|
| `id` | BIGINT UNSIGNED PK | 自增主键 |
| `user_id` | BIGINT UNSIGNED FK | 所属用户 |
| `material_name` | VARCHAR(255) | 资料名称 |
| `material_type` | VARCHAR(32) | PDF/WORD/MD/IMAGE/TEXT/CSV/JSON |
| `source_type` | VARCHAR(32) | UPLOAD/URL/IMPORT |
| `storage_url` | VARCHAR(1024) | 存储路径 |
| `content_hash` | CHAR(64) | 内容哈希（去重） |
| `parse_status` | ENUM | PENDING/PROCESSING/SUCCESS/FAILED |
| `parse_error_msg` | VARCHAR(500) | 解析错误信息 |
| `parsed_at` | DATETIME | 解析完成时间 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

#### 6.3.3 async_task_records（异步任务表）

| 字段 | 类型 | 描述 |
|------|------|------|
| `id` | BIGINT UNSIGNED PK | 自增主键 |
| `task_no` | VARCHAR(64) UK | 任务编号（PARSE-xxx） |
| `task_type` | VARCHAR(64) | 任务类型 |
| `biz_id` | BIGINT UNSIGNED | 关联业务 ID |
| `biz_type` | VARCHAR(64) | 业务类型 |
| `status` | ENUM | PENDING/PROCESSING/SUCCESS/FAILED |
| `progress` | INT UNSIGNED | 进度百分比 |
| `result_json` | TEXT | 结果 JSON |
| `payload_json` | TEXT | 负载 JSON |
| `error_code` | VARCHAR(64) | 错误码 |
| `error_msg` | VARCHAR(500) | 错误信息 |
| `retry_count` | INT UNSIGNED | 重试次数 |
| `max_retry` | INT UNSIGNED | 最大重试次数 |
| `next_retry_at` | DATETIME | 下次重试时间 |
| `created_by` | BIGINT UNSIGNED | 创建用户 |
| `started_at` | DATETIME | 开始时间 |
| `finished_at` | DATETIME | 结束时间 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

#### 6.3.4 questions（题目表）

| 字段 | 类型 | 描述 |
|------|------|------|
| `id` | BIGINT UNSIGNED PK | 自增主键 |
| `material_id` | BIGINT UNSIGNED FK | 来源资料 |
| `creator_user_id` | BIGINT UNSIGNED FK | 创建用户 |
| `question_type` | ENUM | SINGLE_CHOICE/MULTIPLE_CHOICE/SHORT_ANSWER/CODING/INTERVIEW |
| `stem_text` | TEXT | 题目干 |
| `options_json` | JSON | 选项（选择题） |
| `reference_answer` | TEXT | 参考答案 |
| `analysis_text` | TEXT | 解析 |
| `difficulty` | TINYINT UNSIGNED | 难度 1-5 |
| `source_type` | ENUM | AI/MANUAL/IMPORT |
| `model_name` | VARCHAR(128) | 生成模型名称 |
| `status` | TINYINT UNSIGNED | 1=启用，0=禁用 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

#### 6.3.5 user_llm_settings（用户 LLM 设置表）

| 字段 | 类型 | 描述 |
|------|------|------|
| `id` | BIGINT UNSIGNED PK | 自增主键 |
| `user_id` | BIGINT UNSIGNED UK | 用户 ID |
| `provider_name` | VARCHAR(64) | 提供商名称（openai/anthropic 等） |
| `model_name` | VARCHAR(128) | 模型名称 |
| `base_url` | VARCHAR(512) | API 基础 URL |
| `api_key` | VARCHAR(2000) | 加密的 API Key |
| `enabled` | TINYINT | 1=启用，0=禁用 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

---

## 7. 核心功能实现

### 7.1 JWT 认证流程

```
JwtAuthenticationFilter 处理流程：
1. 从 HTTP Header 提取 "Authorization: Bearer <token>"
2. 调用 JwtTokenService.parseUserId(token) 解析 userId
3. 检查 Token 黑名单（Redis）
4. 查询用户是否存在且状态正常（status=1）
5. 设置 SecurityContext，认证成功
```

**关键配置**：
- `app.jwt.secret`：HMAC-SHA 密钥（必填，最小 32 字节）
- `app.jwt.expire-ms`：Token 过期时间（默认 24 小时）
- `app.jwt.refresh-expire-ms`：刷新 Token 过期时间（默认 7 天）

### 7.2 资料上传解析流程

```
MaterialApplicationService.uploadAndCreateParseTask() 事务流程：
1. 保存文件到本地存储 (app.storage.material-dir)
2. materialRepository.save() 保存资料记录
3. asyncTaskRecordRepository.create() 创建解析任务（状态=PENDING）
4. 提交事务
5. 返回 UploadMaterialResult(material, task)

MaterialParseTaskProcessor.processTask() 执行流程：
1. 更新任务状态为 PROCESSING，进度 10%
2. 根据文件类型处理（文本/二进制）
3. 读取文件内容
4. 调用 LLM 分析资料内容
5. 调用 MaterialRagApplicationService.indexMaterial() 进行分块和向量化
6. 更新资料解析成功状态
7. 更新任务状态为 SUCCESS，进度 100%
```

### 7.3 AI 出题流程

```
QuizApplicationService.generate() 流程：
1. 验证输入（userId、materialIds 不能为空）
2. 加载资料列表
3. 规范化题目类型、难度、数量
4. 构建 Prompt (QuizPromptBuilder)
5. 调用 LLM (llmGateway.chat())
6. 解析 LLM 返回的结构化数据 (StructuredQuizPayloadParser)
7. 如解析失败或数量不足，使用回退题目工厂
8. 事务保存所有题目
9. 返回 GeneratedQuizResult

 Resilience4j 保护：
- @CircuitBreaker(name="llmGateway")：LLM 不可用时触发熔断
- @RateLimiter(name="quizGeneration")：限制出题频率
- generateQuizFallback()：熔断时的回退方法，生成确定性模板题
```

### 7.4 多厂商 LLM 路由

```
MultiVendorLlmGateway 配置优先级：
1. 用户 LLM 设置（UserLlmSettings.enabled=true）
2. 系统默认值（application.yml）
3. 本地 Claude settings 文件（~/.claude/settings.json）

支持的提供商：
- OpenAI 兼容格式：DeepSeek、Qwen、Moonshot、Zhipu、Gemini 等
- Anthropic/Claude API

重试机制：
- 默认最大重试 3 次
- 指数退避（backoff-ms=500）
- 流式响应支持 OpenAI 和 Anthropic
```

### 7.5 异步任务调度

```
AsyncTaskScheduler 调度逻辑：
1. 每分钟扫描 async_task_records 表
2. 查询状态为 PENDING 且 next_retry_at <= NOW 的任务
3. 按创建时间排序，限制批次大小
4. 分发到 RabbitMQ 或直接处理

RabbitMQ 配置：
- 队列：material.parse.queue
- 交换机：material.parse.exchange
- 路由键：material.parse

MaterialParseTaskListener 消费逻辑：
1. 接收任务消息
2. 调用 MaterialParseTaskProcessor.processTask()
3. 处理结果更新任务状态
```

---

## 8. API 接口文档

### 8.1 统一响应格式

**成功响应**：
```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

**错误响应**：
```json
{
  "success": false,
  "message": "错误描述",
  "data": null
}
```

### 8.2 认证接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/auth/login` | ❌ | 用户登录，返回 JWT Token |
| POST | `/api/v1/auth/register` | ❌ | 用户注册 |
| GET | `/api/v1/auth/captcha` | ❌ | 生成验证码 |
| POST | `/api/v1/auth/refresh` | ❌ | 刷新 JWT Token |
| POST | `/api/v1/auth/logout` | ✅ | 用户登出（Token 加入黑名单） |

### 8.3 资料接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/materials/upload` | ✅ | 上传资料文件（multipart） |
| GET | `/api/v1/materials` | ✅ | 获取资料列表（按当前用户过滤） |
| DELETE | `/api/v1/materials/{id}` | ✅ | 删除资料 |
| POST | `/api/v1/materials/{id}/retry-parse` | ✅ | 重试资料解析 |

### 8.4 题目接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/quizzes/generate` | ✅ | 从资料生成题目 |
| GET | `/api/v1/quizzes/questions` | ✅ | 获取最近题目（分页） |

### 8.5 面试接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/interviews/sessions` | ✅ | 创建或获取活跃面试会话 |
| GET | `/api/v1/interviews/sessions/active` | ✅ | 获取活跃面试会话 |
| POST | `/api/v1/interviews/{id}/resume` | ✅ | 恢复暂停的面试会话 |

### 8.6 聊天接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/chat` | ✅ | 发送消息给 AI，获取响应 |

### 8.7 错题本接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/wrong-books` | ✅ | 添加错题 |
| GET | `/api/v1/wrong-books` | ✅ | 列出错题（可选 masteryStatus 过滤） |
| PUT | `/api/v1/wrong-books/{id}/mastery` | ✅ | 更新掌握状态 |
| DELETE | `/api/v1/wrong-books/{id}` | ✅ | 删除错题 |

### 8.8 评测接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| POST | `/api/v1/eval/runs` | ❌ | 触发评测运行 |
| GET | `/api/v1/eval/runs` | ❌ | 列出所有评测运行 |
| GET | `/api/v1/eval/runs/{runKey}` | ❌ | 获取指定评测运行 |
| GET | `/api/v1/eval/cases` | ❌ | 列出评测用例 |

### 8.9 异步任务接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| GET | `/api/v1/async-tasks/{taskNo}` | ✅ | 查询异步任务状态 |

### 8.10 LLM 设置接口

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| GET | `/api/v1/llm/settings` | ✅ | 获取用户 LLM 设置 |
| PUT | `/api/v1/llm/settings` | ✅ | 更新用户 LLM 设置 |

### 8.11 健康检查

| 方法 | 路径 | 认证 | 描述 |
|------|------|------|------|
| GET | `/api/v1/health` | ❌ | 健康检查 |
| GET | `/actuator/health` | ❌ | Actuator 健康检查 |

### 8.12 WebSocket

| 端点 | 认证 | 描述 |
|------|------|------|
| `/ws/interview` | ✅ (握手拦截器 JWT) | 实时面试聊天 WebSocket |

---

## 9. 依赖关系

### 9.1 后端模块依赖图

```
                        interview-api
                       /    |    \     \      \
                      /     |     \     \      \
                     v      v      v     v      v
              common  domain  application  infrastructure  ai-gateway
                        ^        ^            ^
                        |        |            |
                        +--------+------------+
                              domain
```

### 9.2 模块详细依赖

```
interview-common
  ← spring-context, spring-web, jakarta.annotation-api

interview-domain
  ← (无外部依赖)

interview-application
  ← interview-domain, spring-context, spring-tx,
    spring-security-crypto, jakarta.validation-api

interview-infrastructure
  ← interview-domain, mybatis-spring-boot-starter,
    spring-boot-starter-data-redis, milvus-sdk-java

interview-ai-gateway
  ← spring-context, spring-web

interview-api
  ← interview-common, interview-domain, interview-application,
    interview-infrastructure, interview-ai-gateway,
    spring-boot-starter-web/security/validation/actuator/websocket,
    jjwt, mysql-connector-j, springdoc-openapi,
    resilience4j, micrometer-tracing, rabbitmq-client
```

### 9.3 前端依赖

```json
{
  "dependencies": {
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "framer-motion": "^12.38.0",
    "lucide-react": "^0.468.0",
    "next": "^15.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "tailwind-merge": "^3.5.0",
    "zustand": "^5.0.12"
  },
  "devDependencies": {
    "@playwright/test": "^1.59.1",
    "autoprefixer": "^10.4.20",
    "eslint": "^9.39.4",
    "postcss": "^8.4.49",
    "prettier": "^3.8.3",
    "tailwindcss": "^3.4.17",
    "typescript": "^5.7.2"
  }
}
```

---

## 10. 项目运行指南

### 10.1 环境要求

| 要求 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 后端运行环境 |
| Node.js | 18+ | 前端运行环境（推荐 20+） |
| Maven | 3.9+ | 后端构建工具 |
| Docker | 最新版 | 运行基础设施 |

### 10.2 启动基础设施

```bash
cd /Users/liuyongze/Documents/New\ project
docker compose -f docker-compose.infrastructure.yml up -d

# 数据库 schema 由 Flyway 在 interview-api 启动时自动迁移
```

### 10.3 启动后端

```bash
cd backend

# 构建项目
mvn clean package

# 启动应用（JWT_SECRET 必填）
JWT_SECRET=<your-secret-at-least-32-chars> mvn -pl interview-api spring-boot:run

# 后端运行在 http://127.0.0.1:8080
```

### 10.4 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 前端运行在 http://127.0.0.1:3000，API 请求自动代理到后端
```

### 10.5 环境变量

#### 后端环境变量

| 变量 | 默认值 | 描述 |
|------|--------|------|
| `JWT_SECRET` | （必填） | JWT HMAC-SHA 密钥，最小 32 字节 |
| `MYSQL_URL` | `jdbc:mysql://127.0.0.1:3306/interview_ai?...` | MySQL 连接 URL |
| `MYSQL_USER` | `interview_user` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `interview_pass` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `MILVUS_HOST` | `127.0.0.1` | Milvus 主机 |
| `MILVUS_PORT` | `19530` | Milvus 端口 |
| `LLM_PROVIDER` | `anthropic` | 默认 LLM 提供商 |
| `LLM_BASE_URL` | （提供商默认） | LLM API URL |
| `LLM_API_KEY` | （空） | LLM API Key |
| `LLM_MODEL` | （提供商默认） | LLM 模型名称 |

> **注意**：默认 LLM 提供商是 `anthropic`。如果存在 `~/.claude/settings.json`，系统会自动读取其中的 `ANTHROPIC_AUTH_TOKEN`、`ANTHROPIC_BASE_URL`、`ANTHROPIC_MODEL` 作为默认配置。

### 10.6 快速验证

```bash
# 健康检查
curl http://127.0.0.1:8080/actuator/health

# 登录（demo_user / demo123456）
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"demo123456"}' | jq -r '.data.token')

# 获取资料列表
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/materials

# 上传资料
curl -X POST http://127.0.0.1:8080/api/v1/materials/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@README.md"

# 生成题目
curl -X POST http://127.0.0.1:8080/api/v1/quizzes/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"materialIds":[1],"questionType":"short","difficulty":3,"count":3,"interviewMode":true}'
```

### 10.7 前端本地 Token 设置

```javascript
// 在浏览器控制台设置 Token
localStorage.setItem("interview_token", "<login token>");
localStorage.setItem("interview_refresh_token", "<refresh token>");
```

### 10.8 可观测性

启动可观测性栈：

```bash
docker compose -f docker-compose.infrastructure.yml up -d prometheus grafana zipkin
```

访问地址：
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)
- Zipkin: http://localhost:9411

新增指标：
- `llm_call_total`：LLM 调用总数
- `llm_call_latency`：LLM 调用延迟
- `llm_call_error_total`：LLM 调用错误总数
- `async_task_failure_total`：异步任务失败总数
- `ws_disconnect_total`：WebSocket 断连总数

---

## 11. 项目文件索引

### 11.1 后端核心文件

| 文件 | 路径 |
|------|------|
| 入口类 | `backend/interview-api/src/main/java/com/interview/api/InterviewApiApplication.java` |
| 安全配置 | `backend/interview-api/src/main/java/com/interview/api/config/SecurityConfig.java` |
| JWT 服务 | `backend/interview-api/src/main/java/com/interview/api/config/JwtTokenService.java` |
| JWT 过滤器 | `backend/interview-api/src/main/java/com/interview/api/security/JwtAuthenticationFilter.java` |
| WebSocket 配置 | `backend/interview-api/src/main/java/com/interview/api/config/InterviewWebSocketConfig.java` |
| WebSocket 处理器 | `backend/interview-api/src/main/java/com/interview/api/websocket/InterviewWebSocketHandler.java` |
| 认证控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/AuthController.java` |
| 资料控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/MaterialController.java` |
| 题目控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/QuizController.java` |
| 面试控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/InterviewController.java` |
| 聊天控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/ChatController.java` |
| 错题本控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/WrongBookController.java` |
| 评测控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/EvalController.java` |
| 异步任务控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/AsyncTaskController.java` |
| LLM 设置控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/UserLlmSettingsController.java` |
| 认证服务 | `backend/interview-application/src/main/java/com/interview/application/service/AuthApplicationService.java` |
| 资料服务 | `backend/interview-application/src/main/java/com/interview/application/service/MaterialApplicationService.java` |
| 题目服务 | `backend/interview-application/src/main/java/com/interview/application/service/QuizApplicationService.java` |
| 聊天服务 | `backend/interview-application/src/main/java/com/interview/application/service/ChatApplicationService.java` |
| 错题本服务 | `backend/interview-application/src/main/java/com/interview/application/service/WrongBookApplicationService.java` |
| 评测服务 | `backend/interview-application/src/main/java/com/interview/application/service/EvalService.java` |
| LLM 网关接口 | `backend/interview-application/src/main/java/com/interview/application/port/LlmGateway.java` |
| 多厂商 LLM 网关 | `backend/interview-ai-gateway/src/main/java/com/interview/aigateway/impl/MultiVendorLlmGateway.java` |
| 应用配置 | `backend/interview-api/src/main/resources/application.yml` |
| 数据库迁移 | `backend/interview-api/src/main/resources/db/migration/` |
| Docker 配置 | `docker-compose.infrastructure.yml` |

### 11.2 前端核心文件

| 文件 | 路径 |
|------|------|
| 首页 | `frontend/app/home/page.tsx` |
| 登录页 | `frontend/app/login/page.tsx` |
| 注册页 | `frontend/app/register/page.tsx` |
| 根布局 | `frontend/app/layout.tsx` |
| 全局样式 | `frontend/app/globals.css` |
| Next.js 配置 | `frontend/next.config.ts` |
| Tailwind 配置 | `frontend/tailwind.config.ts` |
| API 客户端 | `frontend/lib/interview-api.ts` |
| 仪表盘状态 | `frontend/stores/useDashboardStore.ts` |
| 依赖配置 | `frontend/package.json` |

---

## 附录：配置参考

### A.1 Resilience4j 配置

```yaml
resilience4j:
  circuitbreaker:
    instances:
      llmGateway:
        slidingWindowSize: 5
        failureRateThreshold: 60
        waitDurationInOpenState: 60s
      materialService:
        slidingWindowSize: 8
        failureRateThreshold: 40
  retry:
    instances:
      llmGateway:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
  ratelimiter:
    instances:
      quizGeneration:
        limitForPeriod: 10
        limitRefreshPeriod: 1m
      auth:
        limitForPeriod: 5
        limitRefreshPeriod: 1m
  timelimiter:
    instances:
      llmGateway:
        timeoutDuration: 30s
      materialParse:
        timeoutDuration: 60s
  bulkhead:
    instances:
      llmGateway:
        maxConcurrentCalls: 10
        maxWaitDuration: 2s
      fileUpload:
        maxConcurrentCalls: 5
        maxWaitDuration: 5s
```

### A.2 数据库连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000
      leak-detection-threshold: 60000
```

---

**文档结束**

*本 Wiki 文档由代码分析自动生成，如有更新请以实际代码为准。*

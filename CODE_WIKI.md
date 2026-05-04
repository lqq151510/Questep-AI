# AI 智能面试刷题平台 — Code Wiki

> 版本：0.1.0-SNAPSHOT | 最后更新：2026-05-02

---

## 目录

1. [项目概览](#1-项目概览)
2. [整体架构](#2-整体架构)
3. [后端模块详解](#3-后端模块详解)
4. [前端模块详解](#4-前端模块详解)
5. [关键类与函数说明](#5-关键类与函数说明)
6. [数据库设计](#6-数据库设计)
7. [依赖关系](#7-依赖关系)
8. [项目运行方式](#8-项目运行方式)
9. [API 接口清单](#9-api-接口清单)
10. [修改建议](#10-修改建议)

---

## 1. 项目概览

**AI 智能面试刷题平台**是一个前后端分离的全栈应用，核心功能为：

- 用户上传学习资料（PDF/Word/MD/图片）
- AI 自动解析资料、分块向量化
- AI 生成面试题目（单选/多选/简答/编程）
- AI 评分 + 逐题解析
- 错题自动入库 + 回看复习
- WebSocket 面试模拟（AI 面试官流式提问）
- 学习分析报告

| 维度 | 说明 |
|------|------|
| 后端语言 | Java 21 |
| 后端框架 | Spring Boot 3.3.5 |
| 前端框架 | Next.js 15 + React 19 + TypeScript |
| 数据库 | MySQL 8.4 |
| 向量数据库 | Milvus 2.4 |
| 缓存 | Redis 7.2 |
| 构建工具 | Maven（后端）/ npm（前端） |

---

## 2. 整体架构

### 2.1 架构风格

采用**传统分层 + 端口适配器**混合架构：

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend (Next.js)                      │
│                  127.0.0.1:3000 → /api/* 代理到后端           │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│                    interview-api (Web 层)                     │
│         Controller / SecurityConfig / JwtFilter              │
├─────────────────────────────────────────────────────────────┤
│                interview-application (应用层)                  │
│         ApplicationService / DTO / TokenService              │
├─────────────────────────────────────────────────────────────┤
│                  interview-domain (领域层)                     │
│              Model (record) / Repository (接口)               │
├─────────────────────────────────────────────────────────────┤
│             interview-infrastructure (基础设施层)              │
│       RepositoryImpl / Mapper / PO / MyBatis XML             │
├─────────────────────────────────────────────────────────────┤
│              interview-ai-gateway (AI 网关层)                  │
│              LlmGateway (接口) / NoopLlmGateway               │
├─────────────────────────────────────────────────────────────┤
│                 interview-common (公共层)                      │
│              ApiResponse / GlobalExceptionHandler             │
└─────────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐      ┌─────▼─────┐     ┌─────▼─────┐
   │  MySQL  │      │   Redis   │     │  Milvus   │
   │  :3306  │      │  :6379    │     │  :19530   │
   └─────────┘      └───────────┘     └───────────┘
```

### 2.2 请求流转

```
HTTP Request
  → JwtAuthenticationFilter (Token 解析 & 认证)
    → Controller (入参校验、统一响应封装)
      → ApplicationService (用例编排、事务边界)
        → Domain Repository (接口)
          → Infrastructure RepositoryImpl (MyBatis Mapper)
            → MySQL / Redis / Milvus
```

---

## 3. 后端模块详解

### 3.1 interview-common（公共层）

**职责**：提供全局共享的 API 响应封装与异常处理。

| 类 | 说明 |
|----|------|
| `ApiResponse<T>` | 统一响应 record，包含 `success`/`message`/`data` 三个字段 |
| `GlobalExceptionHandler` | 全局异常处理器，捕获校验异常和通用异常，返回统一 `ApiResponse` |

**依赖**：仅依赖 `spring-context` 和 `spring-web`，无业务依赖。

### 3.2 interview-domain（领域层）

**职责**：定义核心领域模型与仓库接口，是整个系统的核心抽象层，不依赖任何基础设施。

| 类 | 类型 | 说明 |
|----|------|------|
| `User` | record | 用户模型：`id`, `username`, `passwordHash`, `status` |
| `Material` | record | 资料模型：`id`, `userId`, `name`, `fileType`, `parseStatus`, `createdAt`, `updatedAt` |
| `AsyncTaskRecord` | record | 异步任务记录：`id`, `taskNo`, `taskType`, `bizId`, `status`, `progress`, `errorMsg`, `createdBy`, `createdAt`, `updatedAt` |
| `UserRepository` | interface | 用户仓库接口：`findByUsername`, `findById` |
| `MaterialRepository` | interface | 资料仓库接口：`save`, `findAll` |
| `AsyncTaskRecordRepository` | interface | 异步任务仓库接口：`create`, `findByTaskNo` |

**依赖**：零外部依赖，纯 Java record + interface。

### 3.3 interview-application（应用层）

**职责**：用例编排、事务边界管理、DTO 定义。

| 类 | 说明 |
|----|------|
| `AuthApplicationService` | 认证用例：校验用户名密码，生成 JWT Token |
| `MaterialApplicationService` | 资料用例：上传资料并创建解析任务（事务性），列出资料 |
| `AsyncTaskApplicationService` | 异步任务用例：按 taskNo 查询任务状态 |
| `TokenService` | 接口：`generateToken`, `parseUserId` |
| `LoginCommand` | DTO：登录请求，含 `@NotBlank` 校验 |
| `LoginResult` | DTO：登录响应，含 `token` 和 `tokenType` |
| `GenerateQuizCommand` | DTO：组卷请求（`materialIds`、`questionType`、`difficulty`、`count`、`interviewMode`） |
| `GeneratedQuizResult` | DTO：组卷响应（`questions`、`modelBrief`） |
| `UploadMaterialResult` | DTO：上传资料响应，包含 `Material` 和 `AsyncTaskRecord` |

**依赖**：`interview-domain` + `spring-context` + `spring-tx` + `spring-security-crypto` + `jakarta.validation-api`

### 3.4 interview-api（Web 层 / 启动模块）

**职责**：HTTP 入口、安全配置、JWT 认证、请求路由。

| 类 | 说明 |
|----|------|
| `InterviewApiApplication` | Spring Boot 启动类，`@MapperScan` 指向 infrastructure 层 |
| `AuthController` | `/api/auth/**` — 登录接口 |
| `MaterialController` | `/api/materials/**` — 资料上传与列表 |
| `AsyncTaskController` | `/api/async-tasks/**` — 异步任务查询 |
| `HealthController` | `/api/health` — 健康检查 |
| `SecurityConfig` | Spring Security 配置：无状态会话、JWT 过滤器、路径权限 |
| `JwtAuthenticationFilter` | JWT Token 解析过滤器，从 `Authorization: Bearer` 提取用户 |
| `JwtTokenService` | JWT Token 生成与解析实现（实现 `TokenService` 接口） |
| `CurrentUser` | 工具类：从 `SecurityContext` 获取当前用户 ID |

**依赖**：依赖所有其他 5 个子模块 + `spring-boot-starter-web/security/websocket/validation/actuator` + `jjwt` + `mysql-connector-j`

### 3.5 interview-infrastructure（基础设施层）

**职责**：领域仓库接口的具体实现，MyBatis Mapper 与 XML 映射。

| 包 | 类 | 说明 |
|----|-----|------|
| persistence.entity | `UserPO` | 用户持久化对象（setter/getter） |
| persistence.entity | `MaterialPO` | 资料持久化对象 |
| persistence.entity | `AsyncTaskRecordPO` | 异步任务持久化对象 |
| persistence.mapper | `UserMapper` | MyBatis Mapper 接口 |
| persistence.mapper | `MaterialMapper` | MyBatis Mapper 接口 |
| persistence.mapper | `AsyncTaskRecordMapper` | MyBatis Mapper 接口 |
| persistence.repository | `UserRepositoryImpl` | `UserRepository` 实现 |
| persistence.repository | `MaterialRepositoryImpl` | `MaterialRepository` 实现 |
| persistence.repository | `AsyncTaskRecordRepositoryImpl` | `AsyncTaskRecordRepository` 实现 |
| resources/mapper | `UserMapper.xml` | SQL 映射：按用户名/ID 查询 |
| resources/mapper | `MaterialMapper.xml` | SQL 映射：插入、按ID查询、列表查询 |
| resources/mapper | `AsyncTaskRecordMapper.xml` | SQL 映射：插入、按ID/TaskNo查询 |

**依赖**：`interview-domain` + `mybatis-spring-boot-starter` + `spring-boot-starter-data-redis`

### 3.6 interview-ai-gateway（AI 网关层）

**职责**：统一 LLM 调用抽象，当前为 Stub 实现。

| 类 | 说明 |
|----|------|
| `LlmGateway` | 接口：`chat(String prompt)` — 统一 LLM 调用入口 |
| `NoopLlmGateway` | Stub 实现：返回提示信息，用于开发阶段测试 |

**依赖**：仅 `spring-context`

---

## 4. 前端模块详解

### 4.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Next.js | 15.x | React 全栈框架（App Router） |
| React | 19.x | UI 库 |
| TypeScript | 5.7.x | 类型安全 |
| Tailwind CSS | 3.4.x | 原子化 CSS（实际以自定义 CSS 为主） |
| lucide-react | 0.468.x | 图标库 |

### 4.2 目录结构

```
frontend/
├── app/
│   ├── globals.css      # 全局样式（~1270行，含完整设计系统）
│   ├── layout.tsx       # 根布局（lang=zh-CN）
│   └── page.tsx         # 首页（单文件 SPA 式实现）
├── artifacts/           # 设计稿截图
├── next.config.ts       # Next.js 配置（API 代理到后端）
├── tailwind.config.ts   # Tailwind 配置
├── postcss.config.mjs   # PostCSS 配置
├── tsconfig.json        # TypeScript 配置
└── package.json         # 依赖管理
```

### 4.3 核心组件

| 组件/函数 | 说明 |
|-----------|------|
| `Home` (默认导出) | 主页面组件，包含侧边栏、顶栏、概览统计、资料列表、题单草稿、任务队列、知识短板面板 |
| `StatTile` | 统计卡片组件：图标 + 标签 + 数值 + 备注 |
| `ParticleField` | 背景粒子动画装饰组件 |
| `refreshMaterials()` | 调用后端 `/api/materials` 同步资料列表 |
| `handleUpload()` | 文件上传处理（本地状态模拟 + 后端同步） |
| `generateQuestions()` | 根据选中资料、题型、难度生成题单草稿 |

### 4.4 API 代理

`next.config.ts` 配置了 rewrite 规则，将前端 `/api/*` 请求代理到后端 `http://127.0.0.1:8080/api/*`，可通过环境变量 `BACKEND_API_URL` 覆盖。

---

## 5. 关键类与函数说明

### 5.1 后端关键类

#### JwtAuthenticationFilter

```
路径: com.interview.api.security.JwtAuthenticationFilter
类型: @Component, OncePerRequestFilter
功能: 从 HTTP Header 提取 Bearer Token → 解析 userId → 查询用户 → 设置 SecurityContext
关键逻辑:
  - Token 解析失败时静默忽略（catch Exception ignored）
  - 仅 status == 1 的用户可认证通过
  - 认证后 Principal 为 userId (Long)，Authority 为 ROLE_USER
```

#### JwtTokenService

```
路径: com.interview.api.config.JwtTokenService
类型: @Service, 实现 TokenService 接口
功能: JWT Token 生成与解析
配置:
  - app.jwt.secret: HMAC-SHA 密钥（默认: change-this-secret-change-this-secret）
  - app.jwt.expire-ms: 过期时间（默认: 86400000ms = 24h）
方法:
  - generateToken(Long userId, String username): 生成 JWT
  - parseUserId(String token): 从 Token 解析 userId
```

#### MaterialApplicationService.uploadAndCreateParseTask

```
路径: com.interview.application.service.MaterialApplicationService
事务: @Transactional
功能: 在同一事务中创建资料记录和异步解析任务
流程:
  1. materialRepository.save() → 保存资料记录
  2. asyncTaskRecordRepository.create() → 创建解析任务（taskNo = "PARSE-" + UUID）
  3. 返回 UploadMaterialResult(material, task)
```

#### CurrentUser

```
路径: com.interview.api.support.CurrentUser
类型: final 工具类
功能: 从 SecurityContext 获取当前认证用户 ID
方法: static Long id() — 未认证时抛 IllegalStateException
```

### 5.2 前端关键函数

#### refreshMaterials()

```
功能: 从后端 /api/materials 拉取资料列表并合并到本地状态
状态管理: apiState → syncing → online/offline
数据映射: 后端字段 → 前端 MaterialItem 类型
容错: 后端不可用时保留本地 seed 数据
```

#### handleUpload()

```
功能: 处理文件上传
流程:
  1. 创建本地 MaterialItem（status=parsing）
  2. 创建本地 TaskItem（status=running）
  3. 自动选中新资料
注意: 当前仅更新本地状态，未实际调用后端上传 API
```

---

## 6. 数据库设计

### 6.1 表清单

| # | 表名 | 说明 | 当前代码是否使用 |
|---|------|------|-----------------|
| 1 | `users` | 用户表 | ✅ |
| 2 | `roles` | 角色表 | ❌ 种子数据已建 |
| 3 | `user_roles` | 用户-角色关联表 | ❌ |
| 4 | `materials` | 学习资料表 | ✅ |
| 5 | `material_chunks` | 资料分块表 | ❌ |
| 6 | `questions` | 题目表 | ❌ |
| 7 | `quiz_records` | 刷题记录表 | ❌ |
| 8 | `wrong_book` | 错题本表 | ❌ |
| 9 | `knowledge_points` | 知识点表 | ❌ |
| 10 | `question_kp_rel` | 题目-知识点关联表 | ❌ |
| 11 | `interview_sessions` | 面试会话表 | ❌ |
| 12 | `interview_messages` | 面试消息表 | ❌ |
| 13 | `learning_reports` | 学习报告表 | ❌ |
| 14 | `async_task_records` | 异步任务记录表 | ✅ |
| 15 | `llm_call_logs` | LLM 调用日志表 | ❌ |
| 16 | `idempotency_records` | 幂等记录表 | ❌ |

### 6.2 核心表结构

#### users

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 自增主键 |
| username | VARCHAR(64) UK | 用户名 |
| email | VARCHAR(128) UK | 邮箱 |
| phone | VARCHAR(32) UK | 手机号 |
| password_hash | VARCHAR(255) | BCrypt 密码哈希 |
| status | TINYINT UNSIGNED | 1=活跃, 0=禁用 |

#### materials

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 自增主键 |
| user_id | BIGINT UNSIGNED FK | 所属用户 |
| material_name | VARCHAR(255) | 资料名称 |
| material_type | VARCHAR(32) | PDF/WORD/MD/IMAGE/TEXT |
| storage_url | VARCHAR(1024) | 存储路径 |
| parse_status | ENUM | PENDING/PROCESSING/SUCCESS/FAILED |

#### async_task_records

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 自增主键 |
| task_no | VARCHAR(64) UK | 任务编号 |
| task_type | VARCHAR(64) | 任务类型 |
| biz_id | BIGINT UNSIGNED | 关联业务 ID |
| status | ENUM | PENDING/PROCESSING/SUCCESS/FAILED |
| progress | INT UNSIGNED | 进度百分比 |
| error_msg | VARCHAR(500) | 错误信息 |

> ⚠️ `init.sql` 中 `async_task_records` 表被定义了两次（第 282 行和第 400 行），结构不一致，会导致建表冲突。

---

## 7. 依赖关系

### 7.1 后端模块依赖图

```
                    interview-api
                   /    |    \     \      \
                  /     |     \     \      \
                 v      v      v     v      v
          common  domain  application  infrastructure  ai-gateway
                    ^        ^            ^
                    |        |            |
                    |        +------------+
                    |        domain
                    +--------+
                    domain
```

**详细依赖**：

```
interview-common ← spring-context, spring-web
interview-domain ← (无外部依赖)
interview-application ← interview-domain, spring-context, spring-tx, spring-security-crypto, jakarta.validation-api
interview-infrastructure ← interview-domain, mybatis-spring-boot-starter, spring-boot-starter-data-redis
interview-ai-gateway ← spring-context
interview-api ← interview-common, interview-domain, interview-application, interview-infrastructure, interview-ai-gateway,
                 spring-boot-starter-web/security/validation/actuator/websocket, jjwt, mysql-connector-j
```

### 7.2 前端依赖

```
ai-interview-frontend
├── next@15.x
├── react@19.x, react-dom@19.x
├── lucide-react@0.468.x (图标)
└── devDependencies
    ├── typescript@5.7.x
    ├── tailwindcss@3.4.x
    ├── postcss@8.4.x
    ├── autoprefixer@10.4.x
    └── @types/node, @types/react, @types/react-dom
```

### 7.3 基础设施依赖（Docker Compose）

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| MySQL | mysql:8.4 | 3306 | 关系型数据库 |
| Redis | redis:7.2-alpine | 6379 | 缓存/会话 |
| Milvus | milvusdb/milvus:v2.4.23 | 19530, 9091 | 向量检索 |
| etcd | quay.io/coreos/etcd:v3.5.18 | 2379 | Milvus 元数据存储 |
| MinIO | minio/minio:RELEASE.2023-03-20 | 9000, 9001 | Milvus 对象存储 |

---

## 8. 项目运行方式

### 8.1 环境要求

- **JDK**: 21+
- **Node.js**: 18+ (推荐 20+)
- **Docker**: 用于运行 MySQL/Redis/Milvus
- **Maven**: 3.9+

### 8.2 启动基础设施

```bash
cd /path/to/project
docker compose -f docker-compose.infrastructure.yml up -d
docker exec -i local-mysql mysql -uroot -proot123456 < backend/sql/init.sql
```

### 8.3 启动后端

```bash
cd backend
mvn clean package
mvn -pl interview-api spring-boot:run
```

后端默认运行在 `http://127.0.0.1:8080`。

### 8.4 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://127.0.0.1:3000`，API 请求自动代理到后端。

### 8.5 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_URL` | `jdbc:mysql://127.0.0.1:3306/interview_ai?...` | MySQL 连接 URL |
| `MYSQL_USER` | `interview_user` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `interview_pass` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `MILVUS_HOST` | `127.0.0.1` | Milvus 主机 |
| `MILVUS_PORT` | `19530` | Milvus 端口 |
| `LLM_PROVIDER` | `openai` | LLM 提供商 |
| `LLM_BASE_URL` | `https://api.openai.com/v1` | LLM API 地址 |
| `LLM_API_KEY` | (空) | LLM API Key |
| `BACKEND_API_URL` | `http://127.0.0.1:8080` | 前端代理目标 |

### 8.6 快速验证

```bash
curl http://127.0.0.1:8080/api/health
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"xxx"}'
curl http://127.0.0.1:8080/api/materials
```

---

## 9. API 接口清单

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/health` | ❌ | 健康检查 |
| POST | `/api/auth/login` | ❌ | 用户登录，返回 JWT |
| POST | `/api/materials/upload` | ✅ | 上传资料文件（multipart） |
| GET | `/api/materials` | ✅ | 获取资料列表 |
| GET | `/api/async-tasks/{taskNo}` | ✅ | 查询异步任务状态 |

**统一响应格式**：

```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

---

## 10. 修改建议

### 🔴 严重问题（必须修复）

#### 10.1 init.sql 中 async_task_records 表重复定义

`init.sql` 第 282-306 行和第 400-420 行分别定义了 `async_task_records` 表，且字段结构不一致（前者有 `biz_type`/`retry_count`/`max_retry`/`next_retry_at`/`error_code`/`payload_json`/`result_json`，后者有 `progress`/`error_msg`/`created_by`）。

**建议**：合并为一张表，保留完整字段集，删除重复定义。当前 Java 代码中的 `AsyncTaskRecordPO` 使用的是简化版字段（`bizId`/`progress`/`errorMsg`/`createdBy`），需要统一。

#### 10.2 JWT 密钥硬编码默认值

`JwtTokenService` 中 `app.jwt.secret` 的默认值为 `change-this-secret-change-this-secret`，生产环境如果忘记修改将导致严重安全问题。

**建议**：
- 移除默认值，启动时如果未配置则抛出异常
- 或使用 `@ConfigurationProperties` + 启动校验

#### 10.3 JwtAuthenticationFilter 异常静默吞没

```java
} catch (Exception ignored) {}
```

Token 解析失败时完全静默，无法排查认证问题。

**建议**：至少记录 WARN 级别日志，区分 Token 过期、签名无效、格式错误等场景。

### 🟡 重要改进（建议尽快处理）

#### 10.4 缺少用户注册接口

当前只有登录接口，无注册接口。`init.sql` 中仅有一个 `demo_user` 种子数据，且密码哈希为占位值 `$2a$10$placeholderhashforlocaldevonly`，无法实际登录。

**建议**：
- 添加 `/api/auth/register` 接口
- 修复 demo_user 的密码哈希为真实 BCrypt 值

#### 10.5 前端文件上传未对接后端

`MaterialController` 提供了 `/api/materials/upload` 接口，但前端 `handleUpload()` 仅更新本地状态，未调用后端 API。

**建议**：在 `handleUpload()` 中使用 `FormData` 调用 `/api/materials/upload`。

#### 10.6 MaterialController.list() 缺少用户隔离

`materialApplicationService.list()` 查询所有资料，未按当前用户过滤。

**建议**：添加 `userId` 参数，只返回当前用户的资料。

#### 10.7 LLM Gateway 仅有 Stub 实现

`NoopLlmGateway` 仅返回固定字符串，无法实际调用 LLM。

**建议**：实现 `OpenAiLlmGateway`，使用 `RestClient` 或 `WebClient` 调用 OpenAI API，配合 `application.yml` 中的 `app.llm.*` 配置。

#### 10.8 缺少异步任务执行器

`AsyncTaskRecord` 创建后无实际执行逻辑，任务永远停留在 PENDING 状态。

**建议**：
- 引入 `@Async` 或 Spring Task Scheduler 定期扫描 PENDING 任务
- 或引入 RabbitMQ 消费者处理任务

### 🟢 优化建议（中期规划）

#### 10.9 前端单文件过大

`page.tsx` 约 650 行，`globals.css` 约 1270 行，所有逻辑和样式集中在单文件中。

**建议**：
- 拆分为独立组件：`Sidebar`, `TopBar`, `MaterialPanel`, `ComposerPanel`, `TaskPanel`, `InsightPanel`
- CSS 使用 CSS Modules 或 Tailwind 类替代自定义 CSS
- 抽取自定义 hooks（如 `useMaterials`, `useTasks`）

#### 10.10 缺少单元测试和集成测试

当前项目无任何测试代码。

**建议**：
- ApplicationService 层编写单元测试（JUnit 5 + Mockito）
- Controller 层编写 `@WebMvcTest` 集成测试
- 引入 Testcontainers 做 MySQL/Redis 集成测试

#### 10.11 缺少 API 文档

项目规划使用 OpenAPI 3，但未集成 Swagger/SpringDoc。

**建议**：添加 `springdoc-openapi-starter-webmvc-ui` 依赖，自动生成 API 文档。

#### 10.12 缺少 Flyway 数据库迁移管理

`init.sql` 为手动执行，无版本化迁移管理。

**建议**：引入 Flyway，将 `init.sql` 转为 `V1__init.sql` 迁移脚本。

#### 10.13 Redis 配置已引入但未使用

`interview-infrastructure` 依赖了 `spring-boot-starter-data-redis`，`application.yml` 配置了 Redis 连接，但代码中无任何 Redis 使用。

**建议**：
- 实现 Token 黑名单（登出时将 JWT 加入 Redis 黑名单）
- 缓存资料列表查询结果
- 或暂时移除依赖以减少启动报错风险

#### 10.14 缺少 CORS 配置

前后端分离架构下，前端（3000 端口）访问后端（8080 端口）可能遇到跨域问题。当前通过 Next.js rewrite 代理绕过，但直接 API 调用仍需 CORS。

**建议**：在 `SecurityConfig` 中添加 CORS 配置。

#### 10.15 PO 对象可改用 record

`UserPO`、`MaterialPO`、`AsyncTaskRecordPO` 使用传统 getter/setter 模式，但 MyBatis XML 使用 constructor 映射。

**建议**：统一为 Java record，与 Domain 层风格一致，减少样板代码。

---

## 附录：项目文件索引

### 后端核心文件

| 文件 | 路径 |
|------|------|
| 启动类 | `backend/interview-api/src/main/java/com/interview/api/InterviewApiApplication.java` |
| 安全配置 | `backend/interview-api/src/main/java/com/interview/api/config/SecurityConfig.java` |
| JWT 服务 | `backend/interview-api/src/main/java/com/interview/api/config/JwtTokenService.java` |
| JWT 过滤器 | `backend/interview-api/src/main/java/com/interview/api/security/JwtAuthenticationFilter.java` |
| 认证控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/AuthController.java` |
| 资料控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/MaterialController.java` |
| 任务控制器 | `backend/interview-api/src/main/java/com/interview/api/controller/AsyncTaskController.java` |
| 健康检查 | `backend/interview-api/src/main/java/com/interview/api/controller/HealthController.java` |
| 认证服务 | `backend/interview-application/src/main/java/com/interview/application/service/AuthApplicationService.java` |
| 资料服务 | `backend/interview-application/src/main/java/com/interview/application/service/MaterialApplicationService.java` |
| 任务服务 | `backend/interview-application/src/main/java/com/interview/application/service/AsyncTaskApplicationService.java` |
| LLM 网关接口（Application Port） | `backend/interview-application/src/main/java/com/interview/application/port/LlmGateway.java` |
| LLM Stub | `backend/interview-ai-gateway/src/main/java/com/interview/aigateway/impl/NoopLlmGateway.java` |
| 应用配置 | `backend/interview-api/src/main/resources/application.yml` |
| 数据库初始化 | `backend/sql/init.sql` |
| Docker 基础设施 | `docker-compose.infrastructure.yml` |

### 前端核心文件

| 文件 | 路径 |
|------|------|
| 首页 | `frontend/app/page.tsx` |
| 根布局 | `frontend/app/layout.tsx` |
| 全局样式 | `frontend/app/globals.css` |
| Next.js 配置 | `frontend/next.config.ts` |
| Tailwind 配置 | `frontend/tailwind.config.ts` |
| 依赖配置 | `frontend/package.json` |

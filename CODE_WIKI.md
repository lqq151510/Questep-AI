# AI Interview Platform — Code Wiki

> Version: 0.1.0-SNAPSHOT | Last Updated: 2026-05-13

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Backend Modules](#3-backend-modules)
4. [Frontend Modules](#4-frontend-modules)
5. [Key Classes and Functions](#5-key-classes-and-functions)
6. [Database Design](#6-database-design)
7. [Dependency Relationships](#7-dependency-relationships)
8. [Project Run Guide](#8-project-run-guide)
9. [API Reference](#9-api-reference)
10. [Improvement Suggestions](#10-improvement-suggestions)

---

## 1. Project Overview

**AI Interview Platform** is a full-stack, front-end/back-end separated application. Core features include:

- Users upload learning materials (PDF/Word/MD/TXT/CSV/JSON/Images)
- AI automatically parses materials, chunks and vectorizes them (Milvus)
- AI generates interview questions (single choice / multiple choice / short answer / programming)
- AI grading + detailed explanations
- Wrong answer automatic storage + review
- WebSocket interview simulation (AI interviewer streaming questions)
- Learning analysis reports
- Multi-vendor LLM routing (OpenAI / Anthropic / DeepSeek)
- AI evaluation framework (Eval) for prompt/model quality regression testing

| Dimension | Description |
| ----- | ---------------------------------- |
| Backend Language | Java 21 |
| Backend Framework | Spring Boot 3.3.5 |
| Frontend Framework | Next.js 15 + React 19 + TypeScript |
| Database | MySQL 8.4 |
| Vector Database | Milvus 2.4 |
| Cache | Redis 7.2 |
| Message Queue | RabbitMQ (optional) |
| Build Tool | Maven (Backend) / npm (Frontend) |
| DB Migration | Flyway |
| Resilience | Resilience4j |
| Observability | Micrometer + Prometheus + Grafana + OpenTelemetry |

---

## 2. Architecture

### 2.1 Architecture Style

Uses **traditional layered + port adapter** hybrid architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Frontend (Next.js 15)                       │
│                  127.0.0.1:3000 → /api/* proxied to backend      │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP / WebSocket
┌──────────────────────────────▼──────────────────────────────────┐
│                  interview-api (Web / Entry Layer)                │
│     Controllers / Security / JWT / WebSocket / Schedulers        │
├─────────────────────────────────────────────────────────────────┤
│                interview-application (Application Layer)          │
│        Application Services / DTO / Ports (interfaces)           │
├─────────────────────────────────────────────────────────────────┤
│                   interview-domain (Domain Layer)                 │
│            Domain Models (record) / Repository (interface)        │
├─────────────────────────────────────────────────────────────────┤
│              interview-infrastructure (Infrastructure Layer)      │
│       RepositoryImpl / MyBatis Mapper / PO / XML / Milvus        │
├─────────────────────────────────────────────────────────────────┤
│                interview-ai-gateway (AI Gateway Layer)            │
│     LlmGateway (interface) / OpenAi / DeepSeek / MultiVendor     │
├─────────────────────────────────────────────────────────────────┤
│                  interview-common (Common Layer)                  │
│           ApiResponse / Exceptions / Utilities                   │
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
      │ RabbitMQ │ (optional)
      │  :5672   │
      └──────────┘
```

### 2.2 Request Flow

```
HTTP Request
  → JwtAuthenticationFilter (Token parse & auth)
    → Controller (input validation, unified response)
      → ApplicationService (use case orchestration, transaction boundary)
        → Domain Repository (interface)
          → Infrastructure RepositoryImpl (MyBatis Mapper)
            → MySQL / Redis / Milvus
```

### 2.3 Async Task Flow

```
Upload Material
  → MaterialApplicationService.uploadAndCreateParseTask()
    → Create Material record (MySQL)
    → Create AsyncTaskRecord (status=PENDING, taskNo=PARSE-xxx)
  → RabbitMQ (if configured) / Scheduler scans PENDING tasks
    → MaterialParseTaskListener / MaterialParseTaskProcessor
      → Parse file content → Chunk → Vectorize (Milvus)
      → Update AsyncTaskRecord (status=SUCCESS/FAILED)
```

---

## 3. Backend Modules

### 3.1 interview-common (Common Layer)

**Responsibility**: Global shared utilities, API response wrapper, exceptions, constants.

| Class | Description |
| ----- | ----------- |
| `ApiResponse<T>` | Unified response record with `success`/`message`/`data` fields |
| `ResourceNotFoundException` | Resource not found exception |
| `UnauthorizedException` | Unauthorized exception |
| `TaskConstants` | Task type constants (e.g., `TASK_TYPE_PARSE`) |
| `LlmProviderNormalizer` | Normalizes LLM provider names (openai-compatible, openai-format, etc.) |

**Dependencies**: `spring-context`, `spring-web`, `jakarta.annotation-api` — no business dependencies.

### 3.2 interview-domain (Domain Layer)

**Responsibility**: Core domain models and repository interfaces. Pure Java records + interfaces, zero external dependencies.

#### Domain Models

| Class | Type | Description |
| ----- | ---- | ----------- |
| `User` | record | User model: `id`, `username`, `email`, `phone`, `passwordHash`, `status` |
| `Material` | record | Material model: `id`, `userId`, `name`, `fileType`, `storageUrl`, `parseStatus`, `parseResult`, `createdAt`, `updatedAt` |
| `MaterialChunk` | record | Material chunk: `id`, `materialId`, `chunkIndex`, `content`, `embedding` |
| `MaterialChunkDraft` | record | Chunk draft for review before finalizing |
| `AsyncTaskRecord` | record | Async task: `id`, `taskNo`, `taskType`, `bizId`, `bizType`, `status`, `progress`, `resultJson`, `payloadJson`, `errorCode`, `errorMsg`, `retryCount`, `maxRetry`, `nextRetryAt`, `createdBy`, `createdAt`, `updatedAt` |
| `Question` | record | Question: `id`, `userId`, `materialIds`, `questionType`, `difficulty`, `content`, `options`, `answer`, `explanation`, `createdAt` |
| `InterviewSession` | record | Interview session: `id`, `userId`, `position`, `difficulty`, `status`, `contextSnapshot`, `createdAt`, `updatedAt` |
| `WrongBook` | record | Wrong answer book: `id`, `userId`, `questionId`, `questionContent`, `userAnswer`, `correctAnswer`, `explanation`, `masteryStatus`, `reviewCount`, `createdAt`, `updatedAt` |
| `UserLlmSettings` | record | User LLM settings: `id`, `userId`, `providerName`, `modelName`, `baseUrl`, `apiKey`, `enabled`, `createdAt`, `updatedAt` |
| `PromptTemplate` | record | Prompt template: `id`, `name`, `version`, `content`, `variables`, `isActive`, `createdAt` |
| `EvalRun` | record | Eval run: `id`, `runKey`, `status`, `totalCases`, `passedCases`, `failedCases`, `startedAt`, `finishedAt` |
| `EvalCase` | record | Eval case: `id`, `runKey`, `caseName`, `input`, `expectedOutput`, `actualOutput`, `status`, `errorMessage` |
| `EvalResult` | record | Eval result: `id`, `runKey`, `metricName`, `metricValue`, `details` |

#### Repository Interfaces

| Interface | Key Methods |
| --------- | ----------- |
| `UserRepository` | `findByUsername`, `findById`, `save` |
| `MaterialRepository` | `save`, `findById`, `findAllByUserId`, `deleteById` |
| `MaterialChunkRepository` | `save`, `findByMaterialId`, `deleteByMaterialId` |
| `AsyncTaskRecordRepository` | `create`, `findByTaskNo`, `findByBizId`, `findPendingTasks`, `updateStatus`, `updateProgress` |
| `QuestionRepository` | `save`, `findById`, `findByUserId`, `findByMaterialIds` |
| `InterviewSessionRepository` | `save`, `findById`, `findActiveByUserId`, `updateStatus` |
| `WrongBookRepository` | `save`, `findById`, `findByUserId`, `findByUserIdAndMasteryStatus`, `deleteById` |
| `UserLlmSettingsRepository` | `findByUserId`, `save`, `upsert` |
| `PromptTemplateRepository` | `findByNameAndVersion`, `findActiveByName` |
| `EvalRunRepository` | `save`, `findByRunKey`, `findAll` |
| `EvalCaseRepository` | `save`, `findByRunKey` |
| `EvalResultRepository` | `save`, `findByRunKey` |

**Dependencies**: Zero external dependencies, pure Java records + interfaces.

### 3.3 interview-application (Application Layer)

**Responsibility**: Use case orchestration, transaction boundaries, DTO definitions, port interfaces.

#### Application Services

| Class | Description |
| ----- | ----------- |
| `AuthApplicationService` | Authentication: login, register, refresh token, captcha integration, login attempt tracking |
| `MaterialApplicationService` | Material use cases: upload, create parse task, list by user, delete, retry parse |
| `AsyncTaskApplicationService` | Async task use cases: query by taskNo |
| `ChatApplicationService` | Chat with AI, send message and get response |
| `QuizApplicationService` | Quiz generation: generate questions from materials, recent questions query |
| `WrongBookApplicationService` | Wrong book management: add, list, update mastery status, delete |
| `EvalService` | AI evaluation framework: run eval, list runs, list cases |
| `UserLlmSettingsApplicationService` | User LLM settings: get, update |
| `MaterialRagApplicationService` | RAG (Retrieval Augmented Generation): retrieve relevant chunks from Milvus |
| `MaterialParseTaskProcessor` | Process material parse tasks: parse file, chunk, vectorize |
| `TokenService` (interface) | Token generation and parsing |
| `TokenBlacklistService` | JWT token blacklist management (Redis) |
| `CaptchaService` | Captcha code generation |
| `LoginAttemptService` | Login attempt tracking and lockout |
| `PromptTemplateService` | Prompt template loading and rendering |

#### DTOs

| Class | Description |
| ----- | ----------- |
| `LoginCommand` | Login request: `username`, `password` |
| `LoginResult` | Login response: `token`, `refreshToken`, `tokenType` |
| `RegisterCommand` | Register request: `username`, `email`, `phone`, `password`, `captchaCode`, `captchaKey` |
| `RefreshTokenCommand` | Refresh request: `refreshToken` |
| `LogoutCommand` | Logout request: `refreshToken` |
| `CaptchaResponse` | Captcha response: `captchaKey`, `captchaImage` |
| `GenerateQuizCommand` | Quiz request: `materialIds`, `questionType`, `difficulty`, `count`, `interviewMode` |
| `GeneratedQuizResult` | Quiz response: `questions`, `modelBrief` |
| `UploadMaterialResult` | Upload response: `material`, `asyncTaskRecord` |
| `ChatRequest` | Chat request: `sessionId`, `message`, `context` |
| `ChatResponse` | Chat response: `reply`, `sessionId` |
| `CreateSessionRequest` | Create session: `position`, `difficulty` |
| `AddWrongBookCommand` | Add wrong book: `questionId`, `questionContent`, `userAnswer`, `correctAnswer`, `explanation` |
| `UpdateMasteryCommand` | Update mastery: `masteryStatus` |
| `WrongBookItem` | Wrong book item view |
| `UpdateUserLlmSettingsCommand` | Update LLM settings: `providerName`, `modelName`, `baseUrl`, `apiKey`, `enabled` |
| `UserLlmSettingsView` | User LLM settings view |
| `UpdateMasteryCommand` | Update mastery status: `masteryStatus` |

#### Port Interfaces (for external capabilities)

| Interface | Description |
| --------- | ----------- |
| `LlmGateway` | LLM call abstraction: `chat(String prompt)` |
| `AsyncTaskDispatcher` | Async task dispatch abstraction |
| `VectorStoreGateway` | Vector store abstraction: `upsert`, `search`, `delete` |

**Dependencies**: `interview-domain` + `spring-context` + `spring-tx` + `spring-security-crypto` + `jakarta.validation-api`

### 3.4 interview-api (Web Layer / Entry Module)

**Responsibility**: HTTP entry point, security config, JWT auth, WebSocket, request routing, schedulers.

#### Controllers

| Controller | Base Path | Description |
| ---------- | --------- | ----------- |
| `AuthController` | `/api/v1/auth` | Login, register, captcha, refresh token, logout |
| `MaterialController` | `/api/v1/materials` | Upload (multipart), list, delete, retry parse |
| `QuizController` | `/api/v1/quizzes` | Generate quiz, recent questions |
| `InterviewController` | `/api/v1/interviews` | Create/get/resume interview sessions |
| `ChatController` | `/api/v1/chat` | AI chat |
| `WrongBookController` | `/api/v1/wrong-books` | Add, list, update mastery, delete wrong books |
| `EvalController` | `/api/v1/eval` | Trigger eval run, list runs, list cases |
| `AsyncTaskController` | `/api/v1/async-tasks` | Query async task status |
| `UserLlmSettingsController` | `/api/v1/llm/settings` | Get/update user LLM settings |
| `HealthController` | `/api/v1/health` | Health check |

#### Configuration Classes

| Class | Description |
| ----- | ----------- |
| `InterviewApiApplication` | Spring Boot entry point, `@MapperScan` for infrastructure |
| `SecurityConfig` | Spring Security: stateless session, JWT filter, path permissions |
| `JwtAuthenticationFilter` | JWT token parse filter, extracts user from `Authorization: Bearer` |
| `JwtTokenService` | JWT token generation and parsing (implements `TokenService`) |
| `InterviewWebSocketConfig` | WebSocket config for interview sessions |
| `JwtWebSocketHandshakeInterceptor` | WebSocket handshake JWT auth interceptor |
| `InterviewWebSocketHandler` | WebSocket handler for real-time interview chat |
| `WebMvcConfig` | Web MVC config, API version interceptor |
| `ApiVersionInterceptor` | API version validation interceptor |
| `OpenApiConfig` | OpenAPI/Swagger config |
| `Resilience4jMetricsConfig` | Resilience4j + Micrometer metrics config |
| `TracingConfig` | OpenTelemetry tracing config |
| `ApiExceptionHandler` | Global exception handler, unified error response |

#### Async / Message Queue

| Class | Description |
| ----- | ----------- |
| `AsyncTaskRabbitMqConfig` | RabbitMQ queue/exchange config for async tasks |
| `MaterialParseTaskListener` | RabbitMQ consumer for material parse tasks |
| `RabbitMqAsyncTaskDispatcher` | RabbitMQ async task dispatcher implementation |
| `NoopAsyncTaskDispatcher` | No-op dispatcher (fallback when RabbitMQ not configured) |
| `AsyncTaskScheduler` | Scheduler for scanning PENDING tasks |

#### Schedulers

| Class | Description |
| ----- | ----------- |
| `AsyncTaskScheduler` | Periodically scans and dispatches PENDING async tasks |
| `QuestionPrewarmScheduler` | Pre-warms question cache |
| `QuestionReviewScheduler` | Reviews generated question quality |

#### Security

| Class | Description |
| ----- | ----------- |
| `JwtAuthenticationFilter` | Extracts Bearer token, parses userId, sets SecurityContext |
| `CurrentUser` | Utility: gets current authenticated user ID from SecurityContext |

**Dependencies**: All 5 sub-modules + `spring-boot-starter-web/security/validation/actuator/websocket` + `jjwt` + `mysql-connector-j` + `springdoc-openapi` + `resilience4j` + `micrometer-tracing` + `rabbitmq-client`

### 3.5 interview-infrastructure (Infrastructure Layer)

**Responsibility**: Concrete implementations of domain repository interfaces, MyBatis Mappers and XML mappings, external system gateways.

#### Persistence

| Package | Class | Description |
| ------- | ----- | ----------- |
| `persistence.entity` | `UserPO` | User persistence object |
| `persistence.entity` | `MaterialPO` | Material persistence object |
| `persistence.entity` | `AsyncTaskRecordPO` | Async task persistence object |
| `persistence.entity` | `QuestionPO` | Question persistence object |
| `persistence.entity` | `InterviewSessionPO` | Interview session persistence object |
| `persistence.entity` | `WrongBookPO` | Wrong book persistence object |
| `persistence.entity` | `UserLlmSettingsPO` | User LLM settings persistence object |
| `persistence.entity` | `MaterialChunkPO` | Material chunk persistence object |
| `persistence.entity` | `PromptTemplatePO` | Prompt template persistence object |
| `persistence.entity` | `EvalRunPO` | Eval run persistence object |
| `persistence.entity` | `EvalCasePO` | Eval case persistence object |
| `persistence.entity` | `EvalResultPO` | Eval result persistence object |
| `persistence.repository` | `UserRepositoryImpl` | `UserRepository` implementation |
| `persistence.repository` | `MaterialRepositoryImpl` | `MaterialRepository` implementation |
| `persistence.repository` | `AsyncTaskRecordRepositoryImpl` | `AsyncTaskRecordRepository` implementation |
| `persistence.repository` | `QuestionRepositoryImpl` | `QuestionRepository` implementation |
| `persistence.repository` | `InterviewSessionRepositoryImpl` | `InterviewSessionRepository` implementation |
| `persistence.repository` | `WrongBookRepositoryImpl` | `WrongBookRepository` implementation |
| `persistence.repository` | `UserLlmSettingsRepositoryImpl` | `UserLlmSettingsRepository` implementation |
| `persistence.repository` | `MaterialChunkRepositoryImpl` | `MaterialChunkRepository` implementation |
| `persistence.repository` | `PromptTemplateRepositoryImpl` | `PromptTemplateRepository` implementation |
| `persistence.repository` | `EvalRunRepositoryImpl` | `EvalRunRepository` implementation |
| `persistence.repository` | `EvalCaseRepositoryImpl` | `EvalCaseRepository` implementation |
| `persistence.repository` | `EvalResultRepositoryImpl` | `EvalResultRepository` implementation |

#### MyBatis Mapper XML

| File | Description |
| ---- | ----------- |
| `UserMapper.xml` | SQL: insert, query by username/ID |
| `MaterialMapper.xml` | SQL: insert, query by ID/user, list, delete |
| `AsyncTaskRecordMapper.xml` | SQL: insert, query by taskNo/bizId, update status/progress |
| `QuestionMapper.xml` | SQL: insert, query by ID/user/materialIds |
| `InterviewSessionMapper.xml` | SQL: insert, query by ID/user, update status |
| `WrongBookMapper.xml` | SQL: insert, query by user/mastery status, delete |
| `UserLlmSettingsMapper.xml` | SQL: insert/upsert, query by userId |
| `MaterialChunkMapper.xml` | SQL: insert, query by materialId, delete by materialId |
| `PromptTemplateMapper.xml` | SQL: query by name/version, active templates |
| `EvalRunMapper.xml` | SQL: insert, query by runKey, list all |
| `EvalCaseMapper.xml` | SQL: insert, query by runKey |
| `EvalResultMapper.xml` | SQL: insert, query by runKey |

#### External Gateways

| Class | Description |
| ----- | ----------- |
| `MilvusVectorStoreGateway` | Milvus vector store gateway implementation (implements `VectorStoreGateway`) |
| `ApiKeyCryptoService` | API key encryption/decryption service |

**Dependencies**: `interview-domain` + `mybatis-spring-boot-starter` + `spring-boot-starter-data-redis` + `milvus-sdk-java`

### 3.6 interview-ai-gateway (AI Gateway Layer)

**Responsibility**: Unified LLM call abstraction, multi-vendor model routing, prompt assembly, output validation.

| Class | Description |
| ----- | ----------- |
| `LlmGateway` | Interface: `chat(String prompt)` — unified LLM call entry |
| `OpenAiLlmGateway` | OpenAI-compatible API implementation (uses `RestClient`) |
| `DeepSeekLlmGateway` | DeepSeek API implementation |
| `MultiVendorLlmGateway` | Multi-vendor router: supports OpenAI-compatible, Anthropic/Claude, resolves config from user settings or defaults, handles retries |
| `NoopLlmGateway` | Stub implementation: returns placeholder message for development testing |

**MultiVendorLlmGateway** supports:
- OpenAI-compatible format (DeepSeek, Qwen, etc.)
- Anthropic/Claude API
- Automatic fallback between providers
- Config resolution from user LLM settings or system defaults

**Dependencies**: `spring-context` + `spring-web` (for RestClient)

---

## 4. Frontend Modules

### 4.1 Tech Stack

| Technology | Version | Purpose |
| ---------- | ------- | ------- |
| Next.js | 15.x | React full-stack framework (App Router) |
| React | 19.x | UI library |
| TypeScript | 5.7.x | Type safety |
| Tailwind CSS | 3.4.x | Utility-first CSS |
| Framer Motion | 12.x | Animation library |
| Zustand | 5.x | State management |
| lucide-react | 0.468.x | Icon library |
| CVA (class-variance-authority) | 0.7.x | Component variant management |

### 4.2 Directory Structure

```
frontend/
├── app/                      # Next.js App Router pages
│   ├── page.tsx              # Landing/redirect page
│   ├── layout.tsx            # Root layout
│   ├── globals.css           # Global styles
│   ├── home/page.tsx         # Home dashboard
│   ├── login/page.tsx        # Login page
│   ├── register/page.tsx     # Registration page
│   ├── ai-interviewer/page.tsx    # AI interviewer simulation
│   ├── ai-qa/page.tsx             # AI Q&A
│   ├── ai-test/page.tsx           # AI test/quiz
│   ├── question-bank/page.tsx     # Question bank
│   ├── wrong-answers/page.tsx     # Wrong answer review
│   ├── knowledge-base/page.tsx    # Knowledge base
│   ├── interview-tips/page.tsx    # Interview tips
│   └── settings/page.tsx          # User settings
├── components/
│   ├── dashboard/            # Dashboard components
│   │   ├── InsightPanel.tsx
│   │   ├── MaterialPanel.tsx
│   │   ├── OverviewGrid.tsx
│   │   ├── QuestionComposer.tsx
│   │   ├── StatTile.tsx
│   │   └── TaskPanel.tsx
│   ├── layout/               # Layout components
│   │   ├── ParticleField.tsx
│   │   ├── Sidebar.tsx
│   │   └── TopBar.tsx
│   ├── new-ui/               # New UI system
│   │   ├── AnimatedCounter.tsx
│   │   ├── AppChrome.tsx
│   │   ├── PageHero.tsx
│   │   ├── ToastProvider.tsx
│   │   ├── cards.tsx
│   │   └── nav-config.ts
│   ├── theme/                # Theme system
│   │   ├── ThemeProvider.tsx
│   │   └── ThemeToggle.tsx
│   └── ui/                   # Base UI components
│       ├── EmptyState.tsx
│       ├── Skeleton.tsx
│       ├── button.tsx
│       ├── error-boundary.tsx
│       ├── loading-overlay.tsx
│       └── progress.tsx
├── lib/                      # Utilities
│   ├── hooks/
│   │   ├── useMaterials.ts
│   │   └── useTasks.ts
│   ├── dashboard-data.ts     # Dashboard data fetching
│   ├── dashboard-format.ts   # Dashboard data formatting
│   ├── fetch-with-retry.ts   # Fetch with retry logic
│   ├── interview-api.ts      # API client for backend
│   └── utils.ts              # General utilities
├── stores/
│   └── useDashboardStore.ts  # Zustand dashboard state store
├── types/
│   └── dashboard.ts          # TypeScript type definitions
├── docs/
│   └── ui-overhaul-plan.md   # UI redesign plan
├── artifacts/                # Design screenshots
├── next.config.ts            # Next.js config (API proxy)
├── tailwind.config.ts        # Tailwind config
├── postcss.config.mjs        # PostCSS config
├── tsconfig.json             # TypeScript config
├── eslint.config.mjs         # ESLint config
├── .prettierrc.json          # Prettier config
└── package.json              # Dependencies
```

### 4.3 Core Components

| Component/Function | Description |
| ------------------ | ----------- |
| `AppChrome` | Main application shell with sidebar and content area |
| `Sidebar` | Navigation sidebar with menu items |
| `TopBar` | Top navigation bar with user info and theme toggle |
| `ParticleField` | Background particle animation decoration |
| `StatTile` | Stat card: icon + label + value + note |
| `MaterialPanel` | Material list panel |
| `QuestionComposer` | Question composition panel |
| `TaskPanel` | Async task queue panel |
| `InsightPanel` | Learning insights panel |
| `OverviewGrid` | Dashboard overview grid |
| `ThemeProvider` | Theme provider (light/dark mode) |
| `ThemeToggle` | Theme toggle button |
| `AnimatedCounter` | Animated number counter |
| `PageHero` | Page hero section component |
| `ToastProvider` | Toast notification provider |
| `useMaterials` | Custom hook for material operations |
| `useTasks` | Custom hook for task operations |
| `useDashboardStore` | Zustand store for dashboard state |

### 4.4 API Proxy

`next.config.ts` configures rewrite rules to proxy `/api/*` requests to the backend `http://127.0.0.1:8080/api/*`, overridable via `BACKEND_API_URL` environment variable.

### 4.5 State Management

- **Zustand**: Used for global dashboard state (`useDashboardStore`)
- **localStorage**: Stores `interview_token` and `interview_refresh_token` for auth persistence
- **React hooks**: Custom hooks (`useMaterials`, `useTasks`) encapsulate API calls and local state

---

## 5. Key Classes and Functions

### 5.1 Backend Key Classes

#### JwtAuthenticationFilter

```
Path: com.interview.api.security.JwtAuthenticationFilter
Type: @Component, OncePerRequestFilter
Function: Extracts Bearer Token from HTTP Header → parses userId → queries user → sets SecurityContext
Key Logic:
  - Silently ignores token parse failures (logs WARN)
  - Only users with status == 1 can authenticate
  - Principal after auth is userId (Long), Authority is ROLE_USER
  - Checks token blacklist before setting authentication
```

#### JwtTokenService

```
Path: com.interview.api.config.JwtTokenService
Type: @Service, implements TokenService
Function: JWT token generation and parsing
Config:
  - app.jwt.secret: HMAC-SHA key (required, min 32 bytes)
  - app.jwt.expire-ms: Expiration time (default: 86400000ms = 24h)
  - app.jwt.refresh-expire-ms: Refresh token expiration (default: 7 days)
Methods:
  - generateToken(Long userId, String username): Generate JWT
  - generateRefreshToken(Long userId): Generate refresh token
  - parseUserId(String token): Parse userId from token
  - getExpireTime(String token): Get remaining token expiration time
```

#### AuthApplicationService

```
Path: com.interview.application.service.AuthApplicationService
Function: Authentication use cases
Methods:
  - login(LoginCommand): Validate credentials, check login attempts, generate tokens
  - register(RegisterCommand): Create new user, generate tokens
  - refresh(String refreshToken): Issue new access token from refresh token
Key Dependencies:
  - UserRepository: User data access
  - TokenService: JWT operations
  - TokenBlacklistService: Token blacklist (Redis)
  - CaptchaService: Captcha validation
  - LoginAttemptService: Login attempt tracking
```

#### MaterialApplicationService.uploadAndCreateParseTask

```
Path: com.interview.application.service.MaterialApplicationService
Transaction: @Transactional
Function: Create material record and async parse task in same transaction
Flow:
  1. materialRepository.save() → Save material record
  2. asyncTaskRecordRepository.create() → Create parse task (taskNo = "PARSE-" + UUID)
  3. Return UploadMaterialResult(material, task)
```

#### MultiVendorLlmGateway

```
Path: com.interview.aigateway.impl.MultiVendorLlmGateway
Function: Multi-vendor LLM router with automatic fallback
Supported Providers:
  - OpenAI-compatible (DeepSeek, Qwen, etc.)
  - Anthropic/Claude
Config Resolution:
  1. User LLM settings (from UserLlmSettings)
  2. System defaults (from application.yml)
Features:
  - Automatic provider detection from config
  - Retry on failure
  - OpenAI streaming support
  - Anthropic streaming support
```

#### CurrentUser

```
Path: com.interview.api.support.CurrentUser
Type: final utility class
Function: Get current authenticated user ID from SecurityContext
Method: static Long id() — throws IllegalStateException if not authenticated
```

### 5.2 Frontend Key Functions

#### interview-api.ts

```
Function: API client for backend communication
Features:
  - Automatic token injection from localStorage
  - Token refresh on 401
  - Unified error handling
Methods:
  - fetchWithAuth(url, options): Authenticated fetch
  - refreshToken(): Refresh access token
```

#### useMaterials hook

```
Function: Custom hook for material operations
Features:
  - Material list fetching
  - Upload handling
  - Delete handling
  - Parse status polling
```

---

## 6. Database Design

### 6.1 Table List

| # | Table Name | Description | Used in Code |
| - | ---------- | ----------- | ------------ |
| 1 | `users` | User table | ✅ |
| 2 | `materials` | Learning materials | ✅ |
| 3 | `material_chunks` | Material chunks | ✅ |
| 4 | `questions` | Questions | ✅ |
| 5 | `async_task_records` | Async task records | ✅ |
| 6 | `interview_sessions` | Interview sessions | ✅ |
| 7 | `wrong_book` | Wrong answer book | ✅ |
| 8 | `user_llm_settings` | User LLM settings | ✅ |
| 9 | `prompt_templates` | Prompt templates | ✅ |
| 10 | `eval_runs` | Eval runs | ✅ |
| 11 | `eval_cases` | Eval cases | ✅ |
| 12 | `eval_results` | Eval results | ✅ |
| 13 | `material_chunk_drafts` | Material chunk drafts | ✅ |
| 14 | `quiz_records` | Quiz records | Planned |
| 15 | `llm_call_logs` | LLM call logs | Planned |

### 6.2 Flyway Migrations

| Migration | Description |
| --------- | ----------- |
| `V1__init.sql` | Initial schema: users, materials, questions, wrong_book |
| `V2__add_material_analysis_text.sql` | Add material_analysis_text column |
| `V3__optimize_questions_table.sql` | Optimize questions table structure |
| `V4__add_async_task_progress.sql` | Add async task progress tracking |
| `V5__align_async_task_record_columns.sql` | Align async task record columns |
| `V6__add_interview_session.sql` | Add interview_sessions table |
| `V7__add_error_detail_to_async_task_records.sql` | Add error details to async tasks |
| `V8__add_user_llm_settings.sql` | Add user LLM settings table |
| `V9__add_biz_type_to_async_task_records.sql` | Add biz_type to async tasks |
| `V10__expand_user_llm_settings_api_key_length.sql` | Expand API key length |
| `V11__add_prompt_templates.sql` | Add prompt templates table |
| `V12__add_eval_framework.sql` | Add eval framework tables |

### 6.3 Core Table Structures

#### users

| Field | Type | Description |
| ----- | ---- | ----------- |
| id | BIGINT UNSIGNED PK | Auto-increment primary key |
| username | VARCHAR(64) UK | Username |
| email | VARCHAR(128) UK | Email |
| phone | VARCHAR(32) UK | Phone number |
| password_hash | VARCHAR(255) | BCrypt password hash |
| status | TINYINT UNSIGNED | 1=active, 0=disabled |
| created_at | DATETIME | Creation time |
| updated_at | DATETIME | Update time |

#### materials

| Field | Type | Description |
| ----- | ---- | ----------- |
| id | BIGINT UNSIGNED PK | Auto-increment primary key |
| user_id | BIGINT UNSIGNED FK | Owner user |
| material_name | VARCHAR(255) | Material name |
| material_type | VARCHAR(32) | PDF/WORD/MD/IMAGE/TEXT/CSV/JSON |
| storage_url | VARCHAR(1024) | Storage path |
| parse_status | ENUM | PENDING/PROCESSING/SUCCESS/FAILED |
| parse_result | TEXT | Parse result JSON |
| created_at | DATETIME | Creation time |
| updated_at | DATETIME | Update time |

#### async_task_records

| Field | Type | Description |
| ----- | ---- | ----------- |
| id | BIGINT UNSIGNED PK | Auto-increment primary key |
| task_no | VARCHAR(64) UK | Task number (PARSE-xxx) |
| task_type | VARCHAR(64) | Task type |
| biz_type | VARCHAR(64) | Business type |
| biz_id | BIGINT UNSIGNED | Related business ID |
| status | ENUM | PENDING/PROCESSING/SUCCESS/FAILED |
| progress | INT UNSIGNED | Progress percentage |
| result_json | TEXT | Result JSON |
| payload_json | TEXT | Payload JSON |
| error_code | VARCHAR(64) | Error code |
| error_msg | VARCHAR(500) | Error message |
| retry_count | INT UNSIGNED | Retry count |
| max_retry | INT UNSIGNED | Max retries |
| next_retry_at | DATETIME | Next retry time |
| created_by | BIGINT UNSIGNED | Creator user ID |
| created_at | DATETIME | Creation time |
| updated_at | DATETIME | Update time |

#### user_llm_settings

| Field | Type | Description |
| ----- | ---- | ----------- |
| id | BIGINT UNSIGNED PK | Auto-increment primary key |
| user_id | BIGINT UNSIGNED UK | User ID |
| provider_name | VARCHAR(64) | Provider name (openai, anthropic, etc.) |
| model_name | VARCHAR(128) | Model name |
| base_url | VARCHAR(512) | API base URL |
| api_key | VARCHAR(2000) | Encrypted API key |
| enabled | TINYINT | 1=enabled, 0=disabled |
| created_at | DATETIME | Creation time |
| updated_at | DATETIME | Update time |

---

## 7. Dependency Relationships

### 7.1 Backend Module Dependency Graph

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

**Detailed Dependencies**:

```
interview-common ← spring-context, spring-web, jakarta.annotation-api
interview-domain ← (no external dependencies)
interview-application ← interview-domain, spring-context, spring-tx, spring-security-crypto, jakarta.validation-api
interview-infrastructure ← interview-domain, mybatis-spring-boot-starter, spring-boot-starter-data-redis, milvus-sdk-java
interview-ai-gateway ← spring-context, spring-web
interview-api ← interview-common, interview-domain, interview-application, interview-infrastructure, interview-ai-gateway,
                 spring-boot-starter-web/security/validation/actuator/websocket, jjwt, mysql-connector-j,
                 springdoc-openapi, resilience4j, micrometer-tracing, rabbitmq-client
```

### 7.2 Frontend Dependencies

```
ai-interview-frontend
├── next@15.x
├── react@19.x, react-dom@19.x
├── framer-motion@12.x (animations)
├── zustand@5.x (state management)
├── lucide-react@0.468.x (icons)
├── class-variance-authority@0.7.x (component variants)
├── clsx@2.x (className merging)
├── tailwind-merge@3.x (Tailwind class merging)
└── devDependencies
    ├── typescript@5.7.x
    ├── tailwindcss@3.4.x
    ├── postcss@8.4.x
    ├── autoprefixer@10.4.x
    ├── eslint@9.x
    ├── prettier@3.x
    └── @playwright/test@1.59.x
```

### 7.3 Infrastructure Dependencies (Docker Compose)

| Service | Image | Port | Purpose |
| ------- | ----- | ---- | ------- |
| MySQL | mysql:8.4 | 3306 | Relational database |
| Redis | redis:7.2-alpine | 6379 | Cache / session / token blacklist |
| Milvus | milvusdb/milvus:v2.4.23 | 19530, 9091 | Vector search |
| etcd | quay.io/coreos/etcd:v3.5.18 | 2379 | Milvus metadata storage |
| MinIO | minio/minio:RELEASE.2023-03-20 | 9000, 9091 | Milvus object storage |
| RabbitMQ | rabbitmq:3-management | 5672, 15672 | Message queue (optional) |

---

## 8. Project Run Guide

### 8.1 Environment Requirements

- **JDK**: 21+
- **Node.js**: 18+ (recommended 20+)
- **Docker**: For running MySQL/Redis/Milvus/RabbitMQ
- **Maven**: 3.9+

### 8.2 Start Infrastructure

```bash
cd /path/to/project
docker compose -f docker-compose.infrastructure.yml up -d
# Database schema is auto-migrated by Flyway when interview-api starts
```

### 8.3 Start Backend

```bash
cd backend
mvn clean package
JWT_SECRET=<your-secret-at-least-32-chars> mvn -pl interview-api spring-boot:run
```

Backend runs at `http://127.0.0.1:8080`.

**Environment Variables**:

| Variable | Default | Description |
| -------- | ------- | ----------- |
| `JWT_SECRET` | (required) | JWT HMAC-SHA key, min 32 bytes |
| `MYSQL_URL` | `jdbc:mysql://127.0.0.1:3306/interview_ai?...` | MySQL connection URL |
| `MYSQL_USER` | `interview_user` | MySQL username |
| `MYSQL_PASSWORD` | `interview_pass` | MySQL password |
| `REDIS_HOST` | `127.0.0.1` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `MILVUS_HOST` | `127.0.0.1` | Milvus host |
| `MILVUS_PORT` | `19530` | Milvus port |
| `LLM_PROVIDER` | `anthropic` | Default LLM provider |
| `LLM_BASE_URL` | (provider default) | LLM API URL |
| `LLM_API_KEY` | (empty) | LLM API key |
| `LLM_MODEL` | (provider default) | LLM model name |

> Default LLM provider is `anthropic`. If `~/.claude/settings.json` exists, the system auto-reads `ANTHROPIC_AUTH_TOKEN`/`ANTHROPIC_BASE_URL`/`ANTHROPIC_MODEL` as default config.

### 8.4 Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://127.0.0.1:3000`, API requests automatically proxy to backend.

### 8.5 Quick Validation

```bash
curl http://127.0.0.1:8080/actuator/health

# demo_user / demo123456
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"demo123456"}' | jq -r '.data.token')

curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/materials

curl -X POST http://127.0.0.1:8080/api/v1/quizzes/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"materialIds":[1],"questionType":"short","difficulty":3,"count":3,"interviewMode":true}'
```

### 8.6 Frontend Local Token Setup

```js
localStorage.setItem("interview_token", "<login token>");
localStorage.setItem("interview_refresh_token", "<refresh token>");
```

---

## 9. API Reference

### 9.1 Authentication

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/auth/login` | ❌ | User login, returns JWT tokens |
| POST | `/api/v1/auth/register` | ❌ | User registration |
| GET | `/api/v1/auth/captcha` | ❌ | Generate captcha code |
| POST | `/api/v1/auth/refresh` | ❌ | Refresh JWT tokens |
| POST | `/api/v1/auth/logout` | ✅ | User logout (blacklists tokens) |

### 9.2 Materials

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/materials/upload` | ✅ | Upload material file (multipart) |
| GET | `/api/v1/materials` | ✅ | Get material list (filtered by current user) |
| DELETE | `/api/v1/materials/{id}` | ✅ | Delete material |
| POST | `/api/v1/materials/{id}/retry-parse` | ✅ | Retry material parsing |

### 9.3 Quizzes

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/quizzes/generate` | ✅ | Generate quiz from materials |
| GET | `/api/v1/quizzes/questions` | ✅ | Get recent questions (paginated) |

### 9.4 Interviews

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/interviews/sessions` | ✅ | Create or get active interview session |
| GET | `/api/v1/interviews/sessions/active` | ✅ | Get active interview session |
| POST | `/api/v1/interviews/{id}/resume` | ✅ | Resume a paused interview session |

### 9.5 Chat

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/chat` | ✅ | Send message to AI, get response |

### 9.6 Wrong Book

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/wrong-books` | ✅ | Add wrong answer |
| GET | `/api/v1/wrong-books` | ✅ | List wrong answers (optional masteryStatus filter) |
| PUT | `/api/v1/wrong-books/{id}/mastery` | ✅ | Update mastery status |
| DELETE | `/api/v1/wrong-books/{id}` | ✅ | Delete wrong answer |

### 9.7 Eval

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/api/v1/eval/runs` | ❌ | Trigger an eval run |
| GET | `/api/v1/eval/runs` | ❌ | List all eval runs |
| GET | `/api/v1/eval/runs/{runKey}` | ❌ | Get specific eval run |
| GET | `/api/v1/eval/cases` | ❌ | List eval cases |

### 9.8 Async Tasks

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| GET | `/api/v1/async-tasks/{taskNo}` | ✅ | Query async task status |

### 9.9 LLM Settings

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| GET | `/api/v1/llm/settings` | ✅ | Get user LLM settings |
| PUT | `/api/v1/llm/settings` | ✅ | Update user LLM settings |

### 9.10 Health

| Method | Path | Auth | Description |
| ------ | ---- | ---- | ----------- |
| GET | `/api/v1/health` | ❌ | Health check |

### 9.11 Unified Response Format

```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

Error response:

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### 9.12 WebSocket

| Endpoint | Auth | Description |
| -------- | ---- | ----------- |
| `/ws/interview` | ✅ (JWT via handshake interceptor) | Real-time interview chat WebSocket |

---

## 10. Improvement Suggestions

### 10.1 Security Hardening

- JWT secret uses default value `change-this-secret-change-this-secret` — should require environment variable with validation at startup
- `JwtAuthenticationFilter` silently swallows exceptions — should at least log WARN level
- API key encryption service exists but may need key rotation support

### 10.2 Testing Coverage

- Current project has limited test coverage
- Add JUnit 5 + Mockito unit tests for ApplicationService layer
- Add `@WebMvcTest` integration tests for Controllers
- Consider Testcontainers for MySQL/Redis integration tests

### 10.3 Frontend Architecture

- Consider extracting shared hooks and API utilities further
- E2E test setup with Playwright is present but may need more test cases
- Consider adding error boundary and loading state consistency across all pages

### 10.4 Observability

- Resilience4j + Micrometer + Prometheus + Grafana are configured but dashboards may need customization
- OpenTelemetry tracing is configured — ensure all LLM calls are traced
- Add alerting rules for task failure rate, model timeout rate, WebSocket disconnect rate

### 10.5 Deployment

- Current Docker Compose is suitable for local/dev
- Consider Kubernetes manifests for production deployment
- Add health check and readiness probe configurations
- Consider implementing blue-green deployment strategy

---

## Appendix: Project File Index

### Backend Core Files

| File | Path |
| ---- | ---- |
| Entry Point | `backend/interview-api/src/main/java/com/interview/api/InterviewApiApplication.java` |
| Security Config | `backend/interview-api/src/main/java/com/interview/api/config/SecurityConfig.java` |
| JWT Service | `backend/interview-api/src/main/java/com/interview/api/config/JwtTokenService.java` |
| JWT Filter | `backend/interview-api/src/main/java/com/interview/api/security/JwtAuthenticationFilter.java` |
| WebSocket Config | `backend/interview-api/src/main/java/com/interview/api/config/InterviewWebSocketConfig.java` |
| WebSocket Handler | `backend/interview-api/src/main/java/com/interview/api/websocket/InterviewWebSocketHandler.java` |
| Auth Controller | `backend/interview-api/src/main/java/com/interview/api/controller/AuthController.java` |
| Material Controller | `backend/interview-api/src/main/java/com/interview/api/controller/MaterialController.java` |
| Quiz Controller | `backend/interview-api/src/main/java/com/interview/api/controller/QuizController.java` |
| Interview Controller | `backend/interview-api/src/main/java/com/interview/api/controller/InterviewController.java` |
| Chat Controller | `backend/interview-api/src/main/java/com/interview/api/controller/ChatController.java` |
| WrongBook Controller | `backend/interview-api/src/main/java/com/interview/api/controller/WrongBookController.java` |
| Eval Controller | `backend/interview-api/src/main/java/com/interview/api/controller/EvalController.java` |
| Async Task Controller | `backend/interview-api/src/main/java/com/interview/api/controller/AsyncTaskController.java` |
| LLM Settings Controller | `backend/interview-api/src/main/java/com/interview/api/controller/UserLlmSettingsController.java` |
| Auth Service | `backend/interview-application/src/main/java/com/interview/application/service/AuthApplicationService.java` |
| Material Service | `backend/interview-application/src/main/java/com/interview/application/service/MaterialApplicationService.java` |
| Quiz Service | `backend/interview-application/src/main/java/com/interview/application/service/QuizApplicationService.java` |
| Chat Service | `backend/interview-application/src/main/java/com/interview/application/service/ChatApplicationService.java` |
| WrongBook Service | `backend/interview-application/src/main/java/com/interview/application/service/WrongBookApplicationService.java` |
| Eval Service | `backend/interview-application/src/main/java/com/interview/application/service/EvalService.java` |
| LLM Gateway Interface | `backend/interview-application/src/main/java/com/interview/application/port/LlmGateway.java` |
| MultiVendor LLM Gateway | `backend/interview-ai-gateway/src/main/java/com/interview/aigateway/impl/MultiVendorLlmGateway.java` |
| Application Config | `backend/interview-api/src/main/resources/application.yml` |
| DB Migrations | `backend/interview-api/src/main/resources/db/migration/` |
| Docker Infrastructure | `docker-compose.infrastructure.yml` |

### Frontend Core Files

| File | Path |
| ---- | ---- |
| Home Page | `frontend/app/home/page.tsx` |
| Login Page | `frontend/app/login/page.tsx` |
| Root Layout | `frontend/app/layout.tsx` |
| Global Styles | `frontend/app/globals.css` |
| Next.js Config | `frontend/next.config.ts` |
| Tailwind Config | `frontend/tailwind.config.ts` |
| API Client | `frontend/lib/interview-api.ts` |
| Dashboard Store | `frontend/stores/useDashboardStore.ts` |
| Dependencies | `frontend/package.json` |

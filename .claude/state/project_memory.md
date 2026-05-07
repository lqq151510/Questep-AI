# New project - 项目记忆

更新时间：2026-05-03（Asia/Shanghai）

## 仓库定位
- 路径：`/Users/liuyongze/Documents/New project`
- 形态：前后端分离
- 后端：Java 21 + Spring Boot 3.3.5（Maven 多模块）
- 前端：Next.js 15 + React 19 + Zustand

## Git 当前状态
- 当前分支：`master`
- 工作区：干净（无未提交改动）
- 远端：未配置（`git remote -v` 为空）

### 本地分支
- `master` -> `de89ccb`（HEAD）
- `fix/critical-issues` -> `a711c18`
- `refactor/code-review-improvements` -> `a711c18`
- `feature/improvements` -> `9691c3d`

### 最近提交链
1. `de89ccb` refactor: comprehensive security, performance, quality, architecture and frontend improvements
2. `5937ab7` feat: 将MyBatis Mapper改为动态SQL，优化代码架构
3. `a711c18` feat: implement async task scheduler and unify material field mapping
4. `35fa779` fix: 修复三个严重问题
5. `9691c3d` Initial commit

## 代码结构速览
- `backend/`
- `backend/interview-api`（Controller、Security、Scheduler、API 启动入口）
- `backend/interview-application`（应用服务层）
- `backend/interview-domain`（领域模型与仓储接口）
- `backend/interview-infrastructure`（MyBatis Mapper、PO、仓储实现）
- `backend/interview-ai-gateway`（LLM 网关）
- `backend/sql/init.sql`（核心表结构）
- `frontend/`（Next.js App Router + 组件 + 状态管理）

## 关键配置入口
- 后端聚合 POM：`backend/pom.xml`
- API 配置：`backend/interview-api/src/main/resources/application.yml`
- 前端依赖脚本：`frontend/package.json`

## 已确认能力（当前代码）
- `POST /api/auth/login` 登录鉴权
- `POST /api/materials/upload` 文件上传与落盘
- 异步任务跟踪（`async_task_records`）

# OnlyFriends 文档索引

本目录为项目文档的**唯一权威入口**。

## 代码与文档对应关系

| 类型 | 权威路径 | 说明 |
|------|----------|------|
| 后端模块 | `backend/onlyfriends-*` | Maven 父 POM 包含 8 个子模块（7 服务 + common） |
| 小程序 | `frontend/onlyfriends-miniprogram/` | API 配置在 `config/index.js`（`app.js` 引用） |
| Web 管理台 | `frontend/server.js` | 端口 5173，静态 HTML + Node 静态服务 |

## 入门指南

| 文档 | 说明 |
|------|------|
| [环境要求](getting-started/prerequisites.md) | JDK、Docker、Maven、Node.js 等 |
| [本地开发指南](getting-started/local-setup.md) | 从克隆到启动的完整流程 |
| [数据库初始化](getting-started/database-init.md) | 五库初始化、演示数据、默认账号 |

## 产品与需求

| 文档 | 说明 |
|------|------|
| [产品说明书](product/产品说明书.md) | 综合产品说明：定位、功能、流程、实现状态与限制 |
| [用户故事](product/user-stories.md) | Epic / Story 与优先级 |
| [验收标准](product/acceptance-criteria.md) | SMART 验收条件 |
| [发布计划](product/release-plan.md) | 迭代路线图 |
| [缺失内容与后续任务](product/outstanding-tasks.md) | 待完成功能与迭代目标 |

## 架构设计

| 文档 | 说明 |
|------|------|
| [系统设计 v3](architecture/system-design-v3.md) | 微服务拆分、数据库、API 契约、部署方案 |

## 微服务文档

| 服务 | 端口 | 文档 |
|------|------|------|
| API Gateway | 8080 | [gateway.md](services/gateway.md) |
| User Service | 8081 | [user-service.md](services/user-service.md) |
| Activity Service | 8082 | [activity-service.md](services/activity-service.md) |
| Social Service | 8083 | [social-service.md](services/social-service.md) |
| IM Service | 8084 | [im-service.md](services/im-service.md) |
| Admin Service | 8085 | [admin-service.md](services/admin-service.md) |
| AI Service | 8001 | [ai-service.md](services/ai-service.md) |
| Common 模块 | — | [common.md](services/common.md) |

## API 与联调

| 文档 | 说明 |
|------|------|
| [网关路由表](api/gateway-routes.md) | 8080 统一入口的路由规则 |
| [API 功能介绍与前端测试指南（中文）](api/overview-zh.md) | 接口说明与联调步骤 |
| [API Test Guide（English）](api/test-guide-en.md) | English API testing guide |

## 前端

| 文档 | 说明 |
|------|------|
| [小程序使用与测试教程](frontend/miniprogram-guide.md) | 微信开发者工具导入与测试 |
| [小程序前端规划与需求](frontend/miniprogram-spec.md) | 页面结构、交互与需求说明 |
| [Web 开发管理台](frontend/dev-console.md) | 5173 端口测试台使用说明 |

## 测试与验收

| 文档 | 说明 |
|------|------|
| [冒烟测试](testing/smoke-tests.md) | 后端自动化冒烟脚本 |
| [完整测试指南](testing/full-test-suite.md) | 单元测试、AI 测试与冒烟分层说明 |
| [演示脚本](testing/demo-script.md) | 验收演示 curl 流程 |

## 相关脚本

| 脚本 | 说明 |
|------|------|
| `scripts/start-all.ps1` | 启动全部后端服务（委托 `backend/scripts/start-all.ps1`） |
| `scripts/start-frontend.ps1` | 启动 Web 开发管理台（日志在根目录 `logs/`） |
| `scripts/stop-all.ps1` | 停止后台后端与前端进程 |
| `backend/scripts/set-local-env.ps1` | 设置本地 JWT、数据库、服务 URI 等环境变量 |
| `backend/scripts/start-service.ps1` | 启动单个服务：`user` / `activity` / `social` / `im` / `admin` / `gateway` / `ai` |
| `backend/test-scripts/backend-smoke.ps1` | 全链路冒烟测试 |

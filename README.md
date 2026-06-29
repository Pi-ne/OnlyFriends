# OnlyFriends

社交活动平台，包含微信小程序、Web 开发管理台与 Spring Boot 微服务后端。

## 技术栈

| 层级 | 技术 |
|------|------|
| 小程序 | 微信原生小程序 |
| 后端 | Spring Boot 3、Spring Cloud Gateway、MyBatis-Plus |
| 数据 | MySQL 8、Redis 7 |
| 可选 | Nacos、MinIO、FastAPI（AI） |

## 快速启动

```powershell
# 1. 启动基础设施
cd backend
docker compose up -d mysql redis

# 2. 初始化数据库（首次）
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
cd ..

# 3. 后台启动全部后端服务
.\scripts\start-all.ps1 -Background

# 4. 启动 Web 开发管理台（可选）
.\scripts\start-frontend.ps1
```

| 入口 | 地址 |
|------|------|
| API 网关 | http://localhost:8080 |
| Web 开发管理台 | http://127.0.0.1:5173 |
| 默认管理员 | `admin` / `Admin123456` |

详细步骤见 [本地开发指南](docs/getting-started/local-setup.md)。

## 项目结构

```text
OnlyFriends/
├── backend/                 # Maven 多模块后端
│   ├── onlyfriends-gateway/
│   ├── onlyfriends-user-service/
│   ├── onlyfriends-activity-service/
│   ├── onlyfriends-social-service/
│   ├── onlyfriends-im-service/
│   ├── onlyfriends-admin-service/
│   ├── onlyfriends-ai-service/
│   ├── onlyfriends-common/
│   ├── sql/                 # 数据库脚本
│   └── scripts/             # 后端启动脚本
├── frontend/
│   ├── miniprogram/         # 微信小程序
│   └── server.js            # Web 开发管理台
├── docs/                    # 项目文档（统一入口）
└── scripts/                 # 根目录启动脚本
```

## 微服务一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | 统一 API 入口、JWT 鉴权、路由转发 |
| User | 8081 | 注册登录、用户资料、商家入驻 |
| Activity | 8082 | 活动全生命周期、通知、AI 代理 |
| Social | 8083 | 关注、好友、兴趣小队 |
| IM | 8084 | 私聊/群聊、WebSocket |
| Admin | 8085 | 后台审核与管理 |
| AI | 8001 | AI 策划/审核/图片分类（可选） |

各服务详情见 [服务文档索引](docs/services/README.md)。

## 文档导航

完整文档索引：[docs/README.md](docs/README.md)

| 分类 | 文档 |
|------|------|
| 入门 | [环境要求](docs/getting-started/prerequisites.md) · [本地开发](docs/getting-started/local-setup.md) · [数据库初始化](docs/getting-started/database-init.md) |
| 架构 | [系统设计 v3](docs/architecture/system-design-v3.md) |
| API | [中文 API 指南](docs/api/overview-zh.md) · [网关路由](docs/api/gateway-routes.md) |
| 前端 | [小程序教程](docs/frontend/miniprogram-guide.md) · [开发管理台](docs/frontend/dev-console.md) |
| 测试 | [冒烟测试](docs/testing/smoke-tests.md) · [演示脚本](docs/testing/demo-script.md) |
| 产品 | [用户故事](docs/product/user-stories.md) · [验收标准](docs/product/acceptance-criteria.md) |

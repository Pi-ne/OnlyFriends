# 环境要求

## 必需

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 运行 Spring Boot 3 微服务 |
| Maven | 3.8+ | 编译后端 |
| Docker Desktop | 最新稳定版 | MySQL、Redis 容器 |
| PowerShell | 5.1+ | 运行启动脚本（Windows） |

## 可选

| 工具 | 版本 | 用途 |
|------|------|------|
| Node.js | 18+ | Web 开发管理台（`frontend/server.js`） |
| 微信开发者工具 | 最新版 | 小程序开发与调试 |
| Python | 3.10+ | FastAPI AI 服务（替代 Java Mock） |
| MySQL 客户端 | 任意 | 手动执行 SQL |

## 验证安装

```powershell
java -version
mvn -version
docker version
docker compose version
node -v          # 可选
```

## PowerShell 执行策略

若脚本被阻止执行，在项目根目录运行一次：

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

## 端口占用

本地开发默认占用以下端口，启动前请确认未被占用：

| 端口 | 服务 |
|------|------|
| 3306 | MySQL |
| 6379 | Redis |
| 5173 | Web 开发管理台 |
| 8001 | AI Service |
| 8080 | API Gateway |
| 8081–8085 | 各业务微服务 |

可选基础设施（`docker compose --profile infra`）：

| 端口 | 服务 |
|------|------|
| 8848, 9848 | Nacos |
| 9000, 9001 | MinIO |

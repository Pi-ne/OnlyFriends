# 本地开发指南

本文档覆盖从克隆仓库到前后端联调的完整流程。所有命令默认在**项目根目录**执行。

## 1. 启动基础设施

```powershell
cd backend
docker compose up -d mysql redis
docker compose ps
cd ..
```

如需 Nacos 与 MinIO：

```powershell
cd backend
docker compose --profile infra up -d
cd ..
```

默认端口：MySQL `3306`、Redis `6379`。

## 2. 初始化数据库

首次运行或需要重置数据时执行：

```powershell
cd backend
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
cd ..
```

导入演示数据（可选）：

```powershell
cd backend
Get-Content .\sql\demo-data.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
cd ..
```

详见 [数据库初始化](database-init.md)。

## 3. 配置环境变量

`start-all.ps1` 会自动调用 `set-local-env.ps1`。手动启动单服务时，在 PowerShell 中执行：

```powershell
cd backend
. .\scripts\set-local-env.ps1
$env:JWT_SECRET = "replace-with-at-least-32-bytes-random-secret"
cd ..
```

所有微服务必须使用**相同的** `JWT_SECRET`。

## 4. 启动后端

### 推荐：后台一键启动

```powershell
.\scripts\start-all.ps1 -Background
```

含 AI 独立服务（可选，日常开发默认由 Activity 内置 Mock 代理，无需 `-WithAi`）：

```powershell
.\scripts\start-all.ps1 -WithAi -Background
```

`start-all.ps1` 会自动调用 `set-local-env.ps1`。该脚本会设置 JWT、数据库连接、服务 URI 等环境变量，并启动 Redis 与 MySQL 容器（MySQL 使用 `--force-recreate`，会重建容器实例；数据卷默认保留）。

**端口冲突自动回退**（由 `set-local-env.ps1` 处理）：

- MySQL `3306` 被占用 → 自动使用 `3307`（需确保 `docker-compose` 映射一致或手动调整）
- IM `8084` 被占用 → 自动使用 `18084`，网关通过 `IM_SERVICE_URI` / `IM_SERVICE_WS_URI` 同步转发

后台日志与 PID 文件目录：`backend/logs/`。

### 前台分窗口启动（便于看日志）

```powershell
.\scripts\start-all.ps1
.\scripts\start-all.ps1 -WithAi
```

启动顺序与端口：

| 服务 | 地址 |
|------|------|
| User Service | http://localhost:8081 |
| Activity Service | http://localhost:8082 |
| Social Service | http://localhost:8083 |
| IM Service | http://localhost:8084 |
| Admin Service | http://localhost:8085 |
| API Gateway | http://localhost:8080 |
| AI Service | http://localhost:8001（仅 `-WithAi`） |

### 单服务启动

```powershell
cd backend
. .\scripts\set-local-env.ps1
.\scripts\start-service.ps1 gateway   # user / activity / social / im / admin / ai
```

### IDE 启动

在 IDE 中运行各模块的 `*Application` 主类，激活 `dev` profile。需先设置环境变量并确保依赖服务已启动。

## 5. 启动 Web 开发管理台

另开终端：

```powershell
.\scripts\start-frontend.ps1
.\scripts\start-frontend.ps1 -Background   # 后台
.\scripts\start-frontend.ps1 -Restart      # 端口占用时重启
```

访问：http://127.0.0.1:5173

后台模式日志与 PID 文件目录：仓库根目录 `logs/`（与后端 `backend/logs/` 分开）。

## 6. 启动微信小程序

1. 打开微信开发者工具
2. 导入目录：`frontend/onlyfriends-miniprogram`
3. 确认 `frontend/onlyfriends-miniprogram/config/index.js` 中 `dev.apiBase` 为 `http://localhost:8080/api/v1`（也可通过 gitignored 的 `config/local.js` 覆盖）
4. 开发阶段在开发者工具中关闭域名校验

详见 [小程序使用教程](../frontend/miniprogram-guide.md)。

## 7. 联调测试流程

### Web 开发管理台

1. 注册并激活用户（激活 token 见 user-service 日志）
2. 登录，Token 自动保存
3. 依次测试活动、AI、社交、IM 模块
4. 管理员登录（`admin` / `Admin123456`），测试审核与封禁

### Swagger

各服务独立文档：

```text
http://localhost:8081/swagger-ui/index.html   # User
http://localhost:8082/swagger-ui/index.html   # Activity
http://localhost:8083/swagger-ui/index.html   # Social
http://localhost:8084/swagger-ui/index.html   # IM
http://localhost:8085/swagger-ui/index.html   # Admin
http://localhost:8001/swagger-ui/index.html   # AI（需 -WithAi）
```

对外统一入口：`http://localhost:8080/api/v1/...`

## 8. 常用检查

```powershell
docker ps
Invoke-WebRequest http://localhost:8080 -UseBasicParsing
Invoke-WebRequest http://127.0.0.1:5173 -UseBasicParsing
```

重新编译：

```powershell
cd backend
mvn -DskipTests package
cd ..
```

运行测试：

```powershell
cd backend
mvn test
cd ..
```

## 9. 停止服务

```powershell
.\scripts\stop-all.ps1          # 停止后台后端与前端
cd backend
docker compose down             # 停止 MySQL / Redis
docker compose down -v          # 同时删除数据卷
cd ..
```

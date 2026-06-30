# Activity Service 测试脚本

本目录用于活动服务接口冒烟测试。

## 前置条件

1. 已启动 MySQL / Redis：

```powershell
cd backend
docker compose up -d mysql redis
```

2. 已初始化数据库（推荐）：

```powershell
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
```

3. 已启动用户服务与活动服务，且两者使用相同 `JWT_SECRET`（通过 `set-local-env.ps1`）。

4. 已通过用户服务注册、激活、登录拿到 `accessToken`。

## 使用方式

```powershell
cd backend
.\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "登录得到的accessToken"
```

可选指定地址：

```powershell
.\test-scripts\activity-service\smoke-activity-service.ps1 -BaseUrl "http://localhost:8082/api/v1" -AccessToken "登录得到的accessToken"
```

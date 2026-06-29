# Activity Service Test Scripts

本目录用于活动服务第一阶段接口冒烟测试。

## 前置条件

1. 已启动 MySQL / Redis：

```powershell
docker compose up -d mysql redis
```

2. 已初始化用户库和活动库：

```powershell
Get-Content .\sql\user-service-schema.sql | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password
Get-Content .\sql\activity-service-schema.sql | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password
```

3. 已启动用户服务和活动服务，且两者使用相同 `JWT_SECRET`。

4. 已通过用户服务注册、激活、登录拿到 `accessToken`。

## 使用方式

```powershell
.\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "登录得到的accessToken"
```

可选指定地址：

```powershell
.\test-scripts\activity-service\smoke-activity-service.ps1 -BaseUrl "http://localhost:8082/api/v1" -AccessToken "登录得到的accessToken"
```

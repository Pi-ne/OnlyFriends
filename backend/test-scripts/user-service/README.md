# User Service 测试脚本

本目录用于用户服务接口冒烟测试，独立于 Java 单元测试。

## 前置条件

1. 已初始化数据库（推荐统一脚本）：

```powershell
cd backend
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
```

2. 已启动用户服务（端口 8081）：

```powershell
cd backend
. .\scripts\set-local-env.ps1
.\scripts\start-service.ps1 user
```

或在 IDE 中运行 `com.onlyfriends.user.UserServiceApplication`（需先加载环境变量）。

3. 如未配置 SMTP，注册接口会在服务日志输出激活链接。复制链接中的 `token`，传给脚本的 `-ActivationToken` 参数。

## 使用方式

先注册并观察服务日志：

```powershell
cd backend
.\test-scripts\user-service\smoke-user-service.ps1
```

拿到激活 token 后完整跑通：

```powershell
.\test-scripts\user-service\smoke-user-service.ps1 -ActivationToken "日志中的token"
```

可选参数：

```powershell
.\test-scripts\user-service\smoke-user-service.ps1 -BaseUrl "http://localhost:8081/api/v1" -Email "dev@example.com" -Password "Abc123456" -Nickname "OnlyFriends测试用户"
```

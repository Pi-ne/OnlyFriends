# User Service Test Scripts

本目录用于用户服务接口冒烟测试，独立于 Java 单元测试。

## 前置条件

1. 已初始化 MySQL：

```powershell
mysql -u root -p < sql/user-service-schema.sql
```

2. 已使用 Java 17+ 启动用户服务：

```powershell
mvn -DskipTests package
& "C:\Program Files\Java\jdk-22\bin\java.exe" -jar onlyfriends-user-service\target\onlyfriends-user-service-1.0.0-SNAPSHOT.jar
```

也可以在 IDE 中直接运行 `com.onlyfriends.user.UserServiceApplication`。

3. 如未配置 SMTP，注册接口会在服务日志输出激活链接。复制链接中的 `token`，传给脚本的 `-ActivationToken` 参数。

## 使用方式

先注册并观察服务日志：

```powershell
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

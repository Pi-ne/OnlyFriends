# 后端本地开发运行指南

本文档用于帮助同学拉取项目后启动后端基础环境，并运行当前已完成的用户服务。

## 环境要求

- JDK 17 或更高版本。Spring Boot 3 不支持 Java 8。
- Maven 3.8+。
- Docker Desktop 或 Docker Engine。
- MySQL 客户端可选，用于手动执行 SQL。

检查命令：

```powershell
java -version
mvn -version
docker version
docker compose version
```

## 启动基础环境

默认启动 MySQL 8.0 和 Redis 7：

```powershell
docker compose up -d mysql redis
```

查看状态：

```powershell
docker compose ps
```

如需要同时启动 Nacos 和 MinIO：

```powershell
docker compose --profile infra up -d
```

默认端口：

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Nacos: `http://localhost:8848`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

本地默认密码只用于开发，可通过环境变量覆盖：

```powershell
$env:MYSQL_ROOT_PASSWORD="your_local_mysql_password"
$env:MINIO_ROOT_USER="your_minio_user"
$env:MINIO_ROOT_PASSWORD="your_minio_password"
docker compose up -d mysql redis
```

## 初始化数据库

用户模块 SQL 位于：

```text
sql/user-service-schema.sql
```

使用 Docker 内 MySQL 初始化：

```powershell
Get-Content .\sql\user-service-schema.sql | docker exec -i ququ-mysql mysql -uroot -pququ_root_password
```

或使用本机 MySQL 客户端：

```powershell
mysql -h 127.0.0.1 -P 3306 -uroot -p < sql/user-service-schema.sql
```

该脚本会创建/重建 `ququ_user` 数据库中的用户模块表：

- `user`
- `merchant_info`
- `merchant_apply`
- `user_ban_record`

活动模块 SQL 位于：

```text
sql/activity-service-schema.sql
```

初始化活动库：

```powershell
Get-Content .\sql\activity-service-schema.sql | docker exec -i ququ-mysql mysql -uroot -pququ_root_password
```

该脚本会创建/重建 `ququ_activity` 数据库中的第一阶段活动表：

- `activity`
- `activity_template`
- `activity_review_record`

## 配置用户服务

开发配置文件：

```text
ququ-user-service/src/main/resources/application-dev.yml
```

该文件不包含真实密钥。建议至少覆盖 JWT 密钥：

```powershell
$env:JWT_SECRET="replace-with-at-least-32-bytes-random-secret"
```

如使用默认 Compose 配置，可直接使用：

```powershell
$env:USER_DB_USERNAME="root"
$env:USER_DB_PASSWORD="ququ_root_password"
$env:USER_REDIS_ENABLED="false"
```

开启 Redis Refresh Token 存储：

```powershell
$env:USER_REDIS_ENABLED="true"
```

## 启动用户服务

方式一：IDE 运行 `com.ququ.user.UserServiceApplication`，激活 `dev` profile。

方式二：命令行打包后运行。注意使用 Java 17+：

```powershell
mvn -DskipTests package
& "C:\Program Files\Java\jdk-22\bin\java.exe" -jar ququ-user-service\target\ququ-user-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

服务启动后监听：

```text
http://localhost:8081
```

如果未配置 SMTP，注册成功后激活链接会输出到用户服务日志中。

## 启动活动服务

活动服务需要用户服务提供 `/internal/users/{id}/valid` 校验创建者状态。启动前请确认用户服务已运行，且两个服务使用相同 `JWT_SECRET`。

```powershell
mvn -DskipTests package
& "C:\Program Files\Java\jdk-22\bin\java.exe" -jar ququ-activity-service\target\ququ-activity-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

服务启动后监听：

```text
http://localhost:8082
```

## 接口测试

已提供 PowerShell 冒烟测试脚本：

```text
test-scripts/user-service/smoke-user-service.ps1
```

先注册并从服务日志复制激活 token：

```powershell
.\test-scripts\user-service\smoke-user-service.ps1
```

拿到 token 后完整测试注册、激活、登录、获取当前用户信息、修改资料、商家申请：

```powershell
.\test-scripts\user-service\smoke-user-service.ps1 -ActivationToken "日志中的token"
```

活动服务第一阶段冒烟测试脚本：

```powershell
.\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "用户登录得到的accessToken"
```

也可手动调用示例：

```powershell
$base = "http://localhost:8081/api/v1"

Invoke-RestMethod -Method Post -Uri "$base/auth/register" -ContentType "application/json" -Body '{"email":"dev@example.com","password":"Abc123456","nickname":"趣聚测试用户"}'

Invoke-RestMethod -Method Get -Uri "$base/auth/activate?token=日志中的token"

$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType "application/json" -Body '{"email":"dev@example.com","password":"Abc123456"}'
$token = $login.data.accessToken
$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Method Get -Uri "$base/users/me/profile" -Headers $headers

Invoke-RestMethod -Method Put -Uri "$base/users/me/profile" -Headers $headers -ContentType "application/json" -Body '{"gender":1,"bio":"热爱户外运动","interestTags":["徒步","篮球"]}'

Invoke-RestMethod -Method Post -Uri "$base/merchant/apply" -Headers $headers -ContentType "application/json" -Body '{"merchantName":"趣聚测试商家","licenseUrl":"https://example.com/license.jpg","focusTags":["运动","户外"]}'
```

## 停止环境

```powershell
docker compose down
```

如需删除本地数据卷：

```powershell
docker compose down -v
```

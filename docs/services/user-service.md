# User Service

用户认证、资料管理、商家入驻申请。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-user-service` |
| 端口 | **8081** |
| 主类 | `com.onlyfriends.user.UserServiceApplication` |
| 数据库 | `onlyfriends_user` |
| Swagger | http://localhost:8081/swagger-ui/index.html |

## 职责

- 用户注册、邮箱激活、登录、Token 刷新
- 用户资料查询与修改、头像上传
- 商家入驻申请与资质上传
- 为其他服务提供用户校验内部接口

## 公开 API

前缀：`/api/v1`

### 认证 `/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 注册 |
| GET | `/auth/activate` | 邮箱激活 |
| POST | `/auth/login` | 登录 |
| POST | `/auth/refresh` | 刷新 Token |

### 用户 `/users`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users/me/profile` | 当前用户资料 |
| PUT | `/users/me/profile` | 更新资料 |
| POST | `/users/me/avatar` | 上传头像 |
| GET | `/users/{id}` | 查看用户公开信息 |

### 商家 `/merchant`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/merchant/apply` | 提交入驻申请 |
| GET | `/merchant/apply/status` | 申请状态 |
| GET | `/merchant/me` | 商家信息 |
| POST | `/merchant/license` | 上传营业执照 |

## 内部 API

前缀：`/internal`（不经过网关）

| 路径 | 说明 |
|------|------|
| `/internal/users/batch` | 批量查询用户 |
| `/internal/users/{id}/valid` | 校验用户有效性 |
| `/internal/users/{id}/credit` | 信用分查询 |
| `/internal/admin/users/**` | 管理端用户操作（供 Admin 服务调用） |

## 环境变量

| 变量 | 说明 |
|------|------|
| `USER_DB_URL` | 数据库连接串 |
| `USER_DB_USERNAME` / `USER_DB_PASSWORD` | 数据库凭据 |
| `JWT_SECRET` | JWT 密钥 |
| `USER_REDIS_ENABLED` | 是否用 Redis 存储 Refresh Token |
| `REDIS_HOST` / `REDIS_PORT` | Redis 连接 |
| `MAIL_HOST` 等 | SMTP 邮件（未配置时激活链接输出到日志） |
| `STORAGE_TYPE` | `local` 或 `minio` |

## 依赖

- MySQL（必需）
- Redis（可选，Refresh Token 持久化）
- MinIO（可选，文件存储）

## 数据库表

`user`、`merchant_info`、`merchant_apply`、`user_ban_record` 等。初始化见 `backend/sql/user-service-schema.sql` 或统一脚本 `init-all.sql`。

## 测试

```powershell
cd backend
.\test-scripts\user-service\smoke-user-service.ps1
.\test-scripts\user-service\smoke-user-service.ps1 -ActivationToken "<token>"
```

## 相关文档

- [API 功能介绍（中文）](../api/overview-zh.md)
- [数据库初始化](../getting-started/database-init.md)

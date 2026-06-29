# Admin Service

后台管理：用户封禁、商家审核、活动审核、小队管理。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-admin-service` |
| 端口 | **8085** |
| 主类 | `com.onlyfriends.admin.AdminServiceApplication` |
| 数据库 | `onlyfriends_admin` |
| Swagger | http://localhost:8085/swagger-ui/index.html |

## 职责

- 管理员登录与密码修改
- 用户列表、封禁 / 解封
- 商家入驻审核
- 活动待审列表、审核、下架、恢复
- 兴趣小队列表、禁用、恢复

## 公开 API

前缀：`/api/v1/admin`

### 认证 `/admin/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/auth/login` | 管理员登录 |
| POST | `/admin/auth/password` | 修改密码 |

### 管理 `/admin`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/users` | 用户列表 |
| GET | `/admin/users/{id}` | 用户详情 |
| POST | `/admin/users/{id}/ban` | 封禁 |
| POST | `/admin/users/{id}/unban` | 解封 |
| GET | `/admin/merchant-applies` | 商家申请列表 |
| GET | `/admin/merchant-applies/{id}` | 申请详情 |
| PUT | `/admin/merchant-applies/{id}/review` | 审核商家 |
| GET | `/admin/activities/pending` | 待审活动 |
| GET | `/admin/activities` | 活动列表 |
| GET | `/admin/activities/{id}` | 活动详情 |
| PUT | `/admin/activities/{id}/review` | 审核活动 |
| POST | `/admin/activities/{id}/offline` | 下架 |
| POST | `/admin/activities/{id}/restore` | 恢复 |
| GET | `/admin/teams` | 小队列表 |
| GET | `/admin/teams/{id}` | 小队详情 |
| GET | `/admin/teams/{id}/members` | 成员列表 |
| POST | `/admin/teams/{id}/disable` | 禁用 |
| POST | `/admin/teams/{id}/restore` | 恢复 |

## 默认账号

数据库初始化后可用：

```text
username: admin
password: Admin123456
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `ADMIN_DB_URL` | 管理库连接 |
| `JWT_SECRET` | JWT 密钥 |
| `USER_SERVICE_BASE_URL` | 调用用户内部接口 |
| `ACTIVITY_SERVICE_BASE_URL` | 调用活动内部接口 |
| `SOCIAL_SERVICE_BASE_URL` | 调用社交内部接口 |

## 依赖

- MySQL（`onlyfriends_admin`，存储管理员账号）
- User / Activity / Social 服务内部 API（Feign 调用）

## 相关文档

- [数据库初始化](../getting-started/database-init.md)
- [Web 开发管理台](../frontend/dev-console.md)

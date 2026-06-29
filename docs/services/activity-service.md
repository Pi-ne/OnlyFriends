# Activity Service

活动全生命周期管理、通知、AI 能力代理入口。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-activity-service` |
| 端口 | **8082** |
| 主类 | `com.onlyfriends.activity.ActivityServiceApplication` |
| 数据库 | `onlyfriends_activity` |
| Swagger | http://localhost:8082/swagger-ui/index.html |

## 职责

- 活动创建、编辑、提交审核、克隆、列表与详情
- 报名、候补、签到（二维码）、评价、活动总结
- 活动模板与标签、图片上传
- 站内通知
- AI 策划 / 内容审核 / 图片分类（代理到 AI 服务或本地 Mock）

## 公开 API

前缀：`/api/v1`

### 活动 `/activities`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/activities` | 创建活动 |
| PUT | `/activities/{id}` | 更新活动 |
| POST | `/activities/{id}/submit` | 提交审核 |
| POST | `/activities/{id}/clone` | 克隆活动 |
| GET | `/activities` | 活动列表 |
| GET | `/activities/nearby` | 附近活动 |
| GET | `/activities/registered` | 已报名活动 |
| GET | `/activities/{id}` | 活动详情 |
| GET | `/activities/templates` | 活动模板 |
| GET | `/activities/tags` | 活动标签 |
| POST | `/activities/{id}/register` | 报名 |
| DELETE | `/activities/{id}/register` | 取消报名 |
| GET | `/activities/{id}/registration/me` | 我的报名状态 |
| GET | `/activities/{id}/registrations` | 报名列表（发起人） |
| POST | `/activities/images` | 上传活动图片 |
| GET | `/activities/{id}/checkin/qrcode` | 签到二维码 |
| POST | `/activities/{id}/checkin` | 签到 |
| POST | `/activities/{id}/summary` | 提交总结 |
| GET | `/activities/{id}/summary` | 查看总结 |
| POST | `/activities/{id}/comments` | 发表评论 |
| GET | `/activities/{id}/comments` | 评论列表 |

### 通知 `/notifications`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notifications` | 通知列表 |
| PUT | `/notifications/{id}/read` | 标记已读 |

### AI `/ai`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/plan-activity` | AI 活动策划 |
| POST | `/ai/classify-images` | 图片分类 |
| POST | `/ai/review-content` | 内容审核 |

## 内部 API

| 路径 | 说明 |
|------|------|
| `/internal/admin/activities/**` | 管理端活动审核（供 Admin 服务调用） |

## 环境变量

| 变量 | 说明 |
|------|------|
| `ACTIVITY_DB_URL` | 数据库连接 |
| `JWT_SECRET` | JWT 密钥 |
| `USER_SERVICE_BASE_URL` | 用户服务地址（校验创建者） |
| `SOCIAL_SERVICE_BASE_URL` | 社交服务地址 |
| `AI_MODE` | `local`（内置 Mock）或 `remote` |
| `AI_SERVICE_URL` | 远程 AI 服务地址（默认 `http://localhost:8001`） |
| `CHECKIN_SECRET` | 签到二维码签名密钥 |
| `APP_REDIS_ENABLED` | 是否启用 Redis 缓存 |
| `STORAGE_TYPE` | 文件存储类型 |

## 依赖

- MySQL（必需）
- User Service（创建活动时校验用户）
- Social Service（部分场景）
- AI Service（`AI_MODE=remote` 时）
- Redis（可选，AI 审核缓存等）

## 测试

```powershell
cd backend
.\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "<token>"
```

## 相关文档

- [AI Service](ai-service.md)
- [API 功能介绍（中文）](../api/overview-zh.md)

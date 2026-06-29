# Social Service

关注、好友关系、兴趣小队（团队）管理。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-social-service` |
| 端口 | **8083** |
| 主类 | `com.onlyfriends.social.SocialServiceApplication` |
| 数据库 | `onlyfriends_social` |
| Swagger | http://localhost:8083/swagger-ui/index.html |

## 职责

- 用户关注 / 取关、关注列表与粉丝列表
- 好友申请、审批、好友列表、好友设置
- 兴趣小队：创建、加入、成员管理、公告、相册、文件、积分、投票

## 公开 API

前缀：`/api/v1`

### 关注 `/follows`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/follows/{userId}` | 关注 |
| DELETE | `/follows/{userId}` | 取消关注 |
| GET | `/follows/following` | 关注列表 |
| GET | `/follows/followers` | 粉丝列表 |

### 好友 `/friends`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/friends/{userId}/applies` | 发送好友申请 |
| GET | `/friends/applies` | 申请列表 |
| PUT | `/friends/applies/{id}` | 处理申请 |
| GET | `/friends` | 好友列表 |
| PUT | `/friends/{userId}/setting` | 好友设置 |
| DELETE | `/friends/{userId}` | 删除好友 |

### 兴趣小队 `/teams`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/teams` | 创建小队 |
| GET | `/teams` | 小队列表 |
| GET | `/teams/{id}` | 小队详情 |
| POST | `/teams/{id}/join` | 申请加入 |
| GET | `/teams/{id}/join-applies` | 入队申请列表 |
| PUT | `/teams/{id}/join-applies/{applyId}` | 审批入队 |
| GET | `/teams/{id}/members` | 成员列表 |
| DELETE | `/teams/{id}/members/me` | 退出小队 |
| DELETE | `/teams/{id}/members/{userId}` | 移除成员 |
| PUT | `/teams/{id}/members/{userId}/admin` | 设管理员 |
| DELETE | `/teams/{id}/members/{userId}/admin` | 取消管理员 |
| PUT | `/teams/{id}/owner/{userId}` | 转让队长 |
| DELETE | `/teams/{id}` | 解散小队 |
| POST/GET | `/teams/{id}/announcements` | 公告 |
| POST/GET | `/teams/{id}/album` | 相册 |
| POST/GET | `/teams/{id}/files` | 文件 |
| GET | `/teams/{id}/scores` | 积分 |
| POST/GET | `/teams/{id}/votes` | 投票 |

## 内部 API

| 路径 | 说明 |
|------|------|
| `/internal/social/friends/check` | 校验好友关系 |
| `/internal/social/teams/{teamId}/members/check` | 校验成员 |
| `/internal/admin/teams/**` | 管理端小队操作 |

## 环境变量

| 变量 | 说明 |
|------|------|
| `SOCIAL_DB_URL` | 数据库连接 |
| `JWT_SECRET` | JWT 密钥 |
| `USER_SERVICE_BASE_URL` | 用户信息补全 |
| `STORAGE_TYPE` | 相册/文件存储 |

## 依赖

- MySQL（必需）
- User Service（用户信息）

## 相关文档

- [API 功能介绍（中文）](../api/overview-zh.md)
- [IM Service](im-service.md)（群聊关联小队）

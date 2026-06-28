# OnlyFriends 后端 API 功能介绍与前端测试指南

本文面向前端测试人员和联调人员，说明当前后端已完成的主要工作、API 入口、鉴权方式、核心字段和推荐测试流程。

## 1. 基本信息

### 1.1 服务入口

统一网关：

```text
http://localhost:8080
```

统一 API Base：

```text
http://localhost:8080/api/v1
```

WebSocket：

```text
ws://localhost:8080/ws/im
```

AI 服务由活动服务代理调用，前端仍访问：

```text
http://localhost:8080/api/v1/ai
```

### 1.2 启动方式

在项目根目录执行：

```powershell
.\scripts\start-all.ps1 -WithAi -Background
```

如需导入演示数据：

```powershell
cd backend
Get-Content .\sql\demo-data.sql -Encoding UTF8 | docker exec -i ququ-mysql mysql -uroot -pququ_root_password --default-character-set=utf8mb4
cd ..
```

### 1.3 Swagger 地址

```text
http://localhost:8081/swagger-ui/index.html  用户服务
http://localhost:8082/swagger-ui/index.html  活动服务
http://localhost:8083/swagger-ui/index.html  社交服务
http://localhost:8084/swagger-ui/index.html  IM 服务
http://localhost:8085/swagger-ui/index.html  管理服务
http://localhost:8001/swagger-ui/index.html  AI 服务
```

## 2. 通用约定

### 2.1 统一响应格式

所有公开 REST API 默认返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1780000000000
}
```

前端判断成功时请优先判断：

```text
code === 200
```

### 2.2 分页格式

分页接口的 `data` 通常为：

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "size": 20
}
```

### 2.3 鉴权方式

登录后请求头携带：

```http
Authorization: Bearer {accessToken}
```

公开接口不需要 Token。需要登录的接口如果无 Token 或 Token 无效，会返回未授权错误。

### 2.4 测试账号

管理员默认账号：

```text
username: admin
password: Admin123456
```

注册普通用户后，需要激活账号才能登录。开发阶段可以从数据库或用户服务日志获取激活 token，然后访问：

```text
GET /auth/activate?token={token}
```

## 3. 用户与认证 API

### 3.1 用户注册

```http
POST /auth/register
```

公开接口。

请求体：

```json
{
  "email": "user@example.com",
  "password": "Abc123456",
  "nickname": "测试用户"
}
```

规则：

- 邮箱必须合法。
- 密码 8-20 位，且同时包含字母和数字。
- 昵称 2-20 个字符。
- 注册后账号默认需要激活。

### 3.2 账号激活

```http
GET /auth/activate?token={activateToken}
```

公开接口。激活成功后用户可登录。

### 3.3 用户登录

```http
POST /auth/login
```

请求体：

```json
{
  "email": "user@example.com",
  "password": "Abc123456"
}
```

响应重点：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "accessTokenExpireAt": 1780000000000,
  "userInfo": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "测试用户"
  }
}
```

### 3.4 刷新 Token

```http
POST /auth/refresh
```

请求体：

```json
{
  "refreshToken": "..."
}
```

### 3.5 我的资料

```http
GET /users/me/profile
PUT /users/me/profile
POST /users/me/avatar
GET /users/{id}
```

`GET /users/me/profile` 需要登录，返回当前用户资料。

`PUT /users/me/profile` 请求体示例：

```json
{
  "nickname": "新昵称",
  "gender": 1,
  "birthday": "2000-01-01",
  "bio": "个人简介",
  "interestTags": ["户外", "桌游"]
}
```

`POST /users/me/avatar` 为 multipart 上传，字段名：

```text
file
```

## 4. 商家申请 API

### 4.1 提交商家申请

```http
POST /merchant/apply
```

需要登录。

用于普通用户申请成为商家。

### 4.2 查看申请状态

```http
GET /merchant/apply/status
```

需要登录。

### 4.3 查看我的商家信息

```http
GET /merchant/me
```

需要登录且已成为商家。

### 4.4 上传营业执照

```http
POST /merchant/license
```

multipart 上传，字段名：

```text
file
```

## 5. 活动 API

活动状态值当前主要包括：

| status | 含义 |
|---:|---|
| 0 | 草稿 |
| 1 | 审核中 |
| 2 | 已发布 |
| 3 | 报名中 |
| 4 | 报名截止 |
| 5 | 进行中 |
| 6 | 已结束 |
| 7 | 已下架 |
| 8 | 审核驳回 |
| 9 | 需要修改 |

### 5.1 创建活动

```http
POST /activities
```

需要登录。

请求体：

```json
{
  "title": "周末飞盘活动",
  "description": "新手友好，现场分组教学。",
  "tags": ["户外", "飞盘"],
  "coverUrl": "https://example.com/cover.jpg",
  "startTime": "2026-07-20T09:00:00",
  "endTime": "2026-07-20T11:00:00",
  "regDeadline": "2026-07-19T18:00:00",
  "locationName": "滨江公园东门",
  "locationLat": 31.2304,
  "locationLng": 121.4737,
  "locationDetail": "东门集合",
  "maxParticipants": 20,
  "fee": 0,
  "locationVerify": 0,
  "locationRadius": 200,
  "templateId": null,
  "teamId": null,
  "isDraft": false
}
```

说明：

- `isDraft=true` 创建草稿，状态为 0。
- `isDraft=false` 直接进入审核流程，状态为 1 或由 AI/审核逻辑后续改变。
- 前端不要在 `isDraft=false` 后再额外调用 `/activities/{id}/submit`。

### 5.2 修改草稿

```http
PUT /activities/{id}
```

需要登录且必须是活动创建者。仅草稿可修改。

### 5.3 提交草稿审核

```http
POST /activities/{id}/submit
```

需要登录且必须是活动创建者。仅草稿可提交。

### 5.4 克隆活动

```http
POST /activities/{id}/clone
```

需要登录。克隆结果为新草稿。

### 5.5 活动列表

```http
GET /activities
```

公开接口。

常用查询参数：

| 参数 | 说明 |
|---|---|
| tab | recommend/latest/nearby |
| keyword | 关键字 |
| tags | 标签，字符串 |
| status | 活动状态 |
| startDate | 开始日期 |
| endDate | 结束日期 |
| city | 城市 |
| locationName | 地点 |
| creatorId | 创建者 ID |
| teamId | 小队 ID |
| registered | 是否查询已报名 |
| minFee/maxFee | 费用区间 |
| minParticipants/maxParticipants | 人数区间 |
| page/size | 分页 |

### 5.6 附近活动

```http
GET /activities/nearby?lat=31.2304&lng=121.4737&radius=5000&page=1&size=20
```

需要登录。根据经纬度和半径查询附近活动。

### 5.7 我报名的活动

```http
GET /activities/registered?page=1&size=20
```

需要登录。

### 5.8 活动详情

```http
GET /activities/{id}
```

公开接口。

响应重点字段：

```json
{
  "activityId": 1,
  "creatorId": 1,
  "creatorNickname": "发起人",
  "title": "活动标题",
  "description": "活动说明",
  "tags": [],
  "coverUrl": "",
  "startTime": "2026-07-20T09:00:00",
  "endTime": "2026-07-20T11:00:00",
  "regDeadline": "2026-07-19T18:00:00",
  "locationName": "地点",
  "locationLat": 31.2304,
  "locationLng": 121.4737,
  "maxParticipants": 20,
  "currentCount": 1,
  "fee": 0,
  "status": 2,
  "statusText": "已发布"
}
```

### 5.9 活动模板和标签

```http
GET /activities/templates
GET /activities/tags?category=outdoor&limit=10
```

模板接口需要登录。标签接口可用于前端筛选和创建活动表单。

### 5.10 报名、取消报名、报名状态

```http
POST /activities/{id}/register
DELETE /activities/{id}/register
GET /activities/{id}/registration/me
GET /activities/{id}/registrations
```

均需要登录。

`GET /activities/{id}/registrations` 用于创建者查看报名成员。

报名状态响应重点：

```json
{
  "activityId": 1,
  "userId": 2,
  "registrationStatus": 1,
  "registrationStatusText": "registered",
  "waitlistStatus": null,
  "queueNo": null,
  "currentCount": 1,
  "maxParticipants": 20
}
```

### 5.11 上传活动图片

```http
POST /activities/images
```

需要登录，multipart 上传，字段名：

```text
file
```

### 5.12 签到

```http
GET /activities/{id}/checkin/qrcode
POST /activities/{id}/checkin
```

二维码接口需要活动创建者权限。

签到请求体：

```json
{
  "qrcodeContent": "二维码内容",
  "lat": 31.2304,
  "lng": 121.4737
}
```

### 5.13 总结和评价

```http
POST /activities/{id}/summary
GET /activities/{id}/summary
POST /activities/{id}/comments
GET /activities/{id}/comments?page=1&size=10
```

总结请求体：

```json
{
  "title": "活动总结",
  "content": "总结内容",
  "imageUrls": []
}
```

评价请求体：

```json
{
  "rating": 5,
  "content": "体验很好"
}
```

## 6. 通知 API

```http
GET /notifications?page=1&size=20&unreadOnly=false
PUT /notifications/{id}/read
```

需要登录。

说明：

- `/activities/notifications` 也存在兼容入口。
- 推荐前端使用独立的 `/notifications`。

响应字段：

```json
{
  "notificationId": 1,
  "type": "ACTIVITY_REVIEW",
  "title": "通知标题",
  "content": "通知内容",
  "relatedType": "activity",
  "relatedId": 1,
  "read": false,
  "createdAt": "2026-06-28T12:00:00"
}
```

## 7. AI API

### 7.1 AI 活动策划

```http
POST /ai/plan-activity
```

请求体：

```json
{
  "theme": "周末户外",
  "locationName": "滨江公园",
  "startTime": "2026-07-20T09:00:00",
  "durationHours": 2,
  "maxParticipants": 20,
  "preferences": ["新手友好", "轻社交"]
}
```

### 7.2 AI 图片分类

```http
POST /ai/classify-images
```

### 7.3 AI 内容审核

```http
POST /ai/review-content
```

说明：前端通常只直接使用活动策划接口；内容审核更多由活动创建/提交流程触发。

## 8. 关注与好友 API

### 8.1 关注

```http
POST /follows/{userId}
DELETE /follows/{userId}
GET /follows/following
GET /follows/followers
```

均需要登录。

### 8.2 好友申请

```http
POST /friends/{userId}/applies
GET /friends/applies?type=received
PUT /friends/applies/{id}
GET /friends
PUT /friends/{userId}/setting
DELETE /friends/{userId}
```

发起好友申请请求体：

```json
{
  "message": "希望添加好友"
}
```

处理好友申请请求体：

```json
{
  "action": 1,
  "reason": "同意"
}
```

说明：

- `action=1` 通常表示同意。
- 其他拒绝值请以前端实际联调结果和后端 `ReviewRequest` 处理为准。

好友备注/分组请求体：

```json
{
  "remark": "备注名",
  "groupName": "同学"
}
```

## 9. 小队/兴趣组 API

### 9.1 创建小队

```http
POST /teams
```

需要登录。

请求体：

```json
{
  "name": "周末运动小队",
  "description": "固定组织户外活动",
  "tags": ["户外", "运动"],
  "joinType": 0,
  "maxMembers": 30
}
```

说明：

- `joinType=0` 直接加入。
- `joinType=1` 需要审核。

### 9.2 小队列表与详情

```http
GET /teams?keyword=户外
GET /teams?joined=true
GET /teams/{id}
GET /teams/{id}/members
```

### 9.3 加入、退出、解散

```http
POST /teams/{id}/join
GET /teams/{id}/join-applies
PUT /teams/{id}/join-applies/{applyId}
DELETE /teams/{id}/members/me
DELETE /teams/{id}
```

加入请求体：

```json
{
  "message": "申请加入小队"
}
```

### 9.4 成员管理

```http
DELETE /teams/{id}/members/{userId}
PUT /teams/{id}/members/{userId}/admin
DELETE /teams/{id}/members/{userId}/admin
PUT /teams/{id}/owner/{userId}
```

需要队长或管理员权限。

### 9.5 公告、相册、文件、积分、投票

```http
POST /teams/{id}/announcements
GET /teams/{id}/announcements
POST /teams/{id}/album
POST /teams/{id}/album/upload
GET /teams/{id}/album
POST /teams/{id}/files
POST /teams/{id}/files/upload
GET /teams/{id}/files
GET /teams/{id}/scores
POST /teams/{id}/votes
GET /teams/{id}/votes
POST /teams/{id}/votes/{voteId}/records
```

公告请求体：

```json
{
  "title": "公告标题",
  "content": "公告内容"
}
```

相册请求体：

```json
{
  "imageUrl": "https://example.com/image.jpg",
  "description": "照片说明"
}
```

文件请求体：

```json
{
  "fileName": "资料.pdf",
  "fileUrl": "https://example.com/file.pdf",
  "fileSize": 1024
}
```

投票创建请求体请参考 Swagger，以后端 DTO 为准。

## 10. IM 即时通讯 API

### 10.1 私聊与群聊 REST

```http
POST /im/messages/private
POST /im/messages/group
GET /im/conversations
GET /im/messages/{convId}?page=1&size=30
GET /im/groups/{teamId}/messages?page=1&size=30
POST /im/messages/{msgId}/recall
POST /im/conversations/{convId}/read
```

私聊请求体：

```json
{
  "receiverId": 2,
  "msgType": 1,
  "content": "你好"
}
```

群聊请求体：

```json
{
  "teamId": 1,
  "msgType": 1,
  "content": "大家好",
  "mentionAll": false,
  "mentionUserIds": [],
  "relatedType": null,
  "relatedId": null
}
```

标记已读请求体：

```json
{
  "lastReadMsgId": 100
}
```

### 10.2 在线状态

```http
GET /im/users/{userId}/online
POST /im/online/heartbeat
```

### 10.3 WebSocket

连接地址：

```text
ws://localhost:8080/ws/im?token={accessToken}
```

STOMP 目的地：

```text
/app/chat.private
/app/chat.group
/app/chat.recall
/user/{userId}/queue/messages
/topic/team/{teamId}
```

说明：

- REST 接口可用于普通页面功能验证。
- WebSocket 用于实时消息验证，建议在两名用户登录后测试私聊和群聊实时接收。

## 11. 管理员 API

管理员登录：

```http
POST /admin/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "Admin123456"
}
```

响应重点：

```json
{
  "accessToken": "...",
  "expiresIn": 7200,
  "adminId": 1,
  "username": "admin",
  "nickname": "管理员"
}
```

修改管理员密码：

```http
POST /admin/auth/password
```

请求体：

```json
{
  "oldPassword": "Admin123456",
  "newPassword": "NewPass123"
}
```

### 11.1 用户管理

```http
GET /admin/users?page=1&size=20&keyword=abc
GET /admin/users/{id}
POST /admin/users/{id}/ban
POST /admin/users/{id}/unban
```

封禁请求体：

```json
{
  "reason": "违规测试",
  "banExpireAt": "2026-07-20T00:00:00"
}
```

当前管理 API 未公开：

- 删除用户
- 重置普通用户密码

如果前端测试需要这两个功能，需要后端新增接口后再联调。

### 11.2 商家申请管理

```http
GET /admin/merchant-applies?page=1&size=20&status=0
GET /admin/merchant-applies/{id}
PUT /admin/merchant-applies/{id}/review
```

审核请求体：

```json
{
  "action": 1,
  "comment": "审核通过"
}
```

商家申请 action：

| action | 含义 |
|---:|---|
| 1 | 通过 |
| 2 | 驳回 |

### 11.3 活动管理

```http
GET /admin/activities/pending?page=1&size=20
GET /admin/activities?page=1&size=20&status=1&keyword=户外
GET /admin/activities/{id}
PUT /admin/activities/{id}/review
POST /admin/activities/{id}/offline
POST /admin/activities/{id}/restore
```

活动审核请求体：

```json
{
  "action": 0,
  "comment": "审核通过"
}
```

活动审核 action：

| action | 含义 |
|---:|---|
| 0 | 通过 |
| 1 | 驳回 |
| 2 | 要求修改 |

下架请求体：

```json
{
  "reason": "测试下架"
}
```

### 11.4 小队管理

```http
GET /admin/teams?page=1&size=20&status=1&keyword=户外
GET /admin/teams/{id}
GET /admin/teams/{id}/members
POST /admin/teams/{id}/disable
POST /admin/teams/{id}/restore
```

停用请求体：

```json
{
  "reason": "测试停用"
}
```

## 12. 推荐前端测试流程

### 12.1 最小主流程

1. 注册普通用户 A。
2. 激活用户 A。
3. 登录用户 A，保存 accessToken。
4. 用户 A 创建活动，`isDraft=false`。
5. 管理员登录。
6. 管理员查询待审核活动。
7. 管理员审核通过活动。
8. 注册并激活用户 B。
9. 用户 B 登录。
10. 用户 B 查看活动列表和详情。
11. 用户 B 报名活动。
12. 用户 B 查看“我报名的活动”。
13. 用户 B 取消报名。

### 12.2 社交和小队流程

1. 用户 A 创建小队。
2. 用户 B 搜索小队。
3. 用户 B 加入小队。
4. 用户 A 查看成员。
5. 用户 A 发布公告。
6. 用户 A 创建投票。
7. 用户 B 参与投票。
8. 管理员停用小队。
9. 管理员恢复小队。

### 12.3 IM 流程

1. 用户 A 和用户 B 登录。
2. 用户 A 发送私聊消息给用户 B。
3. 用户 B 查看会话列表。
4. 用户 B 查看消息历史。
5. 用户 A 和 B 加入同一小队。
6. 用户 A 发送小队群聊消息。
7. 用户 B 查看群聊历史。
8. 可选：连接 WebSocket 验证实时收发。

### 12.4 管理台流程

1. 打开 `http://127.0.0.1:5173`。
2. 登录管理员。
3. 在“概览”确认接口可访问。
4. 在“测试造数”登录普通用户并保存 Token。
5. 创建测试活动和兴趣组。
6. 在“活动管理”审核、下架、恢复。
7. 在“用户管理”封禁、解封。
8. 在“兴趣组/小队”停用、恢复。

## 13. 已知限制

1. 注册后需要激活账号，开发阶段需要从数据库或日志获取激活 token。
2. 管理员 API 当前没有公开删除用户和重置普通用户密码接口。
3. 部分状态文本如果出现乱码，请以前端收到的数值状态字段为准。
4. 上传接口依赖本地文件存储或对象存储配置，测试前请确认后端文件存储配置正常。
5. WebSocket 实时通信需要前端实现 STOMP 协议或对应封装，REST 消息接口可先验证消息数据闭环。

## 14. 快速健康检查

检查网关：

```powershell
Invoke-WebRequest "http://localhost:8080/api/v1/activities?page=1&size=1" -UseBasicParsing
```

运行后端 smoke：

```powershell
.\backend\test-scripts\backend-smoke.ps1 -BaseUrl "http://localhost:8080"
```

仅校验 smoke 脚本参数：

```powershell
.\backend\test-scripts\backend-smoke.ps1 -ValidateOnly
```

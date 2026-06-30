# Demo Script / 演示脚本

面向课程验收或项目演示的 curl 流程。统一 API 入口：

```text
BASE=http://localhost:8080/api/v1
```

**推荐准备方式**：在项目根目录执行 `.\scripts\start-all.ps1 -WithAi -Background`（已含 `set-local-env.ps1`）。下文亦给出手动环境变量方式。

预期响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

Replace placeholders such as `<activationTokenA>`, `<activityId>`, `<userBId>`, `<applyId>`, `<convId>`, and `<msgId>` with values returned by earlier steps.

## 1. Environment Preparation / 环境准备

在 `backend/` 目录启动基础设施：

```powershell
cd backend
docker compose up -d mysql redis
```

如需完整平台演示（Nacos、MinIO 可选）：

```powershell
docker compose --profile infra up -d
```

当前已实现后端**至少需要** MySQL 与 Redis。Nacos、MinIO 可启动以模拟完整平台，但本 curl 流程不依赖服务发现与对象存储上传。

初始化全部数据库：

```powershell
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
```

设置环境变量（或返回项目根目录执行 `..\scripts\start-all.ps1 -WithAi -Background` 自动设置）：

```powershell
. .\scripts\set-local-env.ps1
```

手动设置时示例：

```powershell
$env:JWT_SECRET="replace-with-at-least-32-bytes-dev-secret"
$env:USER_DB_USERNAME="root"
$env:USER_DB_PASSWORD="onlyfriends_root_password"
$env:ACTIVITY_DB_USERNAME="root"
$env:ACTIVITY_DB_PASSWORD="onlyfriends_root_password"
$env:SOCIAL_DB_USERNAME="root"
$env:SOCIAL_DB_PASSWORD="onlyfriends_root_password"
$env:IM_DB_USERNAME="root"
$env:IM_DB_PASSWORD="onlyfriends_root_password"
$env:ADMIN_DB_USERNAME="root"
$env:ADMIN_DB_PASSWORD="onlyfriends_root_password"
$env:AI_MODE="local"
```

在 `backend/` 目录编译并分终端启动（或使用根目录 `..\scripts\start-all.ps1`）：

```powershell
mvn install -DskipTests

mvn -f onlyfriends-user-service/pom.xml spring-boot:run
mvn -f onlyfriends-activity-service/pom.xml spring-boot:run
mvn -f onlyfriends-social-service/pom.xml spring-boot:run
mvn -f onlyfriends-im-service/pom.xml spring-boot:run
mvn -f onlyfriends-admin-service/pom.xml spring-boot:run
mvn -f onlyfriends-ai-service/pom.xml spring-boot:run
mvn -f onlyfriends-gateway/pom.xml spring-boot:run
```

Service ports:

| Service | Port |
| --- | ---: |
| gateway | 8080 |
| user-service | 8081 |
| activity-service | 8082 |
| social-service | 8083 |
| im-service | 8084 |
| admin-service | 8085 |
| ai-service | 8001 |

Optional health smoke checks:

```bash
curl "http://localhost:8080/api/v1/activities?page=1&size=1"
curl "http://localhost:8081/v3/api-docs"
```

Expected: `code=200` for activity list and OpenAPI JSON from the service.

## 2. User Module Demo

Set base URL:

```bash
BASE=http://localhost:8080
```

Register user A:

```bash
curl -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo.a@example.com","password":"Abc123456","nickname":"demoA"}'
```

Expected: `code=200`, `data.userId` is returned. Save it as `USER_A_ID`.

Activation email is currently mock-friendly: if SMTP is not configured, user-service logs the activation link. You can also read `activate_token` from local MySQL for demo use.

Get activation token from database:

```powershell
docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4 -N -e "SELECT activate_token FROM onlyfriends_user.user WHERE email='demo.a@example.com';"
```

Activate user A:

```bash
curl "$BASE/api/v1/auth/activate?token=<activationTokenA>"
```

Expected: `code=200`. User A status becomes active.

Login user A:

```bash
curl -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo.a@example.com","password":"Abc123456"}'
```

Expected: `code=200`, `data.accessToken`, `data.refreshToken`, and `data.userInfo.userId`.

Save token:

```bash
USER_A_TOKEN=<accessTokenA>
USER_A_ID=<userIdA>
```

Get current user profile:

```bash
curl "$BASE/api/v1/users/me/profile" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `code=200`, profile contains `email`, `nickname`, `status`, and `creditScore`.

Update profile:

```bash
curl -X PUT "$BASE/api/v1/users/me/profile" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"gender":1,"birthday":"2002-06-01","bio":"Outdoor and board game fan","interestTags":["hiking","boardgame"]}'
```

Expected: `code=200`. A later profile query shows updated fields.

Submit merchant application:

```bash
curl -X POST "$BASE/api/v1/merchant/apply" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"merchantName":"Demo Outdoor Club","licenseUrl":"https://example.com/license-a.jpg","focusTags":["outdoor","sports"]}'
```

Expected: `code=200`, `data.applyId` is returned. Save it as `MERCHANT_APPLY_ID`.

Check merchant application status:

```bash
curl "$BASE/api/v1/merchant/apply/status" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `code=200`, status is pending before admin review.

## 3. Activity Module Demo

Create activity as user A:

```bash
curl -X POST "$BASE/api/v1/activities" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Weekend Hiking Demo","description":"A light city hiking route for new friends.","tags":["hiking","social"],"coverUrl":"https://example.com/hiking.jpg","startTime":"2026-07-20T09:00:00","endTime":"2026-07-20T12:00:00","regDeadline":"2026-07-19T18:00:00","locationName":"City Park","locationLat":31.2304000,"locationLng":121.4737000,"locationDetail":"Meet at east gate","maxParticipants":20,"fee":0,"locationVerify":0,"locationRadius":500,"isDraft":true}'
```

Expected: `code=200`, `data.activityId`, `data.status`, and `data.statusText`. Save `ACTIVITY_ID`.

Submit activity review:

```bash
curl -X POST "$BASE/api/v1/activities/<activityId>/submit" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected:

- If AI mock returns `pass`, confidence is at least `0.7`, and `maxParticipants <= 50`, activity is auto published.
- If AI mock returns `risk` or `maxParticipants > 50`, activity enters manual review.
- If AI mock returns `reject` with high confidence, activity is rejected.
- If AI call fails or times out, activity enters manual review.

Demonstrate direct AI planning through gateway:

```bash
curl -X POST "$BASE/api/v1/ai/plan-activity" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"theme":"frisbee","locationName":"City Park","durationHours":2,"maxParticipants":16,"preferences":["beginner friendly"]}'
```

Expected: `code=200`, generated title, description, tags, safety notes, and agenda.

Demonstrate direct AI review through gateway:

```bash
curl -X POST "$BASE/api/v1/ai/review-content" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"activityId":<activityId>,"title":"Weekend Hiking Demo","description":"A light city hiking route.","tags":["hiking"],"maxParticipants":20}'
```

Expected: normal content returns `data.result=pass`, `confidence` around `0.93`.

Query activity list:

```bash
curl "$BASE/api/v1/activities?page=1&size=20"
```

Expected: `code=200`, paged list includes created/published activities depending on status filters.

Query activity detail:

```bash
curl "$BASE/api/v1/activities/<activityId>"
```

Expected: `code=200`, detail includes creator id, creator nickname/avatar if user-service is available, activity schedule, location, status, and participant count.

## 4. Registration Flow Demo

Register user B:

```bash
curl -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo.b@example.com","password":"Abc123456","nickname":"demoB"}'
```

Activate user B:

```powershell
docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4 -N -e "SELECT activate_token FROM onlyfriends_user.user WHERE email='demo.b@example.com';"
```

```bash
curl "$BASE/api/v1/auth/activate?token=<activationTokenB>"
```

Login user B:

```bash
curl -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo.b@example.com","password":"Abc123456"}'
```

Expected: `code=200`. Save:

```bash
USER_B_TOKEN=<accessTokenB>
USER_B_ID=<userIdB>
```

User B registers for activity:

```bash
curl -X POST "$BASE/api/v1/activities/<activityId>/register" \
  -H "Authorization: Bearer $USER_B_TOKEN"
```

Expected: `code=200`, `registrationStatus` indicates registered, `currentCount` increases.

Query user B registration status:

```bash
curl "$BASE/api/v1/activities/<activityId>/registration/me" \
  -H "Authorization: Bearer $USER_B_TOKEN"
```

Expected: `code=200`, status reflects registered or waitlist state.

Cancel registration:

```bash
curl -X DELETE "$BASE/api/v1/activities/<activityId>/register" \
  -H "Authorization: Bearer $USER_B_TOKEN"
```

Expected: `code=200`, `registrationStatus` becomes cancelled and `currentCount` is released.

Waiting queue demo:

1. Create another activity with `maxParticipants=1`.
2. Let user B register first.
3. Register another activated user C for the same activity.

Expected: the second user cannot occupy a normal quota and receives waitlist fields such as `waitlistStatus` and `queueNo`, if the activity is published and registration rules are satisfied.

Creator checks registrations:

```bash
curl "$BASE/api/v1/activities/<activityId>/registrations" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `code=200`, list includes registered users with nickname/avatar from user-service.

## 5. Social Module Demo

User A follows user B:

```bash
curl -X POST "$BASE/api/v1/follows/$USER_B_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `code=200`.

Query following list:

```bash
curl "$BASE/api/v1/follows/following" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: user B appears in the list.

User A sends friend application to user B:

```bash
curl -X POST "$BASE/api/v1/friends/$USER_B_ID/applies" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Let us join activities together."}'
```

Expected: `code=200`, `data.applyId`. Save `FRIEND_APPLY_ID`.

User B checks received applications:

```bash
curl "$BASE/api/v1/friends/applies?type=received" \
  -H "Authorization: Bearer $USER_B_TOKEN"
```

Expected: pending application from user A is shown.

User B approves:

```bash
curl -X PUT "$BASE/api/v1/friends/applies/<friendApplyId>" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":1,"reason":"approved"}'
```

Expected: `code=200`. A friend relation is created.

Query friend list:

```bash
curl "$BASE/api/v1/friends" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: user B appears with nickname/avatar fields.

User A creates an interest team:

```bash
curl -X POST "$BASE/api/v1/teams" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Weekend Demo Team","description":"A team for demo activities.","tags":["outdoor","social"],"joinType":0,"maxMembers":20}'
```

Expected: `code=200`, `data.teamId`. Save `TEAM_ID`.

User B joins the team:

```bash
curl -X POST "$BASE/api/v1/teams/<teamId>/join" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"I want to join."}'
```

Expected:

- For `joinType=0`, returns `applyId=0` and user B becomes a member directly.
- For `joinType=1`, returns a pending application id, and owner must approve through `/api/v1/teams/{id}/join-applies/{applyId}`.

Query team members:

```bash
curl "$BASE/api/v1/teams/<teamId>/members" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: owner user A and member user B are listed with basic user info.

## 6. IM Module Demo

Private chat requires the friendship created in the previous section.

User A sends private message to user B:

```bash
curl -X POST "$BASE/api/v1/im/messages/private" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiverId":'$USER_B_ID',"msgType":1,"content":"Hello from user A."}'
```

Expected: `code=200`, `data.msgId`, `data.convId`, `data.content`, and `data.mine=true`. Save `PRIVATE_MSG_ID` and `PRIVATE_CONV_ID`.

Query conversation list:

```bash
curl "$BASE/api/v1/im/conversations" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: conversation with user B appears, with `lastMsgPreview`.

Query history messages:

```bash
curl "$BASE/api/v1/im/messages/<convId>?page=1&size=30" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: message list includes the private message, sender nickname/avatar when user-service is available.

Recall message within 2 minutes:

```bash
curl -X POST "$BASE/api/v1/im/messages/<msgId>/recall" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `code=200`. Query history again and the message status becomes recalled.

Mark conversation as read:

```bash
curl -X POST "$BASE/api/v1/im/conversations/<convId>/read" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"lastReadMsgId":<msgId>}'
```

Expected: `code=200`, unread count is updated for later conversation queries.

Team group chat requires team membership:

```bash
curl -X POST "$BASE/api/v1/im/messages/group" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"teamId":<teamId>,"msgType":1,"content":"Welcome to the demo team."}'
```

Expected: `code=200`, group conversation and message are created for the team.

WebSocket route note:

```text
ws://localhost:8080/ws/im/**
```

The gateway route is reserved for IM WebSocket proxying. The current curl demo uses REST IM endpoints.

## 7. Admin Management Demo

Default admin account is inserted by `sql/init-all.sql`:

```text
username: admin
password: Admin123456
```

Admin login:

```bash
curl -X POST "$BASE/api/v1/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123456"}'
```

Expected: `code=200`, `data.accessToken`, `data.adminId`, `data.username`. Save:

```bash
ADMIN_TOKEN=<adminAccessToken>
ADMIN_ID=<adminId>
```

Review merchant application:

```bash
curl "$BASE/api/v1/admin/merchant-applies?page=1&size=20&status=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: pending application list includes `MERCHANT_APPLY_ID`.

Approve merchant application:

```bash
curl -X PUT "$BASE/api/v1/admin/merchant-applies/<merchantApplyId>/review" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":1,"comment":"documents verified"}'
```

Expected: `code=200`. User A becomes merchant. Query `/api/v1/merchant/me` with user A token to verify.

Query users:

```bash
curl "$BASE/api/v1/admin/users?page=1&size=20&email=demo&status=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: active demo users are returned.

Ban user B:

```bash
curl -X POST "$BASE/api/v1/admin/users/$USER_B_ID/ban" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"demo ban test","banExpireAt":"2026-08-01T00:00:00"}'
```

Expected: `code=200`. User B status becomes banned.

Unban user B:

```bash
curl -X POST "$BASE/api/v1/admin/users/$USER_B_ID/unban" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: `code=200`. User B status returns to active.

Query pending activities:

```bash
curl "$BASE/api/v1/admin/activities/pending?page=1&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: activities requiring manual review are returned.

Manual review activity:

```bash
curl -X PUT "$BASE/api/v1/admin/activities/<activityId>/review" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":0,"comment":"approved by admin"}'
```

Expected: `code=200`, activity becomes approved/published according to service rules. `action=1` rejects, `action=2` asks for modification.

Offline activity:

```bash
curl -X POST "$BASE/api/v1/admin/activities/<activityId>/offline" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"demo offline test"}'
```

Expected: `code=200`, activity status becomes offline.

Restore activity:

```bash
curl -X POST "$BASE/api/v1/admin/activities/<activityId>/restore" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: `code=200`, activity returns to published/restored state.

Admin team management:

```bash
curl "$BASE/api/v1/admin/teams?page=1&size=20&status=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl "$BASE/api/v1/admin/teams/<teamId>/members" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST "$BASE/api/v1/admin/teams/<teamId>/disable" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"demo disable test"}'
```

Expected: team list and members are returned; disable action records an admin operation and changes team status.

## 8. Mock And Demo Notes

- AI: current AI abilities are mock/rule based. Activity submit calls AI review. `/api/v1/ai/**` is exposed by activity-service through the gateway. Direct ai-service endpoints are also available at `http://localhost:8001/ai/**`.
- Email: activation email is mock-friendly. Without SMTP config, user-service logs the activation link. For demo, reading `activate_token` from MySQL is acceptable.
- File upload: avatar upload is local filesystem based under `USER_UPLOAD_DIR` / `data/uploads`; MinIO can be started but is not required by the current implemented avatar flow.
- Nacos: can be started for platform completeness, but current local routes use explicit service URLs from gateway config.
- WebSocket: gateway has `/ws/im/**` route to im-service. The current demonstrable IM flow uses REST endpoints.
- Internal APIs: `/internal/**` endpoints are service-to-service boundaries and are not part of the public demo script.

## 9. Suggested Demonstration Order

1. Show infrastructure and service ports.
2. Register, activate, and login user A.
3. Create and submit an activity, then show AI review behavior.
4. Register and login user B, then register/cancel activity.
5. Follow, friend, create team, and join team.
6. Send private IM and group IM messages, query conversations/history, recall message.
7. Login admin, approve merchant application, query users, ban/unban, review/offline activity.
8. End by showing Swagger/OpenAPI pages and `docs/api-test-guide.md` for reproducible testing.

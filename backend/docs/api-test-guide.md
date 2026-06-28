# API Test Guide

This guide covers the implemented backend APIs for user, activity, social, IM, admin, and AI mock modules.

## 1. Environment

Start infrastructure:

```powershell
docker compose up -d mysql redis
```

Initialize databases as needed:

```powershell
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i ququ-mysql mysql -uroot -pququ_root_password --default-character-set=utf8mb4
```

See `docs/database-init.md` for the full table list and notes about historical module SQL files.

Use the same JWT secret for all services:

```powershell
$env:JWT_SECRET="replace-with-at-least-32-bytes-dev-secret"
```

Recommended service ports:

| Service | Port | Base |
| --- | ---: | --- |
| Gateway | 8080 | `http://localhost:8080` |
| user-service | 8081 | `http://localhost:8081` |
| activity-service | 8082 | `http://localhost:8082` |
| social-service | 8083 | `http://localhost:8083` |
| im-service | 8084 | `http://localhost:8084` |
| admin-service | 8085 | `http://localhost:8085` |
| ai-service | 8001 | `http://localhost:8001` |

## 2. Swagger / OpenAPI

SpringDoc OpenAPI is enabled for servlet services. Swagger UI addresses:

| Service | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| user-service | `http://localhost:8081/swagger-ui/index.html` | `http://localhost:8081/v3/api-docs` |
| activity-service | `http://localhost:8082/swagger-ui/index.html` | `http://localhost:8082/v3/api-docs` |
| social-service | `http://localhost:8083/swagger-ui/index.html` | `http://localhost:8083/v3/api-docs` |
| im-service | `http://localhost:8084/swagger-ui/index.html` | `http://localhost:8084/v3/api-docs` |
| admin-service | `http://localhost:8085/swagger-ui/index.html` | `http://localhost:8085/v3/api-docs` |
| ai-service | `http://localhost:8001/swagger-ui/index.html` | `http://localhost:8001/v3/api-docs` |

Gateway is the unified business entry:

```text
http://localhost:8080
```

## 3. Tokens

User protected APIs require:

```http
Authorization: Bearer <userAccessToken>
```

Admin protected APIs require:

```http
Authorization: Bearer <adminAccessToken>
```

Public APIs include user register, activate, login, refresh, activity list/detail, and Swagger paths.

## 4. User Flow

Register, public:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"u1@example.com","password":"Abc123456","nickname":"user1"}'
```

Activate, public. Copy `activateToken` from user-service logs or database:

```bash
curl "http://localhost:8080/api/v1/auth/activate?token=<activateToken>"
```

Login, public:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"u1@example.com","password":"Abc123456"}'
```

Save token:

```bash
USER_TOKEN=<accessToken>
```

Get current user, user token:

```bash
curl http://localhost:8080/api/v1/users/me/profile \
  -H "Authorization: Bearer $USER_TOKEN"
```

Update profile, user token:

```bash
curl -X PUT http://localhost:8080/api/v1/users/me/profile \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"gender":1,"bio":"喜欢户外活动","interestTags":["徒步","飞盘"]}'
```

Merchant apply, user token:

```bash
curl -X POST http://localhost:8080/api/v1/merchant/apply \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"merchantName":"趣聚测试商家","licenseUrl":"https://example.com/license.jpg","focusTags":["户外","运动"]}'
```

## 5. Activity Flow

Create activity and submit AI review immediately, user token:

```bash
curl -X POST http://localhost:8080/api/v1/activities \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"周末徒步","description":"轻松城市路线","tags":["徒步","社交"],"startTime":"2026-07-20T09:00:00","endTime":"2026-07-20T12:00:00","regDeadline":"2026-07-19T18:00:00","locationName":"城市公园","locationLat":31.2304,"locationLng":121.4737,"locationDetail":"东门集合","maxParticipants":20,"fee":0,"isDraft":false}'
```

Create draft, user token:

```bash
curl -X POST http://localhost:8080/api/v1/activities \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"飞盘体验","description":"新手友好","tags":["飞盘"],"startTime":"2026-07-21T09:00:00","endTime":"2026-07-21T11:00:00","regDeadline":"2026-07-20T18:00:00","locationName":"滨江草坪","locationLat":31.2304,"locationLng":121.4737,"maxParticipants":16,"fee":0,"isDraft":true}'
```

Submit draft for review, user token:

```bash
curl -X POST http://localhost:8080/api/v1/activities/<activityId>/submit \
  -H "Authorization: Bearer $USER_TOKEN"
```

Activity list, public:

```bash
curl "http://localhost:8080/api/v1/activities?page=1&size=20"
```

Activity detail, public:

```bash
curl http://localhost:8080/api/v1/activities/<activityId>
```

Templates, user token:

```bash
curl http://localhost:8080/api/v1/activities/templates \
  -H "Authorization: Bearer $USER_TOKEN"
```

## 6. Registration Flow

Register activity, user token:

```bash
curl -X POST http://localhost:8080/api/v1/activities/<activityId>/register \
  -H "Authorization: Bearer $USER_TOKEN"
```

My registration status, user token:

```bash
curl http://localhost:8080/api/v1/activities/<activityId>/registration/me \
  -H "Authorization: Bearer $USER_TOKEN"
```

Cancel registration, user token:

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/<activityId>/register \
  -H "Authorization: Bearer $USER_TOKEN"
```

Activity registrations, creator user token:

```bash
curl http://localhost:8080/api/v1/activities/<activityId>/registrations \
  -H "Authorization: Bearer $USER_TOKEN"
```

## 7. Social Flow

Follow user, user token:

```bash
curl -X POST http://localhost:8080/api/v1/follows/<targetUserId> \
  -H "Authorization: Bearer $USER_TOKEN"
```

Following list, user token:

```bash
curl http://localhost:8080/api/v1/follows/following \
  -H "Authorization: Bearer $USER_TOKEN"
```

Friend apply, user token:

```bash
curl -X POST http://localhost:8080/api/v1/friends/<targetUserId>/applies \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"一起参加活动"}'
```

Review friend apply, target user token:

```bash
curl -X PUT http://localhost:8080/api/v1/friends/applies/<applyId> \
  -H "Authorization: Bearer $TARGET_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":1}'
```

Friend list, user token:

```bash
curl http://localhost:8080/api/v1/friends \
  -H "Authorization: Bearer $USER_TOKEN"
```

Create team, user token:

```bash
curl -X POST http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"周末小队","description":"一起参加活动","tags":["户外","社交"],"joinType":0,"maxMembers":20}'
```

Team list, user token:

```bash
curl "http://localhost:8080/api/v1/teams?page=1&size=20" \
  -H "Authorization: Bearer $USER_TOKEN"
```

Team members, user token:

```bash
curl http://localhost:8080/api/v1/teams/<teamId>/members \
  -H "Authorization: Bearer $USER_TOKEN"
```

## 8. IM Flow

Private message requires friendship. User token:

```bash
curl -X POST http://localhost:8080/api/v1/im/messages/private \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiverId":<friendUserId>,"msgType":1,"content":"hello"}'
```

Group message requires team membership. User token:

```bash
curl -X POST http://localhost:8080/api/v1/im/messages/group \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"teamId":<teamId>,"msgType":1,"content":"大家好"}'
```

Conversation list, user token:

```bash
curl http://localhost:8080/api/v1/im/conversations \
  -H "Authorization: Bearer $USER_TOKEN"
```

History messages, user token:

```bash
curl "http://localhost:8080/api/v1/im/messages/<convId>?page=1&size=30" \
  -H "Authorization: Bearer $USER_TOKEN"
```

Recall message within 2 minutes, user token:

```bash
curl -X POST http://localhost:8080/api/v1/im/messages/<msgId>/recall \
  -H "Authorization: Bearer $USER_TOKEN"
```

Mark read, user token:

```bash
curl -X POST http://localhost:8080/api/v1/im/conversations/<convId>/read \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"lastReadMsgId":<msgId>}'
```

## 9. Admin Flow

Initialize admin database with `sql/admin-service-schema.sql`.

Default admin:

```text
username: admin
password: Admin123456
```

Admin login, public:

```bash
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123456"}'
```

Save token:

```bash
ADMIN_TOKEN=<adminAccessToken>
```

User list, admin token:

```bash
curl "http://localhost:8080/api/v1/admin/users?page=1&size=20&email=u1&userType=0&status=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Ban user, admin token:

```bash
curl -X POST http://localhost:8080/api/v1/admin/users/<userId>/ban \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"违规发布内容","banExpireAt":"2026-08-01T00:00:00"}'
```

Merchant apply list/detail, admin token:

```bash
curl "http://localhost:8080/api/v1/admin/merchant-applies?page=1&size=20&status=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl http://localhost:8080/api/v1/admin/merchant-applies/<applyId> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Review merchant apply, admin token. `action=1` approve, `action=2` reject:

```bash
curl -X PUT http://localhost:8080/api/v1/admin/merchant-applies/<applyId>/review \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":1,"comment":"资料真实，通过"}'
```

Pending activity list and review, admin token. `action=0` pass, `action=1` reject, `action=2` needs modify:

```bash
curl "http://localhost:8080/api/v1/admin/activities/pending?page=1&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X PUT http://localhost:8080/api/v1/admin/activities/<activityId>/review \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":0,"comment":"审核通过"}'
```

Offline and restore activity, admin token:

```bash
curl -X POST http://localhost:8080/api/v1/admin/activities/<activityId>/offline \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"活动内容违规"}'

curl -X POST http://localhost:8080/api/v1/admin/activities/<activityId>/restore \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Team management, admin token:

```bash
curl "http://localhost:8080/api/v1/admin/teams?page=1&size=20&status=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl http://localhost:8080/api/v1/admin/teams/<teamId>/members \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST http://localhost:8080/api/v1/admin/teams/<teamId>/disable \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"小队存在违规组织行为"}'
```

## 10. AI Mock Flow

Through gateway, activity-service local/remote AI client:

```bash
curl -X POST http://localhost:8080/api/v1/ai/plan-activity \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"theme":"飞盘","locationName":"上海徐汇滨江","durationHours":2,"maxParticipants":16}'
```

Content review through gateway, user token:

```bash
curl -X POST http://localhost:8080/api/v1/ai/review-content \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"夜间极限挑战","description":"high intensity night activity","tags":["夜间"],"maxParticipants":20}'
```

Image classification through gateway, user token:

```bash
curl -X POST http://localhost:8080/api/v1/ai/classify-images \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"activityId":1,"imageUrls":["https://x/group-photo.jpg","https://x/venue-site.jpg","https://x/material-kit.jpg"]}'
```

Direct AI mock service, no token:

```bash
curl -X POST http://localhost:8001/ai/review-content \
  -H "Content-Type: application/json" \
  -d '{"title":"周末徒步","description":"轻松城市路线","tags":["徒步"],"maxParticipants":20}'
```

## 11. Notes

- Gateway injects `X-User-Id`, `X-User-Type`, `X-User-Role`, and `X-Nickname` after JWT validation.
- Internal APIs under `/internal/**` are service-to-service APIs and should not be exposed publicly.
- AI mock can run locally inside activity-service with `AI_MODE=local`, or call ai-service with `AI_MODE=remote` and `AI_SERVICE_URL=http://localhost:8001`.

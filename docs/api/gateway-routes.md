# 网关路由表

API Gateway（端口 **8080**）是所有客户端的统一入口。配置位于 `onlyfriends-gateway/src/main/resources/application.yml`。

## 路由规则

| 路径前缀 | 目标服务 | 端口 |
|----------|----------|------|
| `/api/v1/auth/**` | User Service | 8081 |
| `/api/v1/users/**` | User Service | 8081 |
| `/api/v1/merchant/**` | User Service | 8081 |
| `/api/v1/activities/**` | Activity Service | 8082 |
| `/api/v1/ai/**` | Activity Service | 8082 |
| `/api/v1/notifications/**` | Activity Service | 8082 |
| `/api/v1/follows/**` | Social Service | 8083 |
| `/api/v1/friends/**` | Social Service | 8083 |
| `/api/v1/teams/**` | Social Service | 8083 |
| `/api/v1/im/**` | IM Service | 8084 |
| `/ws/im/**` | IM Service（WebSocket） | 8084 |
| `/api/v1/admin/**` | Admin Service | 8085 |

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `USER_SERVICE_URI` | `http://localhost:8081` | 用户服务地址 |
| `ACTIVITY_SERVICE_URI` | `http://localhost:8082` | 活动服务地址 |
| `SOCIAL_SERVICE_URI` | `http://localhost:8083` | 社交服务地址 |
| `IM_SERVICE_URI` | `http://localhost:8084` | IM HTTP 地址（8084 被占用时 `set-local-env.ps1` 回退为 `18084`） |
| `IM_SERVICE_WS_URI` | `ws://localhost:8084` | IM WebSocket 地址（同上） |
| `ADMIN_SERVICE_URI` | `http://localhost:8085` | 管理服务地址 |
| `JWT_SECRET` | — | 与各服务保持一致 |
| `NACOS_ENABLED` | `false` | 是否启用 Nacos 服务发现 |

## 鉴权

网关对需登录接口校验 JWT，并将用户信息通过请求头传递给下游：

- `X-User-Id`
- `X-User-Type`
- `X-User-Role`
- `X-Nickname`

客户端在 `Authorization: Bearer <accessToken>` 中携带 Token。

## CORS

开发环境允许以下来源（生产部署前需收紧）：

- `http://localhost:5173`、`http://127.0.0.1:5173`（Web 开发管理台）
- `http://localhost:3000`、`http://127.0.0.1:3000`
- `http://localhost:8080`、`http://127.0.0.1:8080`

## 内部接口

以下路径**不经过网关**，仅供服务间调用：

- `/internal/users/**` — User Service
- `/internal/admin/**` — User / Activity / Social 的内部管理接口
- `/internal/social/**` — Social Service

## 示例

```powershell
$base = "http://localhost:8080/api/v1"
$headers = @{ Authorization = "Bearer $accessToken" }

Invoke-RestMethod -Uri "$base/users/me/profile" -Headers $headers
Invoke-RestMethod -Uri "$base/activities" -Headers $headers
```

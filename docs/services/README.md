# 微服务文档索引

OnlyFriends 后端采用「适度微服务」架构：7 个可部署服务 + 1 个共享库模块。

## 服务关系

```text
                    ┌─────────────┐
  客户端 ──────────►│  Gateway    │ :8080
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
     User :8081    Activity :8082   Social :8083
           │               │               │
           │         ┌─────┴─────┐         │
           │         ▼           ▼         │
           │    AI :8001    User/Social    │
           │    (可选)      (Feign)        │
           ▼                               ▼
     Admin :8085 ◄─── Feign ───► IM :8084
```

## 文档列表

| 模块 | 目录 | 文档 |
|------|------|------|
| Gateway | `onlyfriends-gateway` | [gateway.md](gateway.md) |
| User | `onlyfriends-user-service` | [user-service.md](user-service.md) |
| Activity | `onlyfriends-activity-service` | [activity-service.md](activity-service.md) |
| Social | `onlyfriends-social-service` | [social-service.md](social-service.md) |
| IM | `onlyfriends-im-service` | [im-service.md](im-service.md) |
| Admin | `onlyfriends-admin-service` | [admin-service.md](admin-service.md) |
| AI | `onlyfriends-ai-service` | [ai-service.md](ai-service.md) |
| Common | `onlyfriends-common` | [common.md](common.md) |

## 统一约定

- API 前缀：`/api/v1`
- 响应格式：`{ "code": 0, "message": "ok", "data": ... }`
- 认证：JWT Bearer Token（`JWT_SECRET` 全服务一致）
- 数据库：每服务独立库（`onlyfriends_*`）
- Swagger：`http://localhost:{port}/swagger-ui/index.html`

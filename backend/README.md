# OnlyFriends Backend

Spring Boot 多模块微服务后端。

## 快速启动

```powershell
docker compose up -d mysql redis
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
cd ..
.\scripts\start-all.ps1 -Background
```

## 模块

| 模块 | 端口 | 文档 |
|------|------|------|
| onlyfriends-gateway | 8080 | [Gateway](../docs/services/gateway.md) |
| onlyfriends-user-service | 8081 | [User](../docs/services/user-service.md) |
| onlyfriends-activity-service | 8082 | [Activity](../docs/services/activity-service.md) |
| onlyfriends-social-service | 8083 | [Social](../docs/services/social-service.md) |
| onlyfriends-im-service | 8084 | [IM](../docs/services/im-service.md) |
| onlyfriends-admin-service | 8085 | [Admin](../docs/services/admin-service.md) |
| onlyfriends-ai-service | 8001 | [AI](../docs/services/ai-service.md) |
| onlyfriends-common | — | [Common](../docs/services/common.md) |

## 文档

- [本地开发指南](../docs/getting-started/local-setup.md)
- [数据库初始化](../docs/getting-started/database-init.md)
- [完整文档索引](../docs/README.md)

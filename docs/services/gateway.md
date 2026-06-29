# API Gateway

统一 API 入口，负责路由转发、JWT 鉴权、CORS 与 WebSocket 代理。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-gateway` |
| 端口 | **8080** |
| 主类 | `com.onlyfriends.gateway.GatewayApplication` |
| 数据库 | 无 |

## 职责

- 将 `/api/v1/**` 请求路由到对应微服务
- 校验 JWT，解析用户信息并注入下游请求头
- 代理 IM WebSocket（`/ws/im/**`）
- 开发环境 CORS 配置

## 主要配置

文件：`onlyfriends-gateway/src/main/resources/application.yml`

| 环境变量 | 说明 |
|----------|------|
| `JWT_SECRET` | JWT 签名密钥（与各服务一致） |
| `USER_SERVICE_URI` | 用户服务地址 |
| `ACTIVITY_SERVICE_URI` | 活动服务地址 |
| `SOCIAL_SERVICE_URI` | 社交服务地址 |
| `IM_SERVICE_URI` / `IM_SERVICE_WS_URI` | IM HTTP / WebSocket |
| `ADMIN_SERVICE_URI` | 管理服务地址 |
| `NACOS_ENABLED` | 是否启用 Nacos（默认 `false`） |
| `GATEWAY_RATE_LIMIT_PERMITS` | 限流（默认关闭） |

## 路由表

详见 [网关路由文档](../api/gateway-routes.md)。

## 启动

```powershell
cd backend
. .\scripts\set-local-env.ps1
.\scripts\start-service.ps1 gateway
```

或随 `scripts/start-all.ps1` 一并启动（最后启动）。

## 相关文档

- [网关路由表](../api/gateway-routes.md)
- [本地开发指南](../getting-started/local-setup.md)

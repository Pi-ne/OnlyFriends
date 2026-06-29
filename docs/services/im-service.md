# IM Service

即时通讯：私聊、群聊、会话管理、WebSocket 实时消息。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-im-service` |
| 端口 | **8084** |
| 主类 | `com.onlyfriends.im.ImServiceApplication` |
| 数据库 | `onlyfriends_im` |
| Swagger | http://localhost:8084/swagger-ui/index.html |

## 职责

- 私聊与群聊消息发送
- 会话列表、历史消息、已读状态
- 消息撤回
- 在线状态与心跳
- WebSocket 实时推送

## 公开 API

前缀：`/api/v1/im`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/im/messages/private` | 发送私聊消息 |
| POST | `/im/messages/group` | 发送群聊消息 |
| GET | `/im/conversations` | 会话列表 |
| GET | `/im/messages/{convId}` | 会话消息历史 |
| GET | `/im/groups/{teamId}/messages` | 小队群聊历史 |
| POST | `/im/messages/{msgId}/recall` | 撤回消息 |
| POST | `/im/conversations/{convId}/read` | 标记已读 |
| GET | `/im/users/{userId}/online` | 查询在线状态 |
| POST | `/im/online/heartbeat` | 心跳 |

## WebSocket

| 项 | 值 |
|----|-----|
| 网关路径 | `ws://localhost:8080/ws/im/**` |
| 直连路径 | `ws://localhost:8084/ws/im/**` |

客户端经网关连接时，网关将 WebSocket 代理到 IM 服务。

## 环境变量

| 变量 | 说明 |
|------|------|
| `IM_DB_URL` | 数据库连接 |
| `JWT_SECRET` | JWT 密钥 |
| `SOCIAL_SERVICE_BASE_URL` | 校验群聊成员 |
| `USER_SERVICE_BASE_URL` | 用户信息 |

## 依赖

- MySQL（必需）
- Social Service（群聊成员校验）
- User Service（用户信息）

## 相关文档

- [网关路由表](../api/gateway-routes.md)（WebSocket 代理）
- [Social Service](social-service.md)

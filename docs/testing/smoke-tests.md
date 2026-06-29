# 冒烟测试

后端提供 PowerShell 冒烟脚本，用于快速验证各服务核心流程。

## 全链路冒烟

```powershell
cd backend
.\test-scripts\backend-smoke.ps1
```

覆盖注册、登录、活动、社交等主流程。详见 `backend/test-scripts/README.md`。

## 单服务冒烟

### User Service

```powershell
cd backend
.\test-scripts\user-service\smoke-user-service.ps1
.\test-scripts\user-service\smoke-user-service.ps1 -ActivationToken "<日志中的token>"
```

### Activity Service

```powershell
cd backend
.\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "<登录得到的accessToken>"
```

## 单元测试

```powershell
cd backend
mvn test
```

## 前置条件

1. MySQL、Redis 已启动
2. 数据库已执行 `init-all.sql`
3. 对应微服务已运行（全链路测试需全部服务 + 网关）

## 相关文档

- [本地开发指南](../getting-started/local-setup.md)
- [演示脚本](demo-script.md)

# 后端测试脚本

本目录保存后端接口冒烟测试 PowerShell 脚本。

## 全链路冒烟

前置条件：

- 已执行 `sql/init-all.sql`（推荐统一脚本，勿单独跑分模块 SQL）。
- 已启动 gateway、user、activity、social、im、admin 服务（`.\scripts\start-all.ps1 -Background`）。
- 所有服务使用相同 `JWT_SECRET`（`set-local-env.ps1` 已设置）。
- Docker MySQL 容器名默认为 `onlyfriends-mysql`。
- 若本机 8084 被占用，启动脚本会自动将 IM 服务切换到 `18084`。

## 一键全量测试

```powershell
cd backend
.\test-scripts\run-all-tests.ps1
```

详见 [完整测试指南](../../docs/testing/full-test-suite.md)。

## 全链路冒烟

运行：

```powershell
cd backend
.\test-scripts\backend-smoke.ps1
```

仅校验脚本语法/参数：

```powershell
.\test-scripts\backend-smoke.ps1 -ValidateOnly
```

跳过后台管理接口：

```powershell
.\test-scripts\backend-smoke.ps1 -SkipAdmin
```

## 单服务冒烟

| 服务 | 脚本 | 文档 |
|------|------|------|
| User | `test-scripts/user-service/smoke-user-service.ps1` | [README](user-service/README.md) |
| Activity | `test-scripts/activity-service/smoke-activity-service.ps1` | [README](activity-service/README.md) |
| Social | `test-scripts/social-service/smoke-social-service.ps1` | — |
| IM | `test-scripts/im-service/smoke-im-service.ps1` | — |

## 单元测试

```powershell
cd backend
mvn test
```

## 相关文档

- [冒烟测试](../../docs/testing/smoke-tests.md)
- [本地开发指南](../../docs/getting-started/local-setup.md)

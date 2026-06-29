# Backend Test Scripts

本目录保存后端接口冒烟测试脚本。

## 统一后端 smoke

前置条件：

- 已执行 `sql/init-all.sql`。
- 已启动 gateway、user、activity、social、im、admin 服务。
- 所有服务使用相同 `JWT_SECRET`。
- 如果脚本需要自动激活用户，本地 Docker MySQL 容器名默认为 `onlyfriends-mysql`。

运行：

```powershell
.\test-scripts\backend-smoke.ps1
```

只做脚本语法/参数检查：

```powershell
.\test-scripts\backend-smoke.ps1 -ValidateOnly
```

可选跳过后台接口：

```powershell
.\test-scripts\backend-smoke.ps1 -SkipAdmin
```

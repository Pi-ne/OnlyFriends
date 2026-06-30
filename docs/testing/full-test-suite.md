# OnlyFriends 完整测试指南

合并 `ququ-*` 与 `onlyfriends-*` 后，使用本指南验证各模块与整体正确性。

## 一、测试分层

| 层级 | 脚本/命令 | 依赖 | 覆盖范围 |
|------|-----------|------|----------|
| L1 单元测试 | `mvn clean test` | JDK 21、Maven | Gateway/User/Activity/Social/IM 全部 Java 测试 |
| L2 AI 测试 | `python -m pytest tests/ -v` | Python 3.10+ | AI 策划/审核/图片分类 |
| L3 单服务冒烟 | `test-scripts/*/smoke-*.ps1` | 对应服务 + MySQL | 单服务功能闭环 |
| L4 全链路冒烟 | `test-scripts/backend-smoke.ps1` | 全部服务 + 网关 | 注册→活动→社交→IM→Admin |
| L5 一键全测 | `test-scripts/run-all-tests.ps1` | 视参数而定 | L1~L4 自动串联 |

## 二、环境准备

```powershell
cd backend

# 1. 启动基础设施（若 3306/8084 被占用，脚本会自动回退到 3307/18084）
docker compose up -d mysql redis

# 2. 初始化数据库（首次）
Get-Content .\sql\init-all.sql -Encoding UTF8 |
  docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4

# 3. 编译并启动全部服务
mvn -DskipTests package
.\scripts\start-all.ps1 -Background

# 4. 等待网关就绪（约 30 秒）
Invoke-RestMethod http://localhost:8080/api/v1/activities?page=1&size=1
```

**常见端口冲突：**

- MySQL 3306 被占用 → `set-local-env.ps1` 自动使用 3307
- IM 8084 被占用（如 plasticd.exe）→ 自动使用 18084，网关同步转发

## 三、一键运行全部测试

```powershell
cd backend
.\test-scripts\run-all-tests.ps1
```

仅跑单元测试（无需启动服务）：

```powershell
.\test-scripts\run-all-tests.ps1 -UnitOnly
```

跳过集成冒烟（CI 快速门禁）：

```powershell
.\test-scripts\run-all-tests.ps1 -SkipSmoke
```

## 四、分模块测试样例

### 4.1 Gateway

| 用例 | 方法 | 路径 | 预期 |
|------|------|------|------|
| 公开活动列表 | GET | `/api/v1/activities?page=1&size=1` | `code=200` |
| 内部接口拦截 | GET | `/internal/users/1/valid` | HTTP 403/404 |

### 4.2 User Service

```powershell
# 注册（首次运行，从日志获取 activation token 后重跑）
.\test-scripts\user-service\smoke-user-service.ps1

# 完整流程（含激活 token）
.\test-scripts\user-service\smoke-user-service.ps1 `
  -Email "test@example.com" -Password "Abc123456" `
  -ActivationToken "<从日志或数据库获取>"
```

覆盖：注册、激活、登录、刷新 Token、资料更新、商家申请、内部接口。

### 4.3 Activity Service

```powershell
# 先通过 User 冒烟或 backend-smoke 获取 accessToken
.\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "<token>"
```

覆盖：模板、草稿 CRUD、提交审核、列表/详情、大人数活动人工审核。

### 4.4 Social Service

```powershell
.\test-scripts\social-service\smoke-social-service.ps1
```

覆盖：关注、好友申请审批、小队创建/加入/公告。

### 4.5 IM Service

```powershell
# 直连 IM（默认 8084；若端口冲突则为 18084）
.\test-scripts\im-service\smoke-im-service.ps1

# 通过网关
.\test-scripts\im-service\smoke-im-service.ps1 -BaseUrl "http://localhost:8080/api/v1/im"
```

覆盖：好友私聊、群聊、会话列表、已读回执。

### 4.6 AI Service

```powershell
cd onlyfriends-ai-service\python
python -m pytest tests/ -v
```

### 4.7 全链路集成

```powershell
.\test-scripts\backend-smoke.ps1
```

端到端流程：

1. 双用户注册激活登录
2. 活动创建 → 附近查询 → 报名 → 签到 → 评论
3. 关注 → 好友申请审批
4. 小队创建 → 加入 → 公告
5. IM 私聊 → 群聊 → 已读
6. Admin 登录 → 用户/小队查询

## 五、小程序 API 对照测试

小程序通过网关 `http://localhost:8080/api/v1` 访问（配置见 `frontend/onlyfriends-miniprogram/app.js` → `globalData.apiBase`）。

| 小程序 API | 后端路径 | 冒烟覆盖 |
|------------|----------|----------|
| `user.login` | POST `/auth/login` | backend-smoke |
| `user.register` | POST `/auth/register` | backend-smoke |
| `user.getProfile` | GET `/users/me/profile` | user-smoke |
| `activity.listActivities` | GET `/activities` | backend-smoke |
| `activity.createActivity` | POST `/activities` | backend-smoke |
| `activity.registerActivity` | POST `/activities/{id}/register` | backend-smoke |
| `activity.planActivity` | POST `/ai/plan-activity` | activity-smoke |
| `social.*` / `im.*` | `/teams`, `/im/*` | backend-smoke |

微信开发者工具需开启「不校验合法域名」，并将 `app.js` 中 `apiBase` 设为 `http://localhost:8080/api/v1`（或本机局域网 IP）。

## 六、验收标准对照

完整 AC 矩阵见 [acceptance-criteria.md](../product/acceptance-criteria.md)。当前自动化覆盖：

- AC-U-001~006：user-smoke + backend-smoke
- AC-A-001~012, AC-A-018, AC-A-022：activity-smoke + backend-smoke
- AC-S-001~008, AC-S-012：social-smoke + backend-smoke
- AC-I-001~004, AC-I-010：im-smoke + backend-smoke
- AC-AI-001~003：pytest

## 七、故障排查

| 现象 | 原因 | 处理 |
|------|------|------|
| `mvn test` 报 `com.ququ` 包错误 | 合并后 target 缓存 | `mvn clean test` |
| IM 返回 500 | 8084 端口被占用，IM 未启动 | 重启服务，`set-local-env.ps1` 会自动用 18084 |
| 激活 token 找不到 | MySQL 容器名不匹配 | 使用 `onlyfriends-mysql` 或传 `-MysqlContainer` |
| 网关连接重置 | 下游服务未就绪 | 检查 `backend/logs/*.out.log` |

# AI Service

AI 活动策划、内容审核、图片分类。提供 Java Mock 实现与可选 Python FastAPI 实现。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-ai-service` |
| 端口 | **8001** |
| 主类 | `com.onlyfriends.ai.AiServiceApplication` |
| 数据库 | 无 |
| Swagger | http://localhost:8001/swagger-ui/index.html |

## 职责

- 根据用户输入生成活动策划建议
- 文本内容合规审核
- 活动图片分类

Activity Service 通过 `/api/v1/ai/**` 对外暴露 AI 能力，内部根据 `AI_MODE` 决定调用本地 Mock 还是本服务。

## API

前缀：`/ai`（Activity Service 代理后为 `/api/v1/ai`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查（Python 版） |
| POST | `/ai/plan-activity` | 活动策划 |
| POST | `/ai/review-content` | 内容审核 |
| POST | `/ai/classify-images` | 图片分类 |

## 运行模式

### 模式一：Activity 内置 Mock（默认）

```powershell
$env:AI_MODE = "local"
```

无需启动 AI Service。适合日常开发与联调。

### 模式二：Java AI Service

```powershell
.\scripts\start-all.ps1 -WithAi -Background   # 或在根目录执行同命令
```

### 模式三：Python FastAPI

```powershell
cd backend/onlyfriends-ai-service/python
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001
```

Activity Service 配置：

```powershell
$env:AI_MODE = "remote"
$env:AI_SERVICE_URL = "http://localhost:8001"
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `AI_PROVIDER` | AI 提供商（默认 `mock`） |
| `AI_API_KEY` | 外部 API 密钥（接入真实 LLM 时） |
| `AI_MODEL` | 模型名称 |
| `AI_MOCK_ENABLED` | 是否启用 Mock 逻辑 |
| `JWT_SECRET` | JWT 密钥 |

## 依赖

无数据库依赖。接入真实 LLM 时需配置 `AI_API_KEY` 与对应 Provider。

## 相关文档

- [Activity Service](activity-service.md)（AI 代理入口）
- Python 实现说明：`backend/onlyfriends-ai-service/python/README.md`

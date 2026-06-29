# onlyfriends-ai-service

AI 服务，端口 **8001**。默认由 Activity Service 内置 Mock 代理，可选独立启动。

详细文档：[docs/services/ai-service.md](../../docs/services/ai-service.md)

```powershell
cd backend
. .\scripts\set-local-env.ps1
.\scripts\start-service.ps1 ai
```

Python FastAPI 实现见 [python/README.md](python/README.md)。

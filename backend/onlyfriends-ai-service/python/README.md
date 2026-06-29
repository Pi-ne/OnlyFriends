# OnlyFriends AI FastAPI Service

Python implementation of the AI service. It keeps the same response envelope and endpoint paths as the Java mock service.

## Run

```powershell
cd onlyfriends-ai-service/python
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001
```

Activity service can call it with:

```powershell
$env:AI_MODE="remote"
$env:AI_SERVICE_URL="http://localhost:8001"
```

## Endpoints

- `GET /health`
- `POST /ai/plan-activity`
- `POST /ai/review-content`
- `POST /ai/classify-images`

Current implementation uses deterministic mock/rule logic. Real LLM and vision provider integration can be added behind `AiService` without changing the HTTP contract.

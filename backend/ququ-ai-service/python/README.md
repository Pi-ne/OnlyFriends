# Ququ AI FastAPI Service

Python implementation of the AI service required by the platform architecture plan.
Provides three AI capabilities behind a unified REST API consumed by the activity service.

## Quick Start

```powershell
cd ququ-ai-service/python

# Create and activate virtual environment
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Copy and configure environment
cp .env.example .env
# Edit .env with your API keys if using live mode

# Run the service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

Activity service can call it with:
```powershell
$env:AI_MODE="remote"
$env:AI_SERVICE_URL="http://localhost:8001"
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check with provider/mode info |
| POST | `/ai/plan-activity` | AI activity plan generation |
| POST | `/ai/review-content` | Content safety review (3-layer pipeline) |
| POST | `/ai/classify-images` | Batch image classification (5 categories) |

All responses use the standard Result envelope: `{"code": 200, "message": "success", "data": {...}, "timestamp": ...}`

## Architecture

```
app/
├── main.py              # FastAPI app entry point, service wiring
├── config.py            # Configuration from environment variables
├── models.py            # Pydantic request/response models
├── services/
│   ├── rule_engine.py       # Layer 1: keyword + regex content filter
│   ├── review_service.py    # Layer 2: LLM semantic review pipeline
│   ├── plan_service.py      # AI activity plan generation
│   ├── classify_service.py  # Batch image classification
│   ├── llm_client.py        # LLM provider abstraction (OpenAI / 文心一言)
│   └── vision_client.py     # Vision model abstraction
├── routers/
│   ├── review.py        # POST /ai/review-content
│   ├── plan.py          # POST /ai/plan-activity
│   └── classify.py      # POST /ai/classify-images
└── services.py          # Backward-compatible AiService facade

tests/
├── test_ai_service.py   # Original integration tests
├── test_review.py       # Review pipeline tests
├── test_plan.py         # Plan generation tests
└── test_classify.py     # Image classification tests
```

## Running Tests

```bash
# All tests
python -m pytest tests/ -v

# Specific test files
python -m pytest tests/test_review.py -v
python -m pytest tests/test_plan.py -v
python -m pytest tests/test_classify.py -v
```

## Modes

- **mock** (default): Uses deterministic keyword rules and templates. No API keys needed. Good for development and testing.
- **live**: Calls real LLM/vision APIs. Requires `.env` configuration with valid API keys.

Set via `AI_MODE=mock` or `AI_MODE=live` in `.env`.

import time

from fastapi import FastAPI

from .models import (
    AiActivityPlanRequest,
    AiActivityPlanResponse,
    AiImageClassifyRequest,
    AiImageClassifyResponse,
    AiReviewRequest,
    AiReviewResponse,
    HealthResponse,
    Result,
)
from .services import AiService


app = FastAPI(title="Ququ AI Service", version="1.0.0")
ai_service = AiService()


def success(data):
    return Result(code=200, message="success", data=data, timestamp=int(time.time() * 1000))


@app.get("/health", response_model=Result[HealthResponse])
def health():
    settings = ai_service.settings
    return success(
        HealthResponse(
            status="UP",
            provider=settings.provider,
            model=settings.model,
            mode=settings.mode,
        )
    )


@app.post("/ai/plan-activity", response_model=Result[AiActivityPlanResponse])
def plan_activity(request: AiActivityPlanRequest | None = None):
    return success(ai_service.plan_activity(request))


@app.post("/ai/review-content", response_model=Result[AiReviewResponse])
def review_content(request: AiReviewRequest | None = None):
    return success(ai_service.review_content(request))


@app.post("/ai/classify-images", response_model=Result[AiImageClassifyResponse])
def classify_images(request: AiImageClassifyRequest | None = None):
    return success(ai_service.classify_images(request))

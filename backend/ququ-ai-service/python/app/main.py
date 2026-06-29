"""Ququ AI Service — FastAPI application entry point.

Provides three AI capabilities for the OnlyFriends platform:
1. Content safety review  (POST /ai/review-content)
2. Activity plan generation (POST /ai/plan-activity)
3. Image classification     (POST /ai/classify-images)

Usage:
    python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 [--reload]
"""

import logging
import time

from fastapi import FastAPI

from .config import AiSettings
from .models import HealthResponse, Result
from .routers.classify import create_router as create_classify_router
from .routers.plan import create_router as create_plan_router
from .routers.review import create_router as create_review_router
from .services.classify_service import ClassifyService
from .services.llm_client import LlmClient
from .services.plan_service import PlanService
from .services.review_service import ReviewService
from .services.rule_engine import RuleEngine
from .services.vision_client import VisionClient

# ------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------

settings = AiSettings()

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ------------------------------------------------------------------
# Service wiring
# ------------------------------------------------------------------

llm_client = LlmClient(settings)
rule_engine = RuleEngine()
vision_client = VisionClient(settings)

review_service = ReviewService(
    settings=settings,
    rule_engine=rule_engine,
    llm=llm_client,
    redis=None,  # Redis is optional; will be connected on startup if configured
)
plan_service = PlanService(settings=settings, llm=llm_client)
classify_service = ClassifyService(settings=settings, vision=vision_client)

# ------------------------------------------------------------------
# Application
# ------------------------------------------------------------------

app = FastAPI(title="Ququ AI Service", version="1.0.0")

# Register routers
app.include_router(create_review_router(review_service))
app.include_router(create_plan_router(plan_service))
app.include_router(create_classify_router(classify_service))

# ------------------------------------------------------------------
# Startup / Shutdown
# ------------------------------------------------------------------


@app.on_event("startup")
async def on_startup():
    """Connect Redis if a password or host override is configured."""
    if settings.redis_password or settings.redis_host != "localhost":
        try:
            import redis.asyncio as aioredis

            review_service.redis = aioredis.from_url(
                f"redis://:{settings.redis_password}@{settings.redis_host}:{settings.redis_port}/{settings.redis_db}",
                decode_responses=True,
            )
            await review_service.redis.ping()
            logger.info("Redis connected for review cache")
        except Exception as exc:
            logger.warning("Redis unavailable, review cache disabled: %s", exc)

    logger.info(
        "AI Service started (provider=%s, model=%s, mode=%s)",
        settings.provider,
        settings.model,
        settings.mode,
    )


@app.on_event("shutdown")
async def on_shutdown():
    if review_service.redis:
        await review_service.redis.close()
        logger.info("Redis disconnected")


# ------------------------------------------------------------------
# Health endpoint
# ------------------------------------------------------------------


@app.get("/health", response_model=Result[HealthResponse])
def health():
    return Result(
        code=200,
        message="success",
        data=HealthResponse(
            status="UP",
            provider=settings.provider,
            model=settings.model,
            mode=settings.mode,
            details={
                "vision_provider": settings.effective_vision_provider,
                "vision_model": settings.vision_model,
                "llm_timeout_s": settings.llm_timeout_seconds,
            },
        ),
        timestamp=int(time.time() * 1000),
    )

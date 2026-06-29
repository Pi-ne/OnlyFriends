"""Backward-compatible facade over the new modular AI services.

This module is kept so that any code still importing AiService
from app.services continues to work. New code should import
directly from app.services.* submodules.
"""

import asyncio
import os
from dataclasses import dataclass

from .config import AiSettings
from .models import (
    AiActivityPlanRequest,
    AiActivityPlanResponse,
    AiImageClassifyRequest,
    AiImageClassifyResponse,
    AiReviewRequest,
    AiReviewResponse,
)
from .services.classify_service import ClassifyService
from .services.llm_client import LlmClient
from .services.plan_service import PlanService
from .services.review_service import ReviewService
from .services.rule_engine import RuleEngine
from .services.vision_client import VisionClient


@dataclass(frozen=True)
class AiSettingsCompat:
    """Compatibility shim — mirrors the old AiSettings dataclass API."""
    provider: str = os.getenv("AI_PROVIDER", "mock")
    model: str = os.getenv("AI_MODEL", "mock-planner-v1")
    mode: str = os.getenv("AI_MODE", "mock")


class AiService:
    """Backward-compatible facade.

    Delegates to the new ReviewService / PlanService / ClassifyService
    internally. Provides the same three methods with the same signatures
    as before.
    """

    def __init__(self) -> None:
        self.settings = AiSettingsCompat()
        settings_obj = AiSettings()
        llm = LlmClient(settings_obj)
        rule_engine = RuleEngine()
        vision = VisionClient(settings_obj)

        self._review = ReviewService(settings_obj, rule_engine, llm)
        self._plan = PlanService(settings_obj, llm)
        self._classify = ClassifyService(settings_obj, vision)

    def plan_activity(self, request: AiActivityPlanRequest | None) -> AiActivityPlanResponse:
        return asyncio.run(self._plan.plan(request))

    def review_content(self, request: AiReviewRequest | None) -> AiReviewResponse:
        return asyncio.run(self._review.review(request or AiReviewRequest()))

    def classify_images(self, request: AiImageClassifyRequest | None) -> AiImageClassifyResponse:
        return asyncio.run(self._classify.classify(request))

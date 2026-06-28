from datetime import datetime
from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")


class Result(BaseModel, Generic[T]):
    code: int = 200
    message: str = "success"
    data: T | None = None
    timestamp: int


class AiActivityPlanRequest(BaseModel):
    theme: str | None = None
    locationName: str | None = None
    startTime: datetime | None = None
    durationHours: int | None = None
    maxParticipants: int | None = None
    preferences: list[str] | None = None


class AiActivityPlanResponse(BaseModel):
    title: str
    description: str
    tags: list[str]
    locationSuggestion: str
    suggestedDurationHours: int
    suggestedMaxParticipants: int
    safetyNotes: list[str]
    agenda: list[str]


class AiReviewRequest(BaseModel):
    activityId: int | None = None
    title: str | None = None
    description: str | None = None
    tags: list[str] | None = None
    maxParticipants: int | None = None


class AiReviewResponse(BaseModel):
    result: str
    riskLevel: int
    riskCategories: list[str]
    reason: str
    confidence: float = Field(ge=0, le=1)


class AiImageClassifyRequest(BaseModel):
    activityId: int | None = None
    imageUrls: list[str] | None = None


class ImageResult(BaseModel):
    imageUrl: str
    category: str
    tags: list[str]
    moderation: str
    confidence: float = Field(ge=0, le=1)


class AiImageClassifyResponse(BaseModel):
    results: list[ImageResult]


class HealthResponse(BaseModel):
    status: str
    provider: str
    model: str
    mode: str
    details: dict[str, Any] = Field(default_factory=dict)

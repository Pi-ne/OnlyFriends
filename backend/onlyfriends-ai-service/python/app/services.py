import os
from dataclasses import dataclass

from .models import (
    AiActivityPlanRequest,
    AiActivityPlanResponse,
    AiImageClassifyRequest,
    AiImageClassifyResponse,
    AiReviewRequest,
    AiReviewResponse,
    ImageResult,
)


REJECT_KEYWORDS = [
    "fraud",
    "gambling",
    "drug",
    "porn",
    "terror",
    "weapon",
    "诈骗",
    "赌博",
    "毒品",
    "色情",
    "恐怖",
    "枪支",
]
RISK_KEYWORDS = [
    "alcohol",
    "night",
    "danger",
    "high intensity",
    "paid",
    "extreme",
    "酒",
    "夜间",
    "危险",
    "高强度",
    "收费",
    "极限",
]


@dataclass(frozen=True)
class AiSettings:
    provider: str = os.getenv("AI_PROVIDER", "mock")
    model: str = os.getenv("AI_MODEL", "mock-planner-v1")
    mode: str = os.getenv("AI_MODE", "mock")


class AiService:
    def __init__(self, settings: AiSettings | None = None) -> None:
        self.settings = settings or AiSettings()

    def plan_activity(self, request: AiActivityPlanRequest | None) -> AiActivityPlanResponse:
        theme = _text(request.theme if request else None, "城市轻社交")
        location = _text(request.locationName if request else None, "交通便利的公共空间")
        participants = request.maxParticipants if request and request.maxParticipants else 12
        duration = request.durationHours if request and request.durationHours else 2
        return AiActivityPlanResponse(
            title=f"{theme}体验局",
            description=f"围绕{theme}设计的轻量活动，适合破冰交流、共同体验和合影总结。建议选择{location}，并提前明确集合点、时间安排和安全边界。",
            tags=[theme, "社交", "体验", "多人活动" if participants > 30 else "小规模"],
            locationSuggestion=location,
            suggestedDurationHours=duration,
            suggestedMaxParticipants=participants,
            safetyNotes=["提前确认集合点和联系人", "活动前同步天气与交通信息", "控制强度并预留休息时间"],
            agenda=["签到与破冰", "主题体验", "自由交流", "总结合影"],
        )

    def review_content(self, request: AiReviewRequest | None) -> AiReviewResponse:
        content = " ".join(
            [
                _safe(request.title if request else None),
                _safe(request.description if request else None),
                " ".join(request.tags or []) if request else "",
            ]
        ).lower()
        reject_hits = _hits(content, REJECT_KEYWORDS)
        risk_hits = _hits(content, RISK_KEYWORDS)
        if reject_hits:
            return AiReviewResponse(
                result="reject",
                riskLevel=9,
                riskCategories=["severe_violation", *reject_hits],
                reason="Matched severe violation keywords: " + ",".join(reject_hits),
                confidence=0.96,
            )
        if risk_hits:
            return AiReviewResponse(
                result="risk",
                riskLevel=5,
                riskCategories=["safety_risk", *risk_hits],
                reason="Matched risk keywords: " + ",".join(risk_hits),
                confidence=0.82,
            )
        return AiReviewResponse(
            result="pass",
            riskLevel=0,
            riskCategories=[],
            reason="No mock risk keyword matched.",
            confidence=0.93,
        )

    def classify_images(self, request: AiImageClassifyRequest | None) -> AiImageClassifyResponse:
        urls = request.imageUrls if request and request.imageUrls else []
        return AiImageClassifyResponse(results=[self._classify_image(url) for url in urls])

    def _classify_image(self, url: str) -> ImageResult:
        lower = _safe(url).lower()
        if any(token in lower for token in ["group", "people", "photo"]):
            category, tags = "group_photo", ["合影", "用户"]
        elif any(token in lower for token in ["venue", "site", "place"]):
            category, tags = "venue", ["场地", "环境"]
        elif any(token in lower for token in ["process", "record", "run"]):
            category, tags = "process_record", ["过程记录", "活动现场"]
        elif any(token in lower for token in ["material", "supply", "kit"]):
            category, tags = "supplies", ["物资", "准备清单"]
        elif any(token in lower for token in ["result", "work", "achievement"]):
            category, tags = "achievement", ["成果展示", "作品"]
        else:
            category, tags = "process_record", ["活动记录", "待人工确认"]
        return ImageResult(
            imageUrl=url,
            category=category,
            tags=tags,
            moderation="risk" if any(token in lower for token in ["bad", "risk"]) else "pass",
            confidence=0.88,
        )


def _hits(content: str, keywords: list[str]) -> list[str]:
    return [keyword for keyword in keywords if keyword in content]


def _safe(value: str | None) -> str:
    return value or ""


def _text(value: str | None, fallback: str) -> str:
    stripped = value.strip() if value else ""
    return stripped or fallback

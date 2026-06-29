"""Content safety review service — three-layer pipeline.

Layer 1: Rule engine (keywords + regex) → immediate reject if matched
Layer 2: LLM semantic review → auto-pass / auto-reject / escalate
Layer 3: Escalation to manual admin review (handled by activity-service)

Decision matrix (from architecture doc §8.1.3):

| AI result | confidence | max_participants | Decision         |
|-----------|-----------|------------------|------------------|
| pass      | ≥ 0.7     | ≤ 50             | AUTO PUBLISH     |
| pass      | ≥ 0.7     | > 50             | → manual review  |
| pass      | < 0.7     | any              | → manual review  |
| risk      | any       | any              | → manual review  |
| reject    | ≥ 0.9     | any              | AUTO REJECT      |
| reject    | < 0.9     | any              | → manual review  |
| timeout   | —         | —                | → manual review  |
"""

import asyncio
import hashlib
import json
import logging
from typing import Any

from ..config import AiSettings
from ..models import AiReviewRequest, AiReviewResponse
from .llm_client import LlmClient, LlmException, extract_json_block
from .rule_engine import RuleEngine

logger = logging.getLogger(__name__)

# ------------------------------------------------------------------
# LLM Prompts
# ------------------------------------------------------------------

REVIEW_SYSTEM_PROMPT = """你是趣聚平台的活动内容安全审核员。
平台禁止以下类型的活动：
1. 违法违规活动（赌博、传销、非法集会等）
2. 低俗色情内容
3. 危险活动（无专业指导的高风险运动、危险化学品相关）
4. 欺诈性活动（虚假宣传、诱导消费）
5. 歧视性内容（种族、性别、宗教歧视等）

请严格按照以下 JSON 格式返回审核结果，不要输出其他内容：
{
  "result": "pass 或 risk 或 reject",
  "riskLevel": 0到10的整数（0=无风险，10=严重违规）,
  "riskCategories": ["类别1", "类别2"],
  "reason": "简要说明审核理由（50字以内）",
  "confidence": 0.0到1.0的小数（对本次判断的置信度）
}"""


def _build_user_prompt(request: AiReviewRequest) -> str:
    title = request.title or ""
    desc = request.description or ""
    tags = ", ".join(request.tags) if request.tags else "无"
    max_p = request.maxParticipants or 0
    return (
        f"请审核以下活动内容：\n"
        f"- 活动名称：{title}\n"
        f"- 活动简介：{desc}\n"
        f"- 活动标签：{tags}\n"
        f"- 报名人数上限：{max_p}"
    )


# ------------------------------------------------------------------
# Service
# ------------------------------------------------------------------


class ReviewService:
    """Three-layer content safety review service."""

    def __init__(
        self,
        settings: AiSettings,
        rule_engine: RuleEngine,
        llm: LlmClient,
        redis=None,
    ) -> None:
        self.settings = settings
        self.rule_engine = rule_engine
        self.llm = llm
        self.redis = redis

    async def review(self, request: AiReviewRequest) -> AiReviewResponse:
        """Run the full review pipeline for an activity submission."""

        # ── Layer 1: Rule Engine ──────────────────────────────
        rule_result = self.rule_engine.check(
            title=request.title or "",
            description=request.description or "",
            tags=request.tags,
        )
        if rule_result.blocked:
            return AiReviewResponse(
                result="reject",
                riskLevel=rule_result.risk_level,
                riskCategories=rule_result.categories,
                reason=rule_result.reason,
                confidence=1.0,
            )

        # ── Cache check ───────────────────────────────────────
        cache_key = self._cache_key(request)
        cached = await self._cache_get(cache_key)
        if cached is not None:
            logger.info("Review cache hit for key=%s", cache_key)
            return cached

        # ── Layer 2: LLM Semantic Review ──────────────────────
        if self.settings.is_mock:
            llm_result = self._mock_llm_review(request, rule_result)
        else:
            llm_result = await self._llm_review(request)

        # ── Cache result ──────────────────────────────────────
        await self._cache_set(cache_key, llm_result)

        # ── Layer 3 decision is made by the caller ───────────
        # The activity-service reads result/riskLevel/confidence
        # and decides whether to auto-publish, auto-reject, or
        # escalate to manual admin review.
        return llm_result

    # ------------------------------------------------------------------
    # LLM call
    # ------------------------------------------------------------------

    async def _llm_review(self, request: AiReviewRequest) -> AiReviewResponse:
        try:
            async with asyncio.timeout(self.settings.llm_timeout_seconds):
                raw = await self.llm.chat(
                    REVIEW_SYSTEM_PROMPT, _build_user_prompt(request)
                )
            parsed = extract_json_block(raw)
            return AiReviewResponse(
                result=parsed.get("result", "risk"),
                riskLevel=int(parsed.get("riskLevel", 5)),
                riskCategories=list(parsed.get("riskCategories", [])),
                reason=str(parsed.get("reason", ""))[:100],
                confidence=float(parsed.get("confidence", 0.5)),
            )
        except (LlmException, asyncio.TimeoutError) as exc:
            logger.warning("LLM review failed, escalating: %s", exc)
            return AiReviewResponse(
                result="risk",
                riskLevel=5,
                riskCategories=["ai_unavailable"],
                reason=f"AI审核不可用: {exc}",
                confidence=0.0,
            )

    # ------------------------------------------------------------------
    # Mock review (used when mode=mock)
    # ------------------------------------------------------------------

    def _mock_llm_review(
        self, request: AiReviewRequest, rule_result
    ) -> AiReviewResponse:
        """Deterministic mock that uses the rule engine's risk signal
        plus keyword heuristics to produce a realistic review response."""
        if rule_result.is_risk:
            return AiReviewResponse(
                result="risk",
                riskLevel=rule_result.risk_level,
                riskCategories=rule_result.categories,
                reason=rule_result.reason,
                confidence=0.82,
            )

        # Check for potential risky patterns not caught by rule engine
        content = f"{request.title or ''} {request.description or ''}".lower()
        if any(w in content for w in ["收费", "付费", "押金", "deposit", "fee"]):
            return AiReviewResponse(
                result="risk",
                riskLevel=3,
                riskCategories=["fee_risk"],
                reason="Activity involves fees; requires manual review",
                confidence=0.75,
            )

        # Default: pass
        max_p = request.maxParticipants or 0
        confidence = 0.93 if max_p <= 50 else 0.72
        return AiReviewResponse(
            result="pass",
            riskLevel=0,
            riskCategories=[],
            reason="No risk keywords matched in mock mode.",
            confidence=confidence,
        )

    # ------------------------------------------------------------------
    # Redis cache helpers
    # ------------------------------------------------------------------

    def _cache_key(self, request: AiReviewRequest) -> str:
        content = f"{request.title or ''}|{request.description or ''}"
        digest = hashlib.md5(content.encode("utf-8")).hexdigest()
        return f"review:{digest}"

    async def _cache_get(self, key: str) -> AiReviewResponse | None:
        if self.redis is None:
            return None
        try:
            raw = await self.redis.get(key)
            if raw:
                data = json.loads(raw)
                return AiReviewResponse(**data)
        except Exception as exc:
            logger.debug("Redis get error (non-fatal): %s", exc)
        return None

    async def _cache_set(self, key: str, response: AiReviewResponse) -> None:
        if self.redis is None:
            return
        try:
            data = response.model_dump()
            await self.redis.setex(
                key, self.settings.review_cache_ttl_seconds, json.dumps(data, ensure_ascii=False)
            )
        except Exception as exc:
            logger.debug("Redis set error (non-fatal): %s", exc)

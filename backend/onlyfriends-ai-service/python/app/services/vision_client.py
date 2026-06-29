"""Vision model client abstraction.

Supports OpenAI GPT-4V, 通义千问 VL (qwen-vl-plus/max), and any
OpenAI-compatible vision API. Falls back to URL-keyword heuristics
when vision is unavailable or in mock mode.
"""

import asyncio
import json
import logging
from typing import Any

from ..config import AiSettings

logger = logging.getLogger(__name__)

# Classification categories
CATEGORIES = ["合影", "场地", "过程记录", "物资", "成果展示"]

# Providers with OpenAI-compatible vision APIs.
_VISION_COMPATIBLE_PROVIDERS = {"openai", "qwen"}

CLASSIFY_SYSTEM_PROMPT = """你是一个活动图片分类助手。请对以下图片进行分类。

分类标准：
- 合影：多人的正式合照，多人面向镜头
- 场地：活动地点或环境的照片，少人或无人
- 过程记录：活动进行中的抓拍，人物在活动中
- 物资：活动相关物品的特写，无人
- 成果展示：活动的成果或作品展示

请严格按 JSON 格式返回：
{"category": "分类名称", "tags": ["标签1", "标签2"], "confidence": 0.95}"""


class VisionClient:
    """Vision model client with multi-provider support.

    Supported vision providers:
      - openai  : GPT-4V / GPT-4o
      - qwen    : 通义千问 VL (qwen-vl-plus, qwen-vl-max) via DashScope
      - deepseek: NOT supported (DeepSeek has no vision model yet)
    """

    def __init__(self, settings: AiSettings) -> None:
        self.settings = settings

    async def classify(
        self, image_url: str, categories: list[str] | None = None
    ) -> dict[str, Any]:
        """Classify a single image. Returns a dict with category, tags, confidence.

        Falls back to a URL-keyword-based heuristic if vision API is
        unavailable or if running in mock mode.
        """
        cats = categories or CATEGORIES

        if self.settings.is_mock:
            return self._heuristic_classify(image_url, cats)

        provider = self.settings.effective_vision_provider
        try:
            async with asyncio.timeout(self.settings.vision_timeout_seconds):
                if provider in _VISION_COMPATIBLE_PROVIDERS:
                    return await self._vision_classify(provider, image_url, cats)
                else:
                    logger.warning(
                        "Vision provider '%s' not supported (use openai or qwen), using heuristic",
                        provider,
                    )
                    return self._heuristic_classify(image_url, cats)
        except asyncio.TimeoutError:
            logger.warning("Vision classify timed out for %s", image_url[:80])
            return self._heuristic_classify(image_url, cats)
        except Exception as exc:
            logger.warning("Vision classify failed for %s: %s", image_url[:80], exc)
            return self._heuristic_classify(image_url, cats)

    # ------------------------------------------------------------------
    # OpenAI-compatible Vision (works for openai, qwen)
    # ------------------------------------------------------------------

    async def _vision_classify(
        self, provider: str, image_url: str, categories: list[str]
    ) -> dict[str, Any]:
        from openai import AsyncOpenAI

        api_key = self.settings.get_api_key(provider)
        base_url = self.settings.get_base_url(provider)
        model = self.settings.get_model(provider, for_vision=True)

        if not api_key:
            raise ValueError(f"Missing API key for vision provider '{provider}'")

        cats_str = "、".join(categories)
        prompt = CLASSIFY_SYSTEM_PROMPT + f"\n可选分类：{cats_str}"

        client = AsyncOpenAI(
            api_key=api_key,
            base_url=base_url or None,
        )
        response = await client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": prompt},
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": "请对这张活动图片进行分类。"},
                        {"type": "image_url", "image_url": {"url": image_url}},
                    ],
                },
            ],
            max_tokens=300,
            temperature=0.1,
        )
        content = response.choices[0].message.content or ""
        return self._parse_vision_response(content, image_url)

    # ------------------------------------------------------------------
    # Heuristic fallback (URL keyword matching)
    # ------------------------------------------------------------------

    def _heuristic_classify(self, url: str, _categories: list[str] | None = None) -> dict[str, Any]:
        """Classify based on URL keywords. Matches the existing Java mock logic."""
        lower = url.lower()
        if any(t in lower for t in ["group", "people", "photo", "合照", "合影"]):
            category, tags = "group_photo", ["合影", "用户"]
        elif any(t in lower for t in ["venue", "site", "place", "location", "场地", "地点"]):
            category, tags = "venue", ["场地", "环境"]
        elif any(t in lower for t in ["material", "supply", "kit", "物资", "准备"]):
            category, tags = "supplies", ["物资", "准备清单"]
        elif any(t in lower for t in ["result", "work", "achievement", "成果", "作品"]):
            category, tags = "achievement", ["成果展示", "作品"]
        elif any(t in lower for t in ["process", "record", "run", "action", "过程", "记录"]):
            category, tags = "process_record", ["过程记录", "活动现场"]
        else:
            category, tags = "process_record", ["活动记录", "待人工确认"]

        # Moderation check
        risk = any(t in lower for t in ["bad", "risk", "violence", "违规", "不良"])

        return {
            "category": category,
            "tags": tags,
            "moderation": "risk" if risk else "pass",
            "confidence": 0.88,
        }

    # ------------------------------------------------------------------
    # Response parsing
    # ------------------------------------------------------------------

    def _parse_vision_response(self, text: str, image_url: str) -> dict[str, Any]:
        try:
            data = json.loads(text.strip())
            return {
                "category": str(data.get("category", "process_record")),
                "tags": [str(t) for t in data.get("tags", [])],
                "moderation": "pass",
                "confidence": float(data.get("confidence", 0.85)),
            }
        except (json.JSONDecodeError, ValueError, KeyError):
            # If JSON parsing fails, fall back to heuristic
            logger.debug("Failed to parse vision response, using heuristic for %s", image_url[:80])
            return self._heuristic_classify(image_url)

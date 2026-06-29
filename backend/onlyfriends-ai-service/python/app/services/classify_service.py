"""Batch image classification service.

Classifies activity images into 5 categories using a vision model,
with URL-keyword heuristic fallback per image.
"""

import asyncio
import logging
from dataclasses import dataclass

from ..config import AiSettings
from ..models import AiImageClassifyRequest, AiImageClassifyResponse, ImageResult
from .vision_client import VisionClient

logger = logging.getLogger(__name__)

MAX_BATCH_SIZE = 10


class ClassifyService:
    """Batch image classification service."""

    def __init__(self, settings: AiSettings, vision: VisionClient) -> None:
        self.settings = settings
        self.vision = vision

    async def classify(self, request: AiImageClassifyRequest | None) -> AiImageClassifyResponse:
        urls = (request.imageUrls if request and request.imageUrls else [])[:MAX_BATCH_SIZE]

        if not urls:
            return AiImageClassifyResponse(results=[])

        # Classify all images concurrently
        tasks = [self._classify_one(url) for url in urls]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        output: list[ImageResult] = []
        for url, result in zip(urls, results):
            if isinstance(result, Exception):
                logger.warning("Classify failed for %s: %s", url[:80], result)
                output.append(self._fallback_result(url))
            else:
                output.append(result)

        return AiImageClassifyResponse(results=output)

    async def _classify_one(self, url: str) -> ImageResult:
        data = await self.vision.classify(url)
        return ImageResult(
            imageUrl=url,
            category=data.get("category", "process_record"),
            tags=data.get("tags", []),
            moderation=data.get("moderation", "pass"),
            confidence=float(data.get("confidence", 0.88)),
        )

    def _fallback_result(self, url: str) -> ImageResult:
        return ImageResult(
            imageUrl=url,
            category="process_record",
            tags=["活动记录", "分类失败"],
            moderation="pass",
            confidence=0.5,
        )

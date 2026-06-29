"""Image classification router — POST /ai/classify-images"""

from ..models import AiImageClassifyRequest, AiImageClassifyResponse, Result
from ..services.classify_service import ClassifyService
from ._common import success


def create_router(classify_service: ClassifyService):
    """Factory that creates a classify router bound to the given service instance."""

    from fastapi import APIRouter

    router = APIRouter(tags=["图片分类"])

    @router.post("/ai/classify-images", response_model=Result[AiImageClassifyResponse])
    async def classify_images(request: AiImageClassifyRequest | None = None):
        result = await classify_service.classify(request)
        return success(result)

    return router

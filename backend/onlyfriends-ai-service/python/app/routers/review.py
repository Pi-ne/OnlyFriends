"""Content safety review router — POST /ai/review-content"""

from ..models import AiReviewRequest, AiReviewResponse, Result
from ..services.review_service import ReviewService
from ._common import success


def create_router(review_service: ReviewService):
    """Factory that creates a review router bound to the given service instance."""

    from fastapi import APIRouter

    router = APIRouter(tags=["内容审核"])

    @router.post("/ai/review-content", response_model=Result[AiReviewResponse])
    async def review_content(request: AiReviewRequest | None = None):
        req = request or AiReviewRequest()
        result = await review_service.review(req)
        return success(result)

    return router

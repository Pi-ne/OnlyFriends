"""Activity plan generation router — POST /ai/plan-activity"""

from ..models import AiActivityPlanRequest, AiActivityPlanResponse, Result
from ..services.plan_service import PlanService
from ._common import success


def create_router(plan_service: PlanService):
    """Factory that creates a plan router bound to the given service instance."""

    from fastapi import APIRouter

    router = APIRouter(tags=["活动策划"])

    @router.post("/ai/plan-activity", response_model=Result[AiActivityPlanResponse])
    async def plan_activity(request: AiActivityPlanRequest | None = None):
        result = await plan_service.plan(request)
        return success(result)

    return router

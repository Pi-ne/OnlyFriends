"""AI activity plan generation service.

Takes user input (theme, location, preferences) and generates a
complete activity draft via LLM, with template-based fallback.
"""

import asyncio
import logging
from datetime import datetime

from ..config import AiSettings
from ..models import AiActivityPlanRequest, AiActivityPlanResponse
from .llm_client import LlmClient, LlmException, extract_json_block

logger = logging.getLogger(__name__)

# ------------------------------------------------------------------
# LLM Prompt
# ------------------------------------------------------------------

PLAN_SYSTEM_PROMPT = """你是趣聚平台的专业活动策划师。你的任务是根据用户的需求，生成一份完整、安全、可执行的活动策划方案。

要求：
1. 活动标题要吸引人但不浮夸，需包含活动主题关键词，不超过30字
2. 活动描述约200字，需涵盖：活动内容、适合人群、参与者能获得什么
3. 标签选择贴合活动的3-5个中文标签
4. 建议时长根据活动类型合理估算（1-8小时）
5. 人数上限根据活动性质建议（5-100人），户外活动建议不超过30人
6. 安全须知要具体、有针对性，不能是泛泛的"注意安全"
7. 准备清单要包含用户实际需要携带或准备的物品
8. 活动流程（agenda）要包含3-5个具体环节

严格按以下 JSON 格式输出，不要输出其他内容：
{
  "title": "活动标题",
  "description": "约200字的活动描述",
  "tags": ["标签1", "标签2", "标签3"],
  "locationSuggestion": "地点建议",
  "suggestedDurationHours": 3,
  "suggestedMaxParticipants": 20,
  "feeSuggestion": 0,
  "safetyNotes": ["具体安全须知1", "具体安全须知2", "具体安全须知3"],
  "preparationChecklist": ["准备事项1", "准备事项2"],
  "agenda": ["环节1：签到与破冰", "环节2：主题体验", "环节3：自由交流", "环节4：总结合影"]
}"""


def _build_user_prompt(request: AiActivityPlanRequest) -> str:
    theme = request.theme or "社交聚会"
    location = request.locationName or "待定"
    start = request.startTime.strftime("%Y-%m-%d %H:%M") if request.startTime else "待定"
    duration = request.durationHours or 2
    participants = request.maxParticipants or 20
    prefs = "、".join(request.preferences) if request.preferences else "无特殊偏好"

    return (
        f"请为以下活动需求策划方案：\n"
        f"- 活动主题：{theme}\n"
        f"- 活动地点：{location}\n"
        f"- 开始时间：{start}\n"
        f"- 预计时长：{duration}小时\n"
        f"- 预计人数：{participants}人\n"
        f"- 用户偏好：{prefs}"
    )


# ------------------------------------------------------------------
# Service
# ------------------------------------------------------------------


class PlanService:
    """Activity plan generation service with LLM + fallback."""

    def __init__(self, settings: AiSettings, llm: LlmClient) -> None:
        self.settings = settings
        self.llm = llm

    async def plan(self, request: AiActivityPlanRequest | None) -> AiActivityPlanResponse:
        req = request or AiActivityPlanRequest()

        if self.settings.is_mock:
            return self._fallback_plan(req)

        try:
            async with asyncio.timeout(self.settings.llm_timeout_seconds):
                raw = await self.llm.chat(PLAN_SYSTEM_PROMPT, _build_user_prompt(req))
            parsed = extract_json_block(raw)
            return self._parse_plan_response(parsed)
        except (LlmException, asyncio.TimeoutError) as exc:
            logger.warning("LLM plan generation failed, using fallback: %s", exc)
            return self._fallback_plan(req)

    # ------------------------------------------------------------------
    # Fallback (template-based, matches existing Java mock logic)
    # ------------------------------------------------------------------

    def _fallback_plan(self, request: AiActivityPlanRequest) -> AiActivityPlanResponse:
        theme = (request.theme or "").strip() or "城市轻社交"
        location = (request.locationName or "").strip() or "交通便利的公共空间"
        participants = request.maxParticipants or 12
        duration = request.durationHours or 2
        scale = "多人活动" if participants > 30 else "小规模"

        return AiActivityPlanResponse(
            title=f"{theme}体验局",
            description=(
                f"围绕{theme}设计的轻量活动，适合破冰交流、共同体验和合影总结。"
                f"建议选择{location}，并提前明确集合点、时间安排和安全边界。"
            ),
            tags=[theme, "社交", "体验", scale],
            locationSuggestion=location,
            suggestedDurationHours=duration,
            suggestedMaxParticipants=participants,
            feeSuggestion=0,
            safetyNotes=[
                "提前确认集合点和联系人",
                "活动前同步天气与交通信息",
                "控制强度并预留休息时间",
            ],
            preparationChecklist=[
                "舒适着装",
                "饮用水",
                "手机满电",
            ],
            agenda=["签到与破冰", "主题体验", "自由交流", "总结合影"],
        )

    # ------------------------------------------------------------------
    # Response parsing
    # ------------------------------------------------------------------

    def _parse_plan_response(self, parsed: dict) -> AiActivityPlanResponse:
        return AiActivityPlanResponse(
            title=str(parsed.get("title", "趣味活动")),
            description=str(parsed.get("description", "")),
            tags=[str(t) for t in parsed.get("tags", [])],
            locationSuggestion=str(parsed.get("locationSuggestion", "")),
            suggestedDurationHours=int(parsed.get("suggestedDurationHours", 2)),
            suggestedMaxParticipants=int(parsed.get("suggestedMaxParticipants", 20)),
            feeSuggestion=float(parsed.get("feeSuggestion", 0)),
            safetyNotes=[str(s) for s in parsed.get("safetyNotes", [])],
            preparationChecklist=[str(p) for p in parsed.get("preparationChecklist", [])],
            agenda=[str(a) for a in parsed.get("agenda", [])],
        )

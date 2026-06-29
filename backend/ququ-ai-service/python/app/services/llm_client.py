"""LLM client abstraction with multi-provider support.

Supports OpenAI, DeepSeek, 通义千问 (via OpenAI-compatible APIs),
百度文心一言 (native SDK), and mock mode.
"""

import asyncio
import json
import logging
import re
from typing import Any

from ..config import AiSettings

logger = logging.getLogger(__name__)

# Providers that use the OpenAI-compatible protocol.
_OPENAI_COMPATIBLE_PROVIDERS = {"openai", "deepseek", "qwen"}


class LlmClient:
    """Unified LLM client that dispatches to the configured provider."""

    def __init__(self, settings: AiSettings) -> None:
        self.settings = settings

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    async def chat(self, system_prompt: str, user_prompt: str) -> str:
        """Send a chat request and return the raw text response.

        Retries once on failure. Raises LlmException if all attempts fail.
        """
        if self.settings.is_mock:
            return self._mock_chat(system_prompt, user_prompt)

        provider = self.settings.provider
        attempts = max(1, self.settings.llm_retry_times + 1)

        for attempt in range(1, attempts + 1):
            try:
                async with asyncio.timeout(self.settings.llm_timeout_seconds):
                    if provider in _OPENAI_COMPATIBLE_PROVIDERS:
                        return await self._openai_compatible_chat(
                            provider, system_prompt, user_prompt
                        )
                    elif provider == "wenxin":
                        return await self._wenxin_chat(system_prompt, user_prompt)
                    else:
                        raise LlmException(f"Unknown provider: {provider}")
            except asyncio.TimeoutError:
                logger.warning("LLM call timeout (attempt %d/%d)", attempt, attempts)
                if attempt == attempts:
                    raise LlmException(
                        f"LLM call timed out after {self.settings.llm_timeout_seconds}s"
                    )
            except LlmException:
                raise
            except Exception as exc:
                logger.warning("LLM call failed (attempt %d/%d): %s", attempt, attempts, exc)
                if attempt == attempts:
                    raise LlmException(f"LLM call failed after {attempts} attempts: {exc}") from exc

        raise LlmException("LLM call failed")

    # ------------------------------------------------------------------
    # Provider implementations
    # ------------------------------------------------------------------

    async def _openai_compatible_chat(
        self, provider: str, system_prompt: str, user_prompt: str
    ) -> str:
        """Generic handler for all OpenAI-compatible providers.

        DeepSeek and 通义千问 both expose OpenAI-compatible chat APIs,
        so they share this code path. Only the base URL, API key, and
        model name differ — those are resolved from AiSettings.
        """
        from openai import AsyncOpenAI

        api_key = self.settings.get_api_key(provider)
        base_url = self.settings.get_base_url(provider)
        model = self.settings.get_model(provider)

        if not api_key:
            raise LlmException(
                f"Missing API key for provider '{provider}'. "
                f"Set the appropriate env var (see .env.example)."
            )

        client = AsyncOpenAI(
            api_key=api_key,
            base_url=base_url or None,
        )
        response = await client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.3,
            max_tokens=2000,
        )
        content = response.choices[0].message.content
        return content.strip() if content else ""

    async def _wenxin_chat(self, system_prompt: str, user_prompt: str) -> str:
        import qianfan

        chat_comp = qianfan.ChatCompletion(
            ak=self.settings.wenxin_api_key,
            sk=self.settings.wenxin_secret_key,
        )
        full_prompt = f"{system_prompt}\n\n{user_prompt}"
        resp = await asyncio.to_thread(
            chat_comp.do,
            model=self.settings.get_model("wenxin"),
            messages=[{"role": "user", "content": full_prompt}],
            temperature=0.3,
            max_output_tokens=2000,
        )
        return resp.get("result", "").strip()

    def _mock_chat(self, system_prompt: str, user_prompt: str) -> str:
        return json.dumps({"result": "pass", "reason": "mock mode bypass"})


class LlmException(Exception):
    """Raised when an LLM call fails irrecoverably."""


# ------------------------------------------------------------------
# JSON parsing helpers used by all services
# ------------------------------------------------------------------

_JSON_BLOCK_RE = re.compile(r"\{.*\}", re.DOTALL)


def extract_json_block(text: str) -> dict[str, Any]:
    """Extract the first JSON object from a string that may contain
    markdown fences or surrounding prose."""
    # Try direct parse first
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # Remove markdown code fences
    cleaned = re.sub(r"```(?:json)?\s*", "", text)
    cleaned = re.sub(r"```\s*", "", cleaned)
    try:
        return json.loads(cleaned.strip())
    except json.JSONDecodeError:
        pass

    # Try to find a JSON object with regex
    match = _JSON_BLOCK_RE.search(text)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            pass

    raise LlmException(f"Failed to extract JSON from LLM response: {text[:300]}")

import os
from dataclasses import dataclass, field

# Load .env file before reading os.getenv().
try:
    from dotenv import load_dotenv
    _ENV_FILE = os.path.join(os.path.dirname(__file__), "..", ".env")
    load_dotenv(_ENV_FILE)
except ImportError:
    pass


# ------------------------------------------------------------------
# Provider presets: default base URLs and model names
# ------------------------------------------------------------------

_PROVIDER_PRESETS: dict[str, dict[str, str]] = {
    "openai": {
        "base_url": "https://api.openai.com/v1",
        "default_model": "gpt-4o",
        "vision_model": "gpt-4o",
    },
    "deepseek": {
        "base_url": "https://api.deepseek.com",
        "default_model": "deepseek-chat",
        # DeepSeek does not currently offer a vision model;
        # set VISION_PROVIDER=qwen or openai for vision.
        "vision_model": "",
    },
    "qwen": {
        "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "default_model": "qwen-plus",
        "vision_model": "qwen-vl-plus",
    },
    "wenxin": {
        "base_url": "",
        "default_model": "ernie-4.0-8k",
        "vision_model": "",
    },
}

# ------------------------------------------------------------------
# API key env var names per provider (OpenAI-compatible providers)
# ------------------------------------------------------------------

_PROVIDER_KEY_ENV: dict[str, str] = {
    "openai": "OPENAI_API_KEY",
    "deepseek": "DEEPSEEK_API_KEY",
    "qwen": "QWEN_API_KEY",
    "wenxin": "WENXIN_API_KEY",
}

_PROVIDER_BASE_URL_ENV: dict[str, str] = {
    "openai": "OPENAI_BASE_URL",
    "deepseek": "DEEPSEEK_BASE_URL",
    "qwen": "QWEN_BASE_URL",
    "wenxin": "WENXIN_BASE_URL",
}


def _provider_preset(provider: str, key: str, fallback: str = "") -> str:
    preset = _PROVIDER_PRESETS.get(provider, {})
    return preset.get(key, fallback)


# ------------------------------------------------------------------
# Settings
# ------------------------------------------------------------------


@dataclass(frozen=True)
class AiSettings:
    """AI service configuration loaded from environment variables.

    Supported providers (AI_PROVIDER / VISION_PROVIDER):
      - openai    : OpenAI (GPT-4o, GPT-4V)
      - deepseek  : DeepSeek (deepseek-chat, deepseek-reasoner)
      - qwen      : 通义千问 via DashScope OpenAI-compatible API
      - wenxin    : 百度文心一言 (native SDK)
      - mock      : Deterministic keyword rules, no API calls

    Copy .env.example to .env and fill in your API keys.
    """

    # --- General ---
    provider: str = os.getenv("AI_PROVIDER", "mock")
    mode: str = os.getenv("AI_MODE", "mock")
    model: str = os.getenv("AI_MODEL", "")

    # --- OpenAI-compatible API keys (one per provider) ---
    openai_api_key: str = os.getenv("OPENAI_API_KEY", "")
    openai_base_url: str = os.getenv("OPENAI_BASE_URL", "")

    deepseek_api_key: str = os.getenv("DEEPSEEK_API_KEY", "")
    deepseek_base_url: str = os.getenv("DEEPSEEK_BASE_URL", "")

    qwen_api_key: str = os.getenv("QWEN_API_KEY", "")
    qwen_base_url: str = os.getenv("QWEN_BASE_URL", "")

    # --- Wenxin (native SDK, not OpenAI-compatible) ---
    wenxin_api_key: str = os.getenv("WENXIN_API_KEY", "")
    wenxin_secret_key: str = os.getenv("WENXIN_SECRET_KEY", "")

    # --- Vision ---
    vision_provider: str = os.getenv("VISION_PROVIDER", "")
    vision_model: str = os.getenv("VISION_MODEL", "")

    # --- Timeouts & Retries ---
    llm_timeout_seconds: int = int(os.getenv("LLM_TIMEOUT", "30"))
    llm_retry_times: int = int(os.getenv("LLM_RETRY_TIMES", "1"))
    vision_timeout_seconds: int = int(os.getenv("VISION_TIMEOUT", "20"))

    # --- Redis ---
    redis_host: str = os.getenv("REDIS_HOST", "localhost")
    redis_port: int = int(os.getenv("REDIS_PORT", "6379"))
    redis_password: str = os.getenv("REDIS_PASSWORD", "")
    redis_db: int = int(os.getenv("REDIS_DB", "0"))
    review_cache_ttl_seconds: int = int(os.getenv("REVIEW_CACHE_TTL", "86400"))

    # --- Review Decision Thresholds ---
    review_auto_pass_confidence: float = float(os.getenv("REVIEW_AUTO_PASS_CONFIDENCE", "0.7"))
    review_auto_reject_confidence: float = float(os.getenv("REVIEW_AUTO_REJECT_CONFIDENCE", "0.9"))
    review_auto_pass_max_participants: int = int(os.getenv("REVIEW_AUTO_PASS_MAX_PARTICIPANTS", "50"))

    # --- Misc ---
    log_level: str = os.getenv("LOG_LEVEL", "INFO")

    # ------------------------------------------------------------------
    # Derived properties
    # ------------------------------------------------------------------

    @property
    def is_mock(self) -> bool:
        return self.mode == "mock"

    @property
    def effective_vision_provider(self) -> str:
        return self.vision_provider or self.provider

    # ------------------------------------------------------------------
    # Resolved credentials for a given provider
    # ------------------------------------------------------------------

    def get_api_key(self, provider: str) -> str:
        """Return the API key for a provider, checking env var and preset defaults."""
        env_var = _PROVIDER_KEY_ENV.get(provider, "")
        return os.getenv(env_var, "")

    def get_base_url(self, provider: str) -> str:
        """Return the base URL for a provider.

        Priority: explicit env var > preset default > empty string.
        """
        env_var = _PROVIDER_BASE_URL_ENV.get(provider, "")
        explicit = os.getenv(env_var, "")
        if explicit:
            return explicit
        return _provider_preset(provider, "base_url", "")

    def get_model(self, provider: str, for_vision: bool = False) -> str:
        """Return the model name for a provider.

        Priority: explicit AI_MODEL/VISION_MODEL env var > preset default > fallback.
        """
        if for_vision and self.vision_model:
            return self.vision_model
        if not for_vision and self.model:
            return self.model
        key = "vision_model" if for_vision else "default_model"
        preset = _provider_preset(provider, key, "")
        if preset:
            return preset
        return "gpt-4o" if not for_vision else "gpt-4o"

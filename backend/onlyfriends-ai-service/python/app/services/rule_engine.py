"""Keyword and pattern-based rule engine for content safety.

This is the FIRST layer of the review pipeline. It runs before any LLM
call and can short-circuit with an immediate rejection.
"""

import re
from dataclasses import dataclass, field

# --- Keyword lists ---

SEVERE_KEYWORDS: list[str] = [
    "fraud", "gambling", "drug", "porn", "terror", "weapon",
    "诈骗", "赌博", "毒品", "色情", "恐怖", "枪支",
    "传销", "洗钱", "博彩", "裸聊", "违禁",
]

RISK_KEYWORDS: list[str] = [
    "alcohol", "night", "danger", "high intensity", "paid", "extreme",
    "酒", "夜间", "危险", "高强度", "收费", "极限",
    "夜店", "酒吧", "野外生存", "无保护",
]

# --- Regex patterns for sensitive content ---

# Phone numbers (Chinese mobile)
PHONE_RE = re.compile(r"1[3-9]\d{9}")

# Suspicious URLs — matches any URL; used to flag content with links
URL_RE = re.compile(r"https?://\S+")

# Repeated spam-like patterns — any char repeated 8+ times consecutively
SPAM_RE = re.compile(r"(.)\1{7,}")


@dataclass
class RuleCheckResult:
    """Result of a rule engine check."""

    blocked: bool = False
    """If True, the content should be rejected immediately."""

    is_risk: bool = False
    """If True, the content has risk signals but is not an immediate reject."""

    risk_level: int = 0
    """0-10 risk level."""

    categories: list[str] = field(default_factory=list)
    """Matched risk categories."""

    reason: str = ""
    """Human-readable explanation."""


def _has_excessive_repetition(text: str, threshold: int = 8) -> bool:
    """Return True if `text` contains a run of the same non-whitespace
    character repeated `threshold` or more times consecutively.

    This is a fallback for regex engines that may mishandle Unicode
    backreferences like (.)\\1{N,}.
    """
    if not text:
        return False
    count = 1
    prev = ""
    for ch in text:
        if ch == prev and not ch.isspace():
            count += 1
            if count >= threshold:
                return True
        else:
            count = 1
        prev = ch
    return False


class RuleEngine:
    """First-layer content safety check using keywords and regex patterns.

    This runs synchronously (no I/O) and is designed to be fast enough
    to run on every request before any LLM call.
    """

    def __init__(
        self,
        severe_keywords: list[str] | None = None,
        risk_keywords: list[str] | None = None,
    ) -> None:
        self.severe_keywords = [k.lower() for k in (severe_keywords or SEVERE_KEYWORDS)]
        self.risk_keywords = [k.lower() for k in (risk_keywords or RISK_KEYWORDS)]

    def check(self, title: str, description: str, tags: list[str] | None = None) -> RuleCheckResult:
        """Check content and return a result. If blocked=True, content
        should be immediately rejected without calling the LLM."""
        content = " ".join(
            [
                (title or ""),
                (description or ""),
                " ".join(tags or []),
            ]
        ).lower()

        # --- Severe keyword check ---
        severe_hits = [kw for kw in self.severe_keywords if kw in content]
        if severe_hits:
            return RuleCheckResult(
                blocked=True,
                risk_level=10,
                categories=["severe_violation"] + severe_hits,
                reason=f"Matched severe violation keywords: {', '.join(severe_hits)}",
            )

        # --- Pattern checks ---
        if PHONE_RE.search(content):
            return RuleCheckResult(
                blocked=True,
                risk_level=8,
                categories=["severe_violation", "personal_info"],
                reason="Content contains phone numbers (potential privacy leak or spam)",
            )

        if SPAM_RE.search(content) or _has_excessive_repetition(content, threshold=8):
            return RuleCheckResult(
                blocked=True,
                risk_level=7,
                categories=["severe_violation", "spam"],
                reason="Content contains spam-like repeated characters",
            )

        # --- URL check (not blocked, just flagged for manual review) ---
        if URL_RE.search(content):
            return RuleCheckResult(
                is_risk=True,
                risk_level=3,
                categories=["safety_risk", "external_link"],
                reason="Content contains external links — flagged for manual review",
            )

        # --- Risk keyword check ---
        risk_hits = [kw for kw in self.risk_keywords if kw in content]
        if risk_hits:
            return RuleCheckResult(
                is_risk=True,
                risk_level=5,
                categories=["safety_risk"] + risk_hits,
                reason=f"Matched risk keywords: {', '.join(risk_hits)}",
            )

        # --- Clean ---
        return RuleCheckResult(
            blocked=False,
            is_risk=False,
            risk_level=0,
            categories=[],
            reason="No risk keywords or patterns matched.",
        )

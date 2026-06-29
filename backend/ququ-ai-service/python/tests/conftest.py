"""Pytest configuration — force mock mode for all tests.

Tests should NEVER call real LLM APIs. They validate business logic
using deterministic mock responses, regardless of .env settings.

Environment variables MUST be set at module level (not in a pytest hook)
because AiSettings() reads from os.environ at import time.
"""

import os

# Force mock mode BEFORE any test module imports app.main.
os.environ["AI_MODE"] = "mock"
os.environ["AI_PROVIDER"] = "mock"

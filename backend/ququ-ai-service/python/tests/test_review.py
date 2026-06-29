"""Tests for the content safety review pipeline.

Run with:
    pytest tests/test_review.py -v
"""

import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class TestRuleEngine:
    """Tests for Layer 1: rule engine keyword/pattern matching."""

    def test_rejects_severe_keyword(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "测试活动", "description": "包含 赌博 内容", "tags": ["测试"]},
        )
        data = response.json()["data"]
        assert data["result"] == "reject"
        assert data["riskLevel"] == 10
        assert "severe_violation" in data["riskCategories"]
        assert data["confidence"] == 1.0

    def test_rejects_severe_keyword_english(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "test", "description": "involving drug trade", "tags": []},
        )
        data = response.json()["data"]
        assert data["result"] == "reject"
        assert data["riskLevel"] == 10

    def test_rejects_phone_number_in_description(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "测试", "description": "请联系我 13800138000", "tags": []},
        )
        data = response.json()["data"]
        assert data["result"] == "reject"
        assert "personal_info" in data["riskCategories"]

    def test_rejects_spam_pattern(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "测试", "description": "啊啊啊啊啊啊啊啊啊啊", "tags": []},
        )
        data = response.json()["data"]
        assert data["result"] == "reject"
        assert "spam" in data["riskCategories"]

    def test_flags_risk_keyword(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "极限运动挑战", "description": "高强度户外运动", "tags": ["极限"]},
        )
        data = response.json()["data"]
        # In mock mode, risk keywords go through LLM mock and return "risk"
        assert data["result"] in ("risk", "pass")
        assert data["riskLevel"] >= 3

    def test_passes_clean_content(self):
        response = client.post(
            "/ai/review-content",
            json={
                "title": "周末公园徒步",
                "description": "一起来朝阳公园轻松徒步，享受周末时光",
                "tags": ["徒步", "户外", "周末"],
                "maxParticipants": 20,
            },
        )
        data = response.json()["data"]
        assert data["result"] == "pass"
        assert data["riskLevel"] == 0


class TestReviewContract:
    """Tests that the response matches the Java service contract."""

    def test_response_envelope(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "测试", "description": "正常活动", "tags": []},
        )
        body = response.json()
        assert response.status_code == 200
        assert body["code"] == 200
        assert body["message"] == "success"
        assert "timestamp" in body
        assert "data" in body

    def test_required_fields_present(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "测试", "description": "测试内容", "tags": ["测试"]},
        )
        data = response.json()["data"]
        for field in ["result", "riskLevel", "riskCategories", "reason", "confidence"]:
            assert field in data, f"Missing field: {field}"

    def test_confidence_is_valid_range(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "正常活动", "description": "一个正常的活动描述", "tags": ["社交"]},
        )
        confidence = response.json()["data"]["confidence"]
        assert 0.0 <= confidence <= 1.0

    def test_handles_empty_request(self):
        response = client.post("/ai/review-content")
        data = response.json()["data"]
        assert data["result"] in ("pass", "risk", "reject")

    def test_handles_none_fields(self):
        response = client.post(
            "/ai/review-content",
            json={"title": None, "description": None, "tags": None, "maxParticipants": None},
        )
        assert response.status_code == 200


class TestFeeDetection:
    """Tests for fee-related risk detection in mock mode."""

    def test_detects_fee_risk(self):
        response = client.post(
            "/ai/review-content",
            json={"title": "收费培训课程", "description": "这是一个付费课程活动", "tags": ["收费"]},
        )
        data = response.json()["data"]
        assert data["result"] == "risk"

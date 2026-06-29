"""Tests for the AI activity plan generation service.

Run with:
    pytest tests/test_plan.py -v
"""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class TestPlanGeneration:
    """Tests for plan generation."""

    def test_generates_plan_from_theme(self):
        response = client.post(
            "/ai/plan-activity",
            json={
                "theme": "城市徒步",
                "locationName": "人民公园",
                "durationHours": 3,
                "maxParticipants": 16,
            },
        )
        body = response.json()
        assert response.status_code == 200
        assert body["code"] == 200
        data = body["data"]
        # In mock mode, title is derived from theme
        assert "城市徒步" in data["title"]
        assert data["suggestedDurationHours"] == 3
        assert data["suggestedMaxParticipants"] == 16

    def test_generates_plan_with_minimal_input(self):
        response = client.post(
            "/ai/plan-activity",
            json={"theme": "桌游聚会"},
        )
        data = response.json()["data"]
        assert len(data["title"]) > 0
        assert len(data["description"]) > 0
        assert len(data["tags"]) >= 3
        assert len(data["safetyNotes"]) >= 2
        assert len(data["agenda"]) >= 3

    def test_handles_empty_request(self):
        response = client.post("/ai/plan-activity")
        data = response.json()["data"]
        # Should use fallback defaults
        assert data["suggestedDurationHours"] > 0
        assert data["suggestedMaxParticipants"] > 0
        assert data["title"] == "城市轻社交体验局"

    def test_handles_none_fields(self):
        response = client.post(
            "/ai/plan-activity",
            json={
                "theme": None,
                "locationName": None,
                "startTime": None,
                "durationHours": None,
                "maxParticipants": None,
                "preferences": None,
            },
        )
        assert response.status_code == 200

    def test_large_participant_count_flags_in_tags(self):
        response = client.post(
            "/ai/plan-activity",
            json={"theme": "大型活动", "maxParticipants": 100},
        )
        data = response.json()["data"]
        assert "多人活动" in data["tags"]

    def test_small_participant_count_flags_in_tags(self):
        response = client.post(
            "/ai/plan-activity",
            json={"theme": "小型活动", "maxParticipants": 8},
        )
        data = response.json()["data"]
        assert "小规模" in data["tags"]


class TestPlanContract:
    """Tests that the response matches the Java service contract."""

    def test_response_envelope(self):
        response = client.post("/ai/plan-activity", json={"theme": "测试"})
        body = response.json()
        assert response.status_code == 200
        assert body["code"] == 200
        assert body["message"] == "success"
        assert "timestamp" in body

    def test_all_required_fields(self):
        response = client.post("/ai/plan-activity", json={"theme": "测试"})
        data = response.json()["data"]
        required_fields = [
            "title", "description", "tags", "locationSuggestion",
            "suggestedDurationHours", "suggestedMaxParticipants",
            "safetyNotes", "agenda",
        ]
        for field in required_fields:
            assert field in data, f"Missing field: {field}"

    def test_tags_are_strings(self):
        response = client.post("/ai/plan-activity", json={"theme": "测试"})
        tags = response.json()["data"]["tags"]
        assert all(isinstance(t, str) for t in tags)

"""Tests for the image classification service.

Run with:
    pytest tests/test_classify.py -v
"""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class TestImageClassify:
    """Tests for image classification."""

    def test_classifies_group_photo_by_url(self):
        response = client.post(
            "/ai/classify-images",
            json={"activityId": 1, "imageUrls": ["https://img.test/group-photo.jpg"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["category"] == "group_photo"
        assert "合影" in rows[0]["tags"]

    def test_classifies_venue_by_url(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://cdn.test/venue-place.jpg"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["category"] == "venue"
        assert "场地" in rows[0]["tags"]

    def test_classifies_process_record_by_url(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/run-record.png"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["category"] == "process_record"
        assert "活动现场" in rows[0]["tags"]

    def test_classifies_supplies_by_url(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://cdn.test/material-kit.jpg"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["category"] == "supplies"
        assert "物资" in rows[0]["tags"]

    def test_classifies_achievement_by_url(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/result-work.jpg"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["category"] == "achievement"
        assert "成果展示" in rows[0]["tags"]

    def test_defaults_to_process_record_for_unknown(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://cdn.test/unknown-random-image.jpg"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["category"] == "process_record"
        assert "待人工确认" in rows[0]["tags"]

    def test_detects_risk_moderation(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/risk-venue.jpg", "https://img.test/bad-content.png"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["moderation"] == "risk"
        assert rows[1]["moderation"] == "risk"

    def test_moderation_pass_for_clean_urls(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/group-photo.jpg"]},
        )
        rows = response.json()["data"]["results"]
        assert rows[0]["moderation"] == "pass"

    def test_batch_classification(self):
        urls = [
            "https://img.test/group-photo.jpg",
            "https://img.test/venue-place.jpg",
            "https://img.test/process-record.jpg",
        ]
        response = client.post("/ai/classify-images", json={"imageUrls": urls})
        rows = response.json()["data"]["results"]
        assert len(rows) == 3
        categories = [r["category"] for r in rows]
        assert "group_photo" in categories
        assert "venue" in categories


class TestClassifyContract:
    """Tests that the response matches the Java service contract."""

    def test_response_envelope(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/photo.jpg"]},
        )
        body = response.json()
        assert response.status_code == 200
        assert body["code"] == 200
        assert body["message"] == "success"
        assert "timestamp" in body

    def test_handles_empty_urls(self):
        response = client.post("/ai/classify-images", json={"imageUrls": []})
        assert response.json()["data"]["results"] == []

    def test_handles_empty_request(self):
        response = client.post("/ai/classify-images")
        assert response.json()["data"]["results"] == []

    def test_each_result_has_required_fields(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/photo.jpg"]},
        )
        row = response.json()["data"]["results"][0]
        for field in ["imageUrl", "category", "tags", "moderation", "confidence"]:
            assert field in row, f"Missing field: {field}"

    def test_confidence_is_valid_range(self):
        response = client.post(
            "/ai/classify-images",
            json={"imageUrls": ["https://img.test/photo.jpg"]},
        )
        confidence = response.json()["data"]["results"][0]["confidence"]
        assert 0.0 <= confidence <= 1.0

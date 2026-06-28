from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_plan_activity_matches_java_contract():
    response = client.post(
        "/ai/plan-activity",
        json={"theme": "城市徒步", "locationName": "人民公园", "durationHours": 3, "maxParticipants": 16},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["code"] == 200
    assert body["data"]["title"] == "城市徒步体验局"
    assert body["data"]["suggestedDurationHours"] == 3


def test_review_content_rejects_severe_keywords():
    response = client.post(
        "/ai/review-content",
        json={"title": "危险活动", "description": "包含 赌博 内容", "tags": ["测试"]},
    )

    data = response.json()["data"]
    assert data["result"] == "reject"
    assert data["riskLevel"] == 9
    assert "severe_violation" in data["riskCategories"]


def test_classify_images_returns_categories_and_moderation():
    response = client.post(
        "/ai/classify-images",
        json={"activityId": 1, "imageUrls": ["https://img.test/group-photo.jpg", "https://img.test/risk-venue.jpg"]},
    )

    rows = response.json()["data"]["results"]
    assert rows[0]["category"] == "group_photo"
    assert rows[1]["category"] == "venue"
    assert rows[1]["moderation"] == "risk"


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["data"]["status"] == "UP"

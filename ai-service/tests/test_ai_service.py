from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health():
    response = client.get("/ai/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_chat_without_api_key_returns_clear_configuration_message(monkeypatch):
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)
    response = client.post("/ai/chat", json={"question": "边坡监测需要哪些设备？"})
    assert response.status_code == 200
    body = response.json()
    assert body["configured"] is False
    assert "DEEPSEEK_API_KEY" in body["answer"]
    assert body["references"]


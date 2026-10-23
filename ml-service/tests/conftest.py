from collections.abc import Generator
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from ml_service.main import app

ASSETS_DIR = Path(__file__).resolve().parent / "assets"


@pytest.fixture(scope="session")
def client() -> Generator[TestClient, None, None]:
    """Session-scoped FastAPI TestClient triggering the application lifespan."""
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture(scope="session")
def assets_dir() -> Path:
    """Fixture providing the path to the test assets directory."""
    if not ASSETS_DIR.is_dir():
        pytest.fail(f"Test assets directory not found at: {ASSETS_DIR}")
    return ASSETS_DIR

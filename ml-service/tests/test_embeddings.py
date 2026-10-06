from pathlib import Path

import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

ENDPOINT = "/internal/v1/embeddings/reference"


def _read_asset(assets_dir: Path, filename: str) -> bytes:
    """Helper to read asset bytes with clear failure if file does not exist."""
    path = assets_dir / filename
    if not path.is_file():
        pytest.fail(f"Required test asset '{filename}' not found at: {path}")
    return path.read_bytes()


def test_reference_embedding_single_face_success(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 1: Valid single-face image returns 200 and valid schema."""
    image_bytes = _read_asset(assets_dir, "passport.jpeg")

    response = client.post(
        ENDPOINT,
        files={"file": ("passport.jpeg", image_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    data = response.json()

    assert "embedding" in data
    assert "model" in data
    assert "model_version" in data
    assert "dimension" in data

    assert data["model"] == "buffalo_l"
    assert data["model_version"] == "2.0"
    assert data["dimension"] == 512
    assert isinstance(data["embedding"], list)
    assert len(data["embedding"]) == 512
    assert all(isinstance(val, (int, float)) for val in data["embedding"])


def test_reference_embedding_normalization(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 2: Returned embedding is (512,) shape and L2 normalized to ~1.0."""
    image_bytes = _read_asset(assets_dir, "passport.jpeg")

    response = client.post(
        ENDPOINT,
        files={"file": ("passport.jpeg", image_bytes, "image/jpeg")},
    )

    assert response.status_code == 200
    data = response.json()

    embedding = np.array(data["embedding"], dtype=np.float32)
    assert embedding.shape == (512,)

    norm = np.linalg.norm(embedding)
    assert np.isclose(norm, 1.0, atol=1e-5), f"L2 norm {norm} is not approximately 1.0"


def test_reference_embedding_multiple_faces_two_faces(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 3: Two-face GTA artwork returns 422 indicating exactly one face required."""
    image_bytes = _read_asset(
        assets_dir,
        "new-gta-6-artwork-v0-w867zmq3vqlh1.webp",
    )

    response = client.post(
        ENDPOINT,
        files={
            "file": (
                "new-gta-6-artwork-v0-w867zmq3vqlh1.webp",
                image_bytes,
                "image/webp",
            )
        },
    )

    assert response.status_code == 422
    data = response.json()
    assert "detail" in data
    assert "exactly one face" in data["detail"].lower()


def test_reference_embedding_multiple_faces_group_image(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 4: Group image with 6 faces returns 422 with the same semantic error."""
    image_bytes = _read_asset(
        assets_dir,
        "IMG-20251228-WA0178~2_upscayl_4x_upscayl-standard-4x.png",
    )

    response = client.post(
        ENDPOINT,
        files={
            "file": (
                "IMG-20251228-WA0178~2_upscayl_4x_upscayl-standard-4x.png",
                image_bytes,
                "image/png",
            )
        },
    )

    assert response.status_code == 422
    data = response.json()
    assert "detail" in data
    assert "exactly one face" in data["detail"].lower()


def test_reference_embedding_invalid_image_bytes(client: TestClient) -> None:
    """Test 5: Arbitrary non-image bytes return 400 Bad Request."""
    corrupted_bytes = b"NOT_A_VALID_IMAGE_DATA_12345"

    response = client.post(
        ENDPOINT,
        files={"file": ("corrupt.jpg", corrupted_bytes, "image/jpeg")},
    )

    assert response.status_code == 400
    data = response.json()
    assert "detail" in data
    assert "invalid image" in data["detail"].lower()


def test_reference_embedding_missing_file_field(client: TestClient) -> None:
    """Test 6: Request without required file field returns 422 validation error."""
    response = client.post(ENDPOINT)
    assert response.status_code == 422


def test_reference_embedding_empty_file(client: TestClient) -> None:
    """Test 7: Uploading an empty file returns 400."""
    response = client.post(
        ENDPOINT,
        files={"file": ("empty.jpg", b"", "image/jpeg")},
    )

    assert response.status_code == 400
    data = response.json()
    assert "detail" in data
    assert "invalid image" in data["detail"].lower()


def test_reference_embedding_supported_formats(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 8: Supported image formats (JPEG, PNG, WebP) return 200 and 512-dim embedding."""
    jpeg_bytes = _read_asset(assets_dir, "passport.jpeg")
    png_bytes = _read_asset(assets_dir, "sayantan-pic.png")

    # Encode a valid single-face image to WebP format to test WebP decoding of single-face
    passport_img = cv2.imread(str(assets_dir / "passport.jpeg"))
    assert passport_img is not None, "Failed to read passport.jpeg for WebP encoding"
    success, webp_buffer = cv2.imencode(".webp", passport_img)
    assert success, "Failed to encode image to WebP format"
    webp_bytes = webp_buffer.tobytes()

    formats = [
        ("passport.jpeg", jpeg_bytes, "image/jpeg"),
        ("sayantan-pic.png", png_bytes, "image/png"),
        ("passport.webp", webp_bytes, "image/webp"),
    ]

    for filename, content, mime_type in formats:
        response = client.post(
            ENDPOINT,
            files={"file": (filename, content, mime_type)},
        )
        assert response.status_code == 200, f"Failed for format {mime_type}: {response.text}"
        data = response.json()
        assert data["dimension"] == 512
        assert len(data["embedding"]) == 512


def test_reference_embedding_consistency(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 9: Sending the same image twice produces identical embeddings."""
    image_bytes = _read_asset(assets_dir, "passport.jpeg")

    resp1 = client.post(
        ENDPOINT,
        files={"file": ("passport.jpeg", image_bytes, "image/jpeg")},
    )
    resp2 = client.post(
        ENDPOINT,
        files={"file": ("passport.jpeg", image_bytes, "image/jpeg")},
    )

    assert resp1.status_code == 200
    assert resp2.status_code == 200

    emb1 = np.array(resp1.json()["embedding"], dtype=np.float32)
    emb2 = np.array(resp2.json()["embedding"], dtype=np.float32)

    assert np.allclose(emb1, emb2, atol=1e-5), "Embeddings for the same image are not identical"


def test_reference_embedding_different_images(
    client: TestClient,
    assets_dir: Path,
) -> None:
    """Test 10: Two different single-face images both succeed and produce 512-dim vectors."""
    image_bytes_1 = _read_asset(assets_dir, "passport.jpeg")
    image_bytes_2 = _read_asset(assets_dir, "DSC_0732.jpg")

    resp1 = client.post(
        ENDPOINT,
        files={"file": ("passport.jpeg", image_bytes_1, "image/jpeg")},
    )
    resp2 = client.post(
        ENDPOINT,
        files={"file": ("DSC_0732.jpg", image_bytes_2, "image/jpeg")},
    )

    assert resp1.status_code == 200
    assert resp2.status_code == 200

    emb1 = np.array(resp1.json()["embedding"], dtype=np.float32)
    emb2 = np.array(resp2.json()["embedding"], dtype=np.float32)

    assert emb1.shape == (512,)
    assert emb2.shape == (512,)


def test_reference_embedding_no_face_detected(client: TestClient) -> None:
    """Test 11 (Edge case): Image with no face returns 422 indicating no face detected."""
    # Create a 200x200 solid black synthetic image
    blank_image = np.zeros((200, 200, 3), dtype=np.uint8)
    success, buffer = cv2.imencode(".jpg", blank_image)
    assert success, "Failed to encode blank image"

    response = client.post(
        ENDPOINT,
        files={"file": ("blank.jpg", buffer.tobytes(), "image/jpeg")},
    )

    assert response.status_code == 422
    data = response.json()
    assert "detail" in data
    assert "no face detected" in data["detail"].lower()

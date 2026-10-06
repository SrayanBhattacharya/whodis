from typing import Any

import numpy as np
from insightface.app import FaceAnalysis


class InsightFaceService:
    def __init__(self) -> None:
        self._app: FaceAnalysis | None = None

    def initialize(self) -> None:
        """Load the face analysis model."""
        if self._app is not None:
            return

        self._app = FaceAnalysis(
            name="buffalo_l",
            providers=[
                "CUDAExecutionProvider",
                "CPUExecutionProvider",
            ],
        )

        self._app.prepare(
            ctx_id=0,
            det_size=(640, 640),
        )

    def detect_faces(self, image: np.ndarray) -> list[dict[str, Any]]:
        """Detect faces and extract their embeddings."""
        if self._app is None:
            raise RuntimeError("InsightFaceService is not initialized")

        if image is None or image.size == 0:
            raise ValueError("Image must not be empty")

        faces = self._app.get(image)
        results: list[dict[str, Any]] = []

        for face in faces:
            results.append(
                {
                    "bbox": face.bbox.tolist(),
                    "det_score": float(face.det_score),
                    "embedding": face.normed_embedding.tolist(),
                }
            )

        return results

    def compare_embeddings(
        self,
        embedding_a: np.ndarray,
        embedding_b: np.ndarray,
    ) -> float:
        """Calculate cosine similarity between two normalized embeddings."""
        if embedding_a.shape != embedding_b.shape:
            raise ValueError("Embeddings must have the same shape")

        return float(np.dot(embedding_a, embedding_b))

    def generate_reference_embedding(self, image: np.ndarray) -> np.ndarray:
        if self._app is None:
            raise RuntimeError("InsightFaceService is not initialized")

        if image is None or image.size == 0:
            raise ValueError("Image must not be empty")

        faces = self._app.get(image)

        if len(faces) == 0:
            raise ValueError("No face detected")

        if len(faces) > 1:
            raise ValueError("Reference image must contain exactly one face")

        return faces[0].normed_embedding

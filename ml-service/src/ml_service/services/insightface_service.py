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

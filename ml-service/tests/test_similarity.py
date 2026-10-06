from pathlib import Path

import cv2
import numpy as np
from insightface.app import FaceAnalysis


ASSETS_DIR = Path(__file__).parent / "assets"


def get_embedding(app: FaceAnalysis, image_path: Path) -> np.ndarray:
    image = cv2.imread(str(image_path))

    if image is None:
        raise RuntimeError(f"Could not read image: {image_path}")

    faces = app.get(image)

    if not faces:
        raise RuntimeError(f"No face detected: {image_path}")

    if len(faces) > 1:
        raise RuntimeError(
            f"Expected one face in {image_path.name}, "
            f"but detected {len(faces)}"
        )

    return faces[0].normed_embedding


def cosine_similarity(
    embedding_a: np.ndarray,
    embedding_b: np.ndarray,
) -> float:
    return float(np.dot(embedding_a, embedding_b))


def main() -> None:
    app = FaceAnalysis(
        name="buffalo_l",
        providers=[
            "CUDAExecutionProvider",
            "CPUExecutionProvider",
        ],
    )

    app.prepare(
        ctx_id=0,
        det_size=(640, 640),
    )

    images = sorted(
        path
        for path in ASSETS_DIR.iterdir()
        if path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
    )

    embeddings = {}

    for image_path in images:
        try:
            embeddings[image_path.name] = get_embedding(
                app,
                image_path,
            )
        except RuntimeError as error:
            print(error)

    print("\n=== Pairwise Similarities ===")

    names = list(embeddings.keys())

    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            name_a = names[i]
            name_b = names[j]

            similarity = cosine_similarity(
                embeddings[name_a],
                embeddings[name_b],
            )

            print(
                f"{name_a} <-> {name_b}: "
                f"{similarity:.4f}"
            )


if __name__ == "__main__":
    main()
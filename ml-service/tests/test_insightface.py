from pathlib import Path

import cv2
from insightface.app import FaceAnalysis


ASSETS_DIR = Path(__file__).parent / "assets"


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

    print("InsightFace initialized")
    print()

    # Show which providers the loaded ONNX models are actually using.
    for model_name, model in app.models.items():
        print(f"{model_name}: {model.session.get_providers()}")

    print()

    images = [
        path
        for path in ASSETS_DIR.iterdir()
        if path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
    ]

    if not images:
        print(f"No images found in {ASSETS_DIR}")
        return

    for image_path in sorted(images):
        print(f"=== {image_path.name} ===")

        image = cv2.imread(str(image_path))

        if image is None:
            print("Failed to read image")
            print()
            continue

        faces = app.get(image)

        print(f"Faces detected: {len(faces)}")

        for index, face in enumerate(faces, start=1):
            print(f"Face {index}:")
            print(f"  Bounding box: {face.bbox.tolist()}")
            print(f"  Detection score: {float(face.det_score):.4f}")
            print(f"  Embedding shape: {face.normed_embedding.shape}")
            print(f"  Embedding norm: {float((face.normed_embedding ** 2).sum() ** 0.5):.4f}")

        print()


if __name__ == "__main__":
    main()
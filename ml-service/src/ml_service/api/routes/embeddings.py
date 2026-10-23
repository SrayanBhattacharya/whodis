import cv2
import numpy as np
from fastapi import APIRouter, File, HTTPException, Request, UploadFile

from ml_service.models.embedding import EmbeddingResponse

router = APIRouter(prefix="/internal/v1/embeddings")


@router.post("/reference", response_model=EmbeddingResponse)
async def generate_reference_embedding(
    request: Request,
    file: UploadFile = File(...),
):
    contents = await file.read()

    if not contents:
        raise HTTPException(
            status_code=400,
            detail="Invalid image",
        )

    image = cv2.imdecode(
        np.frombuffer(contents, dtype=np.uint8),
        cv2.IMREAD_COLOR,
    )

    if image is None:
        raise HTTPException(
            status_code=400,
            detail="Invalid image",
        )

    service = request.app.state.insightface_service

    try:
        embedding = service.generate_reference_embedding(image)
    except ValueError as exc:
        raise HTTPException(
            status_code=422,
            detail=str(exc),
        ) from exc

    return EmbeddingResponse(
        embedding=embedding.tolist(),
        model="buffalo_l",
        model_version="2.0",
        dimension=int(embedding.shape[0]),
    )
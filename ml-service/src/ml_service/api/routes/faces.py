import cv2
import numpy as np
from fastapi import APIRouter, File, HTTPException, Request, UploadFile
import logging

logger = logging.getLogger(__name__)

from ml_service.schemas.face_detection import (
    BoundingBox,
    DetectedFace,
    FaceDetectionResponse,
)

router = APIRouter(prefix="/internal/v1/faces", tags=["faces"])


@router.post("/detect", response_model=FaceDetectionResponse)
async def detect_faces(
    request: Request,
    file: UploadFile = File(...),
):
    image_bytes = await file.read()
    image_array = np.frombuffer(image_bytes, dtype=np.uint8)
    image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

    if image is None:
        raise HTTPException(
            status_code=400,
            detail="Invalid image",
        )

    service = request.app.state.insightface_service

    try:
        detected_faces = service.detect_face_data(image)
    except Exception:
        logger.exception("Face detection failed")
        raise HTTPException(
            status_code=500,
            detail="Face detection failed",
        )

    return FaceDetectionResponse(
        model="buffalo_l",
        model_version="2.0",
        dimension=512,
        faces=[
            DetectedFace(
                bounding_box=BoundingBox(**face["bounding_box"]),
                det_score=face["det_score"],
                embedding=face["embedding"],
            )
            for face in detected_faces
        ],
    )

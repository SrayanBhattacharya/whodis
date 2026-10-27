from pydantic import BaseModel, Field


class BoundingBox(BaseModel):
    x1: float
    y1: float
    x2: float
    y2: float


class DetectedFace(BaseModel):
    bounding_box: BoundingBox
    det_score: float = Field(ge=0.0, le=1.0)
    embedding: list[float] = Field(min_length=512, max_length=512)


class FaceDetectionResponse(BaseModel):
    model: str
    model_version: str
    dimension: int = 512
    faces: list[DetectedFace]

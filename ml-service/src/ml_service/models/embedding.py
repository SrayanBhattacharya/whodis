from pydantic import BaseModel, Field


class EmbeddingResponse(BaseModel):
    embedding: list[float] = Field(min_length=512, max_length=512)
    model: str
    model_version: str
    dimension: int
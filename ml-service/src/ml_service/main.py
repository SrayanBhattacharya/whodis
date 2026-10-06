from contextlib import asynccontextmanager

from fastapi import FastAPI

from ml_service.api.routes.health import router as health_router
from ml_service.core.config import settings
from ml_service.services.insightface_service import InsightFaceService


@asynccontextmanager
async def lifespan(app: FastAPI):
    insightface_service = InsightFaceService()
    insightface_service.initialize()

    app.state.insightface_service = insightface_service

    yield


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)

app.include_router(health_router)
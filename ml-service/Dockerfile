FROM python:3.14-slim

WORKDIR /app

# Install system dependencies required by OpenCV, InsightFace, and healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    libgl1 \
    libglib2.0-0 \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install uv binary from official image
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Create a non-root user and home directory
RUN groupadd -r whodis && useradd -r -g whodis -m -d /home/whodis whodis

# Set environment variables for InsightFace cache and Python path
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app/src \
    HOME=/home/whodis \
    INSIGHTFACE_HOME=/home/whodis/.insightface

# Ensure insightface directory exists and is owned by whodis
RUN mkdir -p /home/whodis/.insightface && chown -R whodis:whodis /home/whodis /app

# Copy dependency manifests and project files
COPY --chown=whodis:whodis pyproject.toml uv.lock README.md* ./

# Switch to non-root user
USER whodis

# Install Python dependencies using uv sync
RUN uv sync --frozen --no-dev --no-install-project

# Copy application source code
COPY --chown=whodis:whodis src/ ./src/

EXPOSE 8001

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

ENTRYPOINT ["uv", "run", "uvicorn", "ml_service.main:app", "--host", "0.0.0.0", "--port", "8001"]

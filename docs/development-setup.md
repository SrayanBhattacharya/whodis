# Local Development Setup Guide

This guide walks you through setting up and running WhoDis locally, including the PostgreSQL database, Redis instance, Python ML service, and Spring Boot backend.

---

## 1. Prerequisites

Before starting, ensure you have the following installed on your development machine:

- **Docker & Docker Compose:** Docker v24+ with Compose v2+
- **Java Development Kit (JDK):** Java 25 (as specified in `backend/pom.xml` `<java.version>25</java.version>`)
- **Python:** Python >= 3.14 (as specified in `ml-service/pyproject.toml` and `.python-version`)
- **Package Manager for Python:** `uv` (recommended, version >= 0.12.17) or `pip`
- **Build Tools:** `git`, `curl`

---

## 2. Infrastructure Setup (Docker Compose)

The repository root includes a `compose.yml` defining PostgreSQL 18 and Redis 8.

### Start Database and Message Broker
From the repository root:

```bash
docker compose up -d
```

### Verify Infrastructure Health
Check that both containers are healthy:

```bash
docker compose ps
```

You should see:
- `whodis-postgres` on port `5432:5432` (healthy)
- `whodis-redis` on port `6379:6379` (healthy)

---

## 3. Python ML Service Setup

The ML service is located in `ml-service/` and runs on FastAPI with InsightFace.

### Option A: Using `uv` (Recommended)

1. Change into the `ml-service` directory:
   ```bash
   cd ml-service
   ```

2. Sync dependencies and create virtual environment:
   ```bash
   uv sync
   ```

3. Start the FastAPI development server on port 8001:
   ```bash
   uv run uvicorn ml_service.main:app --host 0.0.0.0 --port 8001 --reload
   ```

### Option B: Using Standard Python Virtual Environment

```bash
cd ml-service
python3.14 -m venv .venv
source .venv/bin/activate
pip install -e .
uvicorn ml_service.main:app --host 0.0.0.0 --port 8001 --reload
```

*Note on Model Download:* On first startup, InsightFace will automatically download the `buffalo_l` weights (~300MB) to `~/.insightface/models/buffalo_l`. Ensure your environment has internet connectivity on the initial launch.

### Verify ML Service Health
In a new terminal window:
```bash
curl http://localhost:8001/health
```
Expected output:
```json
{"status":"ok"}
```

---

## 4. Spring Boot Backend Setup

The backend service is located in `backend/` and uses Maven with an included wrapper (`mvnw`).

### 1. Configure Local Environment
By default, the backend connects to `localhost:5432` for PostgreSQL and `localhost:6379` for Redis. If using different ports or credentials, create or set environment variables:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/whodis"
export DB_USERNAME="whodis"
export DB_PASSWORD="whodis"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export ML_SERVICE_URL="http://localhost:8001"
export STORAGE_ROOT="./storage"
```

### 2. Compile and Run the Backend
From the `backend/` directory:

```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

On Windows:
```cmd
cd backend
mvnw.cmd spring-boot:run
```

### Database Migrations
On startup, Flyway will automatically execute migrations `V1` through `V9` in sequence, initializing all tables, indexes, and constraints in the `whodis` database.

### Verify Backend Health
```bash
curl http://localhost:8080/actuator/health
```
Expected output:
```json
{"status":"UP"}
```

---

## 5. End-to-End Verification Walkthrough

You can test the complete system flow using `curl`:

### 1. Create a Session
```bash
SESSION_ID=$(curl -s -X POST http://localhost:8080/api/v1/sessions | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Session ID: $SESSION_ID"
```

### 2. Register a Person
```bash
PERSON_ID=$(curl -s -X POST http://localhost:8080/api/v1/sessions/$SESSION_ID/people \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice"}' | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Person ID: $PERSON_ID"
```

### 3. Upload a Reference Image
Ensure you have a portrait image containing **exactly one face** (`portrait.jpg`):
```bash
curl -X POST http://localhost:8080/api/v1/sessions/$SESSION_ID/people/$PERSON_ID/reference-images \
  -F "file=@portrait.jpg"
```

### 4. Upload a Group Photo
```bash
curl -X POST http://localhost:8080/api/v1/sessions/$SESSION_ID/photos \
  -F "files=@group.jpg"
```

### 5. Check Processing Job Status
```bash
curl http://localhost:8080/api/v1/sessions/$SESSION_ID/photos
```
And check if Alice was recognized:
```bash
curl http://localhost:8080/api/v1/sessions/$SESSION_ID/people/$PERSON_ID/photos
```

---

## 6. Common Issues & Troubleshooting

### 1. Java Version Mismatch
- **Symptom:** `Fatal error compiling: error: release version 25 not supported`.
- **Cause:** Maven is running with a JDK version below 25.
- **Fix:** Install JDK 25 (e.g. Eclipse Temurin or OpenJDK 25) and set `JAVA_HOME`:
  ```bash
  export JAVA_HOME=/path/to/jdk-25
  ```

### 2. InsightFace / ONNX Runtime GPU Library Issues
- **Symptom:** Warnings or crashes regarding `libcuda.so` or CUDA execution provider.
- **Explanation:** `pyproject.toml` lists `onnxruntime-gpu`. If running on a machine without NVIDIA drivers or CUDA, InsightFace automatically falls back to `CPUExecutionProvider`.
- **Resolution:** If CUDA is missing, install CPU onnxruntime or let InsightFace use CPU mode.

### 3. Connection Refused to Redis / PostgreSQL
- **Symptom:** Backend fails to start with `Unable to connect to Redis` or `Connection to localhost:5432 refused`.
- **Fix:** Ensure Docker containers are running:
  ```bash
  docker compose up -d
  docker compose ps
  ```

### 4. Storage Directory Permissions
- **Symptom:** `StorageException: Failed to store file`.
- **Fix:** Ensure the backend process has write permissions to `./storage` (or the directory pointed to by `STORAGE_ROOT`).

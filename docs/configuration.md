# Configuration Guide

This document catalogs all environment variables, configuration properties, and defaults across the Spring Boot backend, Python ML service, PostgreSQL, and Redis.

---

## 1. Configuration Reference Table

| Variable Name | Component | Description | Default in Application | Required? |
|---|---|---|---|---|
| `DB_URL` | Backend | JDBC URL for PostgreSQL connection | `jdbc:postgresql://localhost:5432/whodis` | Optional |
| `DB_USERNAME` | Backend | PostgreSQL database username | `whodis` | Optional |
| `DB_PASSWORD` | Backend | PostgreSQL database password | `whodis` | Optional |
| `REDIS_HOST` | Backend | Hostname of the Redis server | `localhost` | Optional |
| `REDIS_PORT` | Backend | Port number of the Redis server | `6379` | Optional |
| `STORAGE_ROOT` | Backend | Directory path for storing uploaded photos | `./storage` | Optional |
| `ML_SERVICE_URL` | Backend | Base URL for the Python ML FastAPI service | `http://localhost:8001` | Optional |
| `SESSION_DURATION` | Backend | Lifetime duration before a session expires | `48h` | Optional |
| `SESSION_CLEANUP_INTERVAL` | Backend | Interval in ms between expired session cleanup runs | `300000` (5 mins) | Optional |
| `POSTGRES_DB` | PostgreSQL | Database name in Docker container | `whodis` | Yes (for Docker) |
| `POSTGRES_USER` | PostgreSQL | User for PostgreSQL Docker container | `whodis` | Yes (for Docker) |
| `POSTGRES_PASSWORD` | PostgreSQL | Password for PostgreSQL Docker container | `whodis` | Yes (for Docker) |
| `WHODIS_APP_NAME` | ML Service | Application title exposed by FastAPI | `"WhoDis ML Service"` | Optional |
| `WHODIS_APP_VERSION`| ML Service | Application version exposed by FastAPI | `"0.1.0"` | Optional |
| `WHODIS_DEBUG` | ML Service | Enable or disable debug mode | `false` | Optional |

---

## 2. Spring Boot Backend Configuration (`backend/src/main/resources/application.yaml`)

### Spring Infrastructure
- **Flyway Database Migration:**
  - `spring.flyway.enabled: true`
  - Automated migration runs on application startup.
- **JPA & Hibernate:**
  - `spring.jpa.hibernate.ddl-auto: validate` (enforces strict synchronization between entity models and database schema).
  - `spring.jpa.open-in-view: false` (prevents lazy-loading anti-patterns during web view rendering).
  - `spring.jpa.properties.hibernate.format_sql: true`
- **Multipart Upload Limits:**
  - `spring.servlet.multipart.max-file-size: 10MB` (per individual file).
  - `spring.servlet.multipart.max-request-size: 1000MB` (total payload size for bulk batch photo uploads).
- **Redis Connection:**
  - `spring.data.redis.host: ${REDIS_HOST:localhost}`
  - `spring.data.redis.port: ${REDIS_PORT:6379}`
  - `spring.data.redis.connect-timeout: 2s`
  - `spring.data.redis.timeout: 2s`

### Custom WhoDis Domain Properties
- **Session Lifecycle (`whodis.session`):**
  - `duration: ${SESSION_DURATION:48h}`: Length of time an active session remains valid.
  - `cleanup-interval: ${SESSION_CLEANUP_INTERVAL:300000}`: Fixed delay in milliseconds between background sweep runs (default 5 minutes).
- **Storage Subsystem (`whodis.storage`):**
  - `root: ${STORAGE_ROOT:./storage}`: Local filesystem directory where photos and reference images are saved.
- **ML Service Client (`whodis.ml`):**
  - `base-url: ${ML_SERVICE_URL:http://localhost:8001}`: HTTP destination for calling FastAPI.
- **Face Matching (`whodis.face-matching`):**
  - `similarity-threshold: 0.51`: Minimum cosine similarity score required to accept a facial match (code `@Value` fallback: `0.65`).
  - `minimum-margin: 0.05`: Minimum score gap between the best match and second-best candidate required to prevent ambiguous misclassification.
- **Async Processing Worker (`whodis.processing.worker`):**
  - `pending-idle-time: 5m`: Message idle duration before a pending job in Redis Streams is eligible for reclamation (code `@Value` fallback: `60s`).
  - `recovery-interval: 30000`: Milliseconds between execution of `recoverPendingMessages()` (every 30 seconds).
  - `max-attempts: 3`: Maximum processing attempts before a job permanently transitions to `FAILED`.
  - `retry-delays: 10s`: Configured retry delay property.
  - `reconciliation-interval: 30s`: Fixed delay between execution of `PhotoQueueReconciliationService` (every 30 seconds).

### Actuator Observability
- **Management Endpoints (`management.endpoints.web.exposure`):**
  - `include: health,info`: Exposes `/actuator/health` and `/actuator/info`.

---

## 3. Python ML Service Configuration

The ML service utilizes `pydantic-settings.BaseSettings` inside `ml_service.core.config.Settings`:
- **Environment Prefix:** `WHODIS_`
- **Configuration File:** `.env` (read automatically if present)
- **Model Suite Configuration (in code):**
  - `InsightFaceService.initialize()` prepares the `buffalo_l` model with `det_size=(640, 640)` and execution providers `CUDAExecutionProvider` and `CPUExecutionProvider`.
  - Embedding dimensions are fixed to `512`.

---

## 4. Docker Compose Configuration (`compose.yml`)

The provided `compose.yml` provides the backing data infrastructure:
- **`postgres` container:**
  - Image: `postgres:18`
  - Container Name: `whodis-postgres`
  - Port: `5432:5432`
  - Healthcheck: `pg_isready -U whodis -d whodis` (interval: 5s, timeout: 5s, retries: 5)
  - Persistent Volume: `postgres_data:/var/lib/postgresql`
- **`redis` container:**
  - Image: `redis:8-alpine`
  - Container Name: `whodis-redis`
  - Port: `6379:6379`
  - Command: `redis-server --appendonly yes` (AOF durability enabled)
  - Healthcheck: `redis-cli ping` (interval: 5s, timeout: 3s, retries: 5)
  - Persistent Volume: `redis_data:/data`

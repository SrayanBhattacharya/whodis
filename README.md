# WhoDis

WhoDis is a production-oriented, full-stack AI application that automatically organizes group photos based on the people appearing in them.

Users create a temporary session, register participants with reference portrait photos, upload a collection of group photos, and rely on an asynchronous computer vision pipeline to detect and match faces. A single photo can be associated with multiple registered individuals, while unrecognized faces are preserved for manual review and assignment.

---

## Architecture Overview

```text
Next.js Frontend (or REST Client)
              │
              ▼ HTTP (JSON / Multipart)
    Spring Boot Backend (Port 8080)
      ├── Local Filesystem Storage (UUID keys)
      ├── PostgreSQL 18 (Flyway migrations, relational schema)
      └── Redis Streams (whodis:photo-processing queue)
              │
              ▼ Asynchronous Processing & Synchronous Reference Extraction
      Python ML Service (FastAPI - Port 8001)
              │
              ▼ ONNX Runtime
      InsightFace buffalo_l (Face Detection & 512-dim Embeddings)
```

For comprehensive architectural design, refer to [docs/architecture.md](docs/architecture.md).

---

## Key Features

- **Session-Based Isolation:** Ephemeral, privacy-conscious sessions that auto-expire and trigger automated cascading cleanup of database records and disk files.
- **Strict Media Validation:** Uploaded photos are inspected using Apache Tika for real MIME detection and Java ImageIO for dimensions and pixel thresholds.
- **Reference Portrait Verification:** Reference photos are validated synchronously to guarantee that exactly one face is present before extracting reference embeddings.
- **Asynchronous Photo Processing:** Photo uploads publish jobs to Redis Streams with transaction-synchronized commits, resilient message consumption, and automatic stale job reclamation.
- **Accurate Face Matching:** Uses InsightFace's `buffalo_l` model to extract 512-dimensional normalized embeddings with cosine similarity matching and ambiguity margin checks.
- **Manual Face Assignment:** Unmatched or unrecognized faces can be queried and manually linked to registered participants.

---

## Technology Stack

- **Backend:** Java 25, Spring Boot 4.1.1, Spring Data JPA, Hibernate 6, Spring Data Redis, Flyway, Apache Tika.
- **Machine Learning Service:** Python >= 3.14, FastAPI, InsightFace 2.0 (`buffalo_l`), ONNX Runtime, OpenCV, NumPy, Pydantic v2.
- **Data & Message Infrastructure:** PostgreSQL 18, Redis 8 Alpine (Redis Streams with AOF persistence).
- **Frontend:** Next.js, TypeScript, Tailwind CSS, shadcn/ui, TanStack Query (under `frontend/`).

---

## Repository Structure

```text
whodis/
├── backend/                  # Spring Boot 4.1.1 backend application (Java 25)
│   ├── src/main/java/        # Domain services, controllers, entities, repositories
│   ├── src/main/resources/   # application.yaml and Flyway migrations V1..V9
│   ├── storage/              # Default local file storage root
│   └── pom.xml               # Maven configuration
├── ml-service/               # FastAPI ML inference microservice (Python 3.14)
│   ├── src/ml_service/       # InsightFace wrapper, API routes, Pydantic schemas
│   ├── tests/                # Unit tests and assets
│   └── pyproject.toml        # uv / Python project dependencies
├── frontend/                 # Next.js frontend application
├── docs/                     # Comprehensive technical documentation
│   ├── architecture.md       # High-level architecture and system design
│   ├── api-reference.md      # Complete REST API reference with DTO examples
│   ├── database-schema.md    # PostgreSQL schema, Flyway migrations & ER diagram
│   ├── ml-pipeline.md        # Computer vision pipeline, InsightFace & matching rules
│   ├── processing-pipeline.md# Asynchronous Redis Streams processing pipeline
│   ├── configuration.md      # Configuration parameters & environment variables
│   └── development-setup.md  # Step-by-step local development setup guide
├── compose.yml               # Docker Compose configuration for PostgreSQL & Redis
├── .env.example              # Environment variables template
└── README.md                 # Project summary and documentation index
```

---

## Documentation Index

Detailed documentation is available in the [`docs/`](docs/) directory:

- [**System Architecture**](docs/architecture.md): Service boundaries, communication protocols, design decisions, and Mermaid diagrams.
- [**API Reference**](docs/api-reference.md): Every REST endpoint, parameters, payloads, responses, validation rules, and error codes.
- [**Database Schema**](docs/database-schema.md): Complete entity-relationship diagram, table definitions, constraints, indexes, and migration tracking.
- [**Machine Learning Pipeline**](docs/ml-pipeline.md): InsightFace architecture, model parameters, embedding representations, and cosine similarity matching logic.
- [**Processing Pipeline**](docs/processing-pipeline.md): Asynchronous photo processing sequence, Redis Streams worker, retry mechanism, and reconciliation.
- [**Configuration Guide**](docs/configuration.md): Reference table of all environment variables, properties, and defaults.
- [**Development Setup**](docs/development-setup.md): Prerequisites, commands, and troubleshooting for running locally.

---

## Quick-Start Instructions

### 1. Launch with Docker Compose (Recommended)

Run the entire application stack (frontend, backend, ML inference service, database, Redis queue, and Nginx reverse proxy):

```bash
docker compose up --build -d
```

Once running, access the application directly at:
```text
http://localhost
```

All 6 services communicate via an internal isolated network (`whodis-network`), and Nginx acts as the single entrypoint on port `80`.

To view service logs:
```bash
docker compose logs -f
```

To stop all services:
```bash
docker compose down
```

To reset the entire stack and purge volumes (database, files, and caches):
```bash
docker compose down -v
```

---

### 2. Manual Local Development (Optional)

---

## Current Status

- [x] Ephemeral session lifecycle and automated background cleanup scheduler
- [x] Person registration and participant management
- [x] Strict image validation (MIME detection and pixel dimension constraints)
- [x] Synchronous reference portrait processing with 512-dim embedding extraction
- [x] Batch group photo uploads and local filesystem storage
- [x] Asynchronous processing engine using Redis Streams
- [x] InsightFace `buffalo_l` face detection and feature extraction
- [x] Cosine similarity face matching with confidence thresholds and minimum margin evaluation
- [x] Manual assignment API for unmatched faces
- [x] Full Flyway relational migrations (V1 through V9)
- [x] Comprehensive documentation suite in `docs/`
- [ ] Frontend integration with Next.js UI
- [ ] Results export (ZIP archive generation)
- [ ] Production containerization and CI/CD pipelines

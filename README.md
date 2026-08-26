# WhoDis

WhoDis is a production-oriented, full-stack AI application that automatically organizes group photos based on the people appearing in them.

Users can create a temporary session, register participants with reference photos, upload a collection of group photos, and have WhoDis identify which registered people appear in each image.

A single photo can belong to multiple people, while unrecognized faces remain classified as unknown.

## Architecture

```text
Next.js Frontend
        │
        ▼
Spring Boot Backend
        │
   ┌────┼────┐
   ▼    ▼    ▼
PostgreSQL Redis Storage
        │
        ▼
 Async Processing
        │
        ▼
 Python ML Service
        │
        ▼
Face Detection & Recognition
```

## Technology Stack

### Frontend

* Next.js
* TypeScript
* Tailwind CSS
* shadcn/ui
* TanStack Query

### Backend

* Spring Boot
* Java
* Spring Web
* Spring Data JPA
* PostgreSQL
* Redis

### ML Service

* Python
* FastAPI
* PyTorch
* InsightFace / ArcFace
* OpenCV
* NumPy

### Infrastructure

* Docker
* Docker Compose
* GitHub Actions

## Project Structure

```text
whodis/
├── frontend/
├── backend/
├── ml-service/
├── ml/
│   ├── experiments/
│   └── benchmarks/
├── docker/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## Current Status

🚧 **Early Development**

Current progress:

* [x] Project architecture defined
* [x] Spring Boot backend initialized
* [x] PostgreSQL configured
* [x] Flyway database migrations configured
* [x] Temporary session creation
* [x] Session retrieval
* [x] Basic exception handling
* [x] API health check
* [ ] Person registration
* [ ] Reference image processing
* [ ] Face detection and recognition
* [ ] Batch photo processing
* [ ] Asynchronous job processing
* [ ] Frontend
* [ ] Results and ZIP export
* [ ] Automatic session cleanup
* [ ] Testing
* [ ] Dockerized full stack
* [ ] CI/CD

## Development

### Start PostgreSQL

```bash
docker compose up -d postgres
```

### Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### Health Check

```text
GET http://localhost:8080/actuator/health
```

### Create a Session

```text
POST http://localhost:8080/api/v1/sessions
```

## Design Goals

WhoDis is being developed as a production-oriented system rather than a simple computer-vision demo.

Key goals include:

* Clean service boundaries
* Asynchronous processing
* Reliable file lifecycle management
* Temporary, privacy-conscious sessions
* Replaceable ML models
* Input validation
* Idempotent operations
* Structured error handling
* Automated cleanup
* Testing
* Containerized deployment
* CI/CD

## License

License to be determined.

# Asynchronous Processing Pipeline

WhoDis uses an asynchronous, resilient processing pipeline to handle heavy image processing and computer vision workloads. Group photos are uploaded, stored, and queued into **Redis Streams**, while a scheduled background worker claims jobs, invokes the ML service, evaluates facial similarity against registered references, and updates the database.

---

## 1. End-to-End Processing Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Frontend
    participant PC as PhotoController
    participant PS as PhotoService
    participant LS as LocalStorageService
    participant PPS as PhotoPersistenceService
    participant DB as PostgreSQL
    participant Redis as Redis Stream (whodis:photo-processing)
    participant Worker as PhotoProcessingWorker
    participant PProc as PhotoProcessingService
    participant ML as ML Service (FastAPI)
    participant FMS as FaceMatchingService

    Client->>PC: POST /api/v1/sessions/{id}/photos (Multipart files)
    PC->>PS: uploadPhotos(sessionId, files)
    PS->>PS: Validate MIME & dimensions (Tika / ImageIO)
    PS->>LS: Store files on disk (UUID keys)
    PS->>PPS: persistPhotos(session, files, storageKeys)
    activate PPS
    PPS->>DB: Save Photos & ProcessingJobs (Status: PENDING)
    PPS-->>DB: Commit Transaction
    PPS->>Redis: afterCommit() -> XADD whodis:photo-processing jobId
    PPS->>DB: markQueuePublished(jobId) (queue_published_at = now)
    deactivate PPS
    PS-->>Client: 201 Created (photoId, jobId, status=PENDING)

    loop Polling every 1000ms
        Worker->>Redis: XREADGROUP GROUP whodis:photo-processing:workers whodis-worker-1
    end
    Redis-->>Worker: MapRecord(id, {"jobId": "..."})

    Worker->>DB: claimJob(jobId) [UPDATE ... SET status=PROCESSING, attemptCount+1]
    Note over Worker,DB: DB row locked with findByIdForUpdate to coordinate with deletions
    alt Job Claim Succeeded
        Worker->>PProc: processPhoto(jobId, photoId)
        PProc->>LS: Load photo stream from disk
        PProc->>ML: POST /internal/v1/faces/detect (multipart)
        ML-->>PProc: PhotoFaceDetectionResponse (bounding boxes, embeddings)
        PProc->>DB: Load all reference embeddings for session
        loop For each detected face
            PProc->>FMS: findBestMatch(faceEmbedding, sessionRefEmbeddings)
            FMS-->>PProc: FaceMatchResult (personId, similarity) or NULL
        end
        PProc->>DB: Save PhotoFaces & completeJob(jobId) [status=COMPLETED]
        Worker->>Redis: XACK whodis:photo-processing whodis:photo-processing:workers messageId
    else Claim Failed / Unavailable
        Worker->>Redis: XACK messageId
    end
```

---

## 2. Processing Job State Machine

Each photo upload instantiates a `ProcessingJob` tracked in PostgreSQL:

```mermaid
stateDiagram-v2
    [*] --> PENDING: Created on photo upload
    
    PENDING --> PROCESSING: Worker claims job (claimJob / attemptCount++)
    
    PROCESSING --> COMPLETED: Faces extracted, matched, and saved
    
    PROCESSING --> PENDING: Failure occurs & attemptCount < maxAttempts (3)\n(Retry scheduled)
    
    PROCESSING --> FAILED: Failure occurs & attemptCount >= maxAttempts (3)\n(Max retries exhausted)

    PROCESSING --> PROCESSING: Stale job recovered after pendingIdleTime (5m)\n(reclaimStaleJob / attemptCount++)
    
    COMPLETED --> [*]
    FAILED --> [*]
```

### State Transitions & Persistence Rules
1. **PENDING:** Initial state upon upload. `attempt_count = 0`, `started_at = NULL`, `completed_at = NULL`.
2. **PROCESSING:** When a worker picks up the job via `claimJob(jobId)`. Executes an atomic conditional update:
   ```sql
   UPDATE ProcessingJob j
   SET j.status = 'PROCESSING',
       j.attemptCount = j.attemptCount + 1,
       j.startedAt = :now,
       j.errorMessage = null
   WHERE j.id = :jobId AND j.status = 'PENDING'
   ```
3. **COMPLETED:** All faces are detected, matched, persisted in `photo_faces`, and `ProcessingJobService.completeJob(jobId)` sets `status = COMPLETED`, `completed_at = Instant.now()`.
4. **RETRY (to PENDING):** If an exception occurs and `attempt_count < 3`:
   ```sql
   UPDATE ProcessingJob j
   SET j.status = 'PENDING',
       j.startedAt = null,
       j.completedAt = null,
       j.errorMessage = :errorMessage
   WHERE j.id = :jobId
     AND j.status = 'PROCESSING'
     AND j.attemptCount < :maxAttempts
   ```
   *Notice:* The Redis stream message is **not** acknowledged (`XACK`), remaining in the Pending Entries List (PEL) for subsequent recovery.
5. **FAILED:** If an exception occurs and `attempt_count >= 3`, `failJob(jobId, errorMessage)` marks `status = FAILED`, `completed_at = Instant.now()`, and the message is acknowledged with `XACK`.

---

## 3. Redis Streams Message Flow & Group Architecture

```mermaid
flowchart LR
    subgraph RedisStream["Redis Stream: whodis:photo-processing"]
        InitMsg["Marker: {'initialization': 'true'}"]
        M1["Msg 1: {'jobId': 'uuid-1'}"]
        M2["Msg 2: {'jobId': 'uuid-2'}"]
        PEL["Consumer Group: whodis:photo-processing:workers\nPending Entries List (PEL)"]
    end

    subgraph WorkerTasks["PhotoProcessingWorker"]
        ConsumerLoop["consumeMessages()\n@Scheduled(fixedDelay = 1000)\nXREADGROUP COUNT 10"]
        RecoveryLoop["recoverPendingMessages()\n@Scheduled(fixedDelay = 30000)\nXPENDING + XCLAIM"]
    end

    M1 --> ConsumerLoop
    M2 --> ConsumerLoop
    ConsumerLoop -.->|Unacknowledged / Retrying| PEL
    PEL --> RecoveryLoop
```

### Stream & Group Initialization
- **Configuration Class:** `PhotoProcessingStreamConfig` implements `InitializingBean`.
- **Stream Key:** `whodis:photo-processing`
- **Consumer Group:** `whodis:photo-processing:workers`
- **Consumer Name:** `whodis-worker-1`
- **Initialization Logic:**
  - If the stream key does not exist, an initialization entry `Map.of("initialization", "true")` is written via `opsForStream().add()`.
  - The consumer group is created starting from ID `"0"` (`ReadOffset.from("0")`).
  - If the group already exists (`BUSYGROUP` exception), the error is caught and logged.

---

## 4. Resilience, Idempotency & Fault Tolerance

### Stale Job Recovery (`recoverPendingMessages`)
- **Trigger:** Runs every 30,000 ms (`whodis.processing.worker.recovery-interval: 30000`).
- **Idle Cutoff:** Configured to 5 minutes in `application.yaml` (`pending-idle-time: 5m`, fallback `60s`).
- **Mechanism:**
  1. Inspects the Redis PEL using `redisTemplate.opsForStream().pending(...)` for messages idle longer than `pendingIdleTime`.
  2. Claims ownership using `redisTemplate.opsForStream().claim(...)`.
  3. Reclaims the job in PostgreSQL via `reclaimStaleJob(jobId, idleTimeout)`:
     ```sql
     UPDATE ProcessingJob j
     SET j.startedAt = :now,
         j.attemptCount = j.attemptCount + 1,
         j.errorMessage = null
     WHERE j.id = :jobId
       AND j.status = 'PROCESSING'
       AND j.startedAt <= :cutoff
     ```
  4. If the job in PostgreSQL was already `COMPLETED` or `FAILED`, the message is immediately acknowledged (`XACK`) to clean up Redis.

### Queue Reconciliation (`PhotoQueueReconciliationService`)
- **Trigger:** Runs every 30,000 ms (`whodis.processing.worker.reconciliation-interval: 30s`).
- **Problem Solved:** Guarantees delivery when Redis was unavailable at the time a transaction committed.
- **Mechanism:**
  - Queries `findTop50ByStatusAndQueuePublishedAtIsNullOrderByCreatedAtAsc(PENDING)`.
  - For each job, invokes `photoProcessingQueue.publish(jobId)` and updates `queue_published_at = Instant.now()`.

### Concurrency & Idempotency Safeguards
1. **Photo Deletion Lock:** When deleting a photo, `PhotoDeletionService` queries `processingJobRepository.existsByPhotoIdAndStatus(photoId, PROCESSING)`. If active, deletion is rejected with `HTTP 409 Conflict`.
2. **Atomic Job Claiming:** Multiple concurrent workers cannot process the same job because `claimJob` checks `status = 'PENDING'` atomically. Only 1 row is updated.
3. **Idempotent Face Persistence:** `PhotoFacePersistenceService.saveFacesAndCompleteJob()` first executes `photoFaceRepository.deleteAllByPhotoId(photoId)` before inserting newly detected faces, ensuring clean replacement if a job was reprocessed.
4. **Zero-Face Handling:** If a photo contains zero faces, the ML service returns an empty face list (`faces: []`). The backend processes this without error, saves zero `PhotoFace` records, and marks the job as `COMPLETED`.

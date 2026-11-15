# API Reference

This document details every REST API endpoint exposed by the WhoDis Spring Boot backend (`whodis-backend`).

Base URL: `http://localhost:8080` (or configured host/port)

---

## Standard Error Response Structure

All endpoints return errors structured via `com.whodis.backend.common.exception.ApiErrorResponse`:

```json
{
  "timestamp": "2026-11-15T14:30:00.123456Z",
  "status": 404,
  "code": "SESSION_NOT_FOUND",
  "message": "Session not found: 08adaf7e-4119-4fb2-9c41-0289501e36a6"
}
```

### Common Error Codes

| Status Code | Error `code` | Description |
|---|---|---|
| `400 Bad Request` | `VALIDATION_ERROR` | Request body or query parameters failed validation constraints. |
| `400 Bad Request` | `INVALID_IMAGE` | Uploaded image was empty, unparseable, exceeded 10 MB, or exceeded dimension limits. |
| `404 Not Found` | `SESSION_NOT_FOUND` | The specified session UUID does not exist. |
| `404 Not Found` | `PERSON_NOT_FOUND` | The person UUID does not exist in the session. |
| `404 Not Found` | `REFERENCE_IMAGE_NOT_FOUND` | The reference image UUID does not exist for the specified person. |
| `404 Not Found` | Standard Spring 404 | Photo, Face, or Job was not found in the given session scope. |
| `409 Conflict` | `DUPLICATE_PERSON` | A person with this exact name already exists in the session. |
| `409 Conflict` | Standard Spring 409 | Attempting to delete a photo currently in `PROCESSING` status. |
| `410 Gone` | `SESSION_EXPIRED` | The target session has reached or passed its `expiresAt` timestamp. |

---

## 1. Session Management

### Create Session
Creates a new temporary session with a default expiration time of 48 hours.

- **Method:** `POST`
- **Path:** `/api/v1/sessions`
- **Request Body:** None
- **Response Status:** `201 Created`
- **Response Body:**
```json
{
  "id": "e6f47702-861a-47d3-b1d6-d0a0b2302322",
  "createdAt": "2026-11-15T14:00:00Z",
  "expiresAt": "2026-11-17T14:00:00Z",
  "status": "ACTIVE"
}
```

### Get Session
Retrieves details and status of an existing session.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}`
- **Path Parameters:**
  - `sessionId` (UUID, required): The ID of the session.
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "id": "e6f47702-861a-47d3-b1d6-d0a0b2302322",
  "createdAt": "2026-11-15T14:00:00Z",
  "expiresAt": "2026-11-17T14:00:00Z",
  "status": "ACTIVE"
}
```
- **Error Responses:**
  - `404 Not Found` (`SESSION_NOT_FOUND`) if `sessionId` does not exist.
  - `410 Gone` (`SESSION_EXPIRED`) if session expiration timestamp has elapsed.

---

## 2. Person Management

### Create Person
Registers a new person/participant within an active session.

- **Method:** `POST`
- **Path:** `/api/v1/sessions/{sessionId}/people`
- **Path Parameters:**
  - `sessionId` (UUID, required): The session ID.
- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "name": "Alice Johnson"
}
```
- **Validation Rules:**
  - `name`: Must not be blank (`@NotBlank`). Max 100 characters (`@Size(max = 100)`). Trimming is applied automatically.
  - Duplicate check: Name must be unique within the session (case-sensitive exact match).
- **Response Status:** `201 Created`
- **Response Body:**
```json
{
  "id": "270ca887-b08d-4cb0-a548-2b814df39c59",
  "sessionId": "e6f47702-861a-47d3-b1d6-d0a0b2302322",
  "name": "Alice Johnson",
  "createdAt": "2026-11-15T14:05:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request` (`VALIDATION_ERROR`): Name is blank or exceeds 100 characters.
  - `404 Not Found` (`SESSION_NOT_FOUND`): Session does not exist.
  - `409 Conflict` (`DUPLICATE_PERSON`): A person with this name already exists in this session.
  - `410 Gone` (`SESSION_EXPIRED`): Session has expired.

### List People
Lists all registered people in a session, ordered chronologically by creation timestamp ascending.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/people`
- **Path Parameters:**
  - `sessionId` (UUID, required): The session ID.
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "id": "270ca887-b08d-4cb0-a548-2b814df39c59",
    "sessionId": "e6f47702-861a-47d3-b1d6-d0a0b2302322",
    "name": "Alice Johnson",
    "createdAt": "2026-11-15T14:05:00Z"
  },
  {
    "id": "8fa5ba98-0cfa-4961-8d26-47b1929d20c3",
    "sessionId": "e6f47702-861a-47d3-b1d6-d0a0b2302322",
    "name": "Bob Smith",
    "createdAt": "2026-11-15T14:06:00Z"
  }
]
```

### Get Person
Retrieves a specific person by their UUID within the session.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/people/{personId}`
- **Path Parameters:**
  - `sessionId` (UUID, required): The session ID.
  - `personId` (UUID, required): The person ID.
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "id": "270ca887-b08d-4cb0-a548-2b814df39c59",
  "sessionId": "e6f47702-861a-47d3-b1d6-d0a0b2302322",
  "name": "Alice Johnson",
  "createdAt": "2026-11-15T14:05:00Z"
}
```
- **Error Responses:**
  - `404 Not Found` (`PERSON_NOT_FOUND`): Person does not exist or does not belong to this session.

### Get Photos for a Person
Lists all photos containing the person (via matched faces), ordered by face creation date ascending.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/people/{personId}/photos`
- **Path Parameters:**
  - `sessionId` (UUID, required): The session ID.
  - `personId` (UUID, required): The person ID.
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
    "originalFilename": "group_photo_01.jpg",
    "contentType": "image/jpeg",
    "fileSize": 3450912,
    "createdAt": "2026-11-15T14:15:00Z",
    "boundingBox": "{\"x1\":120.5,\"y1\":85.2,\"x2\":280.1,\"y2\":295.4}",
    "similarityScore": 0.8124
  }
]
```

---

## 3. Reference Image Management

Reference images serve as visual ground truth portraits for face recognition.

### Upload Reference Image
Uploads a reference photo for a person. Validates image dimensions and synchronously contacts the ML service to extract a face embedding.

- **Method:** `POST`
- **Path:** `/api/v1/sessions/{sessionId}/people/{personId}/reference-images`
- **Path Parameters:**
  - `sessionId` (UUID, required): The session ID.
  - `personId` (UUID, required): The person ID.
- **Request Headers:** `Content-Type: multipart/form-data`
- **Form Data:**
  - `file` (Binary File, required): The image file (JPEG, PNG, or WebP).
- **Validation Rules:**
  - File size $\le$ 10 MB (`10 * 1024 * 1024` bytes).
  - MIME type verified by Apache Tika: must be `image/jpeg`, `image/png`, or `image/webp`.
  - Dimensions decoded by Java ImageIO: width $\le$ 8192, height $\le$ 8192, total pixels $\le$ 40,000,000.
  - Face count: ML service must detect **exactly 1 face** in the image. If 0 or $>1$ faces are found, ML service returns HTTP 422, causing upload failure.
- **Response Status:** `201 Created`
- **Response Body:**
```json
{
  "id": "c4d32e91-7f91-4cf5-9982-c84eb63e0018",
  "personId": "270ca887-b08d-4cb0-a548-2b814df39c59",
  "originalFilename": "alice_portrait.png",
  "contentType": "image/png",
  "fileSize": 1048576,
  "createdAt": "2026-11-15T14:10:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request` (`INVALID_IMAGE`): Image validation failed or file is corrupted.
  - `404 Not Found` (`PERSON_NOT_FOUND`): Person ID not found.

### List Reference Images
Lists all reference images uploaded for a person.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/people/{personId}/reference-images`
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "id": "c4d32e91-7f91-4cf5-9982-c84eb63e0018",
    "personId": "270ca887-b08d-4cb0-a548-2b814df39c59",
    "originalFilename": "alice_portrait.png",
    "contentType": "image/png",
    "fileSize": 1048576,
    "createdAt": "2026-11-15T14:10:00Z"
  }
]
```

### Get Reference Image Metadata
Retrieves metadata for a specific reference image.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/people/{personId}/reference-images/{imageId}`
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "id": "c4d32e91-7f91-4cf5-9982-c84eb63e0018",
  "personId": "270ca887-b08d-4cb0-a548-2b814df39c59",
  "originalFilename": "alice_portrait.png",
  "contentType": "image/png",
  "fileSize": 1048576,
  "createdAt": "2026-11-15T14:10:00Z"
}
```

### Delete Reference Image
Deletes a reference image and its stored embedding and filesystem record.

- **Method:** `DELETE`
- **Path:** `/api/v1/sessions/{sessionId}/people/{personId}/reference-images/{imageId}`
- **Response Status:** `204 No Content`
- **Error Responses:**
  - `404 Not Found` (`REFERENCE_IMAGE_NOT_FOUND`)

---

## 4. Photo Management & Upload

### Upload Photos (Batch)
Uploads one or more group photos to the session. Photos are saved to disk, assigned a `ProcessingJob` in `PENDING` status, and queued to Redis Streams.

- **Method:** `POST`
- **Path:** `/api/v1/sessions/{sessionId}/photos`
- **Path Parameters:**
  - `sessionId` (UUID, required): The session ID.
- **Request Headers:** `Content-Type: multipart/form-data`
- **Form Data:**
  - `files` (Array of Binary Files, required): One or more image files.
- **Validation Rules:**
  - At least one file must be provided.
  - Each file must pass image validation (max 10 MB, valid format, valid dimensions).
- **Response Status:** `201 Created`
- **Response Body:**
```json
[
  {
    "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
    "jobId": "f784e5a9-4673-455b-bf78-ef72186835a2",
    "originalFilename": "group_photo_01.jpg",
    "contentType": "image/jpeg",
    "fileSize": 3450912,
    "createdAt": "2026-11-15T14:15:00Z",
    "status": "PENDING"
  }
]
```

### List Session Photos
Lists all photos uploaded to the session ordered chronologically by creation timestamp ascending.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/photos`
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
    "originalFilename": "group_photo_01.jpg",
    "contentType": "image/jpeg",
    "fileSize": 3450912,
    "createdAt": "2026-11-15T14:15:00Z"
  }
]
```

### Download Photo Binary
Streams the original raw photo file back to the client.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/photos/{photoId}`
- **Path Parameters:**
  - `sessionId` (UUID, required)
  - `photoId` (UUID, required)
- **Response Status:** `200 OK`
- **Response Headers:**
  - `Content-Type`: Image MIME type (e.g. `image/jpeg`)
  - `Content-Length`: File size in bytes
  - `Content-Disposition`: `inline; filename="<original_filename>"`
- **Response Body:** Binary byte stream of the image file.
- **Error Responses:**
  - `404 Not Found` if photo does not exist in the session.

### Delete Single Photo
Deletes a photo, all associated `photo_faces`, all `processing_jobs`, and its disk file.

- **Method:** `DELETE`
- **Path:** `/api/v1/sessions/{sessionId}/photos/{photoId}`
- **Response Status:** `204 No Content`
- **Error Responses:**
  - `404 Not Found` if photo does not exist.
  - `409 Conflict` if the photo is currently being processed by a worker (`ProcessingJobStatus == PROCESSING`).

### Delete All Session Photos
Deletes all photos belonging to the session along with their faces, jobs, and storage files.

- **Method:** `DELETE`
- **Path:** `/api/v1/sessions/{sessionId}/photos`
- **Response Status:** `204 No Content`
- **Error Responses:**
  - `409 Conflict` if any photo in the session is currently in `PROCESSING` status.

---

## 5. Processing Job Status

### Get Photo Jobs
Retrieves processing history for a specific photo, ordered with newest jobs first.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/photos/{photoId}/jobs`
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "jobId": "f784e5a9-4673-455b-bf78-ef72186835a2",
    "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
    "status": "COMPLETED",
    "attemptCount": 1,
    "createdAt": "2026-11-15T14:15:00Z",
    "startedAt": "2026-11-15T14:15:02Z",
    "completedAt": "2026-11-15T14:15:05Z"
  }
]
```

### Get Specific Job Status
Retrieves current status and execution metrics for a specific processing job.

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/processing-jobs/{jobId}`
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "jobId": "f784e5a9-4673-455b-bf78-ef72186835a2",
  "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
  "status": "COMPLETED",
  "attemptCount": 1,
  "createdAt": "2026-11-15T14:15:00Z",
  "startedAt": "2026-11-15T14:15:02Z",
  "completedAt": "2026-11-15T14:15:05Z"
}
```

---

## 6. Face Recognition & Assignment

### List Unmatched Faces
Lists all detected faces across all photos in the session that are not assigned to any registered person (`person_id IS NULL`).

- **Method:** `GET`
- **Path:** `/api/v1/sessions/{sessionId}/unmatched-faces`
- **Path Parameters:**
  - `sessionId` (UUID, required)
- **Response Status:** `200 OK`
- **Response Body:**
```json
[
  {
    "faceId": "a1b2c3d4-e5f6-7890-1234-56789abcdef0",
    "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
    "originalFilename": "group_photo_01.jpg",
    "contentType": "image/jpeg",
    "boundingBox": "{\"x1\":450.0,\"y1\":120.0,\"x2\":580.0,\"y2\":290.0}",
    "similarityScore": null,
    "createdAt": "2026-11-15T14:15:05Z"
  }
]
```

### Assign Unmatched Face
Allows a user or administrator to manually assign an unrecognized face to a registered person in the session.

- **Method:** `PATCH`
- **Path:** `/api/v1/sessions/{sessionId}/faces/{faceId}/assign`
- **Path Parameters:**
  - `sessionId` (UUID, required)
  - `faceId` (UUID, required)
- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "personId": "270ca887-b08d-4cb0-a548-2b814df39c59"
}
```
- **Response Status:** `200 OK`
- **Response Body:**
```json
{
  "faceId": "a1b2c3d4-e5f6-7890-1234-56789abcdef0",
  "photoId": "e1966aee-9c16-4ca9-a9a3-5ba257dc8da8",
  "personId": "270ca887-b08d-4cb0-a548-2b814df39c59",
  "personName": "Alice Johnson",
  "boundingBox": "{\"x1\":450.0,\"y1\":120.0,\"x2\":580.0,\"y2\":290.0}",
  "similarityScore": null,
  "createdAt": "2026-11-15T14:15:05Z"
}
```
- **Error Responses:**
  - `404 Not Found`: "Face not found" or "Face not found in this session" or "Person not found in this session".

---

## 7. Actuator & Health Endpoints

- **`GET /actuator/health`**: Returns basic health status `{"status": "UP"}`.
- **`GET /actuator/info`**: Returns build/application information.

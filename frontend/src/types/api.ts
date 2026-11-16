export type SessionStatus = "ACTIVE" | "PROCESSING" | "COMPLETED" | "EXPIRED";

export interface SessionResponse {
  id: string;
  createdAt: string;
  expiresAt: string;
  status: SessionStatus;
}

export interface PersonResponse {
  id: string;
  sessionId: string;
  name: string;
  createdAt: string;
}

export interface CreatePersonRequest {
  name: string;
}

export interface ReferenceImageResponse {
  id: string;
  personId: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

export type ProcessingJobStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface ProcessingJobResponse {
  jobId: string;
  photoId: string;
  status: ProcessingJobStatus;
  attemptCount: number;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
}

export interface PhotoListResponse {
  photoId: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

export interface PhotoUploadResponse {
  photoId: string;
  jobId: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  status: ProcessingJobStatus;
}

export interface PersonPhotoResponse {
  photoId: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  boundingBox: string; // JSON string: '{"x1":...,"y1":...,"x2":...,"y2":...}'
  similarityScore?: number | null;
}

export interface UnmatchedFaceResponse {
  faceId: string;
  photoId: string;
  originalFilename: string;
  contentType: string;
  boundingBox: string; // JSON string: '{"x1":...,"y1":...,"x2":...,"y2":...}'
  similarityScore?: number | null;
  createdAt: string;
}

export interface AssignFaceRequest {
  personId: string;
}

export interface FaceAssignmentResponse {
  faceId: string;
  photoId: string;
  personId: string;
  personName: string;
  boundingBox: string;
  similarityScore?: number | null;
  createdAt: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
}

export interface ParsedBoundingBox {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

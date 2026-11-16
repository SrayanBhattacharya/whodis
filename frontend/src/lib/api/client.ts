import {
  SessionResponse,
  PersonResponse,
  CreatePersonRequest,
  ReferenceImageResponse,
  ProcessingJobResponse,
  PhotoListResponse,
  PhotoUploadResponse,
  PersonPhotoResponse,
  UnmatchedFaceResponse,
  AssignFaceRequest,
  FaceAssignmentResponse,
  ApiErrorResponse,
} from "@/src/types/api";

export class ApiError extends Error {
  status: number;
  code: string;
  timestamp: string;

  constructor(status: number, code: string, message: string, timestamp?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.timestamp = timestamp || new Date().toISOString();
  }
}

// In browser, call our Next.js same-origin proxy `/api/backend` to cleanly avoid CORS.
// If explicitly provided via NEXT_PUBLIC_API_BASE_URL, that can be used directly if configured.
const API_PREFIX = "/api/backend";

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_PREFIX}${path}`;
  const headers = new Headers(options.headers || {});

  if (!(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorData: ApiErrorResponse | null = null;
    try {
      errorData = await response.json();
    } catch {
      // not a json error
    }

    const code = errorData?.code || `HTTP_${response.status}`;
    const message = errorData?.message || response.statusText || "Request failed";
    throw new ApiError(response.status, code, message, errorData?.timestamp);
  }

  if (response.status === 204) {
    return null as unknown as T;
  }

  return response.json();
}

export const api = {
  // Session
  createSession: (): Promise<SessionResponse> =>
    request<SessionResponse>("/sessions", { method: "POST" }),

  getSession: (sessionId: string): Promise<SessionResponse> =>
    request<SessionResponse>(`/sessions/${sessionId}`),

  // People
  createPerson: (
    sessionId: string,
    data: CreatePersonRequest
  ): Promise<PersonResponse> =>
    request<PersonResponse>(`/sessions/${sessionId}/people`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  listPeople: (sessionId: string): Promise<PersonResponse[]> =>
    request<PersonResponse[]>(`/sessions/${sessionId}/people`),

  getPerson: (sessionId: string, personId: string): Promise<PersonResponse> =>
    request<PersonResponse>(`/sessions/${sessionId}/people/${personId}`),

  getPersonPhotos: (
    sessionId: string,
    personId: string
  ): Promise<PersonPhotoResponse[]> =>
    request<PersonPhotoResponse[]>(`/sessions/${sessionId}/people/${personId}/photos`),

  // Reference Images
  uploadReferenceImage: (
    sessionId: string,
    personId: string,
    file: File
  ): Promise<ReferenceImageResponse> => {
    const formData = new FormData();
    formData.append("file", file);
    return request<ReferenceImageResponse>(
      `/sessions/${sessionId}/people/${personId}/reference-images`,
      {
        method: "POST",
        body: formData,
      }
    );
  },

  listReferenceImages: (
    sessionId: string,
    personId: string
  ): Promise<ReferenceImageResponse[]> =>
    request<ReferenceImageResponse[]>(
      `/sessions/${sessionId}/people/${personId}/reference-images`
    ),

  deleteReferenceImage: (
    sessionId: string,
    personId: string,
    imageId: string
  ): Promise<void> =>
    request<void>(
      `/sessions/${sessionId}/people/${personId}/reference-images/${imageId}`,
      {
        method: "DELETE",
      }
    ),

  getReferenceImageUrl: (
    sessionId: string,
    personId: string,
    imageId: string
  ): string =>
    `${API_PREFIX}/sessions/${sessionId}/people/${personId}/reference-images/${imageId}/content`,

  // Photos
  uploadPhotos: (
    sessionId: string,
    files: File[]
  ): Promise<PhotoUploadResponse[]> => {
    const formData = new FormData();
    for (const file of files) {
      formData.append("files", file);
    }
    return request<PhotoUploadResponse[]>(`/sessions/${sessionId}/photos`, {
      method: "POST",
      body: formData,
    });
  },

  listPhotos: (sessionId: string): Promise<PhotoListResponse[]> =>
    request<PhotoListResponse[]>(`/sessions/${sessionId}/photos`),

  listGroupPhotos: (sessionId: string): Promise<PhotoListResponse[]> =>
    request<PhotoListResponse[]>(`/sessions/${sessionId}/photos/group`),

  deletePhoto: (sessionId: string, photoId: string): Promise<void> =>
    request<void>(`/sessions/${sessionId}/photos/${photoId}`, {
      method: "DELETE",
    }),

  deleteAllPhotos: (sessionId: string): Promise<void> =>
    request<void>(`/sessions/${sessionId}/photos`, {
      method: "DELETE",
    }),

  getPhotoUrl: (sessionId: string, photoId: string): string =>
    `${API_PREFIX}/sessions/${sessionId}/photos/${photoId}`,

  // Processing Jobs
  getPhotoJobs: (
    sessionId: string,
    photoId: string
  ): Promise<ProcessingJobResponse[]> =>
    request<ProcessingJobResponse[]>(
      `/sessions/${sessionId}/photos/${photoId}/jobs`
    ),

  getJobStatus: (
    sessionId: string,
    jobId: string
  ): Promise<ProcessingJobResponse> =>
    request<ProcessingJobResponse>(
      `/sessions/${sessionId}/processing-jobs/${jobId}`
    ),

  // Faces
  listUnmatchedFaces: (sessionId: string): Promise<UnmatchedFaceResponse[]> =>
    request<UnmatchedFaceResponse[]>(`/sessions/${sessionId}/unmatched-faces`),

  assignFace: (
    sessionId: string,
    faceId: string,
    data: AssignFaceRequest
  ): Promise<FaceAssignmentResponse> =>
    request<FaceAssignmentResponse>(`/sessions/${sessionId}/faces/${faceId}/assign`, {
      method: "PATCH",
      body: JSON.stringify(data),
    }),
};

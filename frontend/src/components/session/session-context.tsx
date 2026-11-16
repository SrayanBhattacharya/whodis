"use client";

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from "react";
import {
  SessionResponse,
  PersonResponse,
  PhotoListResponse,
  UnmatchedFaceResponse,
  ProcessingJobResponse,
} from "@/src/types/api";
import { api, ApiError } from "@/src/lib/api/client";
import { sessionStore } from "@/src/lib/utils/session-store";

interface SessionContextType {
  session: SessionResponse | null;
  sessionId: string;
  isLoading: boolean;
  isExpired: boolean;
  error: string | null;

  // Data
  photos: PhotoListResponse[];
  groupPhotos: PhotoListResponse[];
  people: PersonResponse[];
  unmatchedFaces: UnmatchedFaceResponse[];
  jobStatuses: Record<string, ProcessingJobResponse>; // key is photoId

  // Actions
  refreshPhotos: () => Promise<void>;
  refreshGroupPhotos: () => Promise<void>;
  refreshPeople: () => Promise<void>;
  refreshUnmatchedFaces: () => Promise<void>;
  refreshAll: () => Promise<void>;
  deletePhoto: (photoId: string) => Promise<void>;
  deleteAllPhotos: () => Promise<void>;
  createPerson: (name: string) => Promise<PersonResponse>;
  assignFace: (faceId: string, personId: string) => Promise<void>;
}

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionProvider({
  sessionId,
  children,
}: {
  sessionId: string;
  children: ReactNode;
}) {
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isExpired, setIsExpired] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [photos, setPhotos] = useState<PhotoListResponse[]>([]);
  const [groupPhotos, setGroupPhotos] = useState<PhotoListResponse[]>([]);
  const [people, setPeople] = useState<PersonResponse[]>([]);
  const [unmatchedFaces, setUnmatchedFaces] = useState<UnmatchedFaceResponse[]>([]);
  const [jobStatuses, setJobStatuses] = useState<Record<string, ProcessingJobResponse>>({});

  const handleError = useCallback((err: unknown) => {
    if (err instanceof ApiError) {
      if (err.status === 410 || err.code === "SESSION_EXPIRED") {
        setIsExpired(true);
        sessionStore.clear();
      } else if (err.status === 404 || err.code === "SESSION_NOT_FOUND") {
        setError("Session not found. It may have expired and been cleaned up.");
        sessionStore.clear();
      } else {
        setError(err.message);
      }
    } else if (err instanceof Error) {
      setError(err.message);
    } else {
      setError("An unexpected error occurred");
    }
  }, []);

  const refreshPhotos = useCallback(async () => {
    try {
      const data = await api.listPhotos(sessionId);
      setPhotos(data);
    } catch (err) {
      handleError(err);
    }
  }, [sessionId, handleError]);

  const refreshGroupPhotos = useCallback(async () => {
    try {
      const data = await api.listGroupPhotos(sessionId);
      setGroupPhotos(data);
    } catch (err) {
      handleError(err);
    }
  }, [sessionId, handleError]);

  const refreshPeople = useCallback(async () => {
    try {
      const data = await api.listPeople(sessionId);
      setPeople(data);
    } catch (err) {
      handleError(err);
    }
  }, [sessionId, handleError]);

  const refreshUnmatchedFaces = useCallback(async () => {
    try {
      const data = await api.listUnmatchedFaces(sessionId);
      setUnmatchedFaces(data);
    } catch (err) {
      handleError(err);
    }
  }, [sessionId, handleError]);

  const refreshAll = useCallback(async () => {
    try {
      const s = await api.getSession(sessionId);
      setSession(s);
      sessionStore.set(sessionId);
      await Promise.all([refreshPhotos(), refreshGroupPhotos(), refreshPeople(), refreshUnmatchedFaces()]);
    } catch (err) {
      handleError(err);
    }
  }, [sessionId, refreshPhotos, refreshGroupPhotos, refreshPeople, refreshUnmatchedFaces, handleError]);

  // Initial load
  useEffect(() => {
    let active = true;

    api
      .getSession(sessionId)
      .then(async (s) => {
        if (!active) return;
        setSession(s);
        sessionStore.set(sessionId);
        await Promise.all([refreshPhotos(), refreshGroupPhotos(), refreshPeople(), refreshUnmatchedFaces()]);
      })
      .catch((err) => {
        if (active) handleError(err);
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [sessionId, refreshPhotos, refreshGroupPhotos, refreshPeople, refreshUnmatchedFaces, handleError]);

  // Poll for jobs if there are any pending/processing photos
  useEffect(() => {
    if (photos.length === 0 || isExpired) return;

    let timer: NodeJS.Timeout;

    const checkJobStatuses = async () => {
      try {
        const updates: Record<string, ProcessingJobResponse> = {};
        let hasActiveJobs = false;

        for (const photo of photos) {
          const currentStatus = jobStatuses[photo.photoId]?.status;
          if (currentStatus === "COMPLETED" || currentStatus === "FAILED") {
            continue;
          }

          try {
            const jobs = await api.getPhotoJobs(sessionId, photo.photoId);
            if (jobs && jobs.length > 0) {
              const latestJob = jobs[0];
              updates[photo.photoId] = latestJob;
              if (
                latestJob.status === "PENDING" ||
                latestJob.status === "PROCESSING"
              ) {
                hasActiveJobs = true;
              }
            }
          } catch {
            // ignore individual status error
          }
        }

        if (Object.keys(updates).length > 0) {
          setJobStatuses((prev) => ({ ...prev, ...updates }));
          // If a job transitioned to completed, refresh people and unmatched faces
          const justCompleted = Object.values(updates).some(
            (j) => j.status === "COMPLETED"
          );
          if (justCompleted) {
            refreshUnmatchedFaces();
            refreshPeople();
            refreshGroupPhotos();
          }
        }

        if (hasActiveJobs) {
          timer = setTimeout(checkJobStatuses, 3000);
        }
      } catch {
        // ignore polling errors
      }
    };

    checkJobStatuses();

    return () => {
      clearTimeout(timer);
    };
  }, [photos, sessionId, isExpired, jobStatuses, refreshPeople, refreshUnmatchedFaces, refreshGroupPhotos]);

  const deletePhoto = async (photoId: string) => {
    await api.deletePhoto(sessionId, photoId);
    setPhotos((prev) => prev.filter((p) => p.photoId !== photoId));
    setGroupPhotos((prev) => prev.filter((p) => p.photoId !== photoId));
    setUnmatchedFaces((prev) => prev.filter((f) => f.photoId !== photoId));
    setJobStatuses((prev) => {
      const copy = { ...prev };
      delete copy[photoId];
      return copy;
    });
    refreshPeople();
  };

  const deleteAllPhotos = async () => {
    await api.deleteAllPhotos(sessionId);
    setPhotos([]);
    setGroupPhotos([]);
    setUnmatchedFaces([]);
    setJobStatuses({});
    refreshPeople();
  };

  const createPerson = async (name: string): Promise<PersonResponse> => {
    const person = await api.createPerson(sessionId, { name });
    setPeople((prev) => [...prev, person]);
    return person;
  };

  const assignFace = async (faceId: string, personId: string) => {
    await api.assignFace(sessionId, faceId, { personId });
    setUnmatchedFaces((prev) => prev.filter((f) => f.faceId !== faceId));
    await refreshPeople();
  };

  return (
    <SessionContext.Provider
      value={{
        session,
        sessionId,
        isLoading,
        isExpired,
        error,
        photos,
        groupPhotos,
        people,
        unmatchedFaces,
        jobStatuses,
        refreshPhotos,
        refreshGroupPhotos,
        refreshPeople,
        refreshUnmatchedFaces,
        refreshAll,
        deletePhoto,
        deleteAllPhotos,
        createPerson,
        assignFace,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession must be used within a SessionProvider");
  }
  return context;
}

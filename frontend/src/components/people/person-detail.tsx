"use client";

import React, { useState, useEffect } from "react";
import { ArrowLeft, Upload, Trash2, Camera, User } from "lucide-react";
import { PersonResponse, PersonPhotoResponse, ReferenceImageResponse } from "@/src/types/api";
import { api, ApiError } from "@/src/lib/api/client";
import { Button } from "@/src/components/ui/button";
import { formatBytes } from "@/src/lib/utils/format";

interface PersonDetailProps {
  sessionId: string;
  person: PersonResponse;
  onBack: () => void;
}

export function PersonDetail({ sessionId, person, onBack }: PersonDetailProps) {
  const [photos, setPhotos] = useState<PersonPhotoResponse[]>([]);
  const [referenceImages, setReferenceImages] = useState<ReferenceImageResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploadingRef, setIsUploadingRef] = useState(false);
  const [refUploadError, setRefUploadError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([
      api.getPersonPhotos(sessionId, person.id),
      api.listReferenceImages(sessionId, person.id),
    ])
      .then(([photosData, refsData]) => {
        if (!active) return;
        setPhotos(photosData);
        setReferenceImages(refsData);
      })
      .catch(() => {})
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [sessionId, person.id]);

  const loadData = async () => {
    try {
      const [photosData, refsData] = await Promise.all([
        api.getPersonPhotos(sessionId, person.id),
        api.listReferenceImages(sessionId, person.id),
      ]);
      setPhotos(photosData);
      setReferenceImages(refsData);
    } catch {
      // ignore
    }
  };

  const handleReferenceUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsUploadingRef(true);
      setRefUploadError(null);
      await api.uploadReferenceImage(sessionId, person.id, file);
      await loadData();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setRefUploadError(err.message);
      } else {
        setRefUploadError("Failed to upload reference portrait");
      }
    } finally {
      setIsUploadingRef(false);
      e.target.value = "";
    }
  };

  const handleDeleteReference = async (imageId: string) => {
    try {
      await api.deleteReferenceImage(sessionId, person.id, imageId);
      setReferenceImages((prev) => prev.filter((img) => img.id !== imageId));
    } catch {
      // ignore
    }
  };

  return (
    <div className="space-y-8 animate-fade-in font-mono">
      {/* Top Bar Navigation */}
      <div className="flex items-center justify-between pb-4 border-b border-[#1c1c1c]">
        <button
          onClick={onBack}
          className="flex items-center gap-2 text-xs text-[#888888] hover:text-white transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>[BACK TO DIRECTORY]</span>
        </button>
      </div>

      {/* Header Info */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded bg-[#0a0a0a] border border-[#222222] text-white flex items-center justify-center font-bold text-xl">
            {person.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white font-sans">
              {person.name}
            </h1>
            <p className="text-xs text-[#888888] mt-0.5">
              {"// matched_exposures:"} {photos.length} • reference_tensors: {referenceImages.length}
            </p>
          </div>
        </div>

        {/* Upload Reference Image Button */}
        <div>
          <label className="cursor-pointer">
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={handleReferenceUpload}
              className="hidden"
              disabled={isUploadingRef}
            />
            <Button
              variant="secondary"
              size="sm"
              isLoading={isUploadingRef}
              className="pointer-events-none text-xs"
            >
              <Upload className="w-3.5 h-3.5 mr-1.5" />
              Upload Reference Portrait
            </Button>
          </label>
          {refUploadError && (
            <p className="text-xs text-red-400 mt-2 font-mono">
              [error] {refUploadError}
            </p>
          )}
        </div>
      </div>

      {/* Reference Portraits Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between border-b border-[#1c1c1c] pb-2 text-xs">
          <span className="text-[#888888] uppercase tracking-wider">
            {"// GROUND-TRUTH CALIBRATION PORTRAITS"}
          </span>
          <span className="text-[#666666]">
            requires exactly 1 face (HTTP 422 enforced)
          </span>
        </div>

        {referenceImages.length === 0 ? (
          <div className="p-6 border border-dashed border-[#222222] bg-[#0a0a0a] rounded text-center">
            <p className="text-xs text-[#cccccc] mb-1">
              No calibration portrait uploaded for {person.name}.
            </p>
            <p className="text-[11px] text-[#666666]">
              Upload a clear single-face image to generate ground-truth 512-D embeddings.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
            {referenceImages.map((refImg) => {
              const refUrl = api.getReferenceImageUrl(sessionId, person.id, refImg.id);
              return (
                <div
                  key={refImg.id}
                  className="group relative border border-[#222222] bg-[#0a0a0a] rounded p-2.5 text-xs hover:border-[#444444] transition-colors"
                >
                  <div className="aspect-square bg-black rounded flex items-center justify-center overflow-hidden mb-2 relative border border-[#1a1a1a]">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={refUrl}
                      alt={refImg.originalFilename}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        (e.currentTarget as HTMLElement).style.display = "none";
                        const fallback = e.currentTarget.parentElement?.querySelector(".ref-fallback");
                        if (fallback) (fallback as HTMLElement).classList.remove("hidden");
                      }}
                    />
                    <div className="ref-fallback hidden w-full h-full flex items-center justify-center bg-[#0d0d0d]">
                      <User className="w-6 h-6 text-[#666666]" />
                    </div>
                  </div>
                  <p className="truncate text-[#cccccc] text-xs font-mono" title={refImg.originalFilename}>
                    {refImg.originalFilename}
                  </p>
                  <p className="text-[10px] text-[#666666] font-mono mt-0.5">
                    {formatBytes(refImg.fileSize)}
                  </p>
                  <button
                    onClick={() => handleDeleteReference(refImg.id)}
                    className="absolute top-1.5 right-1.5 p-1 bg-black/80 rounded text-[#888888] hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer border border-[#222222]"
                    title="Delete reference image"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Matched Photos Section */}
      <div className="space-y-4 pt-4 border-t border-[#1c1c1c]">
        <div className="flex items-center justify-between text-xs text-[#888888]">
          <span>{"// CLUSTERED EXPOSURES FOR IDENTITY:"} {person.name.toUpperCase()}</span>
          <span>COUNT: {photos.length}</span>
        </div>

        {isLoading ? (
          <div className="py-12 text-center text-xs text-[#888888]">
            SCANNING TENSOR CLUSTERS...
          </div>
        ) : photos.length === 0 ? (
          <div className="py-16 text-center max-w-sm mx-auto">
            <Camera className="w-6 h-6 text-[#333333] mx-auto mb-2" />
            <p className="text-xs text-[#cccccc] mb-1">
              Zero exposures matched
            </p>
            <p className="text-[11px] text-[#666666]">
              Upload group photos or manually bind unassigned faces.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            {photos.map((item, idx) => {
              const photoUrl = api.getPhotoUrl(sessionId, item.photoId);
              return (
                <div
                  key={`${item.photoId}-${idx}`}
                  className="group relative bg-[#0a0a0a] border border-[#222222] rounded overflow-hidden flex flex-col hover:border-[#555555] transition-all"
                >
                  <div className="aspect-[4/3] w-full bg-[#050505] overflow-hidden relative">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={photoUrl}
                      alt={item.originalFilename}
                      className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-102"
                      loading="lazy"
                    />

                    {item.similarityScore != null && (
                      <div className="absolute top-2 right-2 px-1.5 py-0.5 rounded bg-black/90 text-white border border-[#333333] text-[10px] font-mono">
                        SIM: {item.similarityScore.toFixed(3)}
                      </div>
                    )}
                  </div>
                  <div className="p-2.5 bg-[#0a0a0a] text-xs">
                    <p className="truncate text-[#cccccc]">
                      {item.originalFilename}
                    </p>
                    <p className="text-[10px] text-[#666666] mt-0.5">
                      {formatBytes(item.fileSize)}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

"use client";

import React, { useState } from "react";
import { Eye, Trash2, Camera, AlertCircle, Plus } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { PhotoListResponse } from "@/src/types/api";
import { api } from "@/src/lib/api/client";
import { StatusBadge } from "@/src/components/ui/badge";
import { Modal } from "@/src/components/ui/modal";
import { Button } from "@/src/components/ui/button";
import { formatBytes, formatDate } from "@/src/lib/utils/format";

interface PhotosViewProps {
  onOpenUpload: () => void;
}

export function PhotosView({ onOpenUpload }: PhotosViewProps) {
  const { sessionId, photos, jobStatuses, deletePhoto } = useSession();
  const [selectedPhoto, setSelectedPhoto] = useState<PhotoListResponse | null>(null);
  const [deletingPhotoId, setDeletingPhotoId] = useState<string | null>(null);
  const [photoToDelete, setPhotoToDelete] = useState<PhotoListResponse | null>(null);

  const confirmDelete = async () => {
    if (!photoToDelete) return;
    try {
      setDeletingPhotoId(photoToDelete.photoId);
      await deletePhoto(photoToDelete.photoId);
      if (selectedPhoto?.photoId === photoToDelete.photoId) {
        setSelectedPhoto(null);
      }
      setPhotoToDelete(null);
    } catch {
      // handled in context
    } finally {
      setDeletingPhotoId(null);
    }
  };

  if (photos.length === 0) {
    return (
      <div className="py-24 text-center max-w-md mx-auto px-4 font-mono">
        <div className="w-12 h-12 mx-auto mb-5 flex items-center justify-center bg-[#0a0a0a] border border-[#222222] rounded text-[#888888]">
          <Camera className="w-6 h-6 text-white" />
        </div>
        <h3 className="text-base font-bold text-white mb-2 font-mono">
          NO EXPOSURES FOUND
        </h3>
        <p className="text-xs text-[#888888] mb-6 leading-relaxed font-sans">
          Upload group photos. The asynchronous pipeline extracts and indexes detected faces automatically.
        </p>
        <Button onClick={onOpenUpload} size="md">
          <Plus className="w-4 h-4 mr-1.5" />
          Upload Exposures
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in font-mono">
      {/* Top stats bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#1c1c1c]">
        <div>
          <h2 className="text-base font-bold text-white">
            Exposures
          </h2>
          <p className="text-xs text-[#888888] mt-0.5">
            {"// count:"} {photos.length} photos registered
          </p>
        </div>
        <Button size="sm" onClick={onOpenUpload}>
          <Plus className="w-3.5 h-3.5 mr-1.5" />
          Upload Exposures
        </Button>
      </div>

      {/* Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
        {photos.map((photo) => {
          const photoUrl = api.getPhotoUrl(sessionId, photo.photoId);
          const job = jobStatuses[photo.photoId];

          return (
            <div
              key={photo.photoId}
              className="group relative bg-[#0a0a0a] border border-[#222222] rounded overflow-hidden flex flex-col transition-all duration-150 hover:border-[#555555]"
            >
              {/* Image Container with 4:3 Aspect Ratio */}
              <div
                className="aspect-[4/3] w-full bg-[#050505] overflow-hidden relative cursor-pointer"
                onClick={() => setSelectedPhoto(photo)}
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={photoUrl}
                  alt={photo.originalFilename}
                  className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-102"
                  loading="lazy"
                />

                {/* Status Badge Overlay */}
                {job && (
                  <div className="absolute top-2 left-2">
                    <StatusBadge status={job.status} />
                  </div>
                )}

                {/* Hover actions */}
                <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity duration-150 flex items-center justify-center gap-2">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedPhoto(photo);
                    }}
                    className="p-2 bg-white text-black rounded hover:bg-[#e0e0e0] transition-colors cursor-pointer"
                    title="Inspect Photo"
                  >
                    <Eye className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setPhotoToDelete(photo);
                    }}
                    className="p-2 bg-[#1a1a1a] text-red-400 border border-red-500/30 rounded hover:bg-red-950/40 transition-colors cursor-pointer"
                    title="Delete Photo"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Card Meta */}
              <div className="p-3 bg-[#0a0a0a] flex items-center justify-between text-xs">
                <span className="truncate text-[#cccccc]" title={photo.originalFilename}>
                  {photo.originalFilename}
                </span>
                <span className="text-[10px] text-[#666666] shrink-0 ml-2">
                  {formatBytes(photo.fileSize)}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Lightbox Modal */}
      {selectedPhoto && (
        <Modal
          isOpen={!!selectedPhoto}
          onClose={() => setSelectedPhoto(null)}
          title={selectedPhoto.originalFilename}
          maxWidth="4xl"
        >
          <div className="space-y-4 font-mono text-xs">
            <div className="bg-black rounded flex items-center justify-center min-h-[400px] max-h-[70vh] overflow-hidden border border-[#222222]">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={api.getPhotoUrl(sessionId, selectedPhoto.photoId)}
                alt={selectedPhoto.originalFilename}
                className="max-h-[70vh] max-w-full object-contain"
              />
            </div>
            <div className="flex flex-wrap items-center justify-between gap-4 pt-2 text-[#888888]">
              <div className="flex items-center gap-4">
                <span>SIZE: {formatBytes(selectedPhoto.fileSize)}</span>
                <span>MIME: {selectedPhoto.contentType}</span>
                <span>TS: {formatDate(selectedPhoto.createdAt)}</span>
              </div>
              <Button
                variant="danger"
                size="sm"
                onClick={() => setPhotoToDelete(selectedPhoto)}
              >
                <Trash2 className="w-3.5 h-3.5 mr-1.5" />
                Purge Exposure
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={!!photoToDelete}
        onClose={() => setPhotoToDelete(null)}
        title="Delete Photo"
        maxWidth="sm"
      >
        <div className="space-y-4 font-mono text-xs">
          <div className="flex items-start gap-3 text-[#ededed]">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              Delete <span className="text-white font-bold">{photoToDelete?.originalFilename}</span>? Associated faces and embeddings will also be removed.
            </p>
          </div>
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#222222]">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPhotoToDelete(null)}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={deletingPhotoId === photoToDelete?.photoId}
              onClick={confirmDelete}
            >
              Confirm Delete
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

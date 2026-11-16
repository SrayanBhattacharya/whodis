"use client";

import React, { useState } from "react";
import { Eye, Trash2, UsersRound, AlertCircle, Plus } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { PhotoListResponse } from "@/src/types/api";
import { api } from "@/src/lib/api/client";
import { Modal } from "@/src/components/ui/modal";
import { Button } from "@/src/components/ui/button";
import { formatBytes, formatDate } from "@/src/lib/utils/format";

interface GroupPhotosViewProps {
  onOpenUpload: () => void;
}

export function GroupPhotosView({ onOpenUpload }: GroupPhotosViewProps) {
  const { sessionId, groupPhotos, deletePhoto } = useSession();
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

  if (groupPhotos.length === 0) {
    return (
      <div className="py-24 text-center max-w-md mx-auto px-4 font-mono">
        <div className="w-12 h-12 mx-auto mb-5 flex items-center justify-center bg-[#0a0a0a] border border-[#222222] rounded text-[#888888]">
          <UsersRound className="w-6 h-6 text-white" />
        </div>
        <h3 className="text-base font-bold text-white mb-2 font-mono">
          NO GROUP EXPOSURES FOUND
        </h3>
        <p className="text-xs text-[#888888] mb-6 leading-relaxed font-sans">
          Photos containing 3 or more detected faces are automatically cataloged here as group photos and kept distinct from individual face classification.
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
            Group Photos
          </h2>
          <p className="text-xs text-[#888888] mt-0.5">
            {"// count:"} {groupPhotos.length} group exposures (3+ detected faces)
          </p>
        </div>
        <Button size="sm" onClick={onOpenUpload}>
          <Plus className="w-3.5 h-3.5 mr-1.5" />
          Upload Exposures
        </Button>
      </div>

      {/* Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
        {groupPhotos.map((photo) => {
          const photoUrl = api.getPhotoUrl(sessionId, photo.photoId);

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

                {/* Hover overlay with actions */}
                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedPhoto(photo);
                    }}
                    className="p-2 bg-black/80 rounded-full text-white hover:text-white transition-colors cursor-pointer border border-[#333333]"
                    title="View exposure"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setPhotoToDelete(photo);
                    }}
                    className="p-2 bg-black/80 rounded-full text-white hover:text-red-400 transition-colors cursor-pointer border border-[#333333]"
                    title="Delete exposure"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Metadata */}
              <div className="p-3 border-t border-[#1a1a1a] flex items-center justify-between text-xs">
                <div className="truncate pr-2">
                  <p className="truncate text-[#cccccc] font-mono" title={photo.originalFilename}>
                    {photo.originalFilename}
                  </p>
                  <p className="text-[10px] text-[#666666] font-mono mt-0.5">
                    {formatBytes(photo.fileSize)} • {formatDate(photo.createdAt)}
                  </p>
                </div>

                <button
                  onClick={() => setPhotoToDelete(photo)}
                  className="text-[#666666] hover:text-red-400 transition-colors p-1 cursor-pointer sm:opacity-0 group-hover:opacity-100"
                  title="Delete exposure"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Lightbox / Inspector Modal */}
      {selectedPhoto && (
        <Modal
          isOpen={!!selectedPhoto}
          onClose={() => setSelectedPhoto(null)}
          title={selectedPhoto.originalFilename}
          maxWidth="4xl"
        >
          <div className="space-y-4">
            <div className="relative aspect-[16/10] bg-black rounded overflow-hidden flex items-center justify-center border border-[#1c1c1c]">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={api.getPhotoUrl(sessionId, selectedPhoto.photoId)}
                alt={selectedPhoto.originalFilename}
                className="max-h-full max-w-full object-contain"
              />
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-3 bg-[#0a0a0a] border border-[#222222] rounded font-mono text-xs">
              <div>
                <span className="text-[#666666] block text-[10px]">CATEGORY</span>
                <span className="text-[#ededed] font-medium">Group Exposure</span>
              </div>
              <div>
                <span className="text-[#666666] block text-[10px]">PAYLOAD SIZE</span>
                <span className="text-[#ededed]">{formatBytes(selectedPhoto.fileSize)}</span>
              </div>
              <div>
                <span className="text-[#666666] block text-[10px]">MIME TYPE</span>
                <span className="text-[#ededed]">{selectedPhoto.contentType}</span>
              </div>
              <div>
                <span className="text-[#666666] block text-[10px]">REGISTERED AT</span>
                <span className="text-[#ededed]">{formatDate(selectedPhoto.createdAt)}</span>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <Button
                variant="danger"
                size="sm"
                onClick={() => {
                  setPhotoToDelete(selectedPhoto);
                }}
              >
                <Trash2 className="w-3.5 h-3.5 mr-1.5" />
                Delete Exposure
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Delete Confirmation Modal */}
      {photoToDelete && (
        <Modal
          isOpen={!!photoToDelete}
          onClose={() => setPhotoToDelete(null)}
          title="CONFIRM DELETION"
          maxWidth="md"
        >
          <div className="space-y-4 font-mono">
            <div className="flex items-start gap-3 p-3 bg-red-950/20 border border-red-900/30 rounded text-xs text-red-200">
              <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-bold text-white mb-1">
                  Permanent Removal
                </p>
                <p className="text-[#888888] font-sans">
                  Are you sure you want to delete <span className="font-mono text-white">{photoToDelete.originalFilename}</span>? This will permanently remove the image and any detected face vectors from the session.
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPhotoToDelete(null)}
                disabled={!!deletingPhotoId}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={confirmDelete}
                isLoading={!!deletingPhotoId}
              >
                Delete Exposure
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

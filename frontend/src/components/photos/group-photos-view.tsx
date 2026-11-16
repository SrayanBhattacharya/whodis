"use client";

import React, { useState } from "react";
import { Eye, Trash2, UsersRound, AlertCircle, Plus, Download, CheckSquare, Square } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { PhotoListResponse } from "@/src/types/api";
import { api } from "@/src/lib/api/client";
import { Modal } from "@/src/components/ui/modal";
import { Button } from "@/src/components/ui/button";
import { formatBytes, formatDate } from "@/src/lib/utils/format";
import { downloadFile } from "@/src/lib/utils/download";

interface GroupPhotosViewProps {
  onOpenUpload: () => void;
}

export function GroupPhotosView({ onOpenUpload }: GroupPhotosViewProps) {
  const { sessionId, groupPhotos, deletePhoto } = useSession();
  const [selectedPhoto, setSelectedPhoto] = useState<PhotoListResponse | null>(null);
  const [deletingPhotoId, setDeletingPhotoId] = useState<string | null>(null);
  const [photoToDelete, setPhotoToDelete] = useState<PhotoListResponse | null>(null);

  // Selection & bulk download state
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<Set<string>>(new Set());
  const [isDownloadingBulk, setIsDownloadingBulk] = useState(false);
  const [downloadingPhotoId, setDownloadingPhotoId] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const toggleSelectPhoto = (photoId: string) => {
    setSelectedPhotoIds((prev) => {
      const next = new Set(prev);
      if (next.has(photoId)) {
        next.delete(photoId);
      } else {
        next.add(photoId);
      }
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedPhotoIds.size === groupPhotos.length) {
      setSelectedPhotoIds(new Set());
    } else {
      setSelectedPhotoIds(new Set(groupPhotos.map((p) => p.photoId)));
    }
  };

  const handleDownloadSingle = async (photo: PhotoListResponse) => {
    if (downloadingPhotoId) return;
    try {
      setDownloadingPhotoId(photo.photoId);
      setDownloadError(null);
      await downloadFile(
        api.getPhotoDownloadUrl(sessionId, photo.photoId),
        {},
        photo.originalFilename
      );
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to download photo";
      setDownloadError(msg);
    } finally {
      setDownloadingPhotoId(null);
    }
  };

  const handleDownloadBulk = async () => {
    if (selectedPhotoIds.size === 0 || isDownloadingBulk) return;
    try {
      setIsDownloadingBulk(true);
      setDownloadError(null);
      await downloadFile(
        `/api/backend/sessions/${sessionId}/photos/download`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ photoIds: Array.from(selectedPhotoIds) }),
        },
        "whodis-group-photos.zip"
      );
      setSelectedPhotoIds(new Set());
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to download photos archive";
      setDownloadError(msg);
    } finally {
      setIsDownloadingBulk(false);
    }
  };

  const confirmDelete = async () => {
    if (!photoToDelete) return;
    try {
      setDeletingPhotoId(photoToDelete.photoId);
      await deletePhoto(photoToDelete.photoId);
      if (selectedPhoto?.photoId === photoToDelete.photoId) {
        setSelectedPhoto(null);
      }
      setSelectedPhotoIds((prev) => {
        const next = new Set(prev);
        next.delete(photoToDelete.photoId);
        return next;
      });
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
      {/* Top stats & action bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#1c1c1c]">
        <div>
          <h2 className="text-base font-bold text-white">
            Group Photos
          </h2>
          <p className="text-xs text-[#888888] mt-0.5">
            {"// count:"} {groupPhotos.length} group exposures (3+ detected faces)
            {selectedPhotoIds.size > 0 && (
              <span className="text-white ml-2 font-bold">
                • {selectedPhotoIds.size} selected
              </span>
            )}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {groupPhotos.length > 0 && (
            <Button
              variant="secondary"
              size="sm"
              onClick={toggleSelectAll}
              className="text-xs"
            >
              {selectedPhotoIds.size === groupPhotos.length ? (
                <>
                  <Square className="w-3.5 h-3.5 mr-1.5" />
                  Deselect All
                </>
              ) : (
                <>
                  <CheckSquare className="w-3.5 h-3.5 mr-1.5" />
                  Select All
                </>
              )}
            </Button>
          )}

          <Button
            variant="secondary"
            size="sm"
            onClick={handleDownloadBulk}
            disabled={selectedPhotoIds.size === 0 || isDownloadingBulk}
            isLoading={isDownloadingBulk}
            className="text-xs"
          >
            <Download className="w-3.5 h-3.5 mr-1.5" />
            Download Selected ({selectedPhotoIds.size})
          </Button>

          <Button size="sm" onClick={onOpenUpload}>
            <Plus className="w-3.5 h-3.5 mr-1.5" />
            Upload Exposures
          </Button>
        </div>
      </div>

      {downloadError && (
        <div className="p-3 bg-red-950/20 border border-red-900/30 rounded flex items-center justify-between text-xs text-red-200">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
            <span>[error] {downloadError}</span>
          </div>
          <button
            onClick={() => setDownloadError(null)}
            className="text-red-400 hover:text-white ml-4 font-mono text-[10px]"
          >
            [DISMISS]
          </button>
        </div>
      )}

      {/* Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
        {groupPhotos.map((photo) => {
          const photoUrl = api.getPhotoUrl(sessionId, photo.photoId);
          const isSelected = selectedPhotoIds.has(photo.photoId);

          return (
            <div
              key={photo.photoId}
              className={`group relative bg-[#0a0a0a] border rounded overflow-hidden flex flex-col transition-all duration-150 ${
                isSelected
                  ? "border-white ring-1 ring-white"
                  : "border-[#222222] hover:border-[#555555]"
              }`}
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

                {/* Selection Checkbox */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleSelectPhoto(photo.photoId);
                  }}
                  className={`absolute top-2 right-2 p-1.5 rounded transition-all cursor-pointer ${
                    isSelected
                      ? "bg-white text-black opacity-100"
                      : "bg-black/60 text-white opacity-0 group-hover:opacity-100 hover:bg-black"
                  }`}
                  title={isSelected ? "Deselect" : "Select"}
                >
                  {isSelected ? (
                    <CheckSquare className="w-4 h-4" />
                  ) : (
                    <Square className="w-4 h-4" />
                  )}
                </button>

                {/* Hover overlay with actions */}
                <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2 pointer-events-none">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedPhoto(photo);
                    }}
                    className="p-2 bg-white text-black rounded hover:bg-[#e0e0e0] transition-colors cursor-pointer pointer-events-auto"
                    title="View exposure"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDownloadSingle(photo);
                    }}
                    disabled={downloadingPhotoId === photo.photoId}
                    className="p-2 bg-[#1a1a1a] text-white border border-[#333333] rounded hover:bg-[#2a2a2a] transition-colors cursor-pointer pointer-events-auto"
                    title="Download Original Photo"
                  >
                    <Download className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setPhotoToDelete(photo);
                    }}
                    className="p-2 bg-[#1a1a1a] text-red-400 border border-red-500/30 rounded hover:bg-red-950/40 transition-colors cursor-pointer pointer-events-auto"
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
                variant="secondary"
                size="sm"
                onClick={() => handleDownloadSingle(selectedPhoto)}
                isLoading={downloadingPhotoId === selectedPhoto.photoId}
              >
                <Download className="w-3.5 h-3.5 mr-1.5" />
                Download Original
              </Button>
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

"use client";

import React, { useState, useRef, DragEvent, ChangeEvent } from "react";
import { Upload, X, CheckCircle, AlertCircle } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { api } from "@/src/lib/api/client";
import { Button } from "@/src/components/ui/button";
import { formatBytes } from "@/src/lib/utils/format";

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface QueuedFile {
  id: string;
  file: File;
  previewUrl: string;
  status: "ready" | "uploading" | "success" | "error";
  errorMessage?: string;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];

export function PhotoUploadModal({ isOpen, onClose }: UploadModalProps) {
  const { sessionId, refreshPhotos } = useSession();
  const [queuedFiles, setQueuedFiles] = useState<QueuedFile[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFiles = (files: FileList | File[]) => {
    const newItems: QueuedFile[] = [];

    Array.from(files).forEach((file) => {
      let status: QueuedFile["status"] = "ready";
      let errorMessage: string | undefined;

      if (!ALLOWED_TYPES.includes(file.type)) {
        status = "error";
        errorMessage = "JPEG, PNG, or WebP only";
      } else if (file.size > MAX_FILE_SIZE) {
        status = "error";
        errorMessage = "Exceeds 10MB limit";
      }

      newItems.push({
        id: `${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
        file,
        previewUrl: URL.createObjectURL(file),
        status,
        errorMessage,
      });
    });

    setQueuedFiles((prev) => [...prev, ...newItems]);
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleFiles(e.target.files);
    }
  };

  const removeFile = (id: string) => {
    setQueuedFiles((prev) => {
      const target = prev.find((item) => item.id === id);
      if (target?.previewUrl) {
        URL.revokeObjectURL(target.previewUrl);
      }
      return prev.filter((item) => item.id !== id);
    });
  };

  const handleStartUpload = async () => {
    const validItems = queuedFiles.filter((item) => item.status === "ready");
    if (validItems.length === 0) return;

    try {
      setIsUploading(true);

      setQueuedFiles((prev) =>
        prev.map((item) =>
          item.status === "ready" ? { ...item, status: "uploading" } : item
        )
      );

      const filesToUpload = validItems.map((item) => item.file);
      await api.uploadPhotos(sessionId, filesToUpload);

      setQueuedFiles((prev) =>
        prev.map((item) =>
          item.status === "uploading" ? { ...item, status: "success" } : item
        )
      );

      await refreshPhotos();

      setTimeout(() => {
        onClose();
        setQueuedFiles([]);
      }, 1000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Upload failed";
      setQueuedFiles((prev) =>
        prev.map((item) =>
          item.status === "uploading"
            ? { ...item, status: "error", errorMessage: msg }
            : item
        )
      );
    } finally {
      setIsUploading(false);
    }
  };

  const readyCount = queuedFiles.filter((f) => f.status === "ready").length;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/80 backdrop-blur-sm animate-fade-in font-mono text-xs">
      <div className="relative w-full max-w-2xl bg-[#0a0a0a] border border-[#222222] rounded shadow-2xl flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#1c1c1c]">
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">
              Batch Exposure Ingest
            </h3>
            <p className="text-[11px] text-[#888888] mt-0.5">
              multipart/form-data payload queued to Redis Streams
            </p>
          </div>
          <button
            onClick={onClose}
            disabled={isUploading}
            className="p-1 text-[#888888] hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {/* Drop Zone */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border border-dashed rounded p-8 text-center cursor-pointer transition-all ${
              isDragging
                ? "border-white bg-[#141414]"
                : "border-[#222222] hover:border-[#444444] bg-black"
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp"
              onChange={handleInputChange}
              className="hidden"
            />
            <Upload className="w-6 h-6 mx-auto text-white mb-3" />
            <p className="text-xs font-bold text-white uppercase tracking-wider">
              Drag & Drop Images or Click to Select
            </p>
            <p className="text-[10px] text-[#666666] mt-1 font-mono uppercase">
              JPEG, PNG, WEBP // MAX 10MB PER FILE
            </p>
          </div>

          {/* Queued List */}
          {queuedFiles.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center justify-between text-[#888888]">
                <span>PAYLOAD QUEUE ({queuedFiles.length})</span>
                <button
                  onClick={() => setQueuedFiles([])}
                  disabled={isUploading}
                  className="hover:text-white transition-colors cursor-pointer"
                >
                  CLEAR
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-60 overflow-y-auto p-1">
                {queuedFiles.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center gap-3 p-2 border border-[#1c1c1c] bg-black rounded"
                  >
                    <div className="w-10 h-10 bg-[#111111] rounded shrink-0 overflow-hidden relative">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={item.previewUrl}
                        alt={item.file.name}
                        className="w-full h-full object-cover"
                      />
                    </div>

                    <div className="min-w-0 flex-1">
                      <p className="truncate text-xs text-white">
                        {item.file.name}
                      </p>
                      <p className="text-[10px] text-[#666666]">
                        {formatBytes(item.file.size)}
                      </p>
                      {item.errorMessage && (
                        <p className="text-[10px] text-red-400 truncate mt-0.5">
                          {item.errorMessage}
                        </p>
                      )}
                    </div>

                    <div className="shrink-0">
                      {item.status === "uploading" && (
                        <span className="text-white text-[10px] animate-pulse">
                          STREAMING
                        </span>
                      )}
                      {item.status === "success" && (
                        <CheckCircle className="w-4 h-4 text-white" />
                      )}
                      {item.status === "error" && (
                        <AlertCircle className="w-4 h-4 text-red-400" />
                      )}
                      {item.status === "ready" && !isUploading && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            removeFile(item.id);
                          }}
                          className="p-1 text-[#666666] hover:text-white cursor-pointer"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-[#1c1c1c] bg-black">
          <p className="text-[#888888] text-[11px]">
            {readyCount > 0 ? `${readyCount} EXPOSURES READY` : "QUEUE EMPTY"}
          </p>
          <div className="flex items-center gap-3">
            <Button
              variant="secondary"
              size="sm"
              disabled={isUploading}
              onClick={onClose}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              disabled={readyCount === 0 || isUploading}
              isLoading={isUploading}
              onClick={handleStartUpload}
            >
              Push to Queue {readyCount > 0 ? `(${readyCount})` : ""}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

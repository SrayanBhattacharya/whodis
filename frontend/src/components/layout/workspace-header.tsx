"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Users, Image as ImageIcon, Sparkles, Clock, AlertCircle, Plus } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { formatTimeRemaining } from "@/src/lib/utils/format";
import { Modal } from "@/src/components/ui/modal";
import { Button } from "@/src/components/ui/button";

interface HeaderProps {
  currentTab: "photos" | "people" | "unmatched";
  onTabChange: (tab: "photos" | "people" | "unmatched") => void;
  onOpenUpload: () => void;
}

export function WorkspaceHeader({
  currentTab,
  onTabChange,
  onOpenUpload,
}: HeaderProps) {
  const { session, photos, people, unmatchedFaces, deleteAllPhotos } = useSession();
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [isClearing, setIsClearing] = useState(false);

  const timeRemaining = session ? formatTimeRemaining(session.expiresAt) : null;

  const handleClearAll = async () => {
    try {
      setIsClearing(true);
      await deleteAllPhotos();
      setShowClearConfirm(false);
    } finally {
      setIsClearing(false);
    }
  };

  return (
    <>
      <header className="sticky top-0 z-40 w-full border-b border-[#222222] bg-black/90 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Left: Branding & Session Info */}
            <div className="flex items-center gap-6">
              <Link href="/" className="group">
                <span className="text-lg font-bold tracking-tight text-white group-hover:text-[#cccccc] transition-colors">
                  WhoDis
                </span>
              </Link>

              {timeRemaining && (
                <div className="hidden md:flex items-center gap-2 text-xs text-[#888888] border-l border-[#222222] pl-6 font-mono">
                  <Clock className="w-3.5 h-3.5 text-white" />
                  <span>{timeRemaining.formatted}</span>
                </div>
              )}
            </div>

            {/* Center: Tabs */}
            <nav className="flex items-center gap-1 sm:gap-2 text-xs font-mono">
              <button
                onClick={() => onTabChange("photos")}
                className={`flex items-center gap-2 px-3 py-1.5 rounded transition-all cursor-pointer ${
                  currentTab === "photos"
                    ? "bg-[#1f1f1f] text-white border border-[#333333]"
                    : "text-[#888888] hover:text-white"
                }`}
              >
                <ImageIcon className="w-3.5 h-3.5" />
                <span>PHOTOS</span>
                <span className="text-[11px] px-1.5 py-0.2 rounded bg-black text-[#888888] border border-[#222222]">
                  {photos.length}
                </span>
              </button>

              <button
                onClick={() => onTabChange("people")}
                className={`flex items-center gap-2 px-3 py-1.5 rounded transition-all cursor-pointer ${
                  currentTab === "people"
                    ? "bg-[#1f1f1f] text-white border border-[#333333]"
                    : "text-[#888888] hover:text-white"
                }`}
              >
                <Users className="w-3.5 h-3.5" />
                <span>PEOPLE</span>
                <span className="text-[11px] px-1.5 py-0.2 rounded bg-black text-[#888888] border border-[#222222]">
                  {people.length}
                </span>
              </button>

              <button
                onClick={() => onTabChange("unmatched")}
                className={`flex items-center gap-2 px-3 py-1.5 rounded transition-all cursor-pointer ${
                  currentTab === "unmatched"
                    ? "bg-[#1f1f1f] text-white border border-[#333333]"
                    : "text-[#888888] hover:text-white"
                }`}
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>UNMATCHED</span>
                {unmatchedFaces.length > 0 && (
                  <span className="text-[11px] px-1.5 py-0.2 rounded bg-white text-black font-bold">
                    {unmatchedFaces.length}
                  </span>
                )}
              </button>
            </nav>

            {/* Right: Actions */}
            <div className="flex items-center gap-3 font-mono">
              {photos.length > 0 && (
                <button
                  onClick={() => setShowClearConfirm(true)}
                  className="hidden sm:inline-block text-xs text-[#888888] hover:text-red-400 transition-colors px-2 py-1 cursor-pointer"
                >
                  CLEAR ALL
                </button>
              )}
              <Button size="sm" onClick={onOpenUpload}>
                <Plus className="w-3.5 h-3.5 mr-1" />
                Upload Photos
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Clear All Confirmation Modal */}
      <Modal
        isOpen={showClearConfirm}
        onClose={() => setShowClearConfirm(false)}
        title="Clear All Photographs"
        maxWidth="sm"
      >
        <div className="space-y-4 font-mono text-xs">
          <div className="flex items-start gap-3 text-[#ededed]">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              Purge all {photos.length} photos and face indexes in this session? This action cannot be reversed.
            </p>
          </div>
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#222222]">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setShowClearConfirm(false)}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={isClearing}
              onClick={handleClearAll}
            >
              Confirm Purge
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

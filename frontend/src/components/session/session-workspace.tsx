"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ArrowRight, RefreshCw, AlertTriangle } from "lucide-react";
import { SessionProvider, useSession } from "@/src/components/session/session-context";
import { WorkspaceHeader } from "@/src/components/layout/workspace-header";
import { PhotosView } from "@/src/components/photos/photos-view";
import { PeopleView } from "@/src/components/people/people-view";
import { UnmatchedFacesView } from "@/src/components/photos/unmatched-faces-view";
import { PhotoUploadModal } from "@/src/components/upload/upload-modal";
import { Button } from "@/src/components/ui/button";

function WorkspaceContent() {
  const { isLoading, isExpired, error, refreshAll } = useSession();
  const [currentTab, setCurrentTab] = useState<"photos" | "people" | "unmatched">("photos");
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-black flex flex-col items-center justify-center p-6 text-center text-[#ededed] font-mono">
        <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-sm font-bold tracking-wider text-white">
          SYNCHRONIZING ENGINE
        </p>
        <p className="text-xs text-[#666666] mt-1">
          Pulling relational clusters & pending stream offsets
        </p>
      </div>
    );
  }

  if (isExpired) {
    return (
      <div className="min-h-screen bg-black text-[#ededed] flex flex-col items-center justify-center p-6 text-center max-w-md mx-auto font-mono">
        <div className="w-12 h-12 bg-[#0a0a0a] border border-[#222222] rounded flex items-center justify-center text-white mb-6">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold tracking-tight text-white mb-2">
          SESSION EXPIRED [48H TTL]
        </h2>
        <p className="text-xs text-[#888888] mb-8 leading-relaxed font-sans">
          WhoDis sessions automatically clean up after 48 hours. Stored files, embeddings, and database rows have been purged.
        </p>
        <Link href="/">
          <Button size="lg" className="font-mono text-xs">
            Start New Session
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
        </Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-black text-[#ededed] flex flex-col items-center justify-center p-6 text-center max-w-md mx-auto font-mono">
        <div className="w-12 h-12 bg-[#0a0a0a] border border-red-500/30 rounded flex items-center justify-center text-red-400 mb-6">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold tracking-tight text-white mb-2">
          SESSION UNREACHABLE
        </h2>
        <p className="text-xs text-[#888888] mb-6 leading-relaxed font-sans">
          {error}
        </p>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={() => refreshAll()}>
            <RefreshCw className="w-3.5 h-3.5 mr-2" />
            Retry
          </Button>
          <Link href="/">
            <Button>Return Home</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black text-[#ededed] flex flex-col">
      <WorkspaceHeader
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        onOpenUpload={() => setIsUploadOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-8 py-8 sm:py-10">
        {currentTab === "photos" && (
          <PhotosView onOpenUpload={() => setIsUploadOpen(true)} />
        )}
        {currentTab === "people" && <PeopleView />}
        {currentTab === "unmatched" && <UnmatchedFacesView />}
      </main>

      <PhotoUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
      />
    </div>
  );
}

export function SessionWorkspace({ sessionId }: { sessionId: string }) {
  return (
    <SessionProvider sessionId={sessionId}>
      <WorkspaceContent />
    </SessionProvider>
  );
}

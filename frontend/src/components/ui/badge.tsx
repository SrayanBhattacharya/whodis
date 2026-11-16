import React from "react";
import { ProcessingJobStatus } from "@/src/types/api";

interface BadgeProps {
  status: ProcessingJobStatus | "EXPIRED" | "ACTIVE";
}

export function StatusBadge({ status }: BadgeProps) {
  switch (status) {
    case "PENDING":
      return (
        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 text-[11px] font-mono rounded bg-black/80 text-[#888888] border border-[#333333] backdrop-blur-sm">
          <span className="w-1.5 h-1.5 rounded-full bg-[#888888] animate-pulse" />
          QUEUED
        </span>
      );
    case "PROCESSING":
      return (
        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 text-[11px] font-mono rounded bg-black/80 text-white border border-[#444444] backdrop-blur-sm">
          <span className="w-1.5 h-1.5 rounded-full bg-white animate-spin" />
          ANALYZING
        </span>
      );
    case "COMPLETED":
      return (
        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 text-[11px] font-mono rounded bg-black/80 text-white border border-[#333333] backdrop-blur-sm">
          <span className="w-1.5 h-1.5 rounded-full bg-white" />
          INDEXED
        </span>
      );
    case "FAILED":
      return (
        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 text-[11px] font-mono rounded bg-black/80 text-red-400 border border-red-500/30 backdrop-blur-sm">
          <span className="w-1.5 h-1.5 rounded-full bg-red-400" />
          FAILED
        </span>
      );
    case "ACTIVE":
      return (
        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 text-[11px] font-mono rounded bg-black/80 text-white border border-[#333333] backdrop-blur-sm">
          <span className="w-1.5 h-1.5 rounded-full bg-white" />
          ACTIVE
        </span>
      );
    case "EXPIRED":
      return (
        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 text-[11px] font-mono rounded bg-black/80 text-[#666666] border border-[#222222] backdrop-blur-sm">
          EXPIRED
        </span>
      );
    default:
      return null;
  }
}

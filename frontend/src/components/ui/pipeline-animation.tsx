"use client";

import React, { useEffect, useState } from "react";
import { Cpu, Database, Eye, Layers, CheckCircle2 } from "lucide-react";

export function PipelineAnimation() {
  const [activeStep, setActiveStep] = useState(0);
  const [detectedFaces, setDetectedFaces] = useState(0);
  const [assignedCount, setAssignedCount] = useState(0);

  const steps = [
    {
      id: "ingest",
      title: "RAW INGEST",
      code: "XADD whodis:stream",
      desc: "Multipart stream queued to Redis broker",
      icon: Layers,
    },
    {
      id: "detect",
      title: "FACE DETECTION",
      code: "ONNX Buffalo_L",
      desc: "Sub-pixel bounding box localization",
      icon: Eye,
    },
    {
      id: "embed",
      title: "VECTOR EMBEDDING",
      code: "dim: 512 float32",
      desc: "L2-normalized ArcFace embedding",
      icon: Cpu,
    },
    {
      id: "match",
      title: "COSINE CLUSTERING",
      code: "sim >= 0.51 margin >= 0.05",
      desc: "Assigned to participant identity",
      icon: Database,
    },
  ];

  useEffect(() => {
    const interval = setInterval(() => {
      setActiveStep((prev) => {
        const next = (prev + 1) % steps.length;
        if (next === 1) {
          setDetectedFaces((f) => ((f % 6) + 1));
        }
        if (next === 3) {
          setAssignedCount((c) => c + 1);
        }
        return next;
      });
    }, 1800);

    return () => clearInterval(interval);
  }, [steps.length]);

  return (
    <div className="w-full border border-[#222222] bg-[#0a0a0a] rounded-lg overflow-hidden p-6 sm:p-8 font-mono">
      {/* Terminal Titlebar */}
      <div className="flex items-center justify-between pb-6 border-b border-[#1c1c1c] text-xs text-[#888888]">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#333333]" />
          <div className="w-2.5 h-2.5 rounded-full bg-[#333333]" />
          <div className="w-2.5 h-2.5 rounded-full bg-[#333333]" />
          <span className="ml-2 text-white font-medium">pipeline.worker.daemon</span>
        </div>
        <div className="flex items-center gap-4 text-[11px]">
          <span>fps: 60</span>
          <span>model: buffalo_l/2.0</span>
          <span className="text-emerald-400 flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            ONLINE
          </span>
        </div>
      </div>

      {/* Pipeline Stages Flow */}
      <div className="py-8 grid grid-cols-1 md:grid-cols-4 gap-4 sm:gap-6 relative">
        {steps.map((s, idx) => {
          const Icon = s.icon;
          const isCurrent = activeStep === idx;
          const isPassed = activeStep > idx;

          return (
            <div
              key={s.id}
              className={`p-4 rounded border transition-all duration-300 relative ${
                isCurrent
                  ? "border-white bg-[#141414] shadow-[0_0_20px_rgba(255,255,255,0.05)]"
                  : isPassed
                  ? "border-[#262626] bg-[#0d0d0d] text-[#666666]"
                  : "border-[#1c1c1c] bg-[#080808] opacity-50"
              }`}
            >
              {/* Step indicator */}
              <div className="flex items-center justify-between mb-3 text-[10px]">
                <span className={isCurrent ? "text-white font-bold" : "text-[#666666]"}>
                  STAGE 0{idx + 1}
                </span>
                {isPassed && <CheckCircle2 className="w-3.5 h-3.5 text-white" />}
                {isCurrent && (
                  <span className="px-1.5 py-0.2 rounded bg-white text-black text-[9px] font-bold animate-pulse">
                    ACTIVE
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2 mb-2">
                <Icon className={`w-4 h-4 ${isCurrent ? "text-white" : "text-[#888888]"}`} />
                <h4 className={`text-xs font-bold tracking-tight ${isCurrent ? "text-white" : "text-[#aaaaaa]"}`}>
                  {s.title}
                </h4>
              </div>

              <p className="text-[10px] text-[#888888] mb-2 leading-relaxed font-sans">
                {s.desc}
              </p>

              <div className="text-[10px] px-2 py-1 bg-black rounded border border-[#222222] text-[#aaaaaa] truncate">
                {s.code}
              </div>
            </div>
          );
        })}
      </div>

      {/* Live Pipeline Telemetry Log */}
      <div className="pt-4 border-t border-[#1c1c1c] flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs text-[#888888]">
        <div className="flex items-center gap-3">
          <span className="text-white">[$]</span>
          <span className="text-[#cccccc]">
            {activeStep === 0 && "INGESTING: Batch job partitioned -> Stream offset $"}
            {activeStep === 1 && `INSPECTING: Detected ${detectedFaces} bounding box coordinates [x1, y1, x2, y2]`}
            {activeStep === 2 && "EXTRACTING: 512-dim ArcFace feature tensor computed"}
            {activeStep === 3 && `MATCHING: Cosine similarity validated -> Person album clustered (+${assignedCount})`}
          </span>
        </div>
        <div className="text-[11px] text-[#666666] shrink-0">
          latency: ~180ms/img
        </div>
      </div>
    </div>
  );
}

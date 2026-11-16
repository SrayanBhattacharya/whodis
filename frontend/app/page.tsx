"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, Terminal, Cpu, Database } from "lucide-react";
import { api } from "@/src/lib/api/client";
import { sessionStore } from "@/src/lib/utils/session-store";
import { Button } from "@/src/components/ui/button";
import { PipelineAnimation } from "@/src/components/ui/pipeline-animation";

export default function LandingPage() {
  const router = useRouter();
  const [isCreating, setIsCreating] = useState(false);
  const [existingSessionId] = useState<string | null>(() => {
    return sessionStore.get();
  });
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleStartSession = async () => {
    try {
      setIsCreating(true);
      setErrorMessage(null);
      const session = await api.createSession();
      sessionStore.set(session.id);
      router.push(`/session/${session.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to start session";
      setErrorMessage(msg);
      setIsCreating(false);
    }
  };

  const specs = [
    { label: "MODEL", val: "InsightFace Buffalo_L" },
    { label: "EMBEDDING", val: "512-dim IEEE-754 float32" },
    { label: "SIMILARITY", val: "Cosine Dot Product (L2 Norm)" },
    { label: "MATCH THRESHOLD", val: ">= 0.51 score (min margin 0.05)" },
    { label: "QUEUE BROKER", val: "Redis Streams + PEL Claiming" },
    { label: "LIFECYCLE", val: "48-Hour TTL Auto-Purge" },
  ];

  return (
    <div className="min-h-screen bg-black text-[#ededed] flex flex-col justify-between selection:bg-white selection:text-black">
      {/* Vercel-Style Monochrome Technical Header */}
      <header className="w-full px-6 sm:px-12 py-5 flex items-center justify-between border-b border-[#1c1c1c] sticky top-0 z-40 bg-black/85 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <Link href="/" className="group">
            <span className="font-bold text-xl tracking-tight text-white group-hover:text-[#cccccc] transition-colors">
              WhoDis
            </span>
          </Link>
        </div>

        <nav className="flex items-center gap-4 sm:gap-6 font-mono text-xs">
          {existingSessionId && (
            <Link
              href={`/session/${existingSessionId}`}
              className="text-[#888888] hover:text-white transition-colors flex items-center gap-1"
            >
              RESUME [active] <ArrowRight className="w-3.5 h-3.5 ml-0.5" />
            </Link>
          )}
          <Button
            size="sm"
            onClick={handleStartSession}
            isLoading={isCreating}
          >
            Start Engine
          </Button>
        </nav>
      </header>

      {/* Hero Section: Monolithic Vercel Typography & Rulers */}
      <main className="flex-1">
        <section className="px-6 sm:px-12 pt-16 sm:pt-28 pb-16 max-w-7xl mx-auto relative">
          {/* Subtle Technical Grid Lines */}
          <div className="absolute inset-0 tech-grid opacity-15 pointer-events-none" />

          <div className="relative z-10 max-w-5xl space-y-6">
            {/* Pill */}
            <div className="inline-flex items-center gap-2 px-3 py-1 text-xs font-mono border border-[#222222] bg-[#0a0a0a] rounded-full text-[#888888]">
              <span className="w-2 h-2 rounded-full bg-white" />
              <span>Version 2.0 • Asynchronous Face Ingestion</span>
            </div>

            {/* Giant Vercel-Style Heading */}
            <h1 className="text-[14vw] sm:text-[11vw] font-black uppercase tracking-tight text-white font-display leading-[0.88] select-none">
              EVERY FACE <br />
              HAS A PLACE.
            </h1>

            <p className="text-base sm:text-xl text-[#888888] font-normal max-w-2xl leading-relaxed">
              WhoDis organizes group photographs around recognized individuals using 512-dimensional vector clustering. No accounts. No persistent data.
            </p>

            <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-3 max-w-md font-mono">
              <Button
                size="lg"
                onClick={handleStartSession}
                isLoading={isCreating}
                className="text-xs font-bold tracking-wider"
              >
                INITIALIZE SESSION
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
              {existingSessionId && (
                <Link href={`/session/${existingSessionId}`}>
                  <Button variant="secondary" size="lg" className="w-full text-xs font-mono">
                    RESUME ACTIVE
                  </Button>
                </Link>
              )}
            </div>

            {errorMessage && (
              <p className="text-xs text-red-400 font-mono mt-2">
                [error] {errorMessage}
              </p>
            )}
          </div>

          {/* Interactive Live Pipeline Component */}
          <div className="mt-20 relative z-10">
            <div className="mb-4 flex items-center justify-between text-xs font-mono text-[#888888]">
              <span>{"// PIPELINE TELEMETRY DEMONSTRATION"}</span>
              <span>STATE: STREAMING</span>
            </div>
            <PipelineAnimation />
          </div>

          {/* Technical Spec Matrix (Vercel ruler style) */}
          <div className="mt-20 pt-8 border-t border-[#1c1c1c] grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-6 font-mono text-xs">
            {specs.map((s) => (
              <div key={s.label} className="space-y-1">
                <span className="text-[10px] text-[#666666] uppercase tracking-wider block">
                  {s.label}
                </span>
                <span className="text-white text-xs block truncate" title={s.val}>
                  {s.val}
                </span>
              </div>
            ))}
          </div>
        </section>

        {/* Section: Architectural Overview (Nerdy Specs) */}
        <section className="border-t border-[#1c1c1c] bg-[#050505] py-20 px-6 sm:px-12 font-mono">
          <div className="max-w-7xl mx-auto">
            <div className="flex flex-col sm:flex-row sm:items-baseline justify-between gap-4 mb-12 border-b border-[#1c1c1c] pb-6">
              <div>
                <span className="text-xs text-[#888888] tracking-widest uppercase block mb-1">
                  CORE ARCHITECTURE
                </span>
                <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white font-sans">
                  System Specifications
                </h2>
              </div>
              <span className="text-xs text-[#666666]">
                POSTGRESQL • REDIS • FASTAPI • SPRING BOOT
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-xs">
              <div className="p-6 border border-[#1c1c1c] bg-[#0a0a0a] rounded space-y-3">
                <div className="flex items-center gap-2 text-white font-bold">
                  <Terminal className="w-4 h-4" />
                  <span>SYNCHRONOUS GROUND TRUTH</span>
                </div>
                <p className="text-[#888888] leading-relaxed font-sans text-xs">
                  Reference images require exactly 1 detected face (HTTP 422 enforcement). Vectors are normalized into 512-dim IEEE 754 bytearrays.
                </p>
                <div className="text-[11px] text-[#666666] pt-2 border-t border-[#171717]">
                  endpoint: /embeddings/reference
                </div>
              </div>

              <div className="p-6 border border-[#1c1c1c] bg-[#0a0a0a] rounded space-y-3">
                <div className="flex items-center gap-2 text-white font-bold">
                  <Cpu className="w-4 h-4" />
                  <span>REDIS STREAM QUEUING</span>
                </div>
                <p className="text-[#888888] leading-relaxed font-sans text-xs">
                  Group photos publish job IDs after database transaction commits. Consumer workers poll whodis:photo-processing with recovery sweeps.
                </p>
                <div className="text-[11px] text-[#666666] pt-2 border-t border-[#171717]">
                  consumer: whodis-worker-1
                </div>
              </div>

              <div className="p-6 border border-[#1c1c1c] bg-[#0a0a0a] rounded space-y-3">
                <div className="flex items-center gap-2 text-white font-bold">
                  <Database className="w-4 h-4" />
                  <span>COSINE DISAMBIGUATION</span>
                </div>
                <p className="text-[#888888] leading-relaxed font-sans text-xs">
                  Dot-product evaluation requires minimum similarity $\ge 0.51$ and candidate separation $\ge 0.05$ margin to prevent misattribution.
                </p>
                <div className="text-[11px] text-[#666666] pt-2 border-t border-[#171717]">
                  outbox: auto-reconciliation
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* CTA Terminal */}
        <section className="border-t border-[#1c1c1c] bg-black py-20 px-6 sm:px-12 text-center font-mono">
          <div className="max-w-xl mx-auto space-y-6">
            <h2 className="text-3xl sm:text-5xl font-black uppercase tracking-tight text-white font-display">
              READY TO INITIALIZE
            </h2>
            <p className="text-xs text-[#888888] leading-relaxed font-sans">
              Create an ephemeral session and start clustering photographs. No persistent state, zero telemetry.
            </p>
            <div className="pt-2">
              <Button
                size="lg"
                onClick={handleStartSession}
                isLoading={isCreating}
                className="font-bold text-xs px-8"
              >
                INITIALIZE ENGINE
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </div>
          </div>
        </section>
      </main>

      {/* Vercel-Style Clean Monospace Footer */}
      <footer className="border-t border-[#1c1c1c] bg-black py-8 px-6 sm:px-12 text-xs font-mono text-[#666666]">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-[#888888]">
            <span className="text-white font-bold tracking-tight">WhoDis</span>
            <span>{"// Facial Recognition Engine"}</span>
          </div>

          <div className="flex items-center gap-6">
            <span>INSIGHTFACE 2.0</span>
            <span>REDIS STREAMS</span>
            <span>SPRING BOOT 4.1</span>
          </div>
        </div>
      </footer>
    </div>
  );
}

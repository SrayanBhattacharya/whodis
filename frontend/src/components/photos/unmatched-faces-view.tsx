"use client";

import React, { useState } from "react";
import { Sparkles, UserCheck, AlertCircle } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { UnmatchedFaceResponse } from "@/src/types/api";
import { api } from "@/src/lib/api/client";
import { Button } from "@/src/components/ui/button";
import { Modal } from "@/src/components/ui/modal";
import { parseBoundingBox } from "@/src/lib/utils/format";

export function UnmatchedFacesView() {
  const { sessionId, unmatchedFaces, groupPhotos, people, assignFace } = useSession();
  const [selectedFace, setSelectedFace] = useState<UnmatchedFaceResponse | null>(null);
  const [selectedPersonId, setSelectedPersonId] = useState<string>("");
  const [isAssigning, setIsAssigning] = useState(false);
  const [assignError, setAssignError] = useState<string | null>(null);

  const groupPhotoIdSet = new Set(groupPhotos.map((p) => p.photoId));
  const filteredUnmatchedFaces = unmatchedFaces.filter(
    (face) => !groupPhotoIdSet.has(face.photoId)
  );

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFace || !selectedPersonId) return;

    try {
      setIsAssigning(true);
      setAssignError(null);
      await assignFace(selectedFace.faceId, selectedPersonId);
      setSelectedFace(null);
      setSelectedPersonId("");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Assignment failed";
      setAssignError(msg);
    } finally {
      setIsAssigning(false);
    }
  };

  if (filteredUnmatchedFaces.length === 0) {
    return (
      <div className="py-24 text-center max-w-md mx-auto px-4 font-mono">
        <div className="w-12 h-12 mx-auto mb-5 flex items-center justify-center bg-[#0a0a0a] border border-[#222222] rounded text-[#888888]">
          <Sparkles className="w-6 h-6 text-white" />
        </div>
        <h3 className="text-base font-bold text-white mb-2 font-mono">
          QUEUE CLEAN // ZERO UNASSIGNED
        </h3>
        <p className="text-xs text-[#888888] leading-relaxed font-sans">
          All detected face bounding boxes have either matched registered participants or no unclassified faces remain. Group photos are maintained in the Group Photos section.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in font-mono">
      {/* Header */}
      <div className="pb-4 border-b border-[#1c1c1c]">
        <h2 className="text-base font-bold text-white">
          Unassigned Faces
        </h2>
        <p className="text-xs text-[#888888] mt-0.5">
          {"// triage:"} {filteredUnmatchedFaces.length} detected faces awaiting participant binding
        </p>
      </div>

      {/* Grid of unmatched faces */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
        {filteredUnmatchedFaces.map((face) => {
          const photoUrl = api.getPhotoUrl(sessionId, face.photoId);
          const box = parseBoundingBox(face.boundingBox);

          return (
            <div
              key={face.faceId}
              className="group border border-[#222222] bg-[#0a0a0a] rounded overflow-hidden flex flex-col hover:border-[#555555] transition-all duration-150"
            >
              {/* Image Preview Container */}
              <div className="aspect-[4/3] w-full bg-[#050505] overflow-hidden relative">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={photoUrl}
                  alt={face.originalFilename}
                  className="w-full h-full object-cover"
                  loading="lazy"
                />

                <div className="absolute top-2 left-2 px-2 py-0.5 rounded bg-black/90 text-white border border-[#333333] text-[10px] font-mono">
                  UNBOUND
                </div>
              </div>

              {/* Action Box */}
              <div className="p-3 flex flex-col justify-between flex-1 space-y-3 bg-[#0a0a0a]">
                <div>
                  <p className="truncate text-xs text-[#cccccc]" title={face.originalFilename}>
                    {face.originalFilename}
                  </p>
                  {box && (
                    <p className="text-[10px] text-[#666666] mt-0.5">
                      BBOX: [{Math.round(box.x1)}, {Math.round(box.y1)}, {Math.round(box.x2)}, {Math.round(box.y2)}]
                    </p>
                  )}
                </div>

                <Button
                  size="sm"
                  variant="secondary"
                  className="w-full text-xs font-mono"
                  onClick={() => {
                    setSelectedFace(face);
                    setSelectedPersonId(people[0]?.id || "");
                  }}
                >
                  <UserCheck className="w-3.5 h-3.5 mr-1.5" />
                  Bind Identity
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Assign Modal */}
      {selectedFace && (
        <Modal
          isOpen={!!selectedFace}
          onClose={() => setSelectedFace(null)}
          title="Manual Face Assignment"
          maxWidth="md"
        >
          <form onSubmit={handleAssign} className="space-y-4 font-mono text-xs">
            <div>
              <p className="text-[#888888] mb-3">
                Bind detected facial tensor to participant:
              </p>

              {people.length === 0 ? (
                <div className="p-3 bg-black border border-[#222222] text-[#aaaaaa] rounded flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-white" />
                  <p>
                    No participants available. Register an identity in the <strong>PEOPLE</strong> tab first.
                  </p>
                </div>
              ) : (
                <div className="space-y-2 max-h-56 overflow-y-auto p-1">
                  {people.map((p) => (
                    <label
                      key={p.id}
                      className={`flex items-center justify-between p-2.5 border rounded cursor-pointer transition-colors ${
                        selectedPersonId === p.id
                          ? "border-white bg-[#141414]"
                          : "border-[#222222] hover:border-[#444444]"
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <input
                          type="radio"
                          name="person"
                          value={p.id}
                          checked={selectedPersonId === p.id}
                          onChange={() => setSelectedPersonId(p.id)}
                          className="text-white focus:ring-white"
                        />
                        <span className="text-xs font-medium text-white">
                          {p.name}
                        </span>
                      </div>
                    </label>
                  ))}
                </div>
              )}

              {assignError && (
                <p className="text-xs text-red-400 mt-2">
                  [error] {assignError}
                </p>
              )}
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#222222]">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => setSelectedFace(null)}
                disabled={isAssigning}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                size="sm"
                disabled={!selectedPersonId || isAssigning || people.length === 0}
                isLoading={isAssigning}
              >
                Execute Bind
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

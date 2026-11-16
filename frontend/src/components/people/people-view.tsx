"use client";

import React, { useState } from "react";
import { UserPlus, Users, ArrowUpRight } from "lucide-react";
import { useSession } from "@/src/components/session/session-context";
import { PersonResponse } from "@/src/types/api";
import { Button } from "@/src/components/ui/button";
import { Modal } from "@/src/components/ui/modal";
import { PersonDetail } from "@/src/components/people/person-detail";

export function PeopleView() {
  const { sessionId, people, createPerson } = useSession();
  const [selectedPerson, setSelectedPerson] = useState<PersonResponse | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newPersonName, setNewPersonName] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPersonName.trim()) return;

    try {
      setIsSubmitting(true);
      setCreateError(null);
      await createPerson(newPersonName.trim());
      setNewPersonName("");
      setIsCreateModalOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to register person";
      setCreateError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (selectedPerson) {
    return (
      <PersonDetail
        sessionId={sessionId}
        person={selectedPerson}
        onBack={() => setSelectedPerson(null)}
      />
    );
  }

  return (
    <div className="space-y-6 animate-fade-in font-mono">
      {/* Top action header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#1c1c1c]">
        <div>
          <h2 className="text-base font-bold text-white">
            Participants
          </h2>
          <p className="text-xs text-[#888888] mt-0.5">
            {"// count:"} {people.length} registered identities
          </p>
        </div>
        <Button size="sm" onClick={() => setIsCreateModalOpen(true)}>
          <UserPlus className="w-3.5 h-3.5 mr-1.5" />
          Add Participant
        </Button>
      </div>

      {people.length === 0 ? (
        <div className="py-24 text-center max-w-md mx-auto px-4">
          <div className="w-12 h-12 mx-auto mb-5 flex items-center justify-center bg-[#0a0a0a] border border-[#222222] rounded text-[#888888]">
            <Users className="w-6 h-6 text-white" />
          </div>
          <h3 className="text-base font-bold text-white mb-2 font-mono">
            NO IDENTITIES REGISTERED
          </h3>
          <p className="text-xs text-[#888888] mb-6 leading-relaxed font-sans">
            Add individuals to index. Uploading single-face reference portraits generates the ground-truth ArcFace embeddings.
          </p>
          <Button onClick={() => setIsCreateModalOpen(true)} size="md">
            <UserPlus className="w-4 h-4 mr-1.5" />
            Add First Participant
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
          {people.map((person) => (
            <div
              key={person.id}
              onClick={() => setSelectedPerson(person)}
              className="group cursor-pointer border border-[#222222] bg-[#0a0a0a] rounded p-5 flex flex-col justify-between transition-all duration-150 hover:border-[#555555] hover:bg-[#111111]"
            >
              <div className="flex items-start justify-between mb-6">
                <div className="w-10 h-10 rounded bg-[#171717] border border-[#262626] text-white flex items-center justify-center font-bold text-sm group-hover:bg-white group-hover:text-black transition-colors">
                  {person.name.charAt(0).toUpperCase()}
                </div>
                <span className="text-[#666666] group-hover:text-white transition-colors">
                  <ArrowUpRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                </span>
              </div>

              <div>
                <h4 className="text-sm font-bold text-white group-hover:text-white transition-colors mb-1 truncate">
                  {person.name}
                </h4>
                <p className="text-[11px] text-[#666666]">
                  Inspect album & vectors →
                </p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Person Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Register Participant"
        maxWidth="sm"
      >
        <form onSubmit={handleCreate} className="space-y-4 font-mono text-xs">
          <div>
            <label className="block text-[11px] text-[#888888] mb-2 uppercase tracking-wider">
              Identity Name
            </label>
            <input
              type="text"
              required
              maxLength={100}
              placeholder="e.g. Sarah Smith"
              value={newPersonName}
              onChange={(e) => setNewPersonName(e.target.value)}
              className="w-full px-3 py-2 bg-black border border-[#222222] rounded text-white text-xs focus:outline-none focus:border-white transition-colors"
              autoFocus
            />
            {createError && (
              <p className="text-xs text-red-400 mt-2">
                [error] {createError}
              </p>
            )}
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#222222]">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setIsCreateModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              isLoading={isSubmitting}
              disabled={!newPersonName.trim()}
            >
              Commit Identity
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

import { Suspense } from "react";
import { SessionWorkspace } from "@/src/components/session/session-workspace";

interface SessionPageProps {
  params: Promise<{ sessionId: string }>;
}

async function SessionPageContent({ params }: SessionPageProps) {
  const { sessionId } = await params;
  return <SessionWorkspace sessionId={sessionId} />;
}

export default function SessionPage(props: SessionPageProps) {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#fbfbfa] flex flex-col items-center justify-center p-6 text-center">
          <div className="w-8 h-8 border-2 border-neutral-900 border-t-transparent rounded-full animate-spin mb-4" />
          <p className="font-serif text-lg tracking-tight text-neutral-800">
            Initializing Session...
          </p>
        </div>
      }
    >
      <SessionPageContent {...props} />
    </Suspense>
  );
}

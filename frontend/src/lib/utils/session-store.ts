const SESSION_STORAGE_KEY = "whodis_active_session_id";

export const sessionStore = {
  get(): string | null {
    if (typeof window === "undefined") return null;
    try {
      return sessionStorage.getItem(SESSION_STORAGE_KEY);
    } catch {
      return null;
    }
  },
  set(sessionId: string): void {
    if (typeof window === "undefined") return;
    try {
      sessionStorage.setItem(SESSION_STORAGE_KEY, sessionId);
    } catch {
      // ignore
    }
  },
  clear(): void {
    if (typeof window === "undefined") return;
    try {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {
      // ignore
    }
  },
};

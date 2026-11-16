import { ParsedBoundingBox } from "@/src/types/api";

export function formatBytes(bytes: number, decimals = 1): string {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

export function formatDate(dateString: string): string {
  try {
    const d = new Date(dateString);
    return new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
    }).format(d);
  } catch {
    return dateString;
  }
}

export function formatTimeRemaining(expiresAt: string): {
  hours: number;
  minutes: number;
  isExpired: boolean;
  formatted: string;
} {
  const now = new Date().getTime();
  const exp = new Date(expiresAt).getTime();
  const diff = exp - now;

  if (diff <= 0) {
    return { hours: 0, minutes: 0, isExpired: true, formatted: "Expired" };
  }

  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

  if (hours > 0) {
    return {
      hours,
      minutes,
      isExpired: false,
      formatted: `${hours}h ${minutes}m left`,
    };
  }

  return {
    hours,
    minutes,
    isExpired: false,
    formatted: `${minutes}m left`,
  };
}

export function parseBoundingBox(jsonStr: string): ParsedBoundingBox | null {
  try {
    const parsed = JSON.parse(jsonStr);
    if (
      typeof parsed.x1 === "number" &&
      typeof parsed.y1 === "number" &&
      typeof parsed.x2 === "number" &&
      typeof parsed.y2 === "number"
    ) {
      return parsed as ParsedBoundingBox;
    }
    return null;
  } catch {
    return null;
  }
}

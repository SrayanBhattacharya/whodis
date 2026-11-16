/**
 * Triggers a browser download of a binary file from a given URL or fetch call,
 * preserving filename from Content-Disposition header when available,
 * and cleaning up temporary object URLs.
 */
export async function downloadFile(
  url: string,
  options: RequestInit = {},
  fallbackFilename: string = "download"
): Promise<void> {
  const response = await fetch(url, options);

  if (!response.ok) {
    let errorMessage = `Download failed with HTTP ${response.status}`;
    try {
      const errJson = await response.json();
      if (errJson?.message) {
        errorMessage = errJson.message;
      }
    } catch {
      // Ignore if response is not JSON
    }
    throw new Error(errorMessage);
  }

  // Parse filename from Content-Disposition header if present
  let filename = fallbackFilename;
  const disposition = response.headers.get("content-disposition");
  if (disposition) {
    // Check filename*=UTF-8''... (RFC 5987)
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match && utf8Match[1]) {
      try {
        filename = decodeURIComponent(utf8Match[1].trim());
      } catch {
        filename = utf8Match[1].trim();
      }
    } else {
      // Check standard filename="..."
      const match = disposition.match(/filename="?([^";]+)"?/i);
      if (match && match[1]) {
        filename = match[1].trim();
      }
    }
  }

  const blob = await response.blob();
  const blobUrl = window.URL.createObjectURL(blob);

  try {
    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = filename;
    link.style.display = "none";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } finally {
    // Revoke object URL after a brief timeout to ensure browser has queued the save
    setTimeout(() => {
      window.URL.revokeObjectURL(blobUrl);
    }, 1000);
  }
}

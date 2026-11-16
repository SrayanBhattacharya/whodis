import { NextRequest, NextResponse } from "next/server";

const BACKEND_INTERNAL_URL =
  process.env.BACKEND_INTERNAL_URL ||
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  "http://localhost:8080";

async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path?: string[] }> }
) {
  const { path = [] } = await context.params;
  const targetPath = `/api/v1/${path.join("/")}`;
  const targetUrl = new URL(targetPath, BACKEND_INTERNAL_URL);

  request.nextUrl.searchParams.forEach((value, key) => {
    targetUrl.searchParams.append(key, value);
  });

  const headers = new Headers();
  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("content-type", contentType);
  }
  const accept = request.headers.get("accept");
  if (accept) {
    headers.set("accept", accept);
  }

  let body: BodyInit | null = null;
  if (request.method !== "GET" && request.method !== "HEAD") {
    body = await request.arrayBuffer();
  }

  try {
    const backendResponse = await fetch(targetUrl.toString(), {
      method: request.method,
      headers,
      body,
      // @ts-expect-error duplex required in node fetch for streaming/buffers if needed
      duplex: "half",
      cache: "no-store",
    });

    const responseHeaders = new Headers();
    const respContentType = backendResponse.headers.get("content-type");
    if (respContentType) {
      responseHeaders.set("content-type", respContentType);
    }
    const respContentDisp = backendResponse.headers.get("content-disposition");
    if (respContentDisp) {
      responseHeaders.set("content-disposition", respContentDisp);
    }
    const respContentLength = backendResponse.headers.get("content-length");
    if (respContentLength) {
      responseHeaders.set("content-length", respContentLength);
    }

    const data = await backendResponse.arrayBuffer();

    return new NextResponse(data, {
      status: backendResponse.status,
      statusText: backendResponse.statusText,
      headers: responseHeaders,
    });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Failed to connect to backend service";
    return NextResponse.json(
      {
        timestamp: new Date().toISOString(),
        status: 502,
        code: "BACKEND_UNAVAILABLE",
        message: `Backend proxy error: ${message}`,
      },
      { status: 502 }
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PATCH = proxy;
export const DELETE = proxy;

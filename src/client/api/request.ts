/** API が返すエラー。画面はここに入るメッセージをそのまま出してよい。 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

interface RequestOptions {
  method?: string;
  json?: unknown;
}

/**
 * Worker API への呼び出し。
 * エラーの形は { code, message } に揃えてあるので、ここで ApiError へ変換する。
 */
export async function request(path: string, options: RequestOptions = {}): Promise<unknown> {
  const res = await fetch(path, {
    method: options.method ?? "GET",
    headers: options.json ? { "content-type": "application/json" } : undefined,
    body: options.json ? JSON.stringify(options.json) : undefined,
  });

  if (res.status === 204) {
    return undefined;
  }

  const payload: unknown = await res.json().catch(() => undefined);

  if (!res.ok) {
    const detail =
      typeof payload === "object" && payload !== null
        ? (payload as { code?: string; message?: string })
        : {};
    throw new ApiError(res.status, detail.code ?? "UNKNOWN", detail.message ?? "処理に失敗した");
  }

  return payload;
}

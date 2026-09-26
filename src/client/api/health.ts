import { healthResponseSchema, type HealthResponse } from "@/shared/health";

/** Worker の疎通確認。 */
export async function fetchHealth(): Promise<HealthResponse> {
  const res = await fetch("/api/health");
  if (!res.ok) {
    throw new Error(`API の応答が異常: ${res.status}`);
  }
  // 応答の形は共有スキーマで検証する。API 側と同じものを使う。
  return healthResponseSchema.parse(await res.json());
}

import { z } from "zod";

/** 疎通確認の応答。画面と API の両方でこのスキーマを使う。 */
export const healthResponseSchema = z.object({
  status: z.literal("ok"),
  time: z.iso.datetime(),
});

export type HealthResponse = z.infer<typeof healthResponseSchema>;

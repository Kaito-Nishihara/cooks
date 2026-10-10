import { Hono } from "hono";
import { z } from "zod";
import { ForbiddenError } from "@/domain/errors";
import { preferenceInputSchema, type PreferenceListResponse } from "@/shared/preference";
import { getDb } from "../db/client";
import {
  deletePreference,
  listPreferences,
  listUsersForComparison,
  upsertPreference,
} from "../db/queries/preferences";
import { validate } from "../lib/validator";
import { requireUser, type AppEnv } from "../middleware/auth";

export const preferenceRoutes = new Hono<AppEnv>();

const targetParamSchema = z.object({
  userId: z.coerce.number().int().positive(),
  ingredientId: z.coerce.number().int().positive(),
});

preferenceRoutes.use("/api/preferences", requireUser);
preferenceRoutes.use("/api/preferences/*", requireUser);

/**
 * 嗜好の一覧。二人分をまとめて返す。
 * 画面で並べて比較するため、また提案の除外判定で取りこぼさないため、絞り込まずに全件返す。
 */
preferenceRoutes.get("/api/preferences", async (c) => {
  const db = getDb(c.env);
  const [users, preferences] = await Promise.all([
    listUsersForComparison(db),
    listPreferences(db),
  ]);

  const body: PreferenceListResponse = { users, preferences };
  return c.json(body);
});

/** 嗜好を登録または更新する。 */
preferenceRoutes.put(
  "/api/preferences/:userId/:ingredientId",
  validate("param", targetParamSchema),
  validate("json", preferenceInputSchema),
  async (c) => {
    const { userId, ingredientId } = c.req.valid("param");
    assertCanEdit(c.get("user"), userId);

    const input = c.req.valid("json");
    const saved = await upsertPreference(getDb(c.env), {
      userId,
      ingredientId,
      level: input.level,
      note: input.note,
    });
    return c.json(saved);
  },
);

/** 嗜好の登録を消して「普通」に戻す。行が無い状態が NEUTRAL を表す。 */
preferenceRoutes.delete(
  "/api/preferences/:userId/:ingredientId",
  validate("param", targetParamSchema),
  async (c) => {
    const { userId, ingredientId } = c.req.valid("param");
    assertCanEdit(c.get("user"), userId);

    await deletePreference(getDb(c.env), userId, ingredientId);
    return c.body(null, 204);
  },
);

/**
 * 嗜好を編集できるのは本人と、全データを編集できるオーナーに限る。
 * 好き嫌いは本人にしか分からないため、既定では他人の分を書き換えさせない。
 */
function assertCanEdit(actor: { id: number; role: string }, targetUserId: number): void {
  if (actor.id === targetUserId || actor.role === "OWNER") {
    return;
  }
  throw new ForbiddenError("他の利用者の好き嫌いは編集できない");
}

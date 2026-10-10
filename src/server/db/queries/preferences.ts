import { and, asc, eq } from "drizzle-orm";
import { NotFoundError } from "@/domain/errors";
import type { PreferenceLevel } from "@/shared/enums";
import type { Preference } from "@/shared/preference";
import type { Db } from "../client";
import { ingredientPreferences, ingredients, users } from "../schema";

/**
 * 嗜好を全件返す。
 *
 * 意図的に上限を設けていない。
 * 取りこぼすとアレルギーの登録が欠けた状態で除外の判定が走り、
 * 食べられないものを含むレシピを勧めてしまう。
 * 利用者は二人で、食材マスタも数百件のため、全件でも D1 の無料枠に対して十分小さい。
 */
export async function listPreferences(db: Db): Promise<Preference[]> {
  const rows = await db
    .select({
      userId: ingredientPreferences.userId,
      ingredientId: ingredientPreferences.ingredientId,
      level: ingredientPreferences.level,
      note: ingredientPreferences.note,
    })
    .from(ingredientPreferences);
  return rows;
}

/** 嗜好を並べて比較するための利用者一覧。 */
export async function listUsersForComparison(
  db: Db,
): Promise<{ id: number; displayName: string }[]> {
  return db
    .select({ id: users.id, displayName: users.displayName })
    .from(users)
    .orderBy(asc(users.id))
    .limit(50);
}

export interface UpsertPreferenceInput {
  userId: number;
  ingredientId: number;
  level: PreferenceLevel;
  note?: string | undefined;
}

/** 嗜好を登録または更新する。 */
export async function upsertPreference(db: Db, input: UpsertPreferenceInput): Promise<Preference> {
  await assertIngredientExists(db, input.ingredientId);

  const now = new Date();
  const note = input.note && input.note.length > 0 ? input.note : null;

  const rows = await db
    .insert(ingredientPreferences)
    .values({
      userId: input.userId,
      ingredientId: input.ingredientId,
      level: input.level,
      note,
      createdAt: now,
      updatedAt: now,
    })
    .onConflictDoUpdate({
      target: [ingredientPreferences.userId, ingredientPreferences.ingredientId],
      set: { level: input.level, note, updatedAt: now },
    })
    .returning({
      userId: ingredientPreferences.userId,
      ingredientId: ingredientPreferences.ingredientId,
      level: ingredientPreferences.level,
      note: ingredientPreferences.note,
    });

  const saved = rows[0];
  if (!saved) {
    throw new Error("嗜好の保存結果が返らなかった");
  }
  return saved;
}

/**
 * 嗜好の登録を消す。
 * 行が無い状態が NEUTRAL を意味するため、「普通」へ戻す操作はこれで表す。
 */
export async function deletePreference(
  db: Db,
  userId: number,
  ingredientId: number,
): Promise<void> {
  await db
    .delete(ingredientPreferences)
    .where(
      and(
        eq(ingredientPreferences.userId, userId),
        eq(ingredientPreferences.ingredientId, ingredientId),
      ),
    );
}

async function assertIngredientExists(db: Db, ingredientId: number): Promise<void> {
  const rows = await db
    .select({ id: ingredients.id })
    .from(ingredients)
    .where(eq(ingredients.id, ingredientId))
    .limit(1);
  if (rows.length === 0) {
    throw new NotFoundError(`食材 ${ingredientId} が見つからない`);
  }
}

import { and, asc, eq, like, or, sql } from "drizzle-orm";
import { ConflictError, NotFoundError } from "@/domain/errors";
import type { IngredientCategory } from "@/shared/enums";
import { INGREDIENT_LIST_LIMIT, type IngredientInput } from "@/shared/ingredient";
import type { Db } from "../client";
import {
  ingredientAliases,
  ingredientPreferences,
  ingredients,
  recipeIngredients,
  shoppingListItems,
  type Ingredient,
} from "../schema";

export interface ListIngredientsOptions {
  q?: string | undefined;
  category?: IngredientCategory | undefined;
}

/**
 * 食材の一覧。
 * 上限を超えた分は返さず、切れたことを呼び出し元へ伝える。
 * D1 の無料枠は 1 日あたりの読み取り行数に上限があるため、無条件の全件取得をしない。
 */
export async function listIngredients(
  db: Db,
  options: ListIngredientsOptions = {},
): Promise<{ items: Ingredient[]; truncated: boolean }> {
  const conditions = [];

  if (options.category) {
    conditions.push(eq(ingredients.category, options.category));
  }

  if (options.q) {
    // 名称と別名のどちらかに含まれていれば拾う。
    const pattern = `%${escapeLike(options.q)}%`;
    const aliasMatch = db
      .select({ id: ingredientAliases.ingredientId })
      .from(ingredientAliases)
      .where(like(ingredientAliases.name, pattern));
    conditions.push(
      or(like(ingredients.name, pattern), sql`${ingredients.id} IN ${aliasMatch}`),
    );
  }

  // 上限に達したかを判定するため、1 件多く取る。
  const rows = await db
    .select()
    .from(ingredients)
    .where(conditions.length > 0 ? and(...conditions) : undefined)
    .orderBy(asc(ingredients.category), asc(ingredients.name))
    .limit(INGREDIENT_LIST_LIMIT + 1);

  return {
    items: rows.slice(0, INGREDIENT_LIST_LIMIT),
    truncated: rows.length > INGREDIENT_LIST_LIMIT,
  };
}

export async function findIngredient(db: Db, id: number): Promise<Ingredient | undefined> {
  const rows = await db.select().from(ingredients).where(eq(ingredients.id, id)).limit(1);
  return rows[0];
}

export async function createIngredient(db: Db, input: IngredientInput): Promise<Ingredient> {
  await assertNameAvailable(db, input.name);

  const now = new Date();
  const rows = await db
    .insert(ingredients)
    .values({ ...input, createdAt: now, updatedAt: now })
    .returning();

  const created = rows[0];
  if (!created) {
    throw new Error("食材の登録結果が返らなかった");
  }
  return created;
}

export async function updateIngredient(
  db: Db,
  id: number,
  input: IngredientInput,
): Promise<Ingredient> {
  const current = await findIngredient(db, id);
  if (!current) {
    throw new NotFoundError(`食材 ${id} が見つからない`);
  }

  if (current.name !== input.name) {
    await assertNameAvailable(db, input.name);
  }

  const rows = await db
    .update(ingredients)
    .set({ ...input, updatedAt: new Date() })
    .where(eq(ingredients.id, id))
    .returning();

  const updated = rows[0];
  if (!updated) {
    throw new NotFoundError(`食材 ${id} が見つからない`);
  }
  return updated;
}

/**
 * 食材を削除する。
 *
 * レシピ、嗜好、買い物リストから参照されている食材は削除させない。
 * スキーマ側では嗜好に cascade を張っているため、確認せずに消すと
 * 「アレルギー」の登録が黙って失われる。
 */
export async function deleteIngredient(db: Db, id: number): Promise<void> {
  const current = await findIngredient(db, id);
  if (!current) {
    throw new NotFoundError(`食材 ${id} が見つからない`);
  }

  const reasons = await collectReferences(db, id);
  if (reasons.length > 0) {
    throw new ConflictError(`${current.name} は ${reasons.join("、")}から参照されているため削除できない`);
  }

  await db.delete(ingredients).where(eq(ingredients.id, id));
}

/** 参照している場所の名前を集める。件数ではなく有無だけを見るため limit 1 で足りる。 */
async function collectReferences(db: Db, ingredientId: number): Promise<string[]> {
  const [recipeRows, preferenceRows, shoppingRows] = await db.batch([
    db
      .select({ id: recipeIngredients.id })
      .from(recipeIngredients)
      .where(eq(recipeIngredients.ingredientId, ingredientId))
      .limit(1),
    db
      .select({ id: ingredientPreferences.ingredientId })
      .from(ingredientPreferences)
      .where(eq(ingredientPreferences.ingredientId, ingredientId))
      .limit(1),
    db
      .select({ id: shoppingListItems.id })
      .from(shoppingListItems)
      .where(eq(shoppingListItems.ingredientId, ingredientId))
      .limit(1),
  ]);

  const reasons: string[] = [];
  if (recipeRows.length > 0) reasons.push("レシピ");
  if (preferenceRows.length > 0) reasons.push("好き嫌いの登録");
  if (shoppingRows.length > 0) reasons.push("買い物リスト");
  return reasons;
}

async function assertNameAvailable(db: Db, name: string): Promise<void> {
  const rows = await db
    .select({ id: ingredients.id })
    .from(ingredients)
    .where(eq(ingredients.name, name))
    .limit(1);
  if (rows.length > 0) {
    throw new ConflictError(`食材「${name}」は既に登録されている`);
  }
}

/** LIKE のパターンで意味を持つ文字を打ち消す。 */
function escapeLike(value: string): string {
  return value.replaceAll("\\", "\\\\").replaceAll("%", "\\%").replaceAll("_", "\\_");
}

import { z } from "zod";
import { INGREDIENT_CATEGORIES } from "./enums";

/** 一覧で一度に返す食材の上限。食材マスタは 200〜400 件を想定している。 */
export const INGREDIENT_LIST_LIMIT = 500;

export const ingredientSchema = z.object({
  id: z.number().int().positive(),
  name: z.string().min(1),
  category: z.enum(INGREDIENT_CATEGORIES),
  standardUnit: z.string().min(1),
});

export type Ingredient = z.infer<typeof ingredientSchema>;

/**
 * 食材の登録と更新の入力。
 * 成分表の代表食品番号(foodNumber)はここに含めない。
 * 番号の存在を public/data/foods.json と突き合わせて検証する必要があり、
 * その JSON を用意する作業と合わせて追加する。
 */
export const ingredientInputSchema = z.object({
  name: z.string().trim().min(1, "名称を入れる").max(50),
  category: z.enum(INGREDIENT_CATEGORIES),
  standardUnit: z.string().trim().min(1, "単位を入れる").max(10),
});

export type IngredientInput = z.infer<typeof ingredientInputSchema>;

export const ingredientListQuerySchema = z.object({
  /** 名称と別名の部分一致。 */
  q: z.string().trim().max(50).optional(),
  category: z.enum(INGREDIENT_CATEGORIES).optional(),
});

export const ingredientListResponseSchema = z.object({
  ingredients: z.array(ingredientSchema),
  /** 一覧が上限で切れたかどうか。切れている場合は絞り込みを促す。 */
  truncated: z.boolean(),
});

export type IngredientListResponse = z.infer<typeof ingredientListResponseSchema>;

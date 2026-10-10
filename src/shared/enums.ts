/**
 * 画面と API で共有する値の集合。
 * D1 に enum 型はないため、Drizzle の text カラムと Zod の両方がここを参照する。
 * 値を追加するときは、既存行の移行が要るかを必ず確認する。
 */

/** 食材に対する嗜好。未登録の食材は NEUTRAL として扱う。 */
export const PREFERENCE_LEVELS = [
  "LOVE",
  "LIKE",
  "NEUTRAL",
  "DISLIKE",
  "HATE",
  "ALLERGY",
] as const;
export type PreferenceLevel = (typeof PREFERENCE_LEVELS)[number];

/** 嗜好の表示名。 */
export const PREFERENCE_LABELS: Record<PreferenceLevel, string> = {
  LOVE: "大好き",
  LIKE: "好き",
  NEUTRAL: "普通",
  DISLIKE: "苦手",
  HATE: "食べられない",
  ALLERGY: "アレルギー",
};

/** レシピ提案のスコアに与える重み。NEUTRAL は影響しない。 */
export const PREFERENCE_SCORES: Record<PreferenceLevel, number> = {
  LOVE: 3,
  LIKE: 1,
  NEUTRAL: 0,
  DISLIKE: -2,
  HATE: 0,
  ALLERGY: 0,
};

/** 候補から除外する嗜好。ALLERGY は安全に関わるため切り替えで表示させない。 */
export const ALWAYS_EXCLUDED_LEVELS = ["ALLERGY"] as const;
/** 既定で除外するが、画面の切り替えで表示できる嗜好。 */
export const DEFAULT_EXCLUDED_LEVELS = ["HATE"] as const;

/** 食材の分類。 */
export const INGREDIENT_CATEGORIES = [
  "VEGETABLE",
  "MEAT",
  "SEAFOOD",
  "DAIRY",
  "GRAIN",
  "SEASONING",
  "OTHER",
] as const;
export type IngredientCategory = (typeof INGREDIENT_CATEGORIES)[number];

export const INGREDIENT_CATEGORY_LABELS: Record<IngredientCategory, string> = {
  VEGETABLE: "野菜",
  MEAT: "肉",
  SEAFOOD: "魚介",
  DAIRY: "乳製品",
  GRAIN: "穀類",
  SEASONING: "調味料",
  OTHER: "その他",
};

/** レシピの分類。固定の集合のため、マスタ JSON ではなくここを正本とする。 */
export const RECIPE_CATEGORIES = ["MAIN", "SIDE", "SOUP", "STAPLE", "DESSERT"] as const;
export type RecipeCategory = (typeof RECIPE_CATEGORIES)[number];

export const RECIPE_CATEGORY_LABELS: Record<RecipeCategory, string> = {
  MAIN: "主菜",
  SIDE: "副菜",
  SOUP: "汁物",
  STAPLE: "主食",
  DESSERT: "デザート",
};

/** 献立の食事区分。 */
export const MEAL_TYPES = ["BREAKFAST", "LUNCH", "DINNER", "SNACK"] as const;
export type MealType = (typeof MEAL_TYPES)[number];

export const MEAL_TYPE_LABELS: Record<MealType, string> = {
  BREAKFAST: "朝食",
  LUNCH: "昼食",
  DINNER: "夕食",
  SNACK: "間食",
};

/** 利用者の役割。 */
export const USER_ROLES = ["OWNER", "PARTNER"] as const;
export type UserRole = (typeof USER_ROLES)[number];

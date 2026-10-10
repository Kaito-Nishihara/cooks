import {
  ALWAYS_EXCLUDED_LEVELS,
  DEFAULT_EXCLUDED_LEVELS,
  PREFERENCE_SCORES,
  type PreferenceLevel,
} from "@/shared/enums";
import type { Preference } from "@/shared/preference";

/**
 * 嗜好の判定。
 * ブラウザと Worker の両方から使うため、React にも Hono にも Drizzle にも依存しない。
 */

/** 利用者と食材の組から嗜好を引く。登録が無い食材は NEUTRAL として扱う。 */
export function resolveLevel(
  preferences: readonly Preference[],
  userId: number,
  ingredientId: number,
): PreferenceLevel {
  const found = preferences.find((p) => p.userId === userId && p.ingredientId === ingredientId);
  return found?.level ?? "NEUTRAL";
}

/** 利用者ごと、食材ごとに引けるようにまとめる。一覧の描画で毎回走査しないための前処理。 */
export function indexPreferences(
  preferences: readonly Preference[],
): Map<number, Map<number, Preference>> {
  const byUser = new Map<number, Map<number, Preference>>();
  for (const preference of preferences) {
    let byIngredient = byUser.get(preference.userId);
    if (!byIngredient) {
      byIngredient = new Map();
      byUser.set(preference.userId, byIngredient);
    }
    byIngredient.set(preference.ingredientId, preference);
  }
  return byUser;
}

/**
 * この食材を含むレシピを、必ず候補から外すべきか。
 * アレルギーは安全に関わるため、画面の切り替えでも表示させない。
 */
export function isAlwaysExcluded(level: PreferenceLevel): boolean {
  return (ALWAYS_EXCLUDED_LEVELS as readonly PreferenceLevel[]).includes(level);
}

/** 既定では候補から外すが、画面の切り替えで表示できるか。 */
export function isExcludedByDefault(level: PreferenceLevel): boolean {
  return (DEFAULT_EXCLUDED_LEVELS as readonly PreferenceLevel[]).includes(level);
}

/** レシピ提案のスコアへの寄与。除外の対象になる嗜好は 0 とし、加点減点では表さない。 */
export function scoreOf(level: PreferenceLevel): number {
  return PREFERENCE_SCORES[level];
}

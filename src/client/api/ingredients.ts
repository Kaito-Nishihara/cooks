import {
  ingredientListResponseSchema,
  type IngredientInput,
  type IngredientListResponse,
} from "@/shared/ingredient";
import { preferenceListResponseSchema, type PreferenceListResponse } from "@/shared/preference";
import type { PreferenceLevel } from "@/shared/enums";
import { request } from "./request";

export async function fetchIngredients(params: {
  q?: string;
  category?: string;
}): Promise<IngredientListResponse> {
  const query = new URLSearchParams();
  if (params.q) query.set("q", params.q);
  if (params.category) query.set("category", params.category);
  const suffix = query.size > 0 ? `?${query.toString()}` : "";

  return ingredientListResponseSchema.parse(await request(`/api/ingredients${suffix}`));
}

export async function createIngredient(input: IngredientInput): Promise<void> {
  await request("/api/ingredients", { method: "POST", json: input });
}

export async function updateIngredient(id: number, input: IngredientInput): Promise<void> {
  await request(`/api/ingredients/${id}`, { method: "PATCH", json: input });
}

export async function deleteIngredient(id: number): Promise<void> {
  await request(`/api/ingredients/${id}`, { method: "DELETE" });
}

export async function fetchPreferences(): Promise<PreferenceListResponse> {
  return preferenceListResponseSchema.parse(await request("/api/preferences"));
}

export async function savePreference(
  userId: number,
  ingredientId: number,
  level: PreferenceLevel,
  note?: string,
): Promise<void> {
  await request(`/api/preferences/${userId}/${ingredientId}`, {
    method: "PUT",
    json: note ? { level, note } : { level },
  });
}

/** 嗜好の登録を消して「普通」に戻す。 */
export async function clearPreference(userId: number, ingredientId: number): Promise<void> {
  await request(`/api/preferences/${userId}/${ingredientId}`, { method: "DELETE" });
}

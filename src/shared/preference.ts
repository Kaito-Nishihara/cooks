import { z } from "zod";
import { PREFERENCE_LEVELS } from "./enums";

export const preferenceSchema = z.object({
  userId: z.number().int().positive(),
  ingredientId: z.number().int().positive(),
  level: z.enum(PREFERENCE_LEVELS),
  note: z.string().nullable(),
});

export type Preference = z.infer<typeof preferenceSchema>;

export const preferenceInputSchema = z.object({
  level: z.enum(PREFERENCE_LEVELS),
  note: z.string().trim().max(200).optional(),
});

export type PreferenceInput = z.infer<typeof preferenceInputSchema>;

/** 嗜好の一覧。二人分を並べて比較するため、利用者の表示名も併せて返す。 */
export const preferenceListResponseSchema = z.object({
  users: z.array(
    z.object({
      id: z.number().int().positive(),
      displayName: z.string().min(1),
    }),
  ),
  preferences: z.array(preferenceSchema),
});

export type PreferenceListResponse = z.infer<typeof preferenceListResponseSchema>;

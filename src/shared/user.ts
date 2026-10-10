import { z } from "zod";
import { USER_ROLES } from "./enums";

/** 画面へ返す利用者の情報。メールアドレスは本人の確認用に含める。 */
export const currentUserSchema = z.object({
  id: z.number().int().positive(),
  email: z.email(),
  displayName: z.string().min(1),
  role: z.enum(USER_ROLES),
});

export type CurrentUser = z.infer<typeof currentUserSchema>;

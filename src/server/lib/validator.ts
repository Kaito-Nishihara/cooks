import { zValidator } from "@hono/zod-validator";
import type { ValidationTargets } from "hono";
import type { ZodType } from "zod";

/**
 * zValidator の応答を、他のエラーと同じ { code, message } の形へ揃える。
 *
 * 既定の応答は Zod の例外をそのまま流すため、画面側が二通りの形を扱うことになる。
 * どこで失敗したかは issues に残す。
 */
export function validate<T extends ZodType, Target extends keyof ValidationTargets>(
  target: Target,
  schema: T,
) {
  return zValidator(target, schema, (result, c) => {
    if (result.success) return;

    const issues = result.error.issues.map((issue) => ({
      path: issue.path.join("."),
      message: issue.message,
    }));
    return c.json({ code: "VALIDATION", message: "入力の形式が正しくない", issues }, 400);
  });
}

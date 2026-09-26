import type { ErrorHandler } from "hono";
import { HTTPException } from "hono/http-exception";
import {
  ConflictError,
  DomainError,
  ForbiddenError,
  NotFoundError,
  UnauthorizedError,
  ValidationError,
} from "@/domain/errors";

/** ドメインの例外から HTTP ステータスへの対応。 */
const STATUS_BY_ERROR = [
  [NotFoundError, 404],
  [ConflictError, 409],
  [ValidationError, 422],
  [UnauthorizedError, 401],
  [ForbiddenError, 403],
] as const;

/**
 * 例外を JSON の応答へ変換する。
 * ハンドラ側で try/catch を書き散らさないために、変換をここへ集約する。
 */
export const onError: ErrorHandler = (err, c) => {
  if (err instanceof HTTPException) {
    return c.json({ code: "HTTP_ERROR", message: err.message }, err.status);
  }

  if (err instanceof DomainError) {
    const matched = STATUS_BY_ERROR.find(([type]) => err instanceof type);
    const status = matched?.[1] ?? 400;
    return c.json({ code: err.code, message: err.message }, status);
  }

  // 想定外の例外は内容を返さない。原因は Workers のログで追う。
  console.error("未処理の例外", err);
  return c.json({ code: "INTERNAL", message: "サーバー内部でエラーが発生した" }, 500);
};

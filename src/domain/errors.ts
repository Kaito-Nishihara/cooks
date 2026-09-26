/**
 * ドメインの例外。
 * この層は React にも Hono にも Drizzle にも依存しないため、
 * HTTP のステータスをここで決めない。変換は Worker のミドルウェアが行う。
 */
export abstract class DomainError extends Error {
  abstract readonly code: string;

  constructor(message: string) {
    super(message);
    this.name = new.target.name;
  }
}

/** 対象が見つからない。 */
export class NotFoundError extends DomainError {
  readonly code = "NOT_FOUND";
}

/** 既存のデータと衝突する(名称の重複、参照されている食材の削除など)。 */
export class ConflictError extends DomainError {
  readonly code = "CONFLICT";
}

/** 入力が業務上の条件を満たさない。書式の検証は Zod が担うため、ここは業務判断に限る。 */
export class ValidationError extends DomainError {
  readonly code = "VALIDATION";
}

/** 認証されていない。 */
export class UnauthorizedError extends DomainError {
  readonly code = "UNAUTHORIZED";
}

/** 認証済みだが操作を許されていない。 */
export class ForbiddenError extends DomainError {
  readonly code = "FORBIDDEN";
}

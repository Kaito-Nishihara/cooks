import { createMiddleware } from "hono/factory";
import { ForbiddenError, UnauthorizedError } from "@/domain/errors";
import { getDb } from "../db/client";
import { findUserByEmail } from "../db/queries/users";
import type { User } from "../db/schema";
import { AccessVerificationError, verifyAccessIdentity } from "../lib/access";

/** 認証済みの利用者をハンドラへ渡すための型。 */
export interface AuthVariables {
  user: User;
}

export type AppEnv = { Bindings: Env; Variables: AuthVariables };

/**
 * 認証を必須にする。
 *
 * 本番では Cloudflare Access が Worker へ届く前にリクエストを弾くため、
 * ここでの検証は設定漏れに気づくための二重の確認である。
 *
 * ローカル開発では Access が動かないので、DEV_USER_EMAIL の利用者として扱う。
 * この分岐は本番で有効にならないよう、DEV_USER_EMAIL を本番の変数に置かないこと。
 */
export const requireUser = createMiddleware<AppEnv>(async (c, next) => {
  const email = await resolveEmail(c.env, c.req.raw);
  const db = getDb(c.env);
  const user = await findUserByEmail(db, email);

  if (!user) {
    // Access は通ったが、このアプリの利用者として登録されていない。
    // 認証そのものは済んでいるため 401 ではなく 403 を返す。
    throw new ForbiddenError(`${email} はこのアプリの利用者として登録されていない`);
  }

  c.set("user", user);
  await next();
});

async function resolveEmail(env: Env, request: Request): Promise<string> {
  if (env.DEV_USER_EMAIL) {
    return env.DEV_USER_EMAIL.toLowerCase();
  }

  if (!env.ACCESS_TEAM_DOMAIN || !env.ACCESS_AUD) {
    // 設定が欠けたまま公開すると、認証されていないリクエストを通しかねない。
    // 黙って素通りさせず、明示的に失敗させる。
    throw new UnauthorizedError("Access の設定(ACCESS_TEAM_DOMAIN / ACCESS_AUD)が無い");
  }

  try {
    const identity = await verifyAccessIdentity(request, env.ACCESS_TEAM_DOMAIN, env.ACCESS_AUD);
    return identity.email;
  } catch (err) {
    if (err instanceof AccessVerificationError) {
      throw new UnauthorizedError(err.message);
    }
    throw err;
  }
}

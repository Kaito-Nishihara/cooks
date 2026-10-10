import { Hono } from "hono";
import type { HealthResponse } from "@/shared/health";
import type { AppEnv } from "./middleware/auth";
import { onError } from "./middleware/error";
import { ingredientRoutes } from "./routes/ingredients";
import { meRoutes } from "./routes/me";
import { preferenceRoutes } from "./routes/preferences";

/**
 * Worker のエントリ。
 *
 * 静的アセットに存在する URL は Worker へ届かずに配信されるため、
 * ここへ来るのは /api/* と、SPA のフォールバック(index.html)だけである。
 *
 * 無料プランの CPU は 1 リクエストあたり 10ms しかない。
 * ハンドラの仕事は検証、D1 のクエリ、JSON の整形に留めること。
 * 栄養値の算出とレシピ提案のスコア計算はブラウザ側で動かす。
 */
const app = new Hono<AppEnv>();

app.onError(onError);

// 疎通確認。認証を必要としない唯一の API とする。
app.get("/api/health", (c) => {
  const body: HealthResponse = { status: "ok", time: new Date().toISOString() };
  return c.json(body);
});

app.route("/", meRoutes);
app.route("/", ingredientRoutes);
app.route("/", preferenceRoutes);

// 未定義の API パスは 404 を JSON で返す。index.html を返さない。
app.all("/api/*", (c) => c.json({ code: "NOT_FOUND", message: "該当する API がない" }, 404));

// それ以外は SPA のシェルを返す。/recipes のような画面側のパスへ直接来た場合や、
// 再読み込みされた場合がこれにあたる。ASSETS が index.html に解決する。
app.get("*", (c) => c.env.ASSETS.fetch(c.req.raw));

export default app;

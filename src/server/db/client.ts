import { drizzle } from "drizzle-orm/d1";
import * as schema from "./schema";

/** D1 への接続。リクエストごとに作る。使い回すとバインディングの寿命と合わない。 */
export function getDb(env: Env) {
  return drizzle(env.DB, { schema });
}

export type Db = ReturnType<typeof getDb>;

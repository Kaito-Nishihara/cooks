import { eq } from "drizzle-orm";
import type { Db } from "../client";
import { users, type User } from "../schema";

/** メールアドレスで利用者を引く。Access が確認した識別子との突き合わせに使う。 */
export async function findUserByEmail(db: Db, email: string): Promise<User | undefined> {
  const rows = await db
    .select()
    .from(users)
    .where(eq(users.email, email.toLowerCase()))
    .limit(1);
  return rows[0];
}

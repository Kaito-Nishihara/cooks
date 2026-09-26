import { sqliteTable, text, integer, uniqueIndex } from "drizzle-orm/sqlite-core";

/**
 * 利用者。
 * 一般公開しないため、サインアップ画面は設けず初期ユーザーはシードで作成する。
 */
export const users = sqliteTable(
  "users",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    email: text("email").notNull(),
    displayName: text("display_name").notNull(),
    // Workers では bcrypt のネイティブ実装が動かないため、Web Crypto の PBKDF2 を使う。
    // 形式は "pbkdf2$<反復回数>$<salt(base64)>$<hash(base64)>" とする。
    passwordHash: text("password_hash").notNull(),
    role: text("role", { enum: ["OWNER", "PARTNER"] }).notNull(),
    createdAt: integer("created_at", { mode: "timestamp" }).notNull(),
    updatedAt: integer("updated_at", { mode: "timestamp" }).notNull(),
  },
  (t) => [uniqueIndex("users_email_idx").on(t.email)],
);

export type User = typeof users.$inferSelect;
export type NewUser = typeof users.$inferInsert;

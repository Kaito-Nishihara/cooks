/**
 * 初期利用者を投入する SQL を標準出力へ書く。
 *
 * 認証は Cloudflare Access が担うため、ここで作るのはアプリ側の利用者の行だけである。
 * Access 側でも同じメールアドレスを許可しておかないとログインできない。
 *
 * 使い方(package.json の db:seed):
 *   node --env-file-if-exists=.dev.vars scripts/seed.ts > .seed.sql
 *   wrangler d1 execute mana-cooks --local --file .seed.sql
 */

interface SeedUser {
  email: string;
  displayName: string;
  role: "OWNER" | "PARTNER";
}

function required(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`環境変数 ${name} が無い。.dev.vars を用意する(.dev.vars.example を参照)`);
  }
  return value;
}

/** SQL の文字列リテラルへ埋め込む。単引用符だけを退避すればよい。 */
function quote(value: string): string {
  return `'${value.replaceAll("'", "''")}'`;
}

const users: SeedUser[] = [
  {
    email: required("SEED_OWNER_EMAIL").toLowerCase(),
    displayName: process.env["SEED_OWNER_NAME"] ?? "オーナー",
    role: "OWNER",
  },
  {
    email: required("SEED_PARTNER_EMAIL").toLowerCase(),
    displayName: process.env["SEED_PARTNER_NAME"] ?? "パートナー",
    role: "PARTNER",
  },
];

const now = Math.floor(Date.now() / 1000);

const lines = users.map((user) => {
  const values = [quote(user.email), quote(user.displayName), quote(user.role), now, now].join(", ");
  // 再実行できるようにする。既に居る利用者は表示名と役割だけを更新する。
  return [
    "INSERT INTO users (email, display_name, role, created_at, updated_at)",
    `VALUES (${values})`,
    "ON CONFLICT(email) DO UPDATE SET",
    "  display_name = excluded.display_name,",
    "  role = excluded.role,",
    `  updated_at = ${now};`,
  ].join("\n");
});

process.stdout.write(`${lines.join("\n\n")}\n`);

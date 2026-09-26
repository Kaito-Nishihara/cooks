---
name: worker-api
description: Hono の Worker API(ルート、ミドルウェア、D1 クエリ)とドメインロジックを作成・修正するときに使用する。レシピ、食材、嗜好、献立、買い物リスト、調理ログ、認証のエンドポイント追加や修正のリクエストに対して使用する。
---

# Worker API 実装スキル

## 層の分け方

| 層 | 置き場所 | 責務 |
|---|---|---|
| ルート | `src/server/routes/<resource>.ts` | HTTP の受け口。検証、クエリの呼び出し、JSON の整形 |
| ミドルウェア | `src/server/middleware/` | 認証、例外から HTTP ステータスへの変換 |
| クエリ | `src/server/db/queries/<resource>.ts` | Drizzle を使った D1 の読み書き |
| ドメイン | `src/domain/<context>/` | 業務ロジック。React にも Hono にも Drizzle にも依存しない |
| 共有スキーマ | `src/shared/<resource>.ts` | Zod スキーマと型。client と server で同じものを使う |

## 最優先の制約

Workers 無料プランの CPU は 1 リクエストあたり 10 ms である。
ハンドラの仕事は**検証、クエリ、JSON の整形まで**とする。

次のものを Worker で実行しない。

- 栄養値の算出(`src/domain/nutrition/`)
- レシピ提案のスコア計算(`src/domain/suggestion/`)
- 成分表 JSON の読み込みと走査

いずれもブラウザ側で動かす。
Worker は素材となるデータ(レシピ、嗜好、材料)を返すだけにする。

## 実装の手順

1. **Zod スキーマを先に書く**

   `src/shared/<resource>.ts` に入力と出力のスキーマを定義する。
   画面のフォーム検証と API の検証で同じスキーマを使う。

2. **クエリを書く**

   `src/server/db/queries/` に置く。Drizzle のクエリをルートへ直接書かない。
   すべての一覧取得に `limit` を付ける。無条件の全件取得をしない。
   複数テーブルにまたがる更新は `db.batch()` にまとめる。D1 に対話的トランザクションはない。

3. **ルートを書く**

   ```ts
   // 例: レシピの登録
   app.post("/api/recipes", requireUser, zValidator("json", createRecipeSchema), async (c) => {
     const user = c.get("user");
     const input = c.req.valid("json");
     const recipe = await createRecipe(c.env.DB, user.id, input);
     return c.json(recipe, 201);
   });
   ```

   - `requireUser` ミドルウェアを必ず通す。認可は Worker 側で判定する。クライアントのルートガードは表示の都合にすぎない。
   - 他人のデータを引かないよう、クエリの条件に `userId` を入れる。
   - ドメイン例外はミドルウェアでステータスへ変換する。ハンドラで `try/catch` を書き散らさない。

4. **ドメインロジックを書く**

   `src/domain/` は依存を持たない純粋な関数とする。
   ブラウザと Worker の両方で動くことが前提のため、`fs` や Node 固有の API を使わない。
   失敗は `src/domain/errors.ts` の `NotFoundError`、`ConflictError`、`ValidationError` で表す。

5. **テストを書く**

   ドメインロジックは Vitest で素の関数として試す。モックは要らない。
   クエリは D1 のローカルエミュレーションに対して試す。

## Workers で使えないもの

- Node の `fs`、`path`、`crypto` のネイティブ実装
- bcrypt のネイティブ実装。パスワードのハッシュ化は Web Crypto の PBKDF2 等で行う
- 長時間動く処理、バックグラウンドの常駐

## 安全に関わる扱い

嗜好の `ALLERGY` は安全に関わる。
レシピ提案の除外ロジックはブラウザ側で動くが、**そのデータを返す API が嗜好を取りこぼさないこと**を確認する。
嗜好の一覧取得に `limit` を付ける場合、未登録分が欠けて安全側の判定が崩れないよう、件数の上限を十分に取るか全件返す設計にする。

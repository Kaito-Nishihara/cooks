---
name: d1-schema
description: Drizzle のスキーマ変更、D1 のマイグレーション作成と適用、データモデルの追加・修正を行うときに使用する。mana-cooks の食材・嗜好・レシピ・献立・買い物リストのテーブル追加やカラム追加、リレーション変更に対して使用する。
---

# D1 スキーマ スキル

スキーマ: `src/server/db/schema.ts`(Drizzle)
マイグレーション: `drizzle/`(`drizzle-kit` が生成)
実行はすべて Docker コンテナ内で行う。

## データモデル概略

```
User ──< IngredientPreference >── Ingredient ──< IngredientAlias
User ──< CookingLog >── Recipe
User ──< Favorite >── Recipe
Recipe ──< RecipeIngredient >── Ingredient
Recipe ──< RecipeStep
Recipe ──< RecipeTag >── Tag
Recipe ──> Category
Ingredient ──< UnitConversion
Ingredient ──> public/data/foods.json   (代表食品番号。DB外の参照)
MenuEntry ──> Recipe
ShoppingList ──< ShoppingListItem ──> Ingredient
```

嗜好レベルは `LOVE / LIKE / NEUTRAL / DISLIKE / HATE / ALLERGY`。
食事区分は `BREAKFAST / LUNCH / DINNER / SNACK`。
D1 に enum 型はないため `text` に `{ enum: [...] }` を付けて表現し、値の集合は `src/shared/` に定数として一箇所で定義する。

成分表はデータベースに含めない。`public/data/foods.json` が正本である。
`ingredient.foodNumber` はその JSON のキーを指す文字列カラムで、外部キー制約は張れない。存在検証はアプリ側で行う。

## 命名規則

- テーブル名は snake_case の複数形、カラム名は snake_case とする。
- TypeScript 側の変数とプロパティは camelCase とし、Drizzle の定義で対応づける。
- 全テーブルに `createdAt` を持たせる(`integer("created_at", { mode: "timestamp" })`)。
- 更新されるテーブルには `updatedAt` も持たせる。

## 変更手順

1. 既存データへの影響を確認する。NOT NULL カラムの追加にはデフォルト値か二段階の移行が要る。
2. `src/server/db/schema.ts` を編集する。リレーションは `relations()` で両側に明示する。
3. マイグレーションを生成する。

   ```bash
   docker compose exec app npx drizzle-kit generate --name add_ingredient_alias
   ```

   生成された SQL を必ず読む。D1 はカラムの変更でテーブル再作成になる場合があり、意図しないデータ欠落が起きうる。
4. ローカルへ適用する。

   ```bash
   docker compose exec app npx wrangler d1 migrations apply mana-cooks --local
   ```
5. 影響範囲を更新する。
   - `src/server/db/queries/` のクエリ
   - `src/shared/` の Zod スキーマと型
   - `src/domain/` の型
   - `scripts/seed.ts` と対応する `public/data/*.json`
6. 型検査を通す: `docker compose exec app npx tsc --noEmit`

本番への適用は `--remote` を付けて行う。実行前に必ず確認を取る。

## D1 固有の注意

- 小数は浮動小数になる。成分値と分量は概算表示のため許容する。誤差が問題になる値をこの型で扱わない。
- 日時は整数(Unix 時間)で持つ。文字列の日時比較に頼らない。
- 外部キー制約は既定で無効の場合がある。参照整合性をアプリ側でも検証する。
- 無料プランは 1 日あたり読み 500 万行、書き 10 万行を超えるとクエリがエラーになる。全件走査を繰り返すクエリを書かない。一覧の取得には上限件数を設ける。
- トランザクションはバッチ(`db.batch()`)で表現する。D1 は対話的なトランザクションを持たない。複数テーブルにまたがる更新はバッチにまとめる。

## N:M リレーションの書き方

中間テーブルは明示的に定義する。付加情報を後から足せるようにするためである。

```ts
// レシピとタグの中間テーブル
export const recipeTags = sqliteTable("recipe_tags", {
  recipeId: integer("recipe_id").notNull().references(() => recipes.id, { onDelete: "cascade" }),
  tagId: integer("tag_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
}, (t) => ({
  pk: primaryKey({ columns: [t.recipeId, t.tagId] }),
}));
```

## 削除の扱い

物理削除のみとする。
レシピや嗜好から参照されている `ingredient` は削除させず、参照件数を確認してからエラーを返す。
`onDelete: "cascade"` はレシピ配下の従属データ(`recipe_ingredients`、`recipe_steps`、`recipe_tags`)に限って使う。

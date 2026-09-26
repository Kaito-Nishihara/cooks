Drizzle のスキーマ変更と D1 のマイグレーションのみを行います。

> 実装(API・画面)まで含めて自動で行う場合は `/implement` を使ってください。

変更内容: $ARGUMENTS

`.claude/skills/d1-schema/SKILL.md` の手順に従い、以下を進めてください。

1. **変更内容の整理**: 追加・変更・削除するテーブルとカラムを列挙する
2. **既存データへの影響分析**: NOT NULL カラムの追加、リレーションの変更、既存レコードの扱いを確認する
3. **スキーマ更新**: `src/server/db/schema.ts` を編集する
4. **マイグレーション生成**: `docker compose exec app npx drizzle-kit generate --name <name>`
5. **生成SQLの確認**: 生成された `drizzle/*.sql` を読む。D1 はカラム変更でテーブル再作成になる場合があり、データ欠落が起きうる
6. **ローカルへ適用**: `docker compose exec app npx wrangler d1 migrations apply mana-cooks --local`
7. **影響範囲の更新**: `src/server/db/queries/`、`src/shared/` の Zod スキーマ、`src/domain/` の型、`scripts/seed.ts` と `public/data/*.json`
8. **型検査**: `docker compose exec app npx tsc --noEmit` が通ることを確認する

本番(`--remote`)への適用は行わないでください。破壊的な変更(カラムの削除、型の変更)を含む場合は、適用前に影響を報告して確認を取ってください。

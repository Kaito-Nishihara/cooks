# implement - 機能追加オーケストレーター

機能名: **$ARGUMENTS**

あなたはオーケストレーターです。
上記の機能を実装するため、以下の 4 ステップを順番にサブエージェントへ委任してください。
各ステップは前のステップの成果物を引き継ぎます。

## 全エージェント共通の前提

すべてのサブエージェントに、着手前に `CLAUDE.md` と `docs/requirements.md` を読むよう指示してください。
技術構成は Vite + React の SPA、Hono on Cloudflare Workers、Drizzle + D1、画像は R2 です。
層は `src/client/` / `src/server/` / `src/domain/` / `src/shared/` に分かれます。
読み取り専用のマスタ(成分表、カテゴリ)は `public/data/*.json` にあり、データベースには入っていません。

**Workers 無料プランの CPU は 1 リクエストあたり 10 ms です。**
サーバー側で React を描画せず、Worker のハンドラで重い計算をしません。
栄養値の算出とレシピ提案のスコア計算は `src/domain/` に置き、ブラウザ側で実行します。
この制約をすべてのサブエージェントへ明示してください。
コーディング規約はコメント日本語、`any` 禁止、Zod による入力検証、戻り値の型の明示です。

---

## ステップ1: Planner

`/plan $ARGUMENTS` と同じ指示でサブエージェントを起動し、計画書を受け取ってください。
計画書の「データモデルの変更」「作業順序」「懸念点」をユーザーへ提示し、**着手の可否を確認してから**ステップ2へ進んでください。

## ステップ2: スキーマとドメイン層

計画書を渡し、次を実装させてください。

- `.claude/skills/d1-schema/SKILL.md` に従ったスキーマ変更とマイグレーション
- `src/shared/` の Zod スキーマと型
- `src/domain/<context>/` の業務ロジック
- `src/server/db/queries/` のクエリ

`src/domain/` が React にも Hono にも Drizzle にも依存していないことを確認させてください。

## ステップ3: API と画面

ステップ2の成果物を渡し、次を実装させてください。

- `.claude/skills/worker-api/SKILL.md` に従った Hono のルートとミドルウェア
- `.claude/skills/react-screen/SKILL.md` に従った画面、API クライアント、再利用コンポーネント

## ステップ4: テストと検証

`/test-gen` と同じ指示で、ドメインサービスの単体テストを生成・実行させてください。
あわせて次を実行し、結果を報告させてください。

```bash
docker compose exec app npx tsc --noEmit
docker compose exec app npm run test
docker compose exec app npm run build
```

`npm run build` の出力から、Worker のバンドルサイズが増えていないかを確認させてください。

---

## 最終報告

すべてのステップ完了後、次をユーザーへ報告してください。

- 追加・変更したファイルの一覧と、それぞれの役割
- マイグレーション名と、データベースへの影響
- テストの実行結果(通過数と失敗内容)
- 計画から外れた判断と、その理由
- 無料枠への影響(D1 の読み書きが増える箇所、Worker の CPU を使う箇所、バンドルサイズの変化)
- 残課題(実装しなかった部分、手動確認が必要な部分)

失敗したテストや型エラーを残したまま完了と報告しないでください。

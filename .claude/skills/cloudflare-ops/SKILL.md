---
name: cloudflare-ops
description: wrangler.jsonc、Docker Compose のローカル開発環境、Cloudflare への配置、D1 と R2 のバインディング、環境変数、バックアップを作成・修正するときに使用する。開発環境の起動やデプロイ、無料枠の確認に関するリクエストに対して使用する。
---

# Cloudflare 運用スキル

## 構成

Worker は一つだけとする。静的アセットと `/api/*` の両方を同じ Worker が受ける。

| 資源 | バインディング | 用途 |
|---|---|---|
| D1 | `DB` | 利用者が書き込むデータ |
| R2 | `IMAGES` | レシピと手順の画像 |
| Static Assets | `ASSETS` | SPA のビルド成果物と `public/data/*.json` |

## SPA のフォールバック

`not_found_handling: "single-page-application"` を設定しても、Worker があるとアセットに無い URL は
まず Worker へ渡る。Worker 側で受けないと Hono の 404 が返る(実測で確認済み)。

そのため `src/server/index.ts` の最後に次を置いている。消さないこと。

```ts
app.get("*", (c) => c.env.ASSETS.fetch(c.req.raw));
```

`/api/*` の 404 はこれより前で JSON として返す。API のパス誤りに index.html が返ると原因が分かりにくいためである。

## 無料枠

| 資源 | 上限 |
|---|---|
| Workers リクエスト | 10 万/日 |
| Workers CPU | 10 ms/回 |
| 静的アセット | 無制限・無料 |
| D1 | 5 GB、読み 500 万行/日、書き 10 万行/日 |
| R2 | 10 GB、Class A 100 万/月、Class B 1000 万/月 |

D1 は 2026 年 9 月 1 日から、無料アカウントで 1 日の行数上限を超えたクエリがエラーを返す。
超過は障害として現れるため、全件走査を繰り返すクエリを入れない。

CPU の 10 ms が最も厳しい制約である。
超えると `Error 1102` で失敗し、運用費 0 円が要件である以上、有料プランへの移行という逃げ道はない。
依存を足すときは圧縮後のバンドルサイズを確認する。起動時間も CPU に算入される。

## ローカル開発

Docker Compose 上で `vite dev` を動かす。サービスは `app` の一つだけとする。
D1 と R2 はローカルエミュレーションを使い、状態を名前付きボリュームへ永続化する。

開発サーバーは `vite dev` である。`@cloudflare/vite-plugin` が同じポートで Worker も動かすため、
`wrangler dev` を別に起動しない。コンテナ内から待ち受けるため `--host 0.0.0.0` を付ける。

```bash
docker compose up -d                          # http://localhost:5173
docker compose exec app npm run db:migrate    # ローカルの D1 へ適用
docker compose exec app npm run build         # バンドルサイズの確認を兼ねる
docker compose exec app npx wrangler types    # wrangler.jsonc を変えたら実行する
```

## 配置

```bash
docker compose exec app npx wrangler d1 migrations apply mana-cooks --remote   # 事前に確認を取る
docker compose exec app npx wrangler deploy
```

公開 URL は `workers.dev` のサブドメインを使う。独自ドメインは取得しない。
`--remote` を伴う操作は本番データへ影響するため、実行前に必ずユーザーへ確認を取る。

## 秘密情報

- `wrangler secret put <NAME>` で登録する。`wrangler.jsonc` に書かない。
- ローカルは `.dev.vars` を使い、コミットしない。`.dev.vars.example` にキーだけを列挙する。
- `public/data/*.json` は静的アセットとして配信され、ブラウザから直接取得できる。秘密情報を置かない。

| キー | 用途 |
|---|---|
| `AUTH_SECRET` | JWT 署名鍵 |
| `SEED_OWNER_EMAIL` / `SEED_PARTNER_EMAIL` | 初期ユーザー |

## バックアップ

D1 のエクスポートを日次で取得し、7 世代を保持する。

```bash
docker compose exec app npx wrangler d1 export mana-cooks --remote --output "backup/$(date +%F).sql"
```

R2 の画像は、オブジェクトの一覧と併せて保管する。
開発段階では手動実行とし、運用開始後に Cron Triggers か手元の cron へ登録する。

## 破壊的な操作

`wrangler d1 execute` による直接の DELETE や DROP、`--remote` を伴うマイグレーションの適用は、実行前にユーザーへ確認を取る。
D1 に手軽な巻き戻しはない。

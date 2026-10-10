# mana-cooks

二人で使うクッキングアプリ。
好き嫌いの食材管理、レシピ管理、嗜好に基づくレシピ提案、献立カレンダーと買い物リスト、栄養値の概算表示を持つ。

要件定義は [docs/requirements.md](docs/requirements.md) が正本である。

## 技術構成

| レイヤ | 採用 |
|---|---|
| 画面 | Vite + React + TypeScript + Tailwind CSS(SPA) |
| API | Hono on Cloudflare Workers |
| ORM | Drizzle ORM |
| データベース | Cloudflare D1 (SQLite 互換) |
| マスタデータ | `public/data/*.json`。静的アセットとして配信する |
| 画像保存 | Cloudflare R2 |
| ローカル開発 | Docker Compose 上の `vite dev`(@cloudflare/vite-plugin が Worker も動かす) |
| テスト | Vitest(単体) / Playwright(E2E) |

## 最優先の制約: Workers 無料プランの CPU 10 ms

Workers の無料プランは 1 リクエストあたり CPU 時間が 10 ms である。
これを超えると `Error 1102` で失敗する。運用費 0 円が要件のため、有料プランへの移行という逃げ道はない。

この制約から次が決まっている。

- **サーバー側で React を描画しない。** 画面は静的アセットとして配り、ブラウザが描画する。
- **Worker のハンドラで重い計算をしない。** 仕事は検証、D1 のクエリ、JSON の整形に留める。
- **栄養値の算出とレシピ提案のスコア計算はブラウザ側で動かす。** どちらも `src/domain/` に置き、Worker では実行しない。
- **重い依存を足さない。** バンドルの起動時間も CPU に算入される。ORM に Prisma ではなく Drizzle を使うのはこのためである。
- **`public/data/*.json` をバンドルへ取り込まない。** 静的アセットとして配り、ブラウザが取得してキャッシュする。

## 日付と日時

記録した瞬間(`createdAt` 等)は Unix 時間の整数で持つ。
暦の上の一日(献立の日付、調理した日)は `"YYYY-MM-DD"` の文字列で持つ。
後者を時刻付きにすると時間帯の扱いで前後の日にずれる。

## ディレクトリ構成

```
src/
├── client/            # React SPA
│   ├── routes/        # 画面(React Router)
│   ├── components/    # 再利用UI
│   └── api/           # Worker API を呼ぶ fetch クライアント
├── server/            # Hono の Worker
│   ├── index.ts       # エントリ。静的アセットと /api/* の振り分け
│   ├── routes/        # リソースごとのハンドラ
│   ├── middleware/    # 認証、エラー変換
│   └── db/
│       ├── schema.ts  # Drizzle のスキーマ
│       └── queries/   # クエリ
├── domain/            # 業務ロジック。client と server の両方から使う
│   ├── nutrition/     # 栄養値の算出(ブラウザで実行)
│   ├── suggestion/    # レシピ提案のスコア計算(ブラウザで実行)
│   └── errors.ts
└── shared/            # Zod スキーマと型。client と server で共有する
drizzle/               # drizzle-kit が生成するマイグレーション
public/data/           # マスタデータの JSON。/data/*.json として配信される
scripts/               # 成分表の変換、初期データ投入
e2e/                   # Playwright
index.html             # SPA のシェル
wrangler.jsonc
vite.config.ts
compose.yaml
```

型検査は `tsconfig.app.json`(`src/`)と `tsconfig.node.json`(設定と `scripts/`)に分かれている。
`src/` からは Node の型を見せない。Worker でもブラウザでも動かないコードが型検査を通るのを防ぐためである。

`src/domain/` は React にも Hono にも Drizzle にも依存させない。
同じコードをブラウザと Worker の両方で動かすための制約である。
`src/shared/` の Zod スキーマは、画面の入力検証と API の入力検証で同じものを使う。

## データの置き場の使い分け

| 性質 | 置き場 | 対象 |
|---|---|---|
| 読み取り専用で版ごと差し替える | `public/data/*.json` | 成分表、単位換算の初期値 |
| 固定の集合 | `src/shared/enums.ts` | 嗜好レベル、食材の分類、レシピの分類、食事区分 |
| 利用者が書き込む | D1 | 利用者、食材マスタ、嗜好、レシピ、献立、買い物リスト、調理ログ |
| バイナリ | R2 | 画像 |

利用者が書き込むデータを JSON ファイルで持たない。
二人が同時に触る場面で読み書きが競合し、後から書いた側が相手の変更を消すためである。

成分表(`public/data/foods.json`、URL は `/data/foods.json`)はデータベースへ入れない。
`Ingredient.foodNumber` はこの JSON のキーを指すため外部キー制約が効かない。存在検証はアプリ側で行う。

## 認証

Cloudflare Access が Worker の手前で認証する。アプリはパスワードを持たない。
`requireUser`(`src/server/middleware/auth.ts`)が、Access の確認したメールアドレスを
`users` 表と突き合わせて利用者を解決する。

- 疎通確認(`/api/health`)以外のすべての API に `requireUser` を通す。
- ローカルでは `.dev.vars` の `DEV_USER_EMAIL` の利用者として扱う。本番の環境変数に置かないこと。
- パスワードを扱う実装を足さない。workerd の PBKDF2 は 10 万回で頭打ちで、
  その回数で CPU 10〜30 ms を使う。無料プランの 10 ms に収めるには強度を落とすしかない。

## コーディング規約

- コメントは日本語で書く。
- `any` は使わない。型が定まらない箇所は `unknown` から絞り込む。
- 入力値の検証は Zod で行い、スキーマを `src/shared/` に集約する。
  ルートでは `src/server/lib/validator.ts` の `validate` を使う。エラーの形を揃えるためである。
- API のエラーは `{ code, message }` で返す。画面側は `src/client/api/request.ts` 経由で呼ぶ。
- ドメインの例外は `src/domain/errors.ts` の独自例外クラスを使い、Hono のミドルウェアで HTTP ステータスへ変換する。
- すべての公開関数に戻り値の型を明示する。
- D1 のクエリに上限件数を設ける。`SELECT *` の無条件取得をしない。
- 認可は必ず Worker 側で判定する。クライアント側のルートガードは表示の都合にすぎない。
- テストの設定は `vitest.config.ts` に分ける。`@cloudflare/vite-plugin` は Vitest と同居できない。
- Node の API(`fs`、`crypto` のネイティブ実装等)を Worker のコードで使わない。Web 標準の API を使う。

## よく使うコマンド

```bash
docker compose up -d                          # 開発環境の起動。http://localhost:5173
docker compose logs -f app                    # 開発サーバーのログ
docker compose exec app npm run typecheck     # 型検査
docker compose exec app npm run build         # 本番バンドルとサイズの確認
docker compose exec app npm run test          # Vitest
docker compose exec app npm run test:e2e      # Playwright
docker compose exec app npm run db:generate   # マイグレーション生成
docker compose exec app npm run db:migrate    # ローカルの D1 へ適用
docker compose exec app npm run db:seed       # 初期利用者を投入する
docker compose exec app npx wrangler types    # バインディングの型を再生成
```

`wrangler.jsonc` を変更したら `wrangler types` を実行する。`worker-configuration.d.ts` は生成物でコミットしない。

Node の実行は常に Docker コンテナ内で行う。ホスト側に依存を入れない。

日本語を含むリクエストを手で試すときは `curl` に直接渡さない。
Windows のシェルを経由すると UTF-8 が壊れ、壊れた文字列がそのまま保存される。
`docker compose exec -T app node -e '...'` の中で `fetch` を使う。

## 文章の規約

日本語の文章は [.claude/rules/writing-style.md](.claude/rules/writing-style.md) に従う。

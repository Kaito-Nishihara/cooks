# おうちレシピ帖

Claude Code が作ったレシピを、ブラウザで見て、自分の作り方に合わせて直し、栄養の値を確かめるためのアプリ。
直した内容と献立は自分のレシピ帳に保存し、別のアカウントの人を招待して共有できる。

このリポジトリには、もう一つの目的がある。
Next.js、Quarkus、マイクロサービス、Kubernetes を、動くものを作りながら学ぶこと。
そして、この構成で Claude Code に作らせて確かめさせる進め方(AI 駆動開発)を試すことである。

## 今の状態

**設計の段階にある。アプリのコードはまだない。**

要件定義を書き終え、共通の設計から順に仕様書を書いている。
進み具合は [設計フェーズの進行(Issue #16)](https://github.com/Kaito-Nishihara/cooks/issues/16) で追える。

| 段階 | 状態 |
|---|---|
| 要件定義 | 済み |
| 共通の設計、画面の設計、機能ごとの設計 | 進行中 |
| 開発の土台(空のアプリを Kubernetes に載せる) | これから |
| 機能の実装 | これから |

## できるようにすること

- レシピの一覧、検索、ジャンルでの絞り込み
- レシピの詳細と、人数に合わせた分量の換算
- 材料と分量からの栄養計算(エネルギー、たんぱく質、脂質、炭水化物、食塩相当量)
- メールアドレスとパスワードでの登録とログイン
- レシピの編集(材料、分量、手順、コツ)と、レシピ帳への保存
- 今日の献立と、1 日の栄養の値の合計
- レシピ帳への招待と共有(見るだけ、編集できる)

レシピを作るのは Claude Code であり、アプリは AI を呼び出さない。
詳しくは [要件定義](docs/requirements.md) にある。

## 構成

ブラウザからの要求は入口を通り、画面の静的なファイルか、3 つのサービスのどれかに振り分けられる。
サービスはそれぞれ自分のデータベースを持つ。

| 場所 | 役割 | 使うもの |
|---|---|---|
| `frontend` | 画面 | Next.js(静的に書き出す)、React、TypeScript、Tailwind CSS |
| `auth-service` | 登録、ログイン、セッション | Quarkus、Java 21、PostgreSQL |
| `book-service` | レシピ帳、メンバー、招待 | Quarkus、Java 21、PostgreSQL |
| `kitchen-service` | レシピの編集内容、献立 | Quarkus、Java 21、PostgreSQL |
| 実行環境 | コンテナの実行 | Kubernetes(手元では kind) |

図は [docs/architecture.html](docs/architecture.html) にある。
仕様書のサイト(下記)から開くと、図として表示される。

## ディレクトリ

| ディレクトリ | 中身 |
|---|---|
| `docs/` | 仕様書。要件定義、設計、決定の記録、アーキテクチャ図 |
| `docs-site/` | 仕様書をブラウザで読むためのサイト。アプリ本体とは別のもの |
| `frontend/` | 画面。今は開発の決まり(`CLAUDE.md`)だけがある |
| `services/` | サービス。今は開発の決まり(`CLAUDE.md`)だけがある |
| `recipes/` | 作り替える前の静的なレシピ集(38 品)。新しい形式へ移した後に削除する |
| `.claude/` | Claude Code の設定、規約、スキル |

## 仕様書を読む

Docker が要る。

```bash
docker compose -f docs-site/compose.yaml up -d
```

ブラウザで <http://localhost:8000> を開く。
`docs/` の Markdown を保存すると、表示が自動で更新される。

```bash
docker compose -f docs-site/compose.yaml down    # 停止
```

主な文書は次のとおりである。

| 文書 | 内容 |
|---|---|
| [要件定義](docs/requirements.md) | 何を作るか、何を作らないか。受け入れ条件、技術構成、データモデル、API |
| [共通設計: API の共通仕様](docs/design/common-api.md) | パス、JSON、エラー、識別子、一覧の分け方、改訂番号 |
| [共通設計: 認証](docs/design/common-auth.md) | パスワード、セッションとクッキー、入口での確認、サービスの間のトークン |
| [共通設計: データベース](docs/design/common-database.md) | データベースの分け方、名前と型、テーブルを変える手順 |
| [共通設計: サービスの共通構成](docs/design/common-service.md) | パッケージと層、ログ、設定、ほかのサービスの呼び出し方、テストの方針 |
| [デザイン: アプリのメインアイコン](docs/design/app-icon.md) | 絵柄の候補、形と色の決まり、書き出す大きさと形式 |
| [決定の記録](docs/decisions/) | 技術や設計の選択と、その理由 |

## 進め方

- 作業は Issue ごとにブランチを切り、PR で `main` に入れる。
- 機能は、仕様書を書いて承認を得てから実装する。
- 受け入れ条件を先にテストにし、それが通るように実装する。

Claude Code 向けの決まりは [CLAUDE.md](CLAUDE.md) と [.claude/rules/](.claude/rules/) にある。

## 以前の構成

Vite と Cloudflare Workers で作っていた以前のアプリは、`main` の履歴のコミット [`868a0bf`](https://github.com/Kaito-Nishihara/cooks/tree/868a0bf) に残っている。

## 出典

- 文部科学省「日本食品標準成分表(八訂)増補2023年」
- 厚生労働省「日本人の食事摂取基準(2020年版)」
- `recipes/` の写真は Wikimedia Commons のもので、出典とライセンスを各ページのフッターに記している

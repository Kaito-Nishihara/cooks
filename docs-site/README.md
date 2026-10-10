# docs-site

仕様書(`../docs/` の Markdown)をブラウザで読むためのサイト。
アプリ本体とは別のもので、アプリの起動、ビルド、配置には関わらない。

| 置き場 | 中身 |
|---|---|
| `../docs/` | 文書の中身。Markdown と図だけを置く |
| `mkdocs.yml` | サイトの設定と、文書の並び(`nav`) |
| `overrides/assets/docs.css` | 見た目 |
| `hooks/` | 表示のときに Markdown へかける変換 |
| `compose.yaml` | サイトの起動 |

## 使い方

```bash
docker compose -f docs-site/compose.yaml up -d      # 起動。http://localhost:8000
docker compose -f docs-site/compose.yaml logs docs  # 警告を確かめる
docker compose -f docs-site/compose.yaml down       # 停止
```

文書、見た目、変換の処理のどれを保存しても、表示が自動で更新される。

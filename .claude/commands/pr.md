PR の説明文を作成します。

対象ブランチ: **$ARGUMENTS**(省略時は現在のブランチ)

> コード品質のチェックは `/review-code` で先に実施し、🔴 必須修正がゼロになってから実行してください。

## 事前確認

- [ ] `/review-code` を実行済みで 🔴 必須修正がゼロである
- [ ] `docker compose exec app npm run test` が通過している
- [ ] `docker compose exec app npx tsc --noEmit` が通過している

## 作成手順

`git log main..HEAD --oneline` と `git diff main...HEAD --stat` で変更内容を把握し、
`.claude/rules/pr-authoring.md` の構成と分量に従って本文を日本語で書いてください。
文章は `.claude/rules/writing-style.md` に従います。

本文を作成したら、`gh pr create --draft` で作成するかをユーザーに確認してください。

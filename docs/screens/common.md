---
hide:
  - toc
---

# 画面: 共通の部品と状態

ボタン、知らせ、通知、ダイアログ、読み込めないときの表示を並べたものである。画面ではない。

| 項目 | 内容 |
|---|---|
| パス | どの画面にも出る |
| 骨組みと遷移 | [画面設計: 画面の一覧と遷移](../design/screens.md) の 3.3 と 3.8 |

## 対応する機能設計

| 文書 | この画面について決めていること |
|---|---|
| [共通設計: 画面の共通仕様](../design/common-frontend.md) | 部品の一覧、色と書体、読み込み中と空とエラーの出し方、401 と 409 のときの動き |

## 見た目の試作

幅 1,100px のブラウザでの表示である。
枠の中は、押したりスクロールしたりできる。保存はされない。

### 部品と状態の一覧

<div class="screen-frame"><iframe src="../../mockups/states.html" title="共通の部品と状態の試作。部品と状態の一覧" loading="lazy"></iframe></div>

[別のタブで開く](../mockups/states.html)

## ダイアログ

リンクを押すと、ダイアログを開いた状態で、別のタブに出る。

| ダイアログ | 開く |
|---|---|
| ほかの人が先に変更したとき | [開く](../mockups/states.html#dialog=dlg-conflict) |
| ログインが切れたとき | [開く](../mockups/states.html#dialog=dlg-relogin) |
| 取り消せない操作の確認 | [開く](../mockups/states.html#dialog=dlg-confirm) |

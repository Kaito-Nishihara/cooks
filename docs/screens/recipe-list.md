---
hide:
  - toc
---

# 画面: レシピ一覧

レシピを探して選ぶ画面である。検索、ジャンルの絞り込み、献立に足す入口を持つ。

| 項目 | 内容 |
|---|---|
| パス | `/` |
| ログイン | 要らない |
| 骨組みと遷移 | [画面設計: 画面の一覧と遷移](../design/screens.md) の 3.3 と 3.8 |

## 対応する機能設計

| 文書 | この画面について決めていること |
|---|---|
| [機能設計: レシピ一覧と詳細](../design/recipe-browse.md) | カードの項目、検索、ジャンルの絞り込み、並び順、編集済みの印 |
| [機能設計: 今日の献立](../design/menu.md) | 「献立に追加」の動きと、画面の下の献立の枠 |

## 見た目の試作

幅 1,100px のブラウザでの表示である。
枠の中は、押したりスクロールしたりできる。保存はされない。

### ログインしている人(持ち主)

「編集済み」の印と、画面の下の献立の枠が出る。

<div class="screen-frame"><iframe src="../../mockups/list.html" title="レシピ一覧の試作。ログインしている人(持ち主)" loading="lazy"></iframe></div>

[別のタブで開く](../mockups/list.html)

### ログインしていない人

「今日の献立」と「献立に追加」は出すが、押すとログインの画面へ移る。

<div class="screen-frame"><iframe src="../../mockups/list-anonymous.html" title="レシピ一覧の試作。ログインしていない人" loading="lazy"></iframe></div>

[別のタブで開く](../mockups/list-anonymous.html)

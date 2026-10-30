---
hide:
  - toc
---

# 画面: レシピ詳細

1 つのレシピを読んで作る画面である。材料、手順、コツ、栄養の値を出す。

| 項目 | 内容 |
|---|---|
| パス | `/recipes/<slug>` |
| ログイン | 要らない |
| 骨組みと遷移 | [画面設計: 画面の一覧と遷移](../design/screens.md) の 3.3 と 3.8 |

## 対応する機能設計

| 文書 | この画面について決めていること |
|---|---|
| [機能設計: レシピ一覧と詳細](../design/recipe-browse.md) | 項目と文言、人数の切り替えと分量の表記、編集後と元のレシピの切り替え |
| [機能設計: 栄養計算](../design/nutrition.md) | 栄養の値の枠、計算から除いた材料の出し方 |
| [機能設計: 今日の献立](../design/menu.md) | 「献立に追加」の動き |
| [機能設計: レシピ帳とレシピの編集](../design/recipe-edit.md) | 編集内容を元のレシピに重ねる決まり、元のレシピが更新されたときの知らせ |

## 見た目の試作

幅 1,100px のブラウザでの表示である。
枠の中は、押したりスクロールしたりできる。保存はされない。

### ログインしている人(持ち主)。編集内容があるレシピ

編集後の内容(鶏もも肉 300g)と、元のレシピとの切り替えが出る。

<div class="screen-frame"><iframe src="../../mockups/detail.html" title="レシピ詳細の試作。ログインしている人(持ち主)。編集内容があるレシピ" loading="lazy"></iframe></div>

[別のタブで開く](../mockups/detail.html)

### ログインしていない人

元のレシピが出る。「編集する」と「献立に追加」は、押すとログインの画面へ移る。

<div class="screen-frame"><iframe src="../../mockups/detail-anonymous.html" title="レシピ詳細の試作。ログインしていない人" loading="lazy"></iframe></div>

[別のタブで開く](../mockups/detail-anonymous.html)

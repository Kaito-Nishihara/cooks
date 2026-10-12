# 画面一覧

新しいアプリの画面を、1 つずつ並べる。
画面ごとのページに、見た目の試作と、対応する機能設計へのリンクがある。

| 画面 | パス | ログイン | 対応する機能設計 |
|---|---|---|---|
| [レシピ一覧](recipe-list.md) | `/` | 要らない | [レシピ一覧と詳細](../design/recipe-browse.md)、[今日の献立](../design/menu.md) |
| [レシピ詳細](recipe-detail.md) | `/recipes/<slug>` | 要らない | [レシピ一覧と詳細](../design/recipe-browse.md)、[栄養計算](../design/nutrition.md)、[今日の献立](../design/menu.md)、[レシピ帳とレシピの編集](../design/recipe-edit.md) |
| [レシピの編集](recipe-edit.md) | `/recipes/<slug>/edit` | 要る。編集できる人と持ち主 | [レシピ帳とレシピの編集](../design/recipe-edit.md)、[栄養計算](../design/nutrition.md) |
| [今日の献立](menu.md) | `/menu`、`/menu?day=<日付>` | 要る。メンバーの全員。変えられるのは、編集できる人と持ち主 | [今日の献立](../design/menu.md)、[栄養計算](../design/nutrition.md) |
| [レシピ帳の設定](book.md) | `/book` | 要る。メンバーの全員。変えられるのは持ち主 | [レシピ帳とレシピの編集](../design/recipe-edit.md)、[招待と共有](../design/sharing.md) |
| [アカウント](account.md) | `/account` | 要る | [登録とログイン](../design/auth.md) |
| [ログイン](login.md) | `/login` | 要らない | [登録とログイン](../design/auth.md) |
| [登録](register.md) | `/register` | 要らない | [登録とログイン](../design/auth.md)、[招待と共有](../design/sharing.md) |
| [招待の確認と参加](invite.md) | `/invite#<招待の文字列>` | 要る | [招待と共有](../design/sharing.md) |
| [共通の部品と状態](common.md) | どの画面にも出る | なし | [画面の共通仕様](../design/common-frontend.md) |
| 見つからないページ | 上のどれにも当たらないパス | 要らない | 試作は、まだ作っていない |

## 試作の扱い

- 見た目の試作は、動かない HTML である。実装の前に、見た目を確かめるために置く。
- 色、書体、余白、部品は、[共通設計: 画面の共通仕様](../design/common-frontend.md) の 3 章の値で作った。
- 配置は、[画面設計: 画面の一覧と遷移](../design/screens.md) の 3.8 の骨組みに従った。
- 文言と項目は、機能ごとの仕様書に合わせた。食い違うときは、仕様書が正しい。
- 押して動くのは、メニューの開閉、ダイアログの開閉、絞り込みの印の切り替え、手順の「済」だけである。
- 栄養の値は、表示の例である。計算した値ではない。
- 写真は、公開している今のレシピ集のものを読み込む。インターネットにつながっていないと、写真が出ない。
- 実装のコードには使わない。実装は Next.js と Tailwind で書き直す。

## まだ作っていないもの

- 見るだけの人、編集できる人として開いたときの表示
- 読み込み中の骨組みの表示
- 見つからないページ
- アレンジ版のレシピ
- 献立が空のときの画面の全体

## 試作の置き場

`docs/mockups/` に、画面ごとの HTML と、共通の `mock.css`、`mock.js` を置く。
ヘッダーとフッターは、`mock.js` が差し込む。
スマホの幅での見た目は、試作を別のタブで開き、ブラウザの幅を 820px より狭くすると見られる。

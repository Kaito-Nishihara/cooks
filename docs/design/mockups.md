# 画面設計: 見た目の試作

新しいアプリの画面の見た目を、動かない HTML で作ったものである。
実装の前に、スマホと広い画面でどう見えるかを確かめるために置く。

## この試作の扱い

- 色、書体、余白、部品は、[共通設計: 画面の共通仕様](common-frontend.md) の 3 章の値で作った。
- 配置は、[画面設計: 画面の一覧と遷移](screens.md) の 3.8 の骨組みに従った。
- 文言と項目は、機能ごとの仕様書に合わせた。食い違うときは、仕様書が正しい。
- 押して動くのは、メニューの開閉、ダイアログの開閉、絞り込みの印の切り替え、手順の「済」だけである。保存も、画面の間のデータの受け渡しもしない。
- 栄養の値は、表示の例である。計算した値ではない。
- 写真は、公開している今のレシピ集のものを読み込む。インターネットにつながっていないと、写真が出ない。
- 実装のコードには使わない。実装は Next.js と Tailwind で書き直す。

## 見方

下の枠は、幅 390px(スマホ)での表示である。
枠の中は、実際に押したりスクロールしたりできる。
広い画面での見た目は、各画面の「広い幅で開く」を押して、別のタブで見る。
ブラウザの幅を 820px より狭くすると、スマホの並びに変わる。

<style>
.mock-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(390px, 1fr)); gap: 32px 24px; margin: 16px 0 40px; }
.mock-frame { margin: 0; }
.mock-frame iframe { width: 390px; max-width: 100%; height: 760px; border: 1px solid #8f7f63; border-radius: 14px; background: #f7f1e3; display: block; }
.mock-frame figcaption { margin-top: 8px; font-size: 0.75rem; line-height: 1.7; }
</style>

## ログインしていない人の画面

<div class="mock-grid">
<figure class="mock-frame">
<iframe src="../../mockups/list-anonymous.html" title="レシピ一覧(ログインしていない)" loading="lazy"></iframe>
<figcaption>レシピ一覧。「今日の献立」と「献立に追加」は出すが、押すとログインの画面へ移る</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/detail-anonymous.html" title="レシピ詳細(ログインしていない)" loading="lazy"></iframe>
<figcaption>レシピ詳細。編集内容がないので、元のレシピが出る</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/login.html" title="ログイン" loading="lazy"></iframe>
<figcaption>ログイン。失敗したときのエラーを出した状態</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/register.html" title="登録" loading="lazy"></iframe>
<figcaption>登録。招待から来たときの知らせと、欄のエラーを出した状態</figcaption>
</figure>
</div>

広い幅で開く: [レシピ一覧](../mockups/list-anonymous.html)、[レシピ詳細](../mockups/detail-anonymous.html)、[ログイン](../mockups/login.html)、[登録](../mockups/register.html)

## ログインしている人の画面

持ち主として、自分のレシピ帳を開いているときの表示である。

<div class="mock-grid">
<figure class="mock-frame">
<iframe src="../../mockups/list.html" title="レシピ一覧" loading="lazy"></iframe>
<figcaption>レシピ一覧。「編集済み」の印と、画面の下の献立の枠が出る</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/detail.html" title="レシピ詳細" loading="lazy"></iframe>
<figcaption>レシピ詳細。編集後の内容(鶏もも肉 300g)と、元のレシピとの切り替えが出る</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/edit.html" title="レシピの編集" loading="lazy"></iframe>
<figcaption>レシピの編集。自動保存の状態、欄のエラー、消した材料を出した状態</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/menu.html" title="今日の献立" loading="lazy"></iframe>
<figcaption>今日の献立。食事ごとの小計、1 日の合計、割合、目安との比べ</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/book.html" title="レシピ帳の設定" loading="lazy"></iframe>
<figcaption>レシピ帳の設定。招待のリンクを作った直後の状態</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/account.html" title="アカウント" loading="lazy"></iframe>
<figcaption>アカウント。今のパスワードを間違えた状態</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/invite.html" title="招待の確認" loading="lazy"></iframe>
<figcaption>招待の確認。招待された人(けんた)から見た画面</figcaption>
</figure>
<figure class="mock-frame">
<iframe src="../../mockups/states.html" title="状態と部品" loading="lazy"></iframe>
<figcaption>どの画面にも出る、知らせ、通知、ダイアログ、読み込めないときの表示</figcaption>
</figure>
</div>

広い幅で開く: [レシピ一覧](../mockups/list.html)、[レシピ詳細](../mockups/detail.html)、[レシピの編集](../mockups/edit.html)、[今日の献立](../mockups/menu.html)、[レシピ帳の設定](../mockups/book.html)、[アカウント](../mockups/account.html)、[招待の確認](../mockups/invite.html)、[状態と部品](../mockups/states.html)

## ダイアログを開いた状態

リンクを押すと、ダイアログを開いた状態で、別のタブに出る。

| ダイアログ | 開く |
|---|---|
| ほかの人が先に変更したとき | [開く](../mockups/states.html#dialog=dlg-conflict) |
| ログインが切れたとき | [開く](../mockups/states.html#dialog=dlg-relogin) |
| 取り消せない操作の確認 | [開く](../mockups/states.html#dialog=dlg-confirm) |
| 編集内容を捨てる確認 | [開く](../mockups/edit.html#dialog=dlg-revert) |
| 献立に料理を足す | [開く](../mockups/menu.html#dialog=dlg-add) |

## まだ作っていないもの

- 見るだけの人、編集できる人として開いたときの表示
- 読み込み中の骨組みの表示
- 見つからないページ
- アレンジ版のレシピ
- 献立が空のときの画面の全体

## 試作の置き場

`docs/mockups/` に、画面ごとの HTML と、共通の `mock.css`、`mock.js` を置く。
ヘッダーとフッターは、`mock.js` が差し込む。

---
name: recipe
description: recipes/ 配下に料理レシピの HTML ページを 1 品追加する。料理名と、任意でアレンジの指定を受け取る。アプリ本体(src/)とは無関係の静的ページである。「〜のレシピを追加して」「/recipe 肉じゃが」などのリクエストに対して使用する。
argument-hint: <料理名> [アレンジ内容]
---

# レシピページ追加スキル

`recipes/` に料理 1 品分の HTML ページを追加し、目録 `recipes/assets/catalog.js` に 1 件足して一覧に載せる。
アプリ本体(`src/`、D1、Worker)とは無関係で、Docker もビルドも使わない。
見本は `recipes/oyakodon.html` である。構造、クラス名、文体はこれに揃える。

## 引数の解釈

`$ARGUMENTS` の先頭を料理名、残りをアレンジ内容として読む。

- **アレンジ指定なし**: その料理の一般的な作り方(デフォルト)で書く。アレンジの表示は一切入れない。
- **アレンジ指定あり**: 指定に沿って材料と手順を変え、アレンジ版であることをページ上で明示する(後述)。

料理名が無いときだけ、何を作るか利用者に聞く。
アレンジの解釈が一通りに定まらないとき(例: 「ヘルシーに」)は、自分で方針を決めて変更点の枠にその方針を書く。質問で止めない。

## ファイル名

- ファイル名はローマ字の小文字とハイフンにする。例: `nikujaga.html`、`saba-misoni.html`
- アレンジ版はデフォルト版を上書きしない。変更点を表す接尾辞を付ける。例: `oyakodon-torimune.html`
- 同名のファイルが既にあるときは上書きせず、利用者に確認する。

## 手順

### 1. レシピの中身を決める

- 分量は **2 人分** を基準にする。
- 家庭で手に入る材料と道具で作れるものにする。
- 材料は「具」「合わせ調味料」などのグループに分ける。グループ名は料理に合わせてよい(「割下」「衣」「タレ」等)。
- 手順は 4〜8 個。各手順に短い見出しと本文を付け、時間がかかる工程には目安時間を付ける。
- コツは 3〜5 個。失敗しやすい点と代用の方法を入れる。
- 基本情報の帯は 4 項目(調理時間、難易度 ★☆☆〜★★★、カロリー目安、道具など料理に合う 1 項目)。
- カロリーは材料から概算した 1 人分の目安とし、フッターに「目安」である旨を書く。

### 2. 写真を探して保存する

Wikimedia Commons の API で探す。WebFetch で次を呼ぶ。

```
https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=<英語またはローマ字の料理名>&srnamespace=6&format=json&srlimit=20
```

候補のファイルについて、ライセンスと作者を取得する。

```
https://commons.wikimedia.org/w/api.php?action=query&titles=File:<名前>|File:<名前>&prop=imageinfo&iiprop=url|extmetadata|size&format=json
```

- 使ってよいライセンスは CC0、Public domain、CC BY、CC BY-SA に限る。それ以外(NC、ND、不明)は使わない。
- 横長で幅 1280px 以上のものを優先する。飲食チェーンの商品写真はなるべく避ける。
- 3 枚選ぶ。1 枚をヒーロー、2 枚をギャラリーに使う。見つからないときは 1〜2 枚でよく、ギャラリーの節は枚数に合わせる(0 枚なら節ごと削る)。
- 1280px の縮小版をダウンロードし、`recipes/images/<ファイル名の語幹>/` に保存する。直リンクはしない。

```bash
UA="cooks-recipe/1.0"
curl -sSL -A "$UA" -o hero.jpg "https://upload.wikimedia.org/wikipedia/commons/thumb/<a>/<ab>/<File>.jpg/1280px-<File>.jpg"
```

- 保存名は役割で付ける(`hero.jpg`、`closeup.jpg` 等)。
- 保存後に Read で画像を開いて中身を確かめる。料理が違う、写りが悪い、文字や人物が大きく写っているものは差し替える。
- アレンジ版は、デフォルト版の写真フォルダが既にあればそれを使ってよい。見た目が大きく変わるアレンジなら新たに探す。

### 3. HTML を書く

`recipes/oyakodon.html` を読み、同じ骨組みで書く。変えるのは中身だけで、`assets/recipe.css` と `assets/recipe.js` を読み込む点、Google Fonts の読み込み、節の並び(ヒーロー、基本情報、材料と作り方、ギャラリー、フッター)は変えない。

人数換算は `recipe.js` がマークアップの data 属性を読んで行う。材料カードと分量は次の形で書く。

```html
<aside class="ingredients" data-base-servings="2" data-min-servings="1" data-max-servings="6" aria-labelledby="ing-title">

<!-- 数値の分量。初期表示は 2 人分の値を書いておく -->
<span class="ing__qty" data-amount="250" data-style="round5" data-unit="g">250g</span>
<span class="ing__qty" data-amount="0.5" data-style="frac" data-unit="個">1/2個</span>
<span class="ing__qty" data-amount="2" data-style="frac" data-prefix="大さじ">大さじ2</span>
<!-- 換算しない分量は data-amount を付けない -->
<span class="ing__qty">適量</span>
```

| data-style | 使いどころ |
|---|---|
| `round5` | g、ml など 5 刻みで丸めてよい量 |
| `int` | 卵の個数など整数で数えるもの |
| `frac` | 大さじ、小さじ、1/2 個など分数で表すもの |

`recipe.js` の対応していない表記が要るときは、`QuantityFormatter` に style を足して拡張する。ページ側で JavaScript を書かない。

写真のクレジットはフッターに、ファイルページへのリンク、作者、ライセンスを書く。

### 4. アレンジ版の表示

アレンジ指定があるときだけ、次の 3 箇所を入れる。

ヒーローのバッジを 2 つにする。

```html
<span class="hero__kicker">丼もの</span><span class="hero__kicker hero__kicker--arrange">アレンジ</span>
```

基本情報の帯の直後に、変更点の枠を置く。デフォルトから何をどう変えたかを箇条書きにする。デフォルト版のページがあればリンクする。

```html
<section class="arrange" aria-label="アレンジの内容">
  <div class="arrange__box">
    <h2 class="arrange__title">アレンジ: 鶏むね肉でさっぱり</h2>
    <ul>
      <li>鶏もも肉を鶏むね肉に替え、片栗粉をまぶして固くなるのを防ぐ</li>
    </ul>
    <p>基本の作り方は <a href="oyakodon.html">親子丼</a> を参照。</p>
  </div>
</section>
```

`<title>` と目録にもアレンジだと分かる表記を入れる。目録に `arrange: true` を書くと、一覧のカードにバッジが付く。

```html
<title>親子丼(鶏むね肉でさっぱり) | おうちレシピ帖</title>
```

```js
  { slug: "oyakodon-torimune", name: "親子丼", title: "親子丼(鶏むね肉でさっぱり)", arrange: true, /* 以下略 */ },
```

### 5. 目録に 1 件足す

一覧(`recipes/index.html`)のカードと今日の献立(`recipes/menu.html`)は、`recipes/assets/catalog.js` の目録から `recipe.js` が描く。
HTML にカードを書かず、目録の配列の末尾に 1 件足す。

```js
  {
    slug: "soboro-don",
    name: "そぼろ丼",
    kicker: "丼もの",
    time: 25,
    genres: ["和食", "丼もの"],
    reading: "そぼろどん さんしょくどん",
    ingredients: ["鶏ひき肉", "醤油", "砂糖", "卵", "絹さや", "温かいご飯"],
    nutrition: { kcal: 690, protein: 32, fat: 20, carb: 90, salt: 2.9 },
  },
```

| キー | 書く内容 |
|---|---|
| `slug` | ファイル名の語幹。写真は `images/<slug>/hero.jpg` を使う |
| `name` | 料理名。カードの見出しになる |
| `title` | アレンジ版だけ書く。`<title>` の括弧書きまで含めた名前 |
| `arrange` | アレンジ版だけ `true` を書く。カードにバッジが付く |
| `kicker` | ヒーローのバッジと同じ分類。カードのメタ情報になる |
| `time` | 調理時間(分) |
| `genres` | 料理の系統、料理の種類の順に並べる |
| `reading` | 料理名のひらがな読み。別名があれば空白区切りで足す |
| `ingredients` | 材料名。括弧書きと「水」は除く |
| `nutrition` | 1 人分の目安。`kcal` はページの基本情報の帯と同じ値にする |

栄養の値は、材料の分量から日本食品標準成分表の値で概算し、2 で割った 1 人分にする。
たんぱく質と脂質と炭水化物は整数、塩分は小数 1 桁で書く。
たんぱく質 ×4、脂質 ×9、炭水化物 ×4 の合計が `kcal` から 1 割以上ずれないことを確かめる。
ページのカロリーがご飯を含まないときも、目録の値はページと同じにする。献立ではご飯を別の品として足す。

ジャンルの絞り込みボタンは `recipe.js` の `GenreFilter` が `genres` から作るので、ボタン側を書き足す必要はない。

| 軸 | 使っている値 |
|---|---|
| 系統 | 和食、韓国風、中華、洋食 |
| 種類 | 丼もの、ご飯もの、麺、煮物、汁物、炒め物、炒め煮、焼き物、揚げ物、卵料理、副菜、おやつ |

表記ゆれでボタンが分かれるのを防ぐため、既存の値に当てはまるときはそれを使う。
当てはまらないときだけ新しい値を足し、この表に追記する。

### 6. 確認

- `recipes/` 配下で、HTML から参照している画像とアセットのパスがすべて存在することを確かめる。
- data 属性の初期表示が 2 人分の値と一致していることを見直す。
- 目録の `nutrition.kcal` がページのカロリー目安と一致していることを見直す。
- 利用者には、作ったファイル、写真の出典とライセンス、アレンジの有無と内容を報告する。ブラウザで表示を確認していなければ、そう書く。

## 文章

- 文章は `.claude/rules/writing-style.md` に従う。中黒で並列しない。
- ページの文体は見本に揃え、手順は「〜する」、コツとリードは「〜です、〜ます」で書く。
- 共通の見た目を変えたいときは `assets/recipe.css` を直し、ページごとに `style` 属性を書かない。

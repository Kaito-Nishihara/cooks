# Postman で API を試す

サービスの API を、Postman から手で試すためのファイルを置く。
今は `auth-service` の入口だけを持つ。

| ファイル | 中身 |
|---|---|
| `recipe-book.postman_collection.json` | 要求の一覧と、応答を確かめる短いテスト |
| `local.postman_environment.json` | 手元で試すときの、住所などの値 |
| `start-auth-service.sh` | 試すための `auth-service` を起動する。`services/compose.yaml` が動かす |

## 使い方

1. リポジトリの直下で、試すための `auth-service` を起動する。初回は組み立てに数分かかる。

    ```
    docker compose -f services/compose.yaml --profile try up auth-service
    ```

    `started in` の行が出たら、使える。

2. Postman で、2 つの JSON のファイルを読み込む(Import)。
3. 環境の「おうちレシピ帖: 手元」を選ぶ。
4. コレクションを上から順に送るか、Runner でまとめて流す。
5. 終わったら止める。

    ```
    docker compose -f services/compose.yaml --profile try down
    ```

## 決まり

- 登録のたびに、新しいメールアドレス(`misaki+<時刻>@example.com`)を作る。何度流しても、登録が重ならない。
- 登録とログインの応答の `Set-Cookie` から `rb_session` の値を取り、変数 `sessionToken` に入れる。`Secure` のクッキーは `http` で保存されないことがあるので、Postman のクッキーの保管に頼らない。
- `verify` の応答のトークンを、変数 `accessToken` に入れる。ログインが要る API ができたら、`Authorization: Bearer {{accessToken}}` で使う。
- `verify` は、本来は入口(Envoy Gateway)だけが呼ぶ。入口ができるまでは、入口の代わりに直接呼ぶ。
- 試すための `auth-service` は、テスト用の PostgreSQL(`auth_test`)を使う。サービスのテストを走らせると、中身は消える。
- 署名の鍵は、起動のたびにコンテナの中で作る。リポジトリにもホスト側にも置かない。

## 住所

| 変数 | 値 | 中身 |
|---|---|---|
| `authUrl` | `http://localhost:8081` | `auth-service` の、利用者の要求を受けるポート |
| `managementUrl` | `http://localhost:9001` | `auth-service` の、稼働の確認のポート |
| `webOrigin` | `http://localhost:8080` | 画面のオリジン。`Origin` ヘッダーに入れる |

ポートは、kind の入口(`8080`)とぶつからない番号にした。
どちらも `127.0.0.1` だけで待ち受けるので、ほかの機械からは届かない。

## コマンドで流す

Postman の代わりに、Newman をコンテナで動かして流せる。
`auth-service` を起動した後に、リポジトリの直下で動かす。

```
docker run --rm --add-host host.docker.internal:host-gateway \
  -v "$(pwd)/services/postman:/etc/newman" postman/newman:6.1.3-alpine \
  run recipe-book.postman_collection.json -e local.postman_environment.json \
  --env-var authUrl=http://host.docker.internal:8081 \
  --env-var managementUrl=http://host.docker.internal:9001
```

テストの書き間違いで処理が止まると、集計の `failed` が 0 のままでも、その要求の行に `SyntaxError` などが出る。
集計だけで判断しない。

## 入口を足したとき

API を足したら、このコレクションにも要求を足す。
本格的な確かめは、サービスの API のテスト(JUnit)で行う。
ここに置くのは、手で試すための代表的な要求だけである。

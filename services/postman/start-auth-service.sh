#!/usr/bin/env bash
# Postman で試すために、auth-service を 1 つ起動する。services/compose.yaml の auth-service が動かす。
# 書いている間の本格的な起動(Compose で一式を動かすこと)は、段階 0-9 で作る。
set -euo pipefail

cd /work/services

# 署名の鍵は、コンテナの中で作る。リポジトリにも、ホスト側にも置かない
KEY_FILE=/tmp/signing-key.pem
if [ ! -f "${KEY_FILE}" ]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "${KEY_FILE}" 2>/dev/null
fi

# 今のコードで組み立てる。テストは走らせない
mvn --batch-mode --no-transfer-progress -q -pl auth-service -am package -DskipTests

# テスト用の PostgreSQL の auth_test を使う。テストを走らせると、中身は消える
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres-test:5432/auth_test
export QUARKUS_DATASOURCE_USERNAME=auth_app
# テスト用の PostgreSQL は、パスワードを確かめない。設定が欠けていないことを示すだけの値である
export QUARKUS_DATASOURCE_PASSWORD=not-checked
export APP_AUTH_WEB_ORIGIN=http://localhost:8080
export APP_AUTH_SIGNING_KEY_FILE="${KEY_FILE}"

exec java -jar auth-service/target/quarkus-app/quarkus-run.jar

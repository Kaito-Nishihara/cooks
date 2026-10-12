#!/bin/sh
# テスト用の PostgreSQL の、最初の起動のときに動く。
# サービスごとに、接続する利用者とデータベースを作る(「共通設計: データベースの共通仕様」の 5.1)。
# 利用者は、自分のデータベースにだけ接続できる。
set -eu

for name in auth book kitchen; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE ROLE ${name}_app LOGIN;
CREATE DATABASE ${name}_test OWNER ${name}_app;
REVOKE CONNECT ON DATABASE ${name}_test FROM PUBLIC;
SQL
done

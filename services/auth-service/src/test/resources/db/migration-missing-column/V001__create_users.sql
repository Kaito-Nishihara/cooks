-- テストだけで使う。display_name の列がない users を作り、
-- Entity とテーブルが合わないときに起動が失敗することを確かめる(AC-DB-03)。
CREATE TABLE users (
    id            uuid         NOT NULL,
    email         varchar(254) NOT NULL,
    password_hash char(60)     NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

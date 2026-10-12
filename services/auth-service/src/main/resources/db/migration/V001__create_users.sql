-- 利用者(「機能設計: 登録とログイン」の 5.1)。
-- 日時は、データベースの初期値ではなく、サービスが入れる。
CREATE TABLE users (
    id            uuid         NOT NULL,
    email         varchar(254) NOT NULL,
    display_name  varchar(50)  NOT NULL,
    password_hash char(60)     NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    -- サービスが小文字にそろえ忘れたときに、重複が紛れ込むのを止める
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email)),
    CONSTRAINT ck_users_display_name_length CHECK (char_length(display_name) >= 1)
);

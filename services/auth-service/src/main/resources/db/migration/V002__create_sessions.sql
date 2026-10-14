-- セッション(「機能設計: 登録とログイン」の 5.2)。
-- クッキーに入れた値そのものは持たず、SHA-256 のハッシュだけを持つ。
CREATE TABLE sessions (
    id           uuid        NOT NULL,
    user_id      uuid        NOT NULL,
    token_hash   char(64)    NOT NULL,
    last_used_at timestamptz NOT NULL,
    expires_at   timestamptz NOT NULL,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL,
    CONSTRAINT pk_sessions PRIMARY KEY (id),
    CONSTRAINT uq_sessions_token_hash UNIQUE (token_hash),
    -- 利用者を消すと、セッションも消える
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 利用者のセッションを数えるとき、まとめて消すときに使う
CREATE INDEX idx_sessions_user_id ON sessions (user_id);
-- 期限が切れたセッションを、定期の処理で消すときに使う
CREATE INDEX idx_sessions_expires_at ON sessions (expires_at);

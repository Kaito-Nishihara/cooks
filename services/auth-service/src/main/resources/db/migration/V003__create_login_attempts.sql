-- ログインの失敗の記録(「機能設計: 登録とログイン」の 5.3)。
-- 主キーは、小文字にそろえたメールアドレスの SHA-256 である。
-- users を指す列を持たない。登録のないメールアドレスも、同じように記録するためである。
CREATE TABLE login_attempts (
    email_hash        char(64)    NOT NULL,
    failed_count      integer     NOT NULL,
    window_started_at timestamptz NOT NULL,
    locked_until      timestamptz,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL,
    CONSTRAINT pk_login_attempts PRIMARY KEY (email_hash),
    CONSTRAINT ck_login_attempts_failed_count CHECK (failed_count >= 1)
);

-- 古い行を、定期の処理で消すときに使う
CREATE INDEX idx_login_attempts_updated_at ON login_attempts (updated_at);
